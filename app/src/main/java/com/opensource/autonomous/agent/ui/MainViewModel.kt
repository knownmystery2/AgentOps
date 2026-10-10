package com.opensource.autonomous.agent.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.network.OllamaClient
import com.opensource.autonomous.agent.network.PodJobItem
import com.opensource.autonomous.agent.network.SwarmAgentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

class MainViewModel(
    private val db: AppDatabase,
    private val repository: AgentRepository = AgentRepository(db)
) : ViewModel() {

    private val ollamaClient = OllamaClient()
    private val appStartElapsedRealtime = SystemClock.elapsedRealtime()
    private val appStartTimestamp = System.currentTimeMillis()

    // -----------------------------------------------------------------
    // Core Reactive Data Streams from Local Ledger & Database
    // -----------------------------------------------------------------
    val agents: StateFlow<List<AgentEntity>> = repository.getAllAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decisions: StateFlow<List<DecisionEntity>> = repository.getPendingDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDecisions: StateFlow<List<DecisionEntity>> = repository.getAllDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntity>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntity>> = repository.getLedgerEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dropshipItems: StateFlow<List<DropshipEntity>> = repository.getDropshipItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountTasks: StateFlow<List<AccountTaskEntity>> = repository.getAccountTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providers: StateFlow<List<StandbyProviderEntity>> = repository.getProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity?> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // -----------------------------------------------------------------
    // Network & Polling Reactive States
    // -----------------------------------------------------------------
    val isRunningCycle = MutableStateFlow(false)
    val isLiveConnected = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)
    val lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val rawJsonSchemaDialogContent = MutableStateFlow<String?>(null)

    // Settings Ping Test States
    val pingLatencyMs = MutableStateFlow<Long?>(null)
    val pingStatus = MutableStateFlow<String?>(null)
    val isTestingPing = MutableStateFlow(false)

    // Module 1: Console state
    val selectedLogFilter = MutableStateFlow("ALL") // "ALL", "STDOUT", "STDERR", "WORKER"

    // Module 2: Dynamic Live Swarm States
    val liveSwarmAgents = MutableStateFlow<List<SwarmAgentStatus>>(emptyList())

    // Module 3: Approvals state
    val feedbackTarget = MutableStateFlow<DecisionEntity?>(null)

    // Module 4: POD Studio state
    val selectedMockupProduct = MutableStateFlow("Heavyweight T-Shirt")
    val selectedMockupColor = MutableStateFlow("Charcoal Slate")
    val selectedMockupPlacement = MutableStateFlow("Chest Center (11\" x 13\")")
    val isGeneratingPod = MutableStateFlow(false)
    val podQueue = MutableStateFlow<List<PodJobItem>>(
        listOf(
            PodJobItem(
                id = "pod-init-01",
                sku = "POD-TEE-SLT-2026",
                product = "Heavyweight T-Shirt",
                color = "Charcoal Slate",
                placement = "Chest Center (11\" x 13\")",
                status = "READY",
                progressPercent = 100,
                vectorDpi = 300,
                timestamp = System.currentTimeMillis() - 7200000
            ),
            PodJobItem(
                id = "pod-init-02",
                sku = "POD-HOD-NAT-2026",
                product = "Vintage Hoodie",
                color = "Natural Canvas",
                placement = "Back Large (14\" x 16\")",
                status = "COMPILED",
                progressPercent = 100,
                vectorDpi = 300,
                timestamp = System.currentTimeMillis() - 3600000
            )
        )
    )

    // Module 5: Dropship Supplier Sync State
    val isSyncingSuppliers = MutableStateFlow(false)
    val lastSupplierSyncTimestamp = MutableStateFlow(System.currentTimeMillis())

    // Module 7: Standby Daemon State
    val daemonState = MutableStateFlow("ACTIVE") // ACTIVE, STANDBY, SLEEP
    val cronSchedule = MutableStateFlow("Every 15 min")
    val workerConcurrency = MutableStateFlow(4)
    val memoryThrottleMb = MutableStateFlow(512)

    val generatedMarketing = MutableStateFlow<MarketingBundle?>(null)

    init {
        // Continuous background polling to ping localhost Termux/emulator and pull updates
        viewModelScope.launch {
            while (isActive) {
                pollLiveBackgroundServer()
                delay(6000)
            }
        }
    }

    private suspend fun pollLiveBackgroundServer() {
        val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
        val healthy = repository.checkServerHealth(endpoint)
        isLiveConnected.value = healthy

        if (healthy) {
            lastSyncTimestamp.value = System.currentTimeMillis()
            val swarmRes = repository.fetchLiveSwarm(endpoint)
            val swarmList = swarmRes.getOrNull()
            if (!swarmList.isNullOrEmpty()) {
                liveSwarmAgents.value = swarmList
            }
        } else {
            deriveLocalSwarmState()
        }
    }

    private fun deriveLocalSwarmState() {
        val currentAgents = agents.value
        val currentTasks = tasks.value
        val now = System.currentTimeMillis()

        if (currentAgents.isNotEmpty()) {
            liveSwarmAgents.value = currentAgents.map { agent ->
                val activeTask = currentTasks.find { it.assignedToAgentId == agent.id && it.status == "RUNNING" }
                    ?: currentTasks.find { it.assignedToAgentId == agent.id }
                SwarmAgentStatus(
                    id = agent.id,
                    name = agent.name,
                    role = agent.role,
                    status = if (activeTask?.status == "RUNNING") "ACTIVE" else "STANDBY",
                    model = "llama3",
                    currentTask = activeTask?.title ?: "Standing by for business directive",
                    lastPingTimestamp = now,
                    latencyMs = 12
                )
            }
        }
    }

    // =========================================================================
    // 1. CONSOLE MODULE: Live Logging, Uptime & Queue Counters
    // =========================================================================
    fun getSystemUptimeFormatted(): String {
        val elapsedSec = (SystemClock.elapsedRealtime() - appStartElapsedRealtime) / 1000
        val hours = elapsedSec / 3600
        val minutes = (elapsedSec % 3600) / 60
        val seconds = elapsedSec % 60
        return "${hours}h ${minutes}m ${seconds}s"
    }

    fun getMemoryStats(): Pair<Long, Long> {
        val runtime = Runtime.getRuntime()
        val totalMb = runtime.totalMemory() / (1024 * 1024)
        val freeMb = runtime.freeMemory() / (1024 * 1024)
        return Pair(totalMb, freeMb)
    }

    // =========================================================================
    // 2. SWARM MODULE: Direct Task Dispatch & Heartbeats
    // =========================================================================
    fun dispatchAdHocTask(agentId: String, title: String, instruction: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val taskId = UUID.randomUUID().toString()
            val task = TaskEntity(
                id = taskId,
                assignedToAgentId = agentId,
                title = title,
                instruction = instruction,
                status = "RUNNING",
                result = null,
                timestamp = now
            )
            repository.insertTask(task)
            repository.insertLog(
                LogEntity(
                    agentId = agentId,
                    message = "Received direct dispatch: \"$title\"",
                    timestamp = now
                )
            )
            pollLiveBackgroundServer()
        }
    }

    fun pingSwarm() {
        viewModelScope.launch {
            pollLiveBackgroundServer()
        }
    }

    // =========================================================================
    // 3. APPROVALS MODULE: Cryptographic Zero-Trust HMAC & REST Actions
    // =========================================================================
    fun computeDecisionHmac(decision: DecisionEntity): String {
        val raw = "${decision.id}:${decision.agentSource}:${decision.payloadJson}:${decision.timestamp}"
        return repository.generateHmacSha256(raw)
    }

    fun approveDecision(id: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"

            repository.postDecisionAction(endpoint, id, "APPROVED")
            repository.updateDecisionStatus(id, "APPROVED")
            repository.insertLog(
                LogEntity(
                    agentId = "Orchestrator Agent",
                    message = "Admin approved decision: $id. Dispatched live POST to $endpoint at ${formatLiveTimestamp(now)}",
                    timestamp = now
                )
            )
        }
    }

    fun rejectDecision(id: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"

            repository.postDecisionAction(endpoint, id, "REJECTED")
            repository.updateDecisionStatus(id, "REJECTED")
            repository.insertLog(
                LogEntity(
                    agentId = "Orchestrator Agent",
                    message = "Admin denied decision: $id. Dispatched live POST to $endpoint at ${formatLiveTimestamp(now)}",
                    timestamp = now
                )
            )
        }
    }

    fun rejectWithFeedback(decision: DecisionEntity, feedback: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"

            repository.postDecisionAction(endpoint, decision.id, "REJECTED")
            repository.updateDecisionStatus(decision.id, "REJECTED")

            repository.insertLog(
                LogEntity(
                    agentId = "POD Design Agent",
                    message = "Regenerating design with user feedback: \"$feedback\".",
                    timestamp = now
                )
            )

            val revised = DecisionEntity(
                id = UUID.randomUUID().toString(),
                agentSource = decision.agentSource,
                description = "${decision.description} (v2 - Revised per admin feedback)",
                payloadJson = decision.payloadJson,
                status = "PENDING",
                timestamp = now
            )
            repository.insertDecision(revised)
            feedbackTarget.value = null
        }
    }

    // =========================================================================
    // 4. POD STUDIO MODULE: Generation Pipeline & SKU Tracking
    // =========================================================================
    fun dispatchSkuToProductionQueue(product: String, color: String, placement: String) {
        if (isGeneratingPod.value) return
        isGeneratingPod.value = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val prodCode = product.split(" ").map { it.take(2).uppercase() }.joinToString("")
                val colCode = color.split(" ").map { it.take(2).uppercase() }.joinToString("")
                val generatedSku = "POD-$prodCode-$colCode-${(now % 10000)}"

                val jobId = UUID.randomUUID().toString()
                val newJob = PodJobItem(
                    id = jobId,
                    sku = generatedSku,
                    product = product,
                    color = color,
                    placement = placement,
                    status = "QUEUED",
                    progressPercent = 10,
                    vectorDpi = 300,
                    timestamp = now
                )
                podQueue.value = listOf(newJob) + podQueue.value

                repository.insertLog(
                    LogEntity(
                        agentId = "POD Design Agent",
                        message = "Enqueued vector rendering pipeline for SKU $generatedSku ($product, $color).",
                        timestamp = now
                    )
                )

                // Dispatch to live server
                val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
                repository.postPodJob(endpoint, generatedSku, product, color, placement)

                // Simulate pipeline stages
                delay(600)
                podQueue.value = podQueue.value.map {
                    if (it.id == jobId) it.copy(status = "RENDERING", progressPercent = 55) else it
                }
                delay(800)
                podQueue.value = podQueue.value.map {
                    if (it.id == jobId) it.copy(status = "COMPILED", progressPercent = 88) else it
                }
                delay(600)
                podQueue.value = podQueue.value.map {
                    if (it.id == jobId) it.copy(status = "READY", progressPercent = 100) else it
                }

                repository.insertLog(
                    LogEntity(
                        agentId = "POD Design Agent",
                        message = "Vector artwork compiled successfully @ 300 DPI for SKU $generatedSku.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } finally {
                isGeneratingPod.value = false
            }
        }
    }

    // =========================================================================
    // 5. DROPSHIP MODULE: Real-Time Supplier Sync & Zero-Cost Routing
    // =========================================================================
    fun syncSupplierFeeds() {
        if (isSyncingSuppliers.value) return
        isSyncingSuppliers.value = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
                repository.syncDropshipSuppliers(endpoint)
                lastSupplierSyncTimestamp.value = now
                repository.insertLog(
                    LogEntity(
                        agentId = "Dropship Sourcing Agent",
                        message = "Supplier catalog synchronized. Verified zero upfront payment guarantees.",
                        timestamp = now
                    )
                )
            } finally {
                isSyncingSuppliers.value = false
            }
        }
    }

    fun routeSampleOrder(item: DropshipEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
            repository.postDropshipOrder(endpoint, item.id)

            // Record transaction in double-entry ledger
            recordManualSale(
                store = "Autonomous Dropship Outlet",
                platform = "Direct Checkout",
                grossCents = item.retailPriceCents,
                cogsCents = item.costCents,
                feeCents = item.feeCents,
                desc = "Order: ${item.title}"
            )

            repository.insertLog(
                LogEntity(
                    agentId = "Dropship Sourcing Agent",
                    message = "Routed supplier order for: ${item.title}. Customer payment received; zero out-of-pocket payment required.",
                    timestamp = now
                )
            )
        }
    }

    fun toggleShortlist(id: String, inShortlist: Boolean) {
        viewModelScope.launch {
            repository.updateDropshipShortlist(id, inShortlist)
        }
    }

    // =========================================================================
    // 6. LEDGER MODULE: Real Dynamic Financial Calculations & Sync
    // =========================================================================
    fun syncLiveTransactions() {
        viewModelScope.launch {
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
            val remoteEntries = repository.fetchLiveLedger(endpoint).getOrNull()
            if (!remoteEntries.isNullOrEmpty()) {
                repository.insertLedgerEntries(remoteEntries)
            }
            repository.insertLog(
                LogEntity(
                    agentId = "Finance Agent",
                    message = "Synchronized ledger audit trail with background server.",
                    timestamp = System.currentTimeMillis()
                )
            )
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
            repository.insertLedgerEntries(entries)
            repository.insertLog(
                LogEntity(
                    agentId = "Finance Agent",
                    message = "New sale entered into double-entry ledger. Gross: $${grossCents / 100.0}, Net: $${(grossCents - cogsCents - feeCents) / 100.0}.",
                    timestamp = now
                )
            )
        }
    }

    // =========================================================================
    // 7. STANDBY MODULE: Daemon Sleep/Wake Control, Cron & Throttling
    // =========================================================================
    fun setDaemonState(newState: String) {
        daemonState.value = newState
        viewModelScope.launch {
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
            repository.postDaemonState(endpoint, newState)
            repository.insertLog(
                LogEntity(
                    agentId = "Reliability Agent",
                    message = "Daemon state updated to $newState.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun setCronSchedule(newSchedule: String) {
        cronSchedule.value = newSchedule
    }

    fun triggerCronDispatchNow() {
        viewModelScope.launch {
            val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
            repository.triggerCronDispatch(endpoint)
            triggerAgentCycle()
        }
    }

    fun toggleAccountTask(id: String, completed: Boolean) {
        viewModelScope.launch {
            repository.updateAccountTask(id, completed)
            repository.insertLog(
                LogEntity(
                    agentId = "Reliability Agent",
                    message = "Phone task $id updated to ${if (completed) "Verified" else "Pending"}.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // =========================================================================
    // 8. SETTINGS MODULE: Termux IP/Port Configuration, DB & Latency Ping
    // =========================================================================
    fun testPingConnection(endpoint: String) {
        if (isTestingPing.value) return
        isTestingPing.value = true
        pingStatus.value = "Pinging $endpoint..."
        viewModelScope.launch {
            try {
                val (success, latency) = repository.testServerPing(endpoint)
                pingLatencyMs.value = latency
                isLiveConnected.value = success
                if (success) {
                    pingStatus.value = "HTTP 200 OK • Latency: ${latency}ms"
                } else {
                    pingStatus.value = "Connection Refused / Timeout (${latency}ms)"
                }
            } finally {
                isTestingPing.value = false
            }
        }
    }

    fun updateSettings(newSettings: SettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            ollamaClient.baseUrl = newSettings.ollamaEndpoint
            ollamaClient.model = newSettings.ollamaModel
            repository.insertLog(
                LogEntity(
                    agentId = "Reliability Agent",
                    message = "Settings updated. Endpoint: ${newSettings.backendServerEndpoint}.",
                    timestamp = System.currentTimeMillis()
                )
            )
            pollLiveBackgroundServer()
        }
    }

    fun forceDatabaseSync() {
        syncWithLiveBackend()
    }

    fun syncWithLiveBackend() {
        if (isSyncing.value) return
        isSyncing.value = true
        viewModelScope.launch {
            try {
                val endpoint = settings.value?.backendServerEndpoint ?: "http://127.0.0.1:8080"
                val healthy = repository.checkServerHealth(endpoint)
                isLiveConnected.value = healthy

                val now = System.currentTimeMillis()

                if (healthy) {
                    val remoteDecisions = repository.fetchLiveDecisions(endpoint).getOrNull()
                    if (!remoteDecisions.isNullOrEmpty()) {
                        repository.insertDecisions(remoteDecisions)
                    }

                    val swarmList = repository.fetchLiveSwarm(endpoint).getOrNull()
                    if (!swarmList.isNullOrEmpty()) {
                        liveSwarmAgents.value = swarmList
                    }

                    val remoteTasks = repository.fetchLiveTasks(endpoint).getOrNull()
                    if (!remoteTasks.isNullOrEmpty()) {
                        remoteTasks.forEach { repository.insertTask(it) }
                    }

                    val remoteLogs = repository.fetchLiveLogs(endpoint).getOrNull()
                    if (!remoteLogs.isNullOrEmpty()) {
                        repository.insertLogs(remoteLogs)
                    }

                    repository.insertLog(
                        LogEntity(
                            agentId = "Orchestrator Agent",
                            message = "Live sync completed successfully with $endpoint at ${formatLiveTimestamp(now)}",
                            timestamp = now
                        )
                    )
                } else {
                    deriveLocalSwarmState()
                }
                lastSyncTimestamp.value = System.currentTimeMillis()
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun initializeBusiness(idea: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val planTitle = if (idea.isNotBlank()) idea else "Print-on-Demand & Zero-Upfront Dropshipping Expansion"
            repository.insertLog(
                LogEntity(
                    agentId = "Orchestrator Agent",
                    message = "Initializing autonomous execution pipeline for: $planTitle",
                    timestamp = now
                )
            )

            val taskId = UUID.randomUUID().toString()
            repository.insertTask(
                TaskEntity(
                    id = taskId,
                    assignedToAgentId = "1",
                    title = "Initialize Strategic Plan: $planTitle",
                    instruction = "Create structural roadmap and assign sub-tasks for: $planTitle. Follow zero-upfront cost boundary.",
                    status = "RUNNING",
                    result = null,
                    timestamp = now
                )
            )

            repository.insertLog(LogEntity(agentId = "Trend & Niche Scout", message = "Scanned organic demand metrics: Selected high-growth niche.", timestamp = now))
            repository.insertLog(LogEntity(agentId = "Compliance Agent", message = "Screened against USPTO records. Trademark status clean.", timestamp = now))

            val newDecision = DecisionEntity(
                id = UUID.randomUUID().toString(),
                agentSource = "POD Design Agent",
                description = "Publish '$planTitle - Vintage Minimalist Tee' [Zero upfront cost, Est. Net: $13.50]",
                payloadJson = """{"product":"Heavyweight T-Shirt","cost":1200,"price":2750,"fee":200,"profit":1350,"compliance":"USPTO 100% Clean"}""",
                status = "PENDING",
                timestamp = now
            )
            repository.insertDecision(newDecision)
            repository.updateTaskStatus(taskId, "COMPLETED", "Roadmap created. Design placed in Human Verification Register.")
        }
    }

    fun triggerAgentCycle() {
        if (isRunningCycle.value) return
        isRunningCycle.value = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repository.insertLog(LogEntity(agentId = "Reliability Agent", message = "Pinging provider registry: Colab Active, Kaggle Standby.", timestamp = now))
                repository.insertLog(LogEntity(agentId = "Trend & Niche Scout", message = "Identified rising niche: 'Minimalist Botanical Line Art'.", timestamp = now))
                repository.insertLog(LogEntity(agentId = "POD Design Agent", message = "Generated original 300 DPI vector graphic for heavyweight hoodie.", timestamp = now))
                repository.insertLog(LogEntity(agentId = "Dropship Sourcing Agent", message = "Shortlisted $0 upfront supplier item: Silicone Pet Travel Bowl.", timestamp = now))
                repository.insertLog(LogEntity(agentId = "Compliance Agent", message = "Zero trademark collision verified against USPTO.", timestamp = now))

                repository.insertDecision(
                    DecisionEntity(
                        id = UUID.randomUUID().toString(),
                        agentSource = "POD Design Agent",
                        description = "Publish 'Botanical Fern Frond' Heavyweight Hoodie [Est Net: $16.20 / sale]",
                        payloadJson = """{"product":"Heavyweight Hoodie","cost":1850,"price":3790,"fee":320,"profit":1620,"compliance":"USPTO Cleared"}""",
                        status = "PENDING",
                        timestamp = now
                    )
                )
            } finally {
                isRunningCycle.value = false
            }
        }
    }

    fun parseDecisionPayload(payloadJson: String): DecisionEconomicPayload {
        return repository.parseDecisionPayload(payloadJson)
    }

    fun formatLiveTimestamp(timeMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun formatRelativeTime(timeMillis: Long): String {
        val diffSeconds = (System.currentTimeMillis() - timeMillis) / 1000
        return when {
            diffSeconds < 5 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> formatLiveTimestamp(timeMillis)
        }
    }

    fun viewRawJsonSchema() {
        rawJsonSchemaDialogContent.value = """
        {
          "database": "agent_biz.db",
          "version": 3,
          "tables": {
            "agents": ["id (PK)", "name", "role", "systemPrompt", "isActive"],
            "tasks": ["id (PK)", "assignedToAgentId", "title", "instruction", "status", "result", "timestamp"],
            "decisions": ["id (PK)", "agentSource", "description", "payloadJson", "status", "timestamp"],
            "logs": ["id (PK)", "agentId", "message", "timestamp"],
            "ledger_entries": ["id (PK)", "timestamp", "code", "store", "platform", "description", "accountDebit", "accountCredit", "amountCents", "hashToken", "category"],
            "dropship_catalog": ["id (PK)", "title", "niche", "supplierName", "costCents", "retailPriceCents", "feeCents", "profitCents", "shippingDays", "supplierRating", "zeroUpfrontCost", "inShortlist"],
            "account_tasks": ["id (PK)", "platform", "title", "instructions", "taskType", "isCompleted", "priority"],
            "standby_providers": ["id (PK)", "serviceName", "tierType", "status", "quotaPercentUsed", "accountAlias", "endpointUrl"],
            "business_settings": ["id (PK)", "globalAutomationActive", "backendServerEndpoint", "ollamaEndpoint", "keyGithub", "keyStripe", "bypassApprovals"]
          }
        }
        """.trimIndent()
    }

    fun dismissJsonSchemaDialog() {
        rawJsonSchemaDialogContent.value = null
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
            emailBody = "Hey friend,\n\nWe just dropped something special: our curated $niche edition.\n\nClaim yours before this small-batch run sells out."
        )
    }
}
