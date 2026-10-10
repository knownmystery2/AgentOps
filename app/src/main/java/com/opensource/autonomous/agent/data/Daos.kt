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
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<LedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntity>)
}

@Dao
interface DropshipDao {
    @Query("SELECT * FROM dropship_catalog ORDER BY inShortlist DESC, costCents ASC")
    fun getAllItems(): Flow<List<DropshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DropshipEntity>)

    @Query("UPDATE dropship_catalog SET inShortlist = :inShortlist WHERE id = :id")
    suspend fun updateShortlistStatus(id: String, inShortlist: Boolean)
}

@Dao
interface AccountTaskDao {
    @Query("SELECT * FROM account_tasks ORDER BY isCompleted ASC, priority DESC")
    fun getAllTasks(): Flow<List<AccountTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<AccountTaskEntity>)

    @Query("UPDATE account_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: String, isCompleted: Boolean)
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

