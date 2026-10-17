package com.opensource.autonomous.agent.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        GodAuditVerdictEntity::class,
        PodOrderEntity::class,
        PendingTransactionEntity::class
    ],
    version = 13,
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
    abstract fun podOrderDao(): PodOrderDao
    abstract fun pendingTransactionDao(): PendingTransactionDao


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
                        ensureUltraAgentAndPurgeFakeTransactions(database)
                    }
                }
            }
        }

        /**
         * Section 1 & Section 2A/2B/2C enforcement:
         * Ensures only the single ULTRA agent exists and purges any fake/demo purchase requests or staging orders
         * from the transaction and order tables.
         */
        suspend fun ensureUltraAgentAndPurgeFakeTransactions(db: AppDatabase) {
            try {
                // Purge any fake seeded transactions (TX-APPR-101..105 or any entry lacking real Stripe + Printify IDs)
                val allTx = db.pendingTransactionDao().getAllTransactions().first()
                for (tx in allTx) {
                    if (tx.id.startsWith("TX-APPR-10") || !tx.description.contains("pi_") || !tx.description.contains("printify")) {
                        db.pendingTransactionDao().deleteTransaction(tx.id)
                    }
                }

                // Purge any design-staging items from pod_orders (Section 2C: Design approvals are NOT transactions)
                val allOrders = db.podOrderDao().getAllOrders().first()
                for (ord in allOrders) {
                    if (ord.status == "PENDING_STAGING" || ord.id.startsWith("pod-ord-")) {
                        db.podOrderDao().deleteOrder(ord.id)
                    }
                }

                // Consolidate agent roster to the single ULTRA agent
                db.agentDao().clearAgents()
                db.agentDao().insertAgent(
                    AgentEntity(
                        id = "4",
                        name = "ULTRA",
                        role = "Single Autonomous Agent — Creative, Canvas, Publishing, Payments & Integrity",
                        systemPrompt = AgentPrompts.KRITICAL_LIMITZ_POD_DESIGN_DIRECTOR_PROMPT,
                        isActive = true
                    )
                )
            } catch (_: Exception) {}
        }

        /**
         * Initializes clean database with single ULTRA Agent.
         * ZERO demo ledger transactions, ZERO fake purchase requests, ZERO mock sales.
         */
        suspend fun seedDatabase(db: AppDatabase) {
            val now = System.currentTimeMillis()

            // 1. Seed Single ULTRA Agent (Section 1 — No Swarm, Single Agent Only)
            val agents = listOf(
                AgentEntity(
                    id = "4",
                    name = "ULTRA",
                    role = "Single Autonomous Agent — Creative, Canvas, Publishing, Payments & Integrity",
                    systemPrompt = AgentPrompts.KRITICAL_LIMITZ_POD_DESIGN_DIRECTOR_PROMPT,
                    isActive = true
                )
            )
            db.agentDao().insertAgents(agents)


            // 2. Seed Default Production Settings with Active Store Secrets from BuildConfig
            val buildConfigGithub = try { com.example.BuildConfig.GITHUB_TOKEN.trim() } catch (_: Throwable) { "" }
            val buildConfigStripe = try { com.example.BuildConfig.STRIPE_SECRET_KEY.trim() } catch (_: Throwable) { "" }
            val buildConfigStripePub = try { com.example.BuildConfig.STRIPE_PUBLISHABLE_KEY.trim() } catch (_: Throwable) { "" }
            val buildConfigGemini = try { com.example.BuildConfig.GEMINI_API_KEY.trim() } catch (_: Throwable) { "" }

            val initialSettings = SettingsEntity(
                keyGithub = if (!buildConfigGithub.startsWith("placeholder_")) buildConfigGithub else "",
                keyStripe = if (!buildConfigStripe.startsWith("placeholder_")) buildConfigStripe else "",
                keyStripePublishable = if (!buildConfigStripePub.startsWith("placeholder_")) buildConfigStripePub else "",
                keyGemini = if (!buildConfigGemini.startsWith("placeholder_")) buildConfigGemini else "",
                storefrontProvider = "Printify POD Pop-Up Physical Storefront",
                printifyShopDomain = "kriticallimitz.printify.me"
            )
            db.settingsDao().saveSettings(initialSettings)

            // Seed Autonomous Storefront Location & Setup (Only one storefront is active: Printify POD Pop-Up)
            val defaultStorefront = StorefrontEntity(
                id = "storefront-main",
                companyName = "Kritical Limitz",
                businessNiche = "Underground Luxury Streetwear (Physical POD Only)",
                platformType = "Printify POD Pop-Up Physical Storefront",
                status = "ACTIVE",
                repoName = "kriticallimitz-storefront",
                repoUrl = "https://kriticallimitz.printify.me",
                stripePaymentLink = "https://checkout.stripe.com/pay/cs_live_kriticallimitz",
                monthlyRevenueCents = 0L,
                monthlyCostCents = 0L,
                productsCount = 0,
                complianceStatus = "VERIFIED_100_CLEAN",
                setupLog = "Printify POD Pop-Up Physical Storefront online. Domain: kriticallimitz.printify.me. Physical POD garments only.",
                createdAt = now
            )
            db.storefrontDao().insertStorefront(defaultStorefront)

            // 3. Seed Free-Tier Standby Compute Providers
            val providers = listOf(
                StandbyProviderEntity(
                    id = "prov-01",
                    serviceName = "Google Gemini 4 Argon",
                    tierType = "Paid Tier — Server-Side Gemini 4 Argon API (Primary Core)",
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
                    agentId = "ULTRA",
                    message = "ULTRA Single Agent initialized (Gemini 4 Argon). Zero fake data, zero simulated orders. Live Printify POD Pop-Up Studio ready.",
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
            db.pendingTransactionDao().clearAll()
            db.accountTaskDao().clearTasks()
            db.storefrontDao().clearStorefronts()
            db.trademarkDao().clearChecks()
            db.digitalProductDao().clearProducts()
            db.podOrderDao().clearOrders()
            db.agentMessageDao().clearMessages()
            db.logDao().clearLogs()

            // Re-seed single ULTRA agent, settings, and providers
            seedDatabase(db)

            db.logDao().insertLog(
                LogEntity(
                    agentId = "ULTRA",
                    message = "All demo data removed. ULTRA single-agent integrity enforced.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
