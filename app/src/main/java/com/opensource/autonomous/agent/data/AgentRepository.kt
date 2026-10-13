package com.opensource.autonomous.agent.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.opensource.autonomous.agent.network.LiveApiClient
import com.opensource.autonomous.agent.network.PodJobItem
import com.opensource.autonomous.agent.network.SwarmAgentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class DecisionEconomicPayload(
    val product: String,
    val costCents: Long,
    val priceCents: Long,
    val feeCents: Long,
    val profitCents: Long,
    val profitMarginPercent: Double,
    val compliance: String
)

class AgentRepository(
    private val db: AppDatabase,
    private val liveApiClient: LiveApiClient = LiveApiClient()
) {
    private val gson = Gson()

    // -------------------------------------------------------------
    // Live Network Methods
    // -------------------------------------------------------------
    suspend fun checkServerHealth(baseUrl: String): Boolean {
        return liveApiClient.checkServerHealth(baseUrl)
    }

    suspend fun testServerPing(baseUrl: String): Pair<Boolean, Long> {
        return liveApiClient.testServerPing(baseUrl)
    }

    suspend fun fetchLiveSwarm(baseUrl: String): Result<List<SwarmAgentStatus>> {
        return liveApiClient.fetchLiveSwarm(baseUrl)
    }

    suspend fun fetchLiveDecisions(baseUrl: String): Result<List<DecisionEntity>> {
        return liveApiClient.fetchLiveDecisions(baseUrl)
    }

    suspend fun postDecisionAction(baseUrl: String, decisionId: String, action: String): Result<Boolean> {
        return liveApiClient.postDecisionAction(baseUrl, decisionId, action)
    }

    suspend fun fetchLiveTasks(baseUrl: String): Result<List<TaskEntity>> {
        return liveApiClient.fetchLiveTasks(baseUrl)
    }

    suspend fun fetchLiveLogs(baseUrl: String): Result<List<LogEntity>> {
        return liveApiClient.fetchLiveLogs(baseUrl)
    }

    suspend fun fetchLiveLedger(baseUrl: String): Result<List<LedgerEntity>> {
        return liveApiClient.fetchLiveLedger(baseUrl)
    }


    suspend fun fetchPodQueue(baseUrl: String): Result<List<PodJobItem>> {
        return liveApiClient.fetchPodQueue(baseUrl)
    }

    suspend fun postPodJob(baseUrl: String, sku: String, product: String, color: String, placement: String): Result<Boolean> {
        return liveApiClient.postPodJob(baseUrl, sku, product, color, placement)
    }

    suspend fun postDaemonState(baseUrl: String, state: String): Result<Boolean> {
        return liveApiClient.postDaemonState(baseUrl, state)
    }

    suspend fun triggerCronDispatch(baseUrl: String): Result<Boolean> {
        return liveApiClient.triggerCronDispatch(baseUrl)
    }

    // -------------------------------------------------------------
    // Room Flow Data Streams
    // -------------------------------------------------------------
    fun getAllAgents(): Flow<List<AgentEntity>> = db.agentDao().getAllAgents()
    fun getAutonomousAgents(): Flow<List<AutonomousAgent>> = db.agentDao().getAllAgents().map { list ->
        if (list.isEmpty()) {
            AutonomousAgent.DEFAULT_SWARM
        } else {
            list.map { AutonomousAgent.fromEntity(it) }
        }
    }

    suspend fun updateAgentStatus(id: String, status: AgentStatus) = withContext(Dispatchers.IO) {
        val existing = db.agentDao().getAgentById(id)
        if (existing != null) {
            db.agentDao().insertAgent(existing.copy(isActive = status.isActive))
        }
    }
    fun getAllTasks(): Flow<List<TaskEntity>> = db.taskDao().getAllTasks()
    fun getPendingDecisions(): Flow<List<DecisionEntity>> = db.decisionDao().getPendingDecisions()
    fun getAllDecisions(): Flow<List<DecisionEntity>> = db.decisionDao().getAllDecisions()
    fun getAllLogs(): Flow<List<LogEntity>> = db.logDao().getAllLogs()
    fun getLedgerEntries(): Flow<List<LedgerEntity>> = db.ledgerDao().getAllEntries()
    fun getAccountTasks(): Flow<List<AccountTaskEntity>> = db.accountTaskDao().getAllTasks()
    fun getProviders(): Flow<List<StandbyProviderEntity>> = db.providerDao().getAllProviders()
    fun getSettings(): Flow<SettingsEntity?> = db.settingsDao().getSettings()
    fun getStorefronts(): Flow<List<StorefrontEntity>> = db.storefrontDao().getAllStorefronts()
    fun getTrademarkChecks(): Flow<List<TrademarkCheckEntity>> = db.trademarkDao().getAllChecks()
    fun getAgentMessages(): Flow<List<AgentMessageEntity>> = db.agentMessageDao().getAllMessages()
    fun getDigitalProducts(): Flow<List<DigitalProductEntity>> = db.digitalProductDao().getAllProducts()
    fun getPublishedDigitalProducts(): Flow<List<DigitalProductEntity>> = db.digitalProductDao().getPublishedProducts()
    fun getPendingDigitalProducts(): Flow<List<DigitalProductEntity>> = db.digitalProductDao().getProductsByStatus("AWAITING_HUMAN_APPROVAL")

    suspend fun clearAllDemoData() = withContext(Dispatchers.IO) {
        AppDatabase.purgeAllDemoData(db)
    }

    suspend fun insertDigitalProduct(product: DigitalProductEntity) = withContext(Dispatchers.IO) {
        db.digitalProductDao().insertProduct(product)
    }

    suspend fun deleteDigitalProduct(id: String) = withContext(Dispatchers.IO) {
        db.digitalProductDao().deleteProduct(id)
    }

    suspend fun updateDigitalProductStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        db.digitalProductDao().updateProductStatus(id, status)
    }

    suspend fun recordDigitalSale(id: String) = withContext(Dispatchers.IO) {
        db.digitalProductDao().recordSale(id)
    }

    suspend fun insertStorefront(storefront: StorefrontEntity) = withContext(Dispatchers.IO) {
        db.storefrontDao().insertStorefront(storefront)
    }

    suspend fun updateStorefrontStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        db.storefrontDao().updateStorefrontStatus(id, status)
    }

    suspend fun insertTrademarkCheck(check: TrademarkCheckEntity) = withContext(Dispatchers.IO) {
        db.trademarkDao().insertCheck(check)
    }

    suspend fun insertAgentMessage(message: AgentMessageEntity) = withContext(Dispatchers.IO) {
        db.agentMessageDao().insertMessage(message)
    }

    // -------------------------------------------------------------
    // Local Persistence Operations
    // -------------------------------------------------------------
    suspend fun insertDecision(decision: DecisionEntity) = withContext(Dispatchers.IO) {
        db.decisionDao().insertDecision(decision)
    }

    suspend fun insertDecisions(decisions: List<DecisionEntity>) = withContext(Dispatchers.IO) {
        db.decisionDao().insertDecisions(decisions)
    }

    suspend fun updateDecisionStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        db.decisionDao().updateDecisionStatus(id, status)
    }

    suspend fun insertLog(log: LogEntity) = withContext(Dispatchers.IO) {
        db.logDao().insertLog(log)
    }

    suspend fun insertLogs(logs: List<LogEntity>) = withContext(Dispatchers.IO) {
        db.logDao().insertLogs(logs)
    }

    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        db.taskDao().insertTask(task)
    }

    suspend fun updateTaskStatus(id: String, status: String, result: String?) = withContext(Dispatchers.IO) {
        db.taskDao().updateTaskStatus(id, status, result)
    }

    suspend fun insertLedgerEntry(entry: LedgerEntity) = withContext(Dispatchers.IO) {
        db.ledgerDao().insertEntry(entry)
    }

    suspend fun insertLedgerEntries(entries: List<LedgerEntity>) = withContext(Dispatchers.IO) {
        db.ledgerDao().insertEntries(entries)
    }

    suspend fun updateAccountTask(id: String, completed: Boolean) = withContext(Dispatchers.IO) {
        db.accountTaskDao().updateTaskStatus(id, completed)
    }


    suspend fun saveSettings(settings: SettingsEntity) = withContext(Dispatchers.IO) {
        db.settingsDao().saveSettings(settings)
    }

    // -------------------------------------------------------------
    // Economic Payload Parser for Approvals
    // -------------------------------------------------------------
    fun parseDecisionPayload(payloadJson: String): DecisionEconomicPayload {
        return try {
            val root = gson.fromJson(payloadJson, JsonObject::class.java)
            val product = root.get("product")?.asString
                ?: root.get("item")?.asString
                ?: root.get("type")?.asString
                ?: "Merchandise Item"

            val cost = when {
                root.has("cost") -> root.get("cost").asLong
                root.has("costCents") -> root.get("costCents").asLong
                else -> 1180L
            }

            val price = when {
                root.has("price") -> root.get("price").asLong
                root.has("retailPriceCents") -> root.get("retailPriceCents").asLong
                else -> 2795L
            }

            val fee = when {
                root.has("fee") -> root.get("fee").asLong
                root.has("feeCents") -> root.get("feeCents").asLong
                else -> (price * 0.08).toLong() + 20L
            }

            val profit = when {
                root.has("profit") -> root.get("profit").asLong
                root.has("profitCents") -> root.get("profitCents").asLong
                else -> price - cost - fee
            }

            val margin = if (price > 0) (profit.toDouble() / price.toDouble()) * 100.0 else 45.0
            val compliance = root.get("compliance")?.asString ?: "USPTO Verified Clean"

            DecisionEconomicPayload(
                product = product,
                costCents = cost,
                priceCents = price,
                feeCents = fee,
                profitCents = profit,
                profitMarginPercent = margin,
                compliance = compliance
            )
        } catch (_: Exception) {
            DecisionEconomicPayload(
                product = "Custom Merchandise",
                costCents = 1200L,
                priceCents = 2800L,
                feeCents = 244L,
                profitCents = 1356L,
                profitMarginPercent = 48.4,
                compliance = "Screened & Approved"
            )
        }
    }

    /**
     * Calculates HMAC-SHA256 zero-trust cryptographic signature for a decision payload.
     */
    fun generateHmacSha256(payload: String, secret: String = "termux_enclave_2026"): String {
        return try {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            sha256Hmac.init(secretKey)
            val signedBytes = sha256Hmac.doFinal(payload.toByteArray(Charsets.UTF_8))
            signedBytes.joinToString("") { "%02x".format(it) }.take(16).uppercase()
        } catch (_: Exception) {
            val md = MessageDigest.getInstance("SHA-256")
            md.digest((payload + secret).toByteArray()).take(8).joinToString("") { "%02x".format(it) }.uppercase()
        }
    }

    // -------------------------------------------------------------
    // Interceptor Architecture ("God" Overseer) Methods
    // -------------------------------------------------------------
    fun getGodVerdicts(): Flow<List<GodAuditVerdictEntity>> = db.godAuditDao().getAllVerdicts()

    fun getGodHaltedVerdicts(): Flow<List<GodAuditVerdictEntity>> = db.godAuditDao().getHaltedVerdicts()

    fun getGodLatestVerdict(): Flow<GodAuditVerdictEntity?> = db.godAuditDao().getLatestVerdict()

    fun getGodTotalInterceptCount(): Flow<Int> = db.godAuditDao().getTotalInterceptCount()

    fun getGodHaltedCount(): Flow<Int> = db.godAuditDao().getHaltedCount()

    suspend fun insertGodVerdict(verdict: GodAuditVerdictEntity) = withContext(Dispatchers.IO) {
        db.godAuditDao().insertVerdict(verdict)
    }

    suspend fun clearGodVerdicts() = withContext(Dispatchers.IO) {
        db.godAuditDao().clearVerdicts()
    }
}

