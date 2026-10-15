package com.opensource.autonomous.agent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val systemPrompt: String,
    val isActive: Boolean = true
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val assignedToAgentId: String,
    val title: String,
    val instruction: String,
    val status: String, // PENDING, RUNNING, COMPLETED, FAILED
    val result: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey val id: String,
    val agentSource: String,
    val description: String,
    val payloadJson: String,
    val status: String, // PENDING, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Immutable Double-Entry Ledger (All money stored in integer whole cents)
@Entity(tableName = "ledger_entries")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val code: String,
    val store: String,
    val platform: String,
    val description: String,
    val accountDebit: String,
    val accountCredit: String,
    val amountCents: Long, // in whole cents: $28.00 = 2800L
    val hashToken: String,
    val category: String // SALE, COGS, FEE, REFUND
)

// Top Admin Phone Verification Tasks (SMS 2FA, W-9/1099, CAPTCHA)
@Entity(tableName = "account_tasks")
data class AccountTaskEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val title: String,
    val instructions: String,
    val taskType: String, // PHONE_VERIFICATION, TAX_SETUP, CAPTCHA, PAYOUT_LINK
    val isCompleted: Boolean = false,
    val priority: String = "HIGH"
)

// Standby Free-Tier Compute Account Pool & Provider Registry
@Entity(tableName = "standby_providers")
data class StandbyProviderEntity(
    @PrimaryKey val id: String,
    val serviceName: String,
    val tierType: String,
    val status: String, // ACTIVE, STANDBY, COOLDOWN, RATE_LIMITED
    val quotaPercentUsed: Int,
    val accountAlias: String,
    val endpointUrl: String
)

// Comprehensive Multi-Functional Settings Entity for Autonomous Biz & Swarm
@Entity(tableName = "business_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    // 1. Account & Business Profile
    val businessName: String = "Autonomous Studio",
    val operatorAlias: String = "Lead Founder",
    val supportEmail: String = "ops@autonomous.studio",
    val storefrontProvider: String = "GitHub Pages + Stripe",
    val requireBiometricPin: Boolean = false,

    // 2. Appearance & Display
    val themeMode: String = "Dark", // System, Dark, Light
    val amoledMode: Boolean = true,
    val accentColor: String = "Emerald", // Emerald, Cyber Blue, Neon Violet, Amber Gold
    val compactDensity: Boolean = false,
    val currencySymbol: String = "USD ($)", // USD ($), EUR (€), GBP (£), CAD (C$), AUD (A$)
    val uiAnimationsEnabled: Boolean = true,

    // 3. Autonomous Agent Swarm & Orchestration
    val globalAutomationActive: Boolean = true,
    val productFrequency: String = "Daily", // Every 1h, Every 4h, Every 12h, Daily, Weekly
    val riskLevel: String = "Balanced", // Conservative, Balanced, Aggressive
    val minMarginPercent: Int = 35,
    val autoPitchGeneration: Boolean = true,
    val maxDailyCreations: Int = 8,
    val enableAgentChatLogs: Boolean = true,
    val bypassApprovals: Boolean = false,
    val bypassRateLimits: Boolean = false,
    val offlineSimulationMode: Boolean = false,

    // 4. Per-Agent Model Customization (100% Open Source Models & Local Engine)
    val modelCeo: String = "llama3:8b",
    val modelResearch: String = "llama3:8b",
    val modelProduct: String = "mistral:7b",
    val modelMarketing: String = "llama3:8b",
    val modelContent: String = "mistral:7b",
    val modelSupport: String = "mistral:7b",
    val modelFinance: String = "deepseek-coder:6.7b",

    // 5. Storefront & Zero-Cost eCommerce
    val autoDeployOnApproval: Boolean = true,
    val instantStripeCheckout: Boolean = true,
    val githubRepoVisibility: String = "public", // public, private
    val digitalProductFormats: String = "Markdown Guides, SVG Vectors, Code Snippets, Presets, Boilerplates",
    val podProviderSandbox: String = "Direct JAMStack Mock",

    // 6. Trademark, Copyright & Legal Protection
    val strictCopyrightCheck: Boolean = true,
    val trademarkConfidenceMin: Int = 98,
    val autoClearanceCertificate: Boolean = true,
    val allowedStyles: String = "Minimalist line art, Vintage botanical, Retro typographic 70s, Cyber-grid geometric, Cottagecore watercolor",
    val forbiddenItems: String = "Trademarked logos, Disney/Marvel characters, celebrity names, copyrighted song lyrics, brand slogans",
    val preferredPalettes: String = "Earthy terracotta, Charcoal slate, Pastel sage, Amber gold, Monochrome ink",

    // 7. SecretManager & Credentials (Masked)
    val backendServerEndpoint: String = "http://10.0.2.2:8080",
    val ollamaEndpoint: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "llama3",
    val keyGithub: String = "",
    val keyStripe: String = "",
    val keyStripePublishable: String = "",
    val keyGroq: String = "",
    val keyHuggingFace: String = "",
    val keyOpenRouter: String = "",
    val keyGemini: String = "",
    val keyPrintify: String = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIzN2Q0YmQzMDM1ZmUxMWU5YTgwM2FiN2VlYjNjY2M5NyIsImp0aSI6IjA4YmI2ZDE0YTI0ZjE5YjA4MGY4ZGJjMWY3NzgwNGNkZDQ4NGU5NjA2MDU5MGUxZmY2M2MyNjdlYTVmMWNkMmRlNTA5YWFiODJmMDU2ZWRiIiwiaWF0IjoxNzkxODE3NTI3LjkyMzUzMiwibmJmIjoxNzkxODE3NTI3LjkyMzUzNSwiZXhwIjoxODIzMzUzNTI3LjkxNDUyNiwic3ViIjoiMjY2NTA4MzIiLCJzY29wZXMiOlsic2hvcHMubWFuYWdlIiwic2hvcHMucmVhZCIsImNhdGFsb2cucmVhZCIsIm9yZGVycy5yZWFkIiwib3JkZXJzLndyaXRlIiwicHJvZHVjdHMucmVhZCIsInByb2R1Y3RzLndyaXRlIiwid2ViaG9va3MucmVhZCIsIndlYmhvb2tzLndyaXRlIiwidXBsb2Fkcy5yZWFkIiwidXBsb2Fkcy53cml0ZSIsInByaW50X3Byb3ZpZGVycy5yZWFkIiwidXNlci5pbmZvIl19.VM-97U2qhg5acAzIaGFER_vgWRvq5uQ0KEo83Id1QXrMqeJL74-q6fgD9wXFblhFpZPFGV3N0hups8cwUZ936Fvbx1mK0t-f7kWyor_KDoQsXGzHZ1AVm51d1KjE0iDnjY1m54ed4ZKQSfQBJglNBgBDsOmMywqua4NgEZa_BPTx5mS1AnZmjyMO8cN9vFehLwq430MNaSVPMzJ4Eod1wZ2yac167Buu-EI2q_-k0DtKV1rlEINO6bs9DT5AVEks7kdLVxP7g-DZf_ufzAnq7xcPh_ysuoX382WwOLChlVoBqiA_wmJGlNmHYblhtHF5fDF-XI-vmlqE4dN8FWmO7KvvbivsiTLjnJ9OnREhElAuE79uyJbc6qRTC_x6DeaytaT2fN9i3GlJueC9tj7ZAFAiWKteV6s0Rb8Nt5hGCclmU7LWgApkkrU-oEnpnP9Vx_monSyQovgtPDY3K2-5qTKoLtDBGlFNDGHRQ0rqCz_r5cisjfHkq_LKXbfziUNtFqJVn07GmunBUvlTwcgwvTuREnc2qZD2hlneETH35xTDT6cZHqcU2qs_h35xK1RbUR0sWV0ZoJYfyG-8fCFhB8sQnThoBpkPDnQ7jsd2sZ_NFyp3MNxetQ9rW_kmWlQv9141rUGuQus49fE1jBEvdwVNDqa3DkmadLMyjQkeits",
    val printifyShopDomain: String = "kriticallimitz.printify.me",
    val printifyShopId: String = "26650832",

    // 8. Notifications & Alerts
    val pushNotificationsActive: Boolean = true,
    val notificationLevel: String = "All Alerts", // All Alerts, Approvals Only, Silent
    val notifyPitches: Boolean = true,
    val notifySales: Boolean = true,
    val notifyTrademarkAlerts: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,

    // 9. Privacy, Security & Data Management
    val analyticsTelemetry: Boolean = false,
    val localOnlyStorage: Boolean = true,
    val autoBackupEnabled: Boolean = true
)


// Zero-Cost Autonomous Storefront Entity
@Entity(tableName = "storefronts")
data class StorefrontEntity(
    @PrimaryKey val id: String,
    val companyName: String,
    val businessNiche: String,
    val platformType: String, // GitHub Pages, Static Jamstack, Stripe Payment Link Storefront
    val status: String, // PENDING_APPROVAL, LAUNCHED, ACTIVE, PAUSED
    val repoName: String,
    val repoUrl: String,
    val stripePaymentLink: String,
    val monthlyRevenueCents: Long = 0L,
    val monthlyCostCents: Long = 0L, // Guaranteed 0 upfront
    val productsCount: Int = 0,
    val complianceStatus: String = "VERIFIED_100_CLEAN",
    val setupLog: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Cryptographic Trademark & Copyright Clearance Entity
@Entity(tableName = "trademark_checks")
data class TrademarkCheckEntity(
    @PrimaryKey val id: String,
    val assetTitle: String,
    val assetCategory: String, // POD_DESIGN, STORE_NAME, SLOGAN, PRODUCT_TITLE
    val similarityScore: Float, // 0.0 to 1.0 (0.0 = completely distinct/clean)
    val usptoDbChecked: Boolean = true,
    val copyrightDbChecked: Boolean = true,
    val trademarkCollisions: String, // "0 collisions detected" or details
    val verificationStatus: String, // "CLEARED_SAFE", "REJECTED_COLLISION", "PENDING"
    val clearanceHash: String, // SHA-256 clearance certificate
    val checkedTimestamp: Long = System.currentTimeMillis()
)

// Inter-Agent Communication Bus Entity (Multi-Agent Swarm Collaboration)
@Entity(tableName = "agent_messages")
data class AgentMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderAgentId: String,
    val senderAgentName: String,
    val recipientAgentId: String, // "ALL" or specific agent id
    val messageType: String, // PROPOSAL, PITCH, CLEARANCE_REQUEST, AUDIT_RESPONSE, STOREFRONT_PLAN
    val subject: String,
    val body: String,
    val payloadJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)

// Live Print-on-Demand Order & Mockup Job Entity
@Entity(tableName = "pod_orders")
data class PodOrderEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val product: String,
    val color: String,
    val placement: String,
    val status: String, // QUEUED, PROCESSING, READY, FULFILLED, CANCELLED
    val progressPercent: Int = 0,
    val vectorDpi: Int = 300,
    val customerEmail: String = "",
    val priceCents: Long = 0L,
    val payloadJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)

// Autonomous Zero-Cost Digital Product Entity linked to Whole-Cents Ledger
@Entity(tableName = "digital_products")
data class DigitalProductEntity(
    @PrimaryKey val id: String,
    val sku: String = "SKU-DIGITAL",
    val title: String,
    val niche: String,
    val category: String, // EBOOK, CODE_TEMPLATE, NOTION_SYSTEM, GRAPHIC_PACK, PROMPT_GUIDE
    val description: String,
    val priceCents: Long, // e.g. 1900 ($19.00)
    val costCents: Long = 0L, // Always $0 COGS for digital items
    val profitMarginPercent: Int = 100, // 100% pure profit
    val contentPayload: String, // Full text/markdown or code deliverable
    val fileFormat: String, // Markdown (.md), JSON, Vector SVG, ZIP Bundle
    val downloadUrl: String, // Hosted asset download URL
    val accessToken: String = "", // Secure access token / license key
    val metadataJson: String = "{}", // Extended product metadata
    val stripePaymentLink: String, // Live or sandbox Stripe checkout URL
    val githubRepo: String, // Storefront repository where asset is hosted
    val status: String, // DRAFT, PENDING_APPROVAL, PUBLISHED_LIVE
    val clearanceCertificate: String, // USPTO & Copyright verification hash
    val salesCount: Int = 0,
    val totalRevenueCents: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

typealias PodOrder = PodOrderEntity
typealias LedgerEntry = LedgerEntity
typealias DigitalProduct = DigitalProductEntity

// Interceptor Architecture ("God" Overseer) Verdict Entity
@Entity(tableName = "god_audit_verdicts")
data class GodAuditVerdictEntity(
    @PrimaryKey val id: String, // UUID
    val timestamp: Long = System.currentTimeMillis(),
    val sourceAgent: String, // e.g. "AI Administrator", "Creative Design Agent", "DeploymentService"
    val actionType: String, // "GITHUB_COMMIT", "GITHUB_PULL_REQUEST", "TOOL_EXECUTION", "SYSTEM_COMMAND", "STOREFRONT_DEPLOY"
    val targetResource: String, // e.g. "repo/main/index.html", "admin_tool/post_digital_product"
    val payloadPreview: String,
    val status: String, // "APPROVED" or "HALTED"
    val reason: String? = null,
    val hazardDetected: String? = null,
    val remediation: String? = null,
    val isVetoed: Boolean = false,
    val durationMs: Long = 0L
)

/**
 * Diagnostic record verifying the full install state of the God Overseer Agent.
 */
data class GodInstallDiagnostic(
    val isDatabaseSeeded: Boolean = true,
    val isInterceptorActive: Boolean = true,
    val isNotificationServiceRunning: Boolean = true,
    val isSecurityGateScriptVerified: Boolean = true,
    val isAuditTrailFunctional: Boolean = true,
    val isZeroTrustEnclaveArmed: Boolean = true,
    val totalInterceptCount: Int = 0,
    val statusSummary: String = "God Sentinel: 100% Fully Installed & Armed"
)

enum class LayerType { TEXT, IMAGE }

data class DesignLayer(
    val id: String,
    val type: LayerType,
    val name: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val region: String = "Front Side",
    val colorHex: String = "#FFFFFF",
    val opacity: Float = 1.0f,
    val isVisible: Boolean = true,
    val fontStyle: String = "SANS_SERIF",
    val graphicPreset: String = "retro_mountain"
)

// Pending Transaction Entity requiring manual user approval / review
@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey val id: String,
    val transactionCode: String,
    val title: String,
    val description: String,
    val storeOrEntity: String = "Print Defy Studio",
    val platform: String = "Printify Gateway",
    val vendorOrPayee: String = "Printify Global",
    val accountDebit: String = "5010 Cost of Goods Sold",
    val accountCredit: String = "1010 Operating Cash",
    val amountCents: Long = 0L,
    val category: String = "COGS", // COGS, PAYOUT, VENDOR_EXPENSE, REFUND, SALE
    val riskScore: Int = 12, // 0 - 100
    val riskReason: String = "Automated clearance: verified double-entry balance.",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

