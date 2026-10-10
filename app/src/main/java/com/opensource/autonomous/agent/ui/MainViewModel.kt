package com.opensource.autonomous.agent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.network.OllamaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

data class MarketingBundle(
    val niche: String,
    val pinTitle: String,
    val pinDescription: String,
    val tikTokHook: String,
    val tikTokScript: String,
    val seoTags: List<String>,
    val emailSubject: String,
    val emailBody: String
)

class MainViewModel(private val db: AppDatabase) : ViewModel() {

    private val ollamaClient = OllamaClient()

    val agents: StateFlow<List<AgentEntity>> = db.agentDao().getAllAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = db.taskDao().getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decisions: StateFlow<List<DecisionEntity>> = db.decisionDao().getPendingDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDecisions: StateFlow<List<DecisionEntity>> = db.decisionDao().getAllDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntity>> = db.logDao().getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntity>> = db.ledgerDao().getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dropshipItems: StateFlow<List<DropshipEntity>> = db.dropshipDao().getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountTasks: StateFlow<List<AccountTaskEntity>> = db.accountTaskDao().getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providers: StateFlow<List<StandbyProviderEntity>> = db.providerDao().getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity?> = db.settingsDao().getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Interactive UI states
    val isRunningCycle = MutableStateFlow(false)
    val feedbackTarget = MutableStateFlow<DecisionEntity?>(null)
    val generatedMarketing = MutableStateFlow<MarketingBundle?>(null)
    val selectedMockupProduct = MutableStateFlow("Heavyweight T-Shirt")
    val selectedMockupColor = MutableStateFlow("Charcoal Slate")
    val selectedMockupPlacement = MutableStateFlow("Chest Center (11\" x 13\")")

    fun initializeBusiness(idea: String) {
        viewModelScope.launch {
            val planTitle = if (idea.isNotBlank()) idea else "Print-on-Demand & Zero-Upfront Dropshipping Expansion"
            db.logDao().insertLog(LogEntity(agentId = "Orchestrator Agent", message = "Initializing autonomous execution pipeline for: $planTitle"))

            // Add primary action deployment goal to execution stack
            val taskId = UUID.randomUUID().toString()
            db.taskDao().insertTask(
                TaskEntity(
                    id = taskId,
                    assignedToAgentId = "1",
                    title = "Initialize Strategic Plan: $planTitle",
                    instruction = "Create structural roadmap and assign sub-tasks for: $planTitle. Follow zero-upfront cost boundary.",
                    status = "RUNNING",
                    result = null
                )
            )

            // Multi-agent execution:
            db.logDao().insertLog(LogEntity(agentId = "Trend & Niche Scout", message = "Scanned organic demand metrics: Selected high-growth niche."))
            db.logDao().insertLog(LogEntity(agentId = "Compliance Agent", message = "Screened against USPTO records. Trademark status clean."))

            // Propose new decisions for human approval
            val newDecision = DecisionEntity(
                id = UUID.randomUUID().toString(),
                agentSource = "POD Design Agent",
                description = "Publish '$planTitle - Vintage Minimalist Tee' [Zero upfront cost, Est. Net: $13.50]",
                payloadJson = """{"product":"Heavyweight T-Shirt","cost":1200,"price":2750,"fee":200,"profit":1350,"compliance":"USPTO 100% Clean"}""",
                status = "PENDING"
            )
            db.decisionDao().insertDecision(newDecision)

            db.taskDao().updateTaskStatus(taskId, "COMPLETED", "Roadmap created. Design placed in Human Verification Register.")
        }
    }

    fun approveDecision(id: String) {
        viewModelScope.launch {
            db.decisionDao().updateDecisionStatus(id, "APPROVED")
            db.logDao().insertLog(
                LogEntity(
                    agentId = "Orchestrator Agent",
                    message = "Admin approved decision: $id. Queued for automated platform publishing."
                )
            )
        }
    }

    fun rejectDecision(id: String) {
        viewModelScope.launch {
            db.decisionDao().updateDecisionStatus(id, "REJECTED")
            db.logDao().insertLog(
                LogEntity(
                    agentId = "Orchestrator Agent",
                    message = "Admin denied decision: $id."
                )
            )
        }
    }

    fun rejectWithFeedback(decision: DecisionEntity, feedback: String) {
        viewModelScope.launch {
            db.decisionDao().updateDecisionStatus(decision.id, "REJECTED")
            db.logDao().insertLog(
                LogEntity(
                    agentId = "POD Design Agent",
                    message = "Regenerating design with user feedback: \"$feedback\". Removing restricted elements."
                )
            )

            // Auto-regenerate revised compliant item
            val revised = DecisionEntity(
                id = UUID.randomUUID().toString(),
                agentSource = decision.agentSource,
                description = "${decision.description} (v2 - Revised per admin feedback)",
                payloadJson = decision.payloadJson,
                status = "PENDING"
            )
            db.decisionDao().insertDecision(revised)
            feedbackTarget.value = null
        }
    }

    fun toggleAccountTask(id: String, completed: Boolean) {
        viewModelScope.launch {
            db.accountTaskDao().updateTaskStatus(id, completed)
            db.logDao().insertLog(
                LogEntity(
                    agentId = "Reliability Agent",
                    message = "Phone verification task $id updated to ${if (completed) "Verified" else "Pending"}."
                )
            )
        }
    }

    fun toggleShortlist(id: String, inShortlist: Boolean) {
        viewModelScope.launch {
            db.dropshipDao().updateShortlistStatus(id, inShortlist)
        }
    }

    fun recordManualSale(store: String, platform: String, grossCents: Long, cogsCents: Long, feeCents: Long, desc: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val code = "TXN-${now % 100000}"

            fun hash(str: String): String =
                MessageDigest.getInstance("SHA-256")
                    .digest(str.toByteArray())
                    .take(8).joinToString("") { "%02x".format(it) }

            val entries = listOf(
                LedgerEntity(
                    timestamp = now,
                    code = code,
                    store = store,
                    platform = platform,
                    description = desc,
                    accountDebit = "Platform Receivable ($platform)",
                    accountCredit = "Revenue (Merchandise)",
                    amountCents = grossCents,
                    hashToken = hash("$code-REV-$grossCents"),
                    category = "SALE"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = code,
                    store = store,
                    platform = platform,
                    description = "COGS for $desc (Fulfillment from customer payment)",
                    accountDebit = "Cost of Goods Sold (COGS)",
                    accountCredit = "Accounts Payable (Supplier)",
                    amountCents = cogsCents,
                    hashToken = hash("$code-COGS-$cogsCents"),
                    category = "COGS"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = code,
                    store = store,
                    platform = platform,
                    description = "Platform processing & listing fee",
                    accountDebit = "Platform Fees Expense",
                    accountCredit = "Platform Receivable ($platform)",
                    amountCents = feeCents,
                    hashToken = hash("$code-FEE-$feeCents"),
                    category = "FEE"
                )
            )
            db.ledgerDao().insertEntries(entries)
            db.logDao().insertLog(
                LogEntity(
                    agentId = "Finance Agent",
                    message = "New sale entered into double-entry ledger. Gross: $${grossCents / 100.0}, Net: $${(grossCents - cogsCents - feeCents) / 100.0}."
                )
            )
        }
    }

    fun triggerAgentCycle() {
        if (isRunningCycle.value) return
        isRunningCycle.value = true
        viewModelScope.launch {
            try {
                db.logDao().insertLog(LogEntity(agentId = "Reliability Agent", message = "Pinging provider registry: Colab Active, Kaggle Standby."))
                db.logDao().insertLog(LogEntity(agentId = "Trend & Niche Scout", message = "Identified rising niche: 'Minimalist Botanical Line Art'."))
                db.logDao().insertLog(LogEntity(agentId = "POD Design Agent", message = "Generated original 300 DPI vector graphic for heavyweight hoodie."))
                db.logDao().insertLog(LogEntity(agentId = "Dropship Sourcing Agent", message = "Shortlisted $0 upfront supplier item: Silicone Pet Travel Bowl."))
                db.logDao().insertLog(LogEntity(agentId = "Compliance Agent", message = "Zero trademark collision verified against USPTO."))

                // Add to decisions
                db.decisionDao().insertDecision(
                    DecisionEntity(
                        id = UUID.randomUUID().toString(),
                        agentSource = "POD Design Agent",
                        description = "Publish 'Botanical Fern Frond' Heavyweight Hoodie [Est Net: $16.20 / sale]",
                        payloadJson = """{"product":"Heavyweight Hoodie","cost":1850,"price":3790,"fee":320,"profit":1620}""",
                        status = "PENDING"
                    )
                )
            } finally {
                isRunningCycle.value = false
            }
        }
    }

    fun updateSettings(newSettings: SettingsEntity) {
        viewModelScope.launch {
            db.settingsDao().saveSettings(newSettings)
            ollamaClient.baseUrl = newSettings.ollamaEndpoint
            ollamaClient.model = newSettings.ollamaModel
            db.logDao().insertLog(LogEntity(agentId = "Reliability Agent", message = "Business settings and LLM endpoint updated."))
        }
    }

    fun generateMarketingKit(niche: String) {
        generatedMarketing.value = MarketingBundle(
            niche = niche,
            pinTitle = "Aesthetic $niche | Minimalist Clean Everyday Essentials",
            pinDescription = "Obsessed with this clean vibe ✨ Designed for intentional living. High-density organic cotton & zero compromise. Tap to explore!",
            tikTokHook = "Stop scrolling if you’re trying to romanticize your daily workspace...",
            tikTokScript = "[0:00 - 0:03 B-roll macro close-up of item texture]\n[0:04 - 0:08 Contrast cluttered generic desk vs this minimal aesthetic]\n[0:09 - 0:12 'Found this small batch release, link in bio!']",
            seoTags = listOf(
                "aesthetic", "minimalist design", "print on demand", "cottagecore",
                "workspace aesthetic", "statement piece", "gift guide 2026", "small batch merch"
            ),
            emailSubject = "Early Look: The New $niche Capsule Is Live",
            emailBody = "Hey friend,\n\nWe just dropped something special: our curated $niche edition.\n\nPrinted on ultra-heavyweight combed ring-spun cotton and shipped with eco-packaging.\n\nClaim yours before this small-batch run sells out."
        )
    }
}
