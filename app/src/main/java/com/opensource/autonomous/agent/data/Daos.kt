package com.opensource.autonomous.agent.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents")
    fun getAllAgents(): Flow<List<AgentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgents(agents: List<AgentEntity>)

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): AgentEntity?

    @Query("UPDATE agents SET isActive = :isActive WHERE id = :id")
    suspend fun updateAgentStatus(id: String, isActive: Boolean)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' LIMIT 1")
    suspend fun getNextPendingTask(): TaskEntity?

    @Query("UPDATE tasks SET status = :status, result = :result WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, result: String?)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()
}

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingDecisions(): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions ORDER BY timestamp DESC")
    fun getAllDecisions(): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecisions(decisions: List<DecisionEntity>)

    @Query("UPDATE decisions SET status = :status WHERE id = :id")
    suspend fun updateDecisionStatus(id: String, status: String)

    @Query("DELETE FROM decisions")
    suspend fun clearDecisions()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE agentId = :agentId ORDER BY timestamp DESC")
    fun getLogsByAgent(agentId: String): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntity>)

    @Query("DELETE FROM logs")
    suspend fun clearLogs()
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<LedgerEntity>>

    @Query("SELECT COALESCE(SUM(CASE WHEN category = 'SALE' THEN amountCents WHEN category = 'COGS' THEN -amountCents WHEN category = 'FEE' THEN -amountCents WHEN category = 'REFUND' THEN -amountCents ELSE 0 END), 0) FROM ledger_entries")
    fun getLedgerBalance(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntity>)

    @Query("DELETE FROM ledger_entries")
    suspend fun clearLedger()
}

@Dao
interface AccountTaskDao {
    @Query("SELECT * FROM account_tasks ORDER BY isCompleted ASC, priority DESC")
    fun getAllTasks(): Flow<List<AccountTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<AccountTaskEntity>)

    @Query("UPDATE account_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: String, isCompleted: Boolean)

    @Query("DELETE FROM account_tasks")
    suspend fun clearTasks()
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM standby_providers ORDER BY id ASC")
    fun getAllProviders(): Flow<List<StandbyProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<StandbyProviderEntity>)

    @Query("UPDATE standby_providers SET status = :status WHERE id = :id")
    suspend fun updateProviderStatus(id: String, status: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_base")
    fun getAllKnowledge(): Flow<List<KnowledgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: KnowledgeEntity)
}

@Dao
interface StorefrontDao {
    @Query("SELECT * FROM storefronts ORDER BY createdAt DESC")
    fun getAllStorefronts(): Flow<List<StorefrontEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorefront(storefront: StorefrontEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorefronts(storefronts: List<StorefrontEntity>)

    @Query("UPDATE storefronts SET status = :status WHERE id = :id")
    suspend fun updateStorefrontStatus(id: String, status: String)

    @Query("SELECT * FROM storefronts WHERE id = :id")
    suspend fun getStorefrontById(id: String): StorefrontEntity?

    @Query("DELETE FROM storefronts")
    suspend fun clearStorefronts()
}

@Dao
interface TrademarkDao {
    @Query("SELECT * FROM trademark_checks ORDER BY checkedTimestamp DESC")
    fun getAllChecks(): Flow<List<TrademarkCheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: TrademarkCheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecks(checks: List<TrademarkCheckEntity>)

    @Query("SELECT * FROM trademark_checks WHERE id = :id")
    suspend fun getCheckById(id: String): TrademarkCheckEntity?

    @Query("DELETE FROM trademark_checks")
    suspend fun clearChecks()
}

@Dao
interface AgentMessageDao {
    @Query("SELECT * FROM agent_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<AgentMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AgentMessageEntity>)

    @Query("DELETE FROM agent_messages")
    suspend fun clearMessages()
}

@Dao
interface DigitalProductDao {
    @Query("SELECT * FROM digital_products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<DigitalProductEntity>>

    @Query("SELECT * FROM digital_products WHERE status = 'PUBLISHED_LIVE' ORDER BY createdAt DESC")
    fun getPublishedProducts(): Flow<List<DigitalProductEntity>>

    @Query("SELECT * FROM digital_products WHERE status = :status ORDER BY createdAt DESC")
    fun getProductsByStatus(status: String): Flow<List<DigitalProductEntity>>

    @Query("SELECT * FROM digital_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): DigitalProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: DigitalProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<DigitalProductEntity>)

    @Update
    suspend fun updateProduct(product: DigitalProductEntity)

    @Query("UPDATE digital_products SET status = :status WHERE id = :id")
    suspend fun updateProductStatus(id: String, status: String)

    @Query("UPDATE digital_products SET salesCount = salesCount + 1, totalRevenueCents = totalRevenueCents + priceCents WHERE id = :id")
    suspend fun recordSale(id: String)

    @Query("DELETE FROM digital_products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("DELETE FROM digital_products")
    suspend fun clearProducts()
}

@Dao
interface GodAuditDao {
    @Query("SELECT * FROM god_audit_verdicts ORDER BY timestamp DESC")
    fun getAllVerdicts(): Flow<List<GodAuditVerdictEntity>>

    @Query("SELECT * FROM god_audit_verdicts WHERE status = 'HALTED' ORDER BY timestamp DESC")
    fun getHaltedVerdicts(): Flow<List<GodAuditVerdictEntity>>

    @Query("SELECT * FROM god_audit_verdicts ORDER BY timestamp DESC LIMIT 1")
    fun getLatestVerdict(): Flow<GodAuditVerdictEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerdict(verdict: GodAuditVerdictEntity)

    @Query("SELECT COUNT(*) FROM god_audit_verdicts")
    fun getTotalInterceptCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM god_audit_verdicts WHERE status = 'HALTED'")
    fun getHaltedCount(): Flow<Int>

    @Query("DELETE FROM god_audit_verdicts")
    suspend fun clearVerdicts()
}

@Dao
abstract class PodOrderDao {
    @Query("SELECT * FROM pod_orders ORDER BY timestamp DESC")
    abstract fun getAllOrders(): Flow<List<PodOrderEntity>>

    @Query("SELECT * FROM pod_orders WHERE status != 'CANCELLED' ORDER BY timestamp DESC")
    abstract fun getActiveOrders(): Flow<List<PodOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrder(order: PodOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrders(orders: List<PodOrderEntity>)

    @Query("SELECT * FROM pod_orders WHERE id = :id LIMIT 1")
    abstract suspend fun getOrderById(id: String): PodOrderEntity?

    @Query("UPDATE pod_orders SET status = :status, progressPercent = :progress WHERE id = :id")
    abstract suspend fun updateOrderStatus(id: String, status: String, progress: Int)

    @Query("DELETE FROM pod_orders WHERE id = :id")
    abstract suspend fun deleteOrder(id: String)

    @Query("DELETE FROM pod_orders")
    abstract suspend fun clearOrders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLedgerEntryInternal(entry: LedgerEntity)

    @Transaction
    open suspend fun insertOrderWithLedger(order: PodOrderEntity, entry: LedgerEntity) {
        insertOrder(order)
        insertLedgerEntryInternal(entry)
    }
}

@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingTransactions(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): PendingTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PendingTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<PendingTransactionEntity>)

    @Query("UPDATE pending_transactions SET status = :status, reviewedAt = :reviewedAt WHERE id = :id")
    suspend fun updateTransactionStatus(id: String, status: String, reviewedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM pending_transactions")
    suspend fun clearAll()
}


