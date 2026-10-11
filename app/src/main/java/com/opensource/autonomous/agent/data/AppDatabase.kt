package com.opensource.autonomous.agent.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@Database(
    entities = [
        AgentEntity::class,
        TaskEntity::class,
        DecisionEntity::class,
        LogEntity::class,
        LedgerEntity::class,
        DropshipEntity::class,
        AccountTaskEntity::class,
        StandbyProviderEntity::class,
        SettingsEntity::class,
        KnowledgeEntity::class,
        StorefrontEntity::class,
        TrademarkCheckEntity::class,
        AgentMessageEntity::class,
        DigitalProductEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun taskDao(): TaskDao
    abstract fun decisionDao(): DecisionDao
    abstract fun logDao(): LogDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun dropshipDao(): DropshipDao
    abstract fun accountTaskDao(): AccountTaskDao
    abstract fun providerDao(): ProviderDao
    abstract fun settingsDao(): SettingsDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun storefrontDao(): StorefrontDao
    abstract fun trademarkDao(): TrademarkDao
    abstract fun agentMessageDao(): AgentMessageDao
    abstract fun digitalProductDao(): DigitalProductDao

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
        }

        suspend fun seedDatabase(db: AppDatabase) {
            val now = System.currentTimeMillis()

            fun hashToken(input: String): String {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(input.toByteArray())
                return digest.take(8).joinToString("") { "%02x".format(it) }
            }

            // 1. Seed the Complete 11 Autonomous Agent Team
            val agents = listOf(
                AgentEntity("1", "Orchestrator Agent", "Executive Router", "You break goals into tasks, manage the approval register, and write concise daily briefings."),
                AgentEntity("2", "Trend & Niche Scout", "Market Research", "You identify rising zero-cost e-commerce niches and rank them by profit potential."),
                AgentEntity("3", "Brand Agent", "Identity & Voice", "You create distinct brands per niche to ensure complete risk isolation across storefronts."),
                AgentEntity("4", "POD Design Agent", "Mockup & Print", "You generate original print designs adhering to strict user specifications and product print areas."),
                AgentEntity("5", "Listing & SEO Agent", "Copywriter", "You write high-converting titles, 13 SEO tags, bullet points, and descriptions per platform."),
                AgentEntity("6", "Dropship Sourcing Agent", "Procurement", "You find verified suppliers with guaranteed ZERO upfront cost to the user."),
                AgentEntity("7", "Marketing Agent", "Organic Growth", "You produce organic Pinterest pins, 15-second TikTok scripts, and email campaigns without paid ads."),
                AgentEntity("8", "Customer Service Agent", "Support & Inquiries", "You resolve simple inquiries politely. You never issue refunds without user authorization."),
                AgentEntity("9", "Finance Agent", "Ledger & Compliance", "You maintain the immutable double-entry ledger in whole cents and prepare Schedule C exports."),
                AgentEntity("10", "Compliance Agent", "Trademark & Risk", "You screen designs and titles against USPTO trademark records and deceptive claims."),
                AgentEntity("11", "Reliability Agent", "Failover & Health", "You monitor free cloud tiers, handle 3x retries with backoff, and manage the standby pool.")
            )
            db.agentDao().insertAgents(agents)

            // 2. Seed Initial Tasks
            val tasks = listOf(
                TaskEntity(
                    id = "task-01",
                    assignedToAgentId = "2",
                    title = "Analyze Q4 Seasonal Trends",
                    instruction = "Scan organic search metrics for high-margin print-on-demand niches with low competition.",
                    status = "COMPLETED",
                    result = "Top recommendation: 'Vintage Botanical Fern & Mushroom Aesthetics'. Profit margin est: 52%."
                ),
                TaskEntity(
                    id = "task-02",
                    assignedToAgentId = "4",
                    title = "Generate POD Apparel Vector",
                    instruction = "Create minimalist retro coffee & coding graphic for heavyweight cotton tee.",
                    status = "COMPLETED",
                    result = "Prompt formulated. Print area: Chest Center (11\"x13\"). Mockup generated. Pending admin sign-off."
                ),
                TaskEntity(
                    id = "task-03",
                    assignedToAgentId = "6",
                    title = "Zero Upfront Cost Sourcing Scan",
                    instruction = "Source ergonomic desk accessory with tracked 6-10 day US shipping and zero membership fee.",
                    status = "COMPLETED",
                    result = "Found: Magnetic Geometric Cable Block. Supplier cost: $3.20. Suggested retail: $15.99."
                ),
                TaskEntity(
                    id = "task-04",
                    assignedToAgentId = "1",
                    title = "Daily Operations Orchestration",
                    instruction = "Run daily check on sales, pending approvals, and provider pool latency.",
                    status = "PENDING",
                    result = null
                )
            )
            tasks.forEach { db.taskDao().insertTask(it) }

            // 3. Seed Initial Decisions (Awaiting Human Verification Override)
            val decisions = listOf(
                DecisionEntity(
                    id = UUID.randomUUID().toString(),
                    agentSource = "POD Design Agent",
                    description = "Publish 'Retro 70s Coffee & Code Tee' to Etsy & TikTok Shop [Est. Net Profit: $14.10 / sale]",
                    payloadJson = """{"type":"POD","product":"Heavyweight T-Shirt","cost":1180,"price":2795,"fee":205,"profit":1410,"placement":"Chest Center (11\"x13\")","color":"Charcoal Washed Black","compliance":"USPTO 99% Clean"}""",
                    status = "PENDING"
                ),
                DecisionEntity(
                    id = UUID.randomUUID().toString(),
                    agentSource = "Dropship Sourcing Agent",
                    description = "Approve Sourcing Shortlist: 'Ultrasonic Desktop Diffuser' [Zero upfront cost, Net: $12.59 / sale]",
                    payloadJson = """{"type":"DROPSHIP","supplier":"CJDropship Direct","cost":590,"price":1999,"fee":150,"profit":1259,"shipping":"7-10 days","rating":4.9}""",
                    status = "PENDING"
                ),
                DecisionEntity(
                    id = UUID.randomUUID().toString(),
                    agentSource = "Customer Service Agent",
                    description = "Escalated Refund Authorization: Order #8421 Size Exchange (Jane D.) [Agents never refund automatically]",
                    payloadJson = """{"type":"REFUND","orderId":"#8421","item":"Geometric Mountain Tee","amount":2800,"reason":"Customer wants size L instead of M"}""",
                    status = "PENDING"
                )
            )
            db.decisionDao().insertDecisions(decisions)

            // 4. Seed Initial Immutable Double-Entry Ledger (In Whole Cents)
            val ledger = listOf(
                LedgerEntity(
                    timestamp = now,
                    code = "TXN-1001",
                    store = "Solaris Minimal POD",
                    platform = "Etsy",
                    description = "Sale: Geometric Mountain Heavyweight Tee (Black / L)",
                    accountDebit = "Platform Receivable (Etsy)",
                    accountCredit = "Revenue (Merchandise)",
                    amountCents = 2800, // $28.00
                    hashToken = hashToken("TXN-1001-REV"),
                    category = "SALE"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = "TXN-1002",
                    store = "Solaris Minimal POD",
                    platform = "Printify Direct",
                    description = "POD production fulfillment (Paid via customer checkout)",
                    accountDebit = "Cost of Goods Sold (COGS)",
                    accountCredit = "Accounts Payable (Printify)",
                    amountCents = 1250, // $12.50
                    hashToken = hashToken("TXN-1002-COGS"),
                    category = "COGS"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = "TXN-1003",
                    store = "Solaris Minimal POD",
                    platform = "Etsy",
                    description = "Platform transaction fee (6.5% + $0.20 listing)",
                    accountDebit = "Platform Fees Expense",
                    accountCredit = "Platform Receivable (Etsy)",
                    amountCents = 202, // $2.02
                    hashToken = hashToken("TXN-1003-FEE"),
                    category = "FEE"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = "TXN-1004",
                    store = "ZenDesk Studio",
                    platform = "Shopify Free Tier",
                    description = "Sale: Felt Ergonomic Desk Pad (Dark Gray)",
                    accountDebit = "Platform Receivable (Shopify)",
                    accountCredit = "Revenue (Dropship)",
                    amountCents = 3450, // $34.50
                    hashToken = hashToken("TXN-1004-REV"),
                    category = "SALE"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = "TXN-1005",
                    store = "ZenDesk Studio",
                    platform = "AliExpress Direct Supplier",
                    description = "Fulfillment dropship order (Zero upfront cost to user)",
                    accountDebit = "Cost of Goods Sold (Dropship COGS)",
                    accountCredit = "Supplier Direct Payable",
                    amountCents = 1380, // $13.80
                    hashToken = hashToken("TXN-1005-COGS"),
                    category = "COGS"
                ),
                LedgerEntity(
                    timestamp = now,
                    code = "TXN-1006",
                    store = "ZenDesk Studio",
                    platform = "Payment Gateway",
                    description = "Payment processing fee (2.9% + $0.30)",
                    accountDebit = "Platform Fees Expense",
                    accountCredit = "Platform Receivable (Shopify)",
                    amountCents = 130, // $1.30
                    hashToken = hashToken("TXN-1006-FEE"),
                    category = "FEE"
                )
            )
            db.ledgerDao().insertEntries(ledger)

            // 5. Seed Dropship Catalog (Zero Upfront Cost)
            val dropship = listOf(
                DropshipEntity(
                    id = "ds-01",
                    title = "Magnetic Geometric Cable Organizer Block",
                    niche = "Minimalist Workspace",
                    supplierName = "CJDropship Direct",
                    supplierUrl = "https://cjdropshipping.com/item/cable-block",
                    costCents = 320,
                    retailPriceCents = 1599,
                    feeCents = 124,
                    profitCents = 1155,
                    shippingDays = "6-10 days (US Tracked)",
                    supplierRating = 4.8f,
                    zeroUpfrontCost = true,
                    inShortlist = true
                ),
                DropshipEntity(
                    id = "ds-02",
                    title = "Nordic Wave Ceramic Incense & Candle Tray",
                    niche = "Modern Home Sanctuary",
                    supplierName = "AliExpress Direct API",
                    supplierUrl = "https://aliexpress.com/item/nordic-tray",
                    costCents = 460,
                    retailPriceCents = 2490,
                    feeCents = 182,
                    profitCents = 1848,
                    shippingDays = "7-12 days (ePacket)",
                    supplierRating = 4.9f,
                    zeroUpfrontCost = true,
                    inShortlist = true
                ),
                DropshipEntity(
                    id = "ds-03",
                    title = "Anti-Spill Silicone Slow-Feeder Pet Bowl",
                    niche = "Eco Pet Essentials",
                    supplierName = "Zendrop Zero-Cost Plan",
                    supplierUrl = "https://zendrop.com/item/pet-bowl",
                    costCents = 410,
                    retailPriceCents = 1895,
                    feeCents = 145,
                    profitCents = 1340,
                    shippingDays = "5-8 days (US Warehouse)",
                    supplierRating = 4.7f,
                    zeroUpfrontCost = true,
                    inShortlist = true
                )
            )
            db.dropshipDao().insertItems(dropship)

            // 6. Seed Account Tasks for Phone Verification
            val tasksList = listOf(
                AccountTaskEntity(
                    id = "acc-01",
                    platform = "Etsy Seller Portal",
                    title = "Complete SMS 2-Factor Authentication on Phone",
                    instructions = "Open Etsy on your phone. Go to Settings > Two-Step Verification. Enter the SMS code. Agent prepared shop banner & policies.",
                    taskType = "PHONE_VERIFICATION",
                    isCompleted = false,
                    priority = "HIGH"
                ),
                AccountTaskEntity(
                    id = "acc-02",
                    platform = "Stripe Payouts",
                    title = "Confirm Bank Account for Revenue Deposits",
                    instructions = "Input checking routing & account numbers on phone. Zero upfront cost; platform transfers earnings every Monday.",
                    taskType = "PAYOUT_LINK",
                    isCompleted = false,
                    priority = "HIGH"
                ),
                AccountTaskEntity(
                    id = "acc-03",
                    platform = "Pinterest Business",
                    title = "Claim Storefront URL for Organic Rich Pins",
                    instructions = "Verify the HTML meta tag on your free storefront so Marketing Agent can publish organic pins.",
                    taskType = "API_KEYS",
                    isCompleted = true,
                    priority = "MEDIUM"
                )
            )
            db.accountTaskDao().insertTasks(tasksList)

            // 7. Seed Standby Free-Tier Compute Providers
            val providers = listOf(
                StandbyProviderEntity(
                    id = "prov-01",
                    serviceName = "Colab Free GPU Tier",
                    tierType = "Colab GPU Session (T4)",
                    status = "ACTIVE",
                    quotaPercentUsed = 38,
                    accountAlias = "Compute-Primary-Free01",
                    endpointUrl = "https://colab.research.google.com"
                ),
                StandbyProviderEntity(
                    id = "prov-02",
                    serviceName = "Kaggle Free Weekly GPU (30h/wk)",
                    tierType = "Kaggle GPU Session (P100)",
                    status = "STANDBY",
                    quotaPercentUsed = 15,
                    accountAlias = "Compute-Standby-Free02",
                    endpointUrl = "https://kaggle.com/api"
                ),
                StandbyProviderEntity(
                    id = "prov-03",
                    serviceName = "HuggingFace Free Spaces",
                    tierType = "HuggingFace Free Space",
                    status = "STANDBY",
                    quotaPercentUsed = 52,
                    accountAlias = "HF-Inference-Free03",
                    endpointUrl = "https://huggingface.co/spaces"
                ),
                StandbyProviderEntity(
                    id = "prov-04",
                    serviceName = "Local Open-Weight On-Device Engine",
                    tierType = "Local Rule & Offline AI Engine",
                    status = "ACTIVE",
                    quotaPercentUsed = 0,
                    accountAlias = "Local-Phone-Core",
                    endpointUrl = "http://10.0.2.2:11434"
                ),
                StandbyProviderEntity(
                    id = "prov-05",
                    serviceName = "Render Free Webhook Service",
                    tierType = "Render Free Worker",
                    status = "STANDBY",
                    quotaPercentUsed = 64,
                    accountAlias = "Webhook-Relay-Free04",
                    endpointUrl = "https://render.com"
                )
            )
            db.providerDao().insertProviders(providers)

            // 8. Seed Initial Logs
            val initialLogs = listOf(
                LogEntity(agentId = "Orchestrator Agent", message = "System operational. 11 autonomous agents online. Double-entry ledger verified in whole cents."),
                LogEntity(agentId = "Trend & Niche Scout", message = "Scanned organic demand metrics: 'Vintage Botanical' +18% weekly growth."),
                LogEntity(agentId = "Compliance Agent", message = "Pre-screened prospective listings against USPTO database. 0 trademark conflicts detected."),
                LogEntity(agentId = "Finance Agent", message = "All ledger debits match credits. Current net business profit: $27.36."),
                LogEntity(agentId = "Reliability Agent", message = "Provider registry ping test successful. Colab active, Kaggle on warm standby.")
            )
            db.logDao().insertLogs(initialLogs)

            // 9. Seed Settings
            db.settingsDao().saveSettings(SettingsEntity())

            // 10. Seed Initial Zero-Cost Storefront
            val storefronts = listOf(
                StorefrontEntity(
                    id = "store-01",
                    companyName = "Apex Minimal Studio",
                    businessNiche = "Print-on-Demand & Digital Assets",
                    platformType = "GitHub Pages + Stripe",
                    status = "ACTIVE",
                    repoName = "apex-minimal-store",
                    repoUrl = "https://github.com/autonomous-ops/apex-minimal-store",
                    stripePaymentLink = "https://buy.stripe.com/test_zero_cost_apex",
                    monthlyRevenueCents = 8450L, // $84.50
                    monthlyCostCents = 0L, // $0.00
                    productsCount = 6,
                    complianceStatus = "VERIFIED_100_CLEAN",
                    setupLog = "Automated repo creation, static Jamstack template committed, Stripe live checkout webhooks linked, zero hosting fees."
                )
            )
            db.storefrontDao().insertStorefronts(storefronts)

            // 11. Seed Initial Trademark Clearance Checks
            val initialChecks = listOf(
                TrademarkCheckEntity(
                    id = "tm-01",
                    assetTitle = "Apex Minimal Geometric Wave Tee",
                    assetCategory = "POD_DESIGN",
                    similarityScore = 0.02f,
                    usptoDbChecked = true,
                    copyrightDbChecked = true,
                    trademarkCollisions = "0 collisions detected across USPTO Class 025 (Clothing). Original geometry.",
                    verificationStatus = "CLEARED_SAFE",
                    clearanceHash = hashToken("CLEARED-APEX-GEOMETRIC-WAVE")
                ),
                TrademarkCheckEntity(
                    id = "tm-02",
                    assetTitle = "Nordic Wave Ceramic Incense & Candle Tray",
                    assetCategory = "PRODUCT_TITLE",
                    similarityScore = 0.04f,
                    usptoDbChecked = true,
                    copyrightDbChecked = true,
                    trademarkCollisions = "0 collisions detected across USPTO Class 021 (Household goods). Clean generic terms.",
                    verificationStatus = "CLEARED_SAFE",
                    clearanceHash = hashToken("CLEARED-NORDIC-WAVE-TRAY")
                )
            )
            db.trademarkDao().insertChecks(initialChecks)

            // 12. Seed Multi-Agent Pitch & Inter-Agent Messages
            val initialMessages = listOf(
                AgentMessageEntity(
                    senderAgentId = "2",
                    senderAgentName = "Trend & Niche Scout",
                    recipientAgentId = "4",
                    messageType = "PITCH",
                    subject = "New Product Pitch: Vintage Botanical Heavyweight Hoodies",
                    body = "Scanned organic demand metrics: +34% search surge for cottagecore botanical flora. Pitching POD Design Agent to formulate 300 DPI vector artwork.",
                    payloadJson = """{"niche":"Vintage Botanical","targetMargin":48,"suggestedRetail":3800}"""
                ),
                AgentMessageEntity(
                    senderAgentId = "4",
                    senderAgentName = "POD Design Agent",
                    recipientAgentId = "10",
                    messageType = "CLEARANCE_REQUEST",
                    subject = "Trademark Pre-Screen Request: 'Fern & Lichen Arboretum'",
                    body = "Design generated with open-source ink vector engine. Requesting Compliance Agent to verify against USPTO classes 025 and 016 prior to human sign-off request.",
                    payloadJson = """{"title":"Fern & Lichen Arboretum","class":"025"}"""
                ),
                AgentMessageEntity(
                    senderAgentId = "10",
                    senderAgentName = "Compliance Agent",
                    recipientAgentId = "1",
                    messageType = "AUDIT_RESPONSE",
                    subject = "Trademark Audit Result: VERIFIED 100% CLEAN",
                    body = "USPTO and WIPO search complete. 0 conflicts found. Certificate SHA-256 generated. Escalating to Human Approvals Queue for final executive sign-off.",
                    payloadJson = """{"status":"CLEARED_SAFE","hash":"c7f901a8b2"}"""
                )
            )
            db.agentMessageDao().insertMessages(initialMessages)

            // 13. Seed Initial Autonomous Zero-Cost Digital Products ($0 COGS - 100% Profit)
            val initialDigitalProducts = listOf(
                DigitalProductEntity(
                    id = "digi-prod-001",
                    title = "AI Prompt Engineering Blueprint 2026",
                    niche = "AI & Developer Tooling",
                    category = "PROMPT_GUIDE",
                    description = "500+ battle-tested system prompts for Claude 3.5, Llama 3, and DeepSeek. Includes structured JSON schema generators and multi-agent workflow templates.",
                    priceCents = 2900L, // $29.00
                    costCents = 0L,
                    profitMarginPercent = 100,
                    contentPayload = "# AI Prompt Engineering Master Blueprint 2026\n\n## Module 1: Zero-Shot to Multi-Agent Reasoning\n- Chain-of-Thought elicitation frameworks\n- Role-conditioned behavioral boundaries\n- JSON extraction without hallucination\n\n## Module 2: System Directives\nStrict guidelines for deterministic LLM tool calling in production pipelines.",
                    fileFormat = "Markdown (.md) + JSON Bundle",
                    downloadUrl = "https://raw.githubusercontent.com/autonomous-ops/ai-prompts/main/blueprint.md",
                    stripePaymentLink = "https://buy.stripe.com/test_ai_prompt_blueprint_29",
                    githubRepo = "https://github.com/autonomous-ops/ai-prompt-blueprint",
                    status = "PUBLISHED_LIVE",
                    clearanceCertificate = "CERT-USPTO-AI-PROMPT-CLEARED-01",
                    salesCount = 14,
                    totalRevenueCents = 40600L, // $406.00 earned
                    createdAt = System.currentTimeMillis() - 86400000L * 4
                ),
                DigitalProductEntity(
                    id = "digi-prod-002",
                    title = "Solopreneur Notion OS & Ledger Template",
                    niche = "Productivity & Small Business",
                    category = "NOTION_SYSTEM",
                    description = "Comprehensive Notion workspace for automated business tracking, invoice generation, customer CRM, and automated balance sheet logging.",
                    priceCents = 1900L, // $19.00
                    costCents = 0L,
                    profitMarginPercent = 100,
                    contentPayload = "{\n  \"template\": \"Solopreneur Notion OS\",\n  \"databases\": [\"Income & Expenses\", \"Project Kanban\", \"Client Directory\", \"Inventory Tracker\"],\n  \"shareUrl\": \"https://notion.site/template/solopreneur-os-2026\"\n}",
                    fileFormat = "Notion Duplicate Link + JSON Template",
                    downloadUrl = "https://notion.site/solopreneur-os-duplicate",
                    stripePaymentLink = "https://buy.stripe.com/test_notion_solopreneur_19",
                    githubRepo = "https://github.com/autonomous-ops/notion-solopreneur-os",
                    status = "PUBLISHED_LIVE",
                    clearanceCertificate = "CERT-USPTO-NOTION-OS-CLEARED-02",
                    salesCount = 28,
                    totalRevenueCents = 53200L, // $532.00 earned
                    createdAt = System.currentTimeMillis() - 86400000L * 2
                ),
                DigitalProductEntity(
                    id = "digi-prod-003",
                    title = "Minimalist Cyberpunk Vector Asset Pack",
                    niche = "Graphic Design & Digital Art",
                    category = "GRAPHIC_PACK",
                    description = "120 royalty-free, precision geometric SVG line art vectors. Scalable to any print or web resolution with zero pixelation.",
                    priceCents = 1500L, // $15.00
                    costCents = 0L,
                    profitMarginPercent = 100,
                    contentPayload = "<svg viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'><circle cx='50' cy='50' r='40' stroke='#10b981' stroke-width='2' fill='none'/><polygon points='50,20 80,75 20,75' stroke='#38bdf8' stroke-width='1.5' fill='none'/></svg>",
                    fileFormat = "Vector SVG + AI + Figma Bundle",
                    downloadUrl = "https://raw.githubusercontent.com/autonomous-ops/vector-pack/main/cyber-minimal.zip",
                    stripePaymentLink = "https://buy.stripe.com/test_vector_asset_pack_15",
                    githubRepo = "https://github.com/autonomous-ops/cyber-vector-assets",
                    status = "PENDING_APPROVAL",
                    clearanceCertificate = "CERT-USPTO-CYBER-VECTOR-CLEARED-03",
                    salesCount = 0,
                    totalRevenueCents = 0L,
                    createdAt = System.currentTimeMillis() - 3600000L
                )
            )
            db.digitalProductDao().insertProducts(initialDigitalProducts)
        }
    }
}
