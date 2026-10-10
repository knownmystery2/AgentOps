package com.opensource.autonomous.agent.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.network.OllamaClient
import java.util.UUID

class AgentOrchestrationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "agent_biz.db")
        .fallbackToDestructiveMigration()
        .build()
    private val ollamaClient = OllamaClient()

    override suspend fun doWork(): Result {
        val taskDao = db.taskDao()
        val agentDao = db.agentDao()
        val logDao = db.logDao()
        val decisionDao = db.decisionDao()

        val task = taskDao.getNextPendingTask() ?: return Result.success()
        val agent = agentDao.getAgentById(task.assignedToAgentId) ?: return Result.failure()

        val systemPromptMatrix = when(agent.id) {
            "1" -> AgentPrompts.CEO_PROMPT
            "2" -> AgentPrompts.RESEARCH_PROMPT
            "3" -> AgentPrompts.PRODUCT_PROMPT
            "4" -> AgentPrompts.MARKETING_PROMPT
            "5" -> AgentPrompts.CONTENT_PROMPT
            "6" -> AgentPrompts.SUPPORT_PROMPT
            "7" -> AgentPrompts.FINANCE_PROMPT
            else -> agent.systemPrompt
        }

        try {
            logDao.insertLog(LogEntity(agentId = agent.id, message = "Executing Task: ${task.title}"))
            taskDao.insertTask(task.copy(status = "RUNNING"))

            val resultText = ollamaClient.generateCompletionSafe(systemPromptMatrix, task.instruction)

            // Handle CEO Matrix Router Outputs Specially
            if (agent.id == "1") {
                val parser = MatrixParser(db)
                val parsingResult = parser.processCeoMatrix(resultText)

                if (parsingResult.isSuccess) {
                    taskDao.insertTask(task.copy(status = "COMPLETED", result = "Successfully spawned ${parsingResult.getOrNull()} actions."))
                } else {
                    taskDao.insertTask(task.copy(status = "FAILED", result = "Matrix Extraction Failed: ${parsingResult.exceptionOrNull()?.message}"))
                }
            } else {
                // Evaluate safety checks loop guards for agents 2-7
                if (resultText.contains("EXECUTE_PAYOUT") || resultText.contains("PUBLISH_PRODUCTION_CODE")) {
                    decisionDao.insertDecision(
                        DecisionEntity(
                            id = UUID.randomUUID().toString(),
                            agentSource = agent.name,
                            description = "Security clearance check: ${task.title}",
                            payloadJson = resultText,
                            status = "PENDING"
                        )
                    )
                    logDao.insertLog(LogEntity(agentId = agent.id, message = "Execution paused. Pending security confirmation."))
                    taskDao.insertTask(task.copy(status = "PENDING", result = "Awaiting verification clearance"))
                } else {
                    taskDao.insertTask(task.copy(status = "COMPLETED", result = resultText))
                }
            }
        } catch (e: Exception) {
            logDao.insertLog(LogEntity(agentId = agent.id, message = "Runtime Error: ${e.localizedMessage}"))
            taskDao.insertTask(task.copy(status = "FAILED", result = e.localizedMessage))
            return Result.retry()
        }

        return Result.success()
    }
}
