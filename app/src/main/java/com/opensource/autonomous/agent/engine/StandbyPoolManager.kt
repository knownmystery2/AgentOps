package com.opensource.autonomous.agent.engine

import android.content.Context
import androidx.work.*
import com.opensource.autonomous.agent.data.AppDatabase
import com.opensource.autonomous.agent.data.TaskEntity
import com.opensource.autonomous.agent.worker.AgentOrchestrationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Condition flags required for optimal standby task execution
 */
data class ExecutionCondition(
    val requiresNetwork: Boolean = true,
    val requiresBatteryNotLow: Boolean = true,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val minMemoryThresholdMb: Int = 128
)

data class StandbyTaskItem(
    val taskId: String,
    val agentId: String,
    val title: String,
    val instruction: String,
    val enqueuedTimestamp: Long = System.currentTimeMillis(),
    val condition: ExecutionCondition = ExecutionCondition(),
    val workRequestId: UUID? = null,
    val status: String = "QUEUED_STANDBY" // QUEUED_STANDBY, DISPATCHED, COMPLETED, CANCELLED
)

data class StandbyPoolState(
    val queuedTasksCount: Int = 0,
    val activeWorkersCount: Int = 0,
    val isPoolActive: Boolean = true,
    val concurrencyLimit: Int = 2,
    val memoryThrottleMb: Int = 256,
    val standbyTasks: List<StandbyTaskItem> = emptyList(),
    val optimalExecutionReady: Boolean = true,
    val lastDispatchTime: Long = 0L
)

/**
 * StandbyPoolManager
 *
 * Utilizes Jetpack WorkManager to queue and manage idle agent tasks that are waiting
 * for optimal execution conditions or resource availability (Network, Battery, Memory).
 */
class StandbyPoolManager(
    private val context: Context? = null,
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    private val workManager: WorkManager? = context?.let {
        try {
            WorkManager.getInstance(it)
        } catch (_: Exception) {
            null
        }
    }

    private val _poolState = MutableStateFlow(StandbyPoolState())
    val poolState: StateFlow<StandbyPoolState> = _poolState.asStateFlow()

    private val standbyQueue = mutableListOf<StandbyTaskItem>()

    init {
        // Initialize pool state
        updateState()
    }

    /**
     * Enqueues an idle agent task to the standby pool with specific WorkManager constraints
     */
    fun queueStandbyTask(
        taskId: String = UUID.randomUUID().toString(),
        agentId: String,
        title: String,
        instruction: String,
        condition: ExecutionCondition = ExecutionCondition()
    ): StandbyTaskItem {
        val constraintsBuilder = Constraints.Builder()
        if (condition.requiresNetwork) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        if (condition.requiresBatteryNotLow) {
            constraintsBuilder.setRequiresBatteryNotLow(true)
        }
        if (condition.requiresCharging) {
            constraintsBuilder.setRequiresCharging(true)
        }
        if (condition.requiresDeviceIdle && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            constraintsBuilder.setRequiresDeviceIdle(true)
        }

        val constraints = constraintsBuilder.build()

        val workInput = workDataOf(
            "task_id" to taskId,
            "agent_id" to agentId,
            "title" to title,
            "instruction" to instruction
        )

        val workRequest = OneTimeWorkRequestBuilder<AgentOrchestrationWorker>()
            .setConstraints(constraints)
            .setInputData(workInput)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("STANDBY_POOL_AGENT_TASK")
            .addTag(agentId)
            .build()

        val item = StandbyTaskItem(
            taskId = taskId,
            agentId = agentId,
            title = title,
            instruction = instruction,
            condition = condition,
            workRequestId = workRequest.id,
            status = "QUEUED_STANDBY"
        )

        synchronized(standbyQueue) {
            standbyQueue.add(item)
        }

        // Persist task to Room database
        scope.launch(Dispatchers.IO) {
            db.taskDao().insertTask(
                TaskEntity(
                    id = taskId,
                    assignedToAgentId = agentId,
                    title = title,
                    instruction = instruction,
                    status = "PENDING"
                )
            )
        }

        // Enqueue to WorkManager under unique work policy
        workManager?.enqueueUniqueWork(
            "standby_task_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        updateState()
        return item
    }

    /**
     * Manually triggers immediate dispatch of all ready standby tasks
     */
    fun dispatchOptimalTasks() {
        scope.launch(Dispatchers.IO) {
            synchronized(standbyQueue) {
                standbyQueue.forEachIndexed { index, task ->
                    if (task.status == "QUEUED_STANDBY") {
                        standbyQueue[index] = task.copy(status = "DISPATCHED")
                    }
                }
            }
            updateState(lastDispatch = System.currentTimeMillis())
        }
    }

    /**
     * Cancels a queued standby task in WorkManager and database
     */
    fun cancelStandbyTask(taskId: String) {
        val item = synchronized(standbyQueue) {
            standbyQueue.find { it.taskId == taskId }
        }
        if (item?.workRequestId != null) {
            workManager?.cancelWorkById(item.workRequestId)
        }
        synchronized(standbyQueue) {
            standbyQueue.removeAll { it.taskId == taskId }
        }
        scope.launch(Dispatchers.IO) {
            db.taskDao().updateTaskStatus(taskId, "CANCELLED", "Cancelled from StandbyPoolManager")
        }
        updateState()
    }

    /**
     * Clears all queued standby tasks
     */
    fun clearPool() {
        workManager?.cancelAllWorkByTag("STANDBY_POOL_AGENT_TASK")
        synchronized(standbyQueue) {
            standbyQueue.clear()
        }
        updateState()
    }

    /**
     * Updates reactive pool state
     */
    private fun updateState(lastDispatch: Long = _poolState.value.lastDispatchTime) {
        val items = synchronized(standbyQueue) { standbyQueue.toList() }
        val queued = items.count { it.status == "QUEUED_STANDBY" }
        val active = items.count { it.status == "DISPATCHED" }

        val runtime = Runtime.getRuntime()
        val freeMb = (runtime.freeMemory() / (1024 * 1024)).toInt()
        val memoryReady = freeMb >= _poolState.value.memoryThrottleMb

        _poolState.value = _poolState.value.copy(
            queuedTasksCount = queued,
            activeWorkersCount = active,
            standbyTasks = items,
            optimalExecutionReady = memoryReady,
            lastDispatchTime = lastDispatch
        )
    }

    fun updateConcurrency(limit: Int) {
        _poolState.value = _poolState.value.copy(concurrencyLimit = limit.coerceIn(1, 8))
    }

    fun updateMemoryThrottle(mb: Int) {
        _poolState.value = _poolState.value.copy(memoryThrottleMb = mb.coerceIn(64, 1024))
        updateState()
    }
}
