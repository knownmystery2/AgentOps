package com.opensource.autonomous.agent.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

        @Database(
    entities = [
        AgentEntity::class,
        TaskEntity::class,
        DecisionEntity::class,
        LogEntity::class,
        LedgerEntity::class,
        AccountTaskEntity::class,
        StandbyProviderEntity::class,
        SettingsEntity::class,
        KnowledgeEntity::class,
        StorefrontEntity::class,
        TrademarkCheckEntity::class,
        AgentMessageEntity::class,
        DigitalProductEntity::class,
        GodAuditVerdictEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun taskDao(): TaskDao
    abstract fun decisionDao(): DecisionDao
    abstract fun logDao(): LogDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun accountTaskDao(): AccountTaskDao
    abstract fun providerDao(): ProviderDao
    abstract fun settingsDao(): SettingsDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun storefrontDao(): StorefrontDao
    abstract fun trademarkDao(): TrademarkDao
    abstract fun agentMessageDao(): AgentMessageDao
    abstract fun digitalProductDao(): DigitalProductDao
    abstract fun godAuditDao(): GodAuditDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agent_biz.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabase(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        ensureGodAgentSeeded(database)
                    }
                }
            }
        }

        /**
         * Ensures GOD agent is always seeded and active in the database.
         */
        suspend fun ensureGodAgentSeeded(db: AppDatabase) {
            val god = db.agentDao().getAgentById("0")
            if (god == null) {
                db.agentDao().insertAgent(
                    AgentEntity(
                        id = "0",
                        name = "GOD",
                        role = "The Interceptor Overseer & Silent Gatekeeper",
                        systemPrompt = AgentPrompts.GOD_OVERSEER_SYSTEM_PROMPT,
                        isActive = true
                    )
                )
            }
        }

        /**
         * Initializes clean database with GOD Overseer and Swarm Agents.
         * ZERO demo ledger transactions, ZERO fake tasks, ZERO mock sales.
         * Clean for 100% live working function.
         */
        suspend fun seedDatabase(db: AppDatabase) {
            val now = System.currentTimeMillis()

            // 1. Seed Core Multi-Agent Swarm Cluster (All in ACTIVE status)
            val agents = listOf(
                AgentEntity("0", "GOD", "The Interceptor Overseer & Silent Gatekeeper", AgentPrompts.GOD_OVERSEER_SYSTEM_PROMPT, isActive = true),
                AgentEntity("1", "Administrator AI", "Executive Administrator & Router", AgentPrompts.ADMINISTRATOR_SYSTEM_PROMPT, isActive = true),
                AgentEntity("2", "Trend & Identity Agent", "Trend & Pop-Up Store Identity", AgentPrompts.TREND_IDENTITY_SYSTEM_PROMPT, isActive = true),
                AgentEntity("3", "Brand Agent", "Identity & Voice Architecture", "ROLE: Brand Identity Architect. You establish brand tone and visual style guidelines.", isActive = true),
                AgentEntity("4", "Creative Design Agent", "Design & Visual Prompt Formulation", AgentPrompts.CREATIVE_DESIGN_SYSTEM_PROMPT, isActive = true),
                AgentEntity("5", "Listing & SEO Agent", "E-Commerce Copywriter & Metadata", AgentPrompts.CONTENT_PROMPT, isActive = true),
                AgentEntity("7", "Marketing Agent", "Organic Growth & Content Campaigns", AgentPrompts.MARKETING_PROMPT, isActive = true),
                AgentEntity("8", "Customer Service Agent", "Support & Inquiries", AgentPrompts.SUPPORT_PROMPT, isActive = true),
                AgentEntity("9", "Finance Agent", "Double-Entry Ledger & Compliance", AgentPrompts.FINANCE_PROMPT, isActive = true),
                AgentEntity("10", "Legal Compliance Agent", "Trademark & Copyright Safety Filter", AgentPrompts.LEGAL_COMPLIANCE_SYSTEM_PROMPT, isActive = true),
                AgentEntity("11", "Reliability Agent", "Model Failover & Standby Health", "ROLE: Reliability Agent. You monitor free tiers and handle open-source model rotation.", isActive = true)
            )
            db.agentDao().insertAgents(agents)


            // 2. Seed Default Production Settings
            db.settingsDao().saveSettings(SettingsEntity())

            // 3. Seed Free-Tier Standby Compute Providers
            val providers = listOf(
                StandbyProviderEntity(
                    id = "prov-01",
                    serviceName = "Google Gemini 2.5 Pro / Flash",
                    tierType = "Server-Side Gemini API (Primary Core)",
                    status = "ACTIVE",
                    quotaPercentUsed = 0,
                    accountAlias = "Gemini-Core-Engine",
                    endpointUrl = "https://generativelanguage.googleapis.com"
                ),
                StandbyProviderEntity(
                    id = "prov-02",
                    serviceName = "Local Open-Weight On-Device Engine",
                    tierType = "Local Rule & Offline AI Engine (Failover)",
                    status = "ACTIVE",
                    quotaPercentUsed = 0,
                    accountAlias = "Local-Phone-Core",
                    endpointUrl = "http://10.0.2.2:11434"
                ),
                StandbyProviderEntity(
                    id = "prov-03",
                    serviceName = "Colab Free GPU Tier",
                    tierType = "Colab GPU Session (T4)",
                    status = "STANDBY",
                    quotaPercentUsed = 0,
                    accountAlias = "Compute-Standby-Free01",
                    endpointUrl = "https://colab.research.google.com"
                ),
                StandbyProviderEntity(
                    id = "prov-04",
                    serviceName = "HuggingFace Free Inference Spaces",
                    tierType = "HuggingFace Free Space",
                    status = "STANDBY",
                    quotaPercentUsed = 0,
                    accountAlias = "HF-Inference-Free02",
                    endpointUrl = "https://huggingface.co/spaces"
                ),
                StandbyProviderEntity(
                    id = "prov-05",
                    serviceName = "Pollinations.ai Free Image Tier",
                    tierType = "Free Open-Source Image Synthesizer",
                    status = "ACTIVE",
                    quotaPercentUsed = 0,
                    accountAlias = "Pollinations-Visual-Gen",
                    endpointUrl = "https://pollinations.ai"
                )
            )
            db.providerDao().insertProviders(providers)

            // 4. Initial System Startup Log
            db.logDao().insertLog(
                LogEntity(
                    agentId = "1",
                    message = "Multi-Agent Swarm Cluster initialized. 10 Agents Online. Zero demo data. Live operating environment ready.",
                    timestamp = now
                )
            )
        }

        /**
         * Completely clears all demo or historical mock data, leaving a clean live ledger and catalogs.
         */
        suspend fun purgeAllDemoData(db: AppDatabase) {
            db.taskDao().clearTasks()
            db.decisionDao().clearDecisions()
            db.ledgerDao().clearLedger()
            db.accountTaskDao().clearTasks()
            db.storefrontDao().clearStorefronts()
            db.trademarkDao().clearChecks()
            db.digitalProductDao().clearProducts()
            db.agentMessageDao().clearMessages()
            db.logDao().clearLogs()

            // Re-seed agents, settings, and providers if missing
            seedDatabase(db)

            db.logDao().insertLog(
                LogEntity(
                    agentId = "1",
                    message = "All demo data removed. Everything set for live working function.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
