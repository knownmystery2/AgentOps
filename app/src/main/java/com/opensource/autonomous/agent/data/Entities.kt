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

// Zero Upfront Cost Dropshipping Sourcing Shortlist
@Entity(tableName = "dropship_catalog")
data class DropshipEntity(
    @PrimaryKey val id: String,
    val title: String,
    val niche: String,
    val supplierName: String,
    val supplierUrl: String,
    val costCents: Long,
    val retailPriceCents: Long,
    val feeCents: Long,
    val profitCents: Long,
    val shippingDays: String,
    val supplierRating: Float,
    val zeroUpfrontCost: Boolean = true,
    val inShortlist: Boolean = true
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

    // 4. Per-Agent Model Customization (Free Tier & Local)
    val modelCeo: String = "gemini-2.5-flash",
    val modelResearch: String = "llama-3-70b",
    val modelProduct: String = "mistral-large",
    val modelMarketing: String = "llama-3-8b",
    val modelContent: String = "groq-mixtral",
    val modelSupport: String = "gemini-2.5-flash",
    val modelFinance: String = "deepseek-coder",

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

// Autonomous Zero-Cost Digital Product Entity
@Entity(tableName = "digital_products")
data class DigitalProductEntity(
    @PrimaryKey val id: String,
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
    val stripePaymentLink: String, // Live or sandbox Stripe checkout URL
    val githubRepo: String, // Storefront repository where asset is hosted
    val status: String, // DRAFT, PENDING_APPROVAL, PUBLISHED_LIVE
    val clearanceCertificate: String, // USPTO & Copyright verification hash
    val salesCount: Int = 0,
    val totalRevenueCents: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
