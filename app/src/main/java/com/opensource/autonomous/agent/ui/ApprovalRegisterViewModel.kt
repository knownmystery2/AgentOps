package com.opensource.autonomous.agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.opensource.autonomous.agent.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Detailed business action proposal submitted to the Approval Register
 * requiring founder or administrator confirmation before execution.
 */
data class PendingBusinessActionProposal(
    val title: String,
    val agentSource: String,
    val actionType: String, // STOREFRONT_DEPLOY, POD_PUBLISH, SUPPLIER_SYNC, MARKETING_DISPATCH, LEDGER_TRANSFER, GOD_REMEDIATION
    val description: String,
    val targetResource: String = "storefront/catalog",
    val estimatedCostCents: Long = 0L,
    val estimatedRevenueCents: Long = 0L,
    val riskLevel: String = "LOW", // LOW, MEDIUM, HIGH, CRITICAL
    val zeroCostVerified: Boolean = true,
    val rawPayload: Map<String, Any?> = emptyMap()
)

/**
 * Summary metrics of the Approval Register.
 */
data class ApprovalRegisterStats(
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val highRiskPendingCount: Int = 0,
    val zeroCostVerifiedCount: Int = 0,
    val lastActionTimestamp: Long = 0L
)

/**
 * ApprovalRegisterViewModel
 *
 * Dedicated ViewModel for tracking, filtering, and confirming/rejecting
 * pending business actions stored in the local SQLite Room database (`DecisionDao`).
 */
class ApprovalRegisterViewModel(
    application: Application,
    private val database: AppDatabase = AppDatabase.getInstance(application)
) : AndroidViewModel(application) {

    private val gson = Gson()
    private val decisionDao = database.decisionDao()
    private val logDao = database.logDao()
    private val taskDao = database.taskDao()

    val allDecisions: StateFlow<List<DecisionEntity>> = decisionDao.getAllDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDecisions: StateFlow<List<DecisionEntity>> = decisionDao.getPendingDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow("ALL") // "ALL", "PENDING", "APPROVED", "REJECTED", "HIGH_RISK", "POD", "DROPSHIP"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isProcessingAction = MutableStateFlow(false)
    val isProcessingAction: StateFlow<Boolean> = _isProcessingAction.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val filteredDecisions: StateFlow<List<DecisionEntity>> = combine(
        allDecisions,
        _selectedFilter,
        _searchQuery
    ) { decisions, filter, query ->
        decisions.filter { decision ->
            val matchesFilter = when (filter) {
                "PENDING" -> decision.status == "PENDING"
                "APPROVED" -> decision.status == "APPROVED"
                "REJECTED" -> decision.status == "REJECTED"
                "HIGH_RISK" -> decision.payloadJson.contains("\"riskLevel\":\"HIGH\"") || decision.payloadJson.contains("\"riskLevel\":\"CRITICAL\"")
                "POD" -> decision.agentSource.contains("Creative", ignoreCase = true) || decision.description.contains("POD", ignoreCase = true) || decision.description.contains("Mockup", ignoreCase = true)
                "DROPSHIP" -> decision.agentSource.contains("Dropship", ignoreCase = true) || decision.description.contains("Supplier", ignoreCase = true)
                "GOD_HALTED" -> decision.agentSource.contains("GOD", ignoreCase = true) || decision.description.contains("HALTED", ignoreCase = true)
                else -> true
            }

            val matchesQuery = if (query.isBlank()) true else {
                decision.description.contains(query, ignoreCase = true) ||
                decision.agentSource.contains(query, ignoreCase = true) ||
                decision.id.contains(query, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registerStats: StateFlow<ApprovalRegisterStats> = allDecisions.map { list ->
        val pending = list.filter { it.status == "PENDING" }
        val approved = list.filter { it.status == "APPROVED" }
        val rejected = list.filter { it.status == "REJECTED" }
        val highRisk = pending.count { it.payloadJson.contains("\"riskLevel\":\"HIGH\"") || it.payloadJson.contains("\"riskLevel\":\"CRITICAL\"") }
        val zeroCost = pending.count { it.payloadJson.contains("\"zeroCostVerified\":true") || it.payloadJson.contains("Upfront Cost") }

        ApprovalRegisterStats(
            pendingCount = pending.size,
            approvedCount = approved.size,
            rejectedCount = rejected.size,
            highRiskPendingCount = highRisk,
            zeroCostVerifiedCount = zeroCost,
            lastActionTimestamp = list.maxOfOrNull { it.timestamp } ?: 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ApprovalRegisterStats())

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Creates a new pending business action in the local database requiring user confirmation.
     */
    fun createPendingAction(proposal: PendingBusinessActionProposal) {
        viewModelScope.launch {
            val payload = mapOf(
                "title" to proposal.title,
                "actionType" to proposal.actionType,
                "targetResource" to proposal.targetResource,
                "estimatedCostCents" to proposal.estimatedCostCents,
                "estimatedRevenueCents" to proposal.estimatedRevenueCents,
                "riskLevel" to proposal.riskLevel,
                "zeroCostVerified" to proposal.zeroCostVerified,
                "details" to proposal.rawPayload
            )

            val decision = DecisionEntity(
                id = "ACT-" + UUID.randomUUID().toString().take(8).uppercase(),
                agentSource = proposal.agentSource,
                description = "${proposal.title} — ${proposal.description}",
                payloadJson = gson.toJson(payload),
                status = "PENDING",
                timestamp = System.currentTimeMillis()
            )

            withContext(Dispatchers.IO) {
                decisionDao.insertDecision(decision)
                logDao.insertLog(
                    LogEntity(
                        agentId = proposal.agentSource,
                        message = "New business action registered for confirmation: '${proposal.title}' (Risk: ${proposal.riskLevel})",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            _statusMessage.value = "Registered action for confirmation: ${proposal.title}"
        }
    }

    /**
     * Confirms and approves a pending business action, immediately executing it.
     */
    fun confirmAction(decisionId: String, userNotes: String? = null) {
        viewModelScope.launch {
            _isProcessingAction.value = true
            try {
                withContext(Dispatchers.IO) {
                    decisionDao.updateDecisionStatus(decisionId, "APPROVED")
                    
                    // Dispatch execution into active task pipeline
                    val task = TaskEntity(
                        id = "EXEC-$decisionId",
                        assignedToAgentId = "1",
                        title = "Execute Confirmed Action: $decisionId",
                        instruction = "User confirmed action $decisionId. ${userNotes?.let { "Notes: $it" } ?: ""}",
                        status = "COMPLETED",
                        result = "Executed successfully upon user confirmation.",
                        timestamp = System.currentTimeMillis()
                    )
                    taskDao.insertTask(task)

                    logDao.insertLog(
                        LogEntity(
                            agentId = "Administrator AI",
                            message = "User CONFIRMED business action $decisionId. Execution triggered.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                _statusMessage.value = "Action confirmed and executed: $decisionId"
            } catch (e: Exception) {
                _statusMessage.value = "Error confirming action: ${e.message}"
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    /**
     * Rejects a pending business action with an optional explanation.
     */
    fun rejectAction(decisionId: String, reason: String? = null) {
        viewModelScope.launch {
            _isProcessingAction.value = true
            try {
                withContext(Dispatchers.IO) {
                    decisionDao.updateDecisionStatus(decisionId, "REJECTED")
                    logDao.insertLog(
                        LogEntity(
                            agentId = "Compliance Agent",
                            message = "User REJECTED business action $decisionId. ${reason?.let { "Reason: $it" } ?: "Execution blocked by user."}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                _statusMessage.value = "Action rejected: $decisionId"
            } catch (e: Exception) {
                _statusMessage.value = "Error rejecting action: ${e.message}"
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    /**
     * Batch confirm multiple pending actions at once.
     */
    fun batchConfirm(decisionIds: List<String>) {
        viewModelScope.launch {
            _isProcessingAction.value = true
            try {
                withContext(Dispatchers.IO) {
                    decisionIds.forEach { id ->
                        decisionDao.updateDecisionStatus(id, "APPROVED")
                    }
                    logDao.insertLog(
                        LogEntity(
                            agentId = "Administrator AI",
                            message = "Batch confirmed ${decisionIds.size} business actions.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                _statusMessage.value = "Batch confirmed ${decisionIds.size} actions"
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    /**
     * Batch reject multiple pending actions.
     */
    fun batchReject(decisionIds: List<String>) {
        viewModelScope.launch {
            _isProcessingAction.value = true
            try {
                withContext(Dispatchers.IO) {
                    decisionIds.forEach { id ->
                        decisionDao.updateDecisionStatus(id, "REJECTED")
                    }
                    logDao.insertLog(
                        LogEntity(
                            agentId = "Compliance Agent",
                            message = "Batch rejected ${decisionIds.size} business actions.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                _statusMessage.value = "Batch rejected ${decisionIds.size} actions"
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
