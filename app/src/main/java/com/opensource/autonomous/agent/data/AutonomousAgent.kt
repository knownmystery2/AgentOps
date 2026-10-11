package com.opensource.autonomous.agent.data

import com.opensource.autonomous.agent.network.SwarmAgentStatus

/**
 * Operational status of an autonomous agent in the swarm.
 * Reflects whether the agent is currently driving business workflows (ACTIVE)
 * or waiting in ready-reserve for failover/triggers (STANDBY).
 */
enum class AgentStatus(val label: String, val description: String) {
    ACTIVE("Active", "Agent is currently processing workflows, monitoring live streams, or executing directives"),
    STANDBY("Standby", "Agent is in ready-reserve listening for triggers, failover, or scheduled cron events"),
    PAUSED("Paused", "Agent execution has been explicitly paused by executive intervention"),
    ERROR("Error", "Agent encountered a runtime failure and is awaiting retry or reset");

    val isActive: Boolean get() = this == ACTIVE
    val isStandby: Boolean get() = this == STANDBY

    companion object {
        fun fromString(value: String?): AgentStatus {
            return when (value?.uppercase()?.trim()) {
                "ACTIVE", "RUNNING", "PROCESSING" -> ACTIVE
                "STANDBY", "IDLE", "WAITING", "READY" -> STANDBY
                "PAUSED", "STOPPED" -> PAUSED
                "ERROR", "FAILED" -> ERROR
                else -> STANDBY
            }
        }
    }
}

/**
 * High-level business functional departments within the autonomous enterprise.
 */
enum class BusinessDepartment(val departmentName: String, val code: String) {
    INTERCEPTOR_GATEWAY("Silent Overseer & Security Interceptor", "GOD"),
    EXECUTIVE("Executive & Strategic Operations", "EXEC"),
    MARKET_INTELLIGENCE("Market Intelligence & Niche Scouting", "INTEL"),
    BRAND_STRATEGY("Brand Identity & Voice Architecture", "BRAND"),
    PRODUCT_DESIGN("Product Engineering & POD Mockups", "PROD"),
    CATALOG_SEO("E-Commerce Catalog & SEO Copywriting", "SALES"),
    SUPPLY_CHAIN("Procurement & Zero-Capital Sourcing", "SUPPLY"),
    GROWTH_MARKETING("Organic Acquisition & Funnel Growth", "GROWTH"),
    CUSTOMER_SUCCESS("Customer Support & CRM Operations", "CRM"),
    FINANCE_LEDGER("Financial Accounting & Unit Economics", "FIN"),
    LEGAL_COMPLIANCE("Trademark Audit & Regulatory Defense", "LEGAL"),
    INFRASTRUCTURE("DevOps, Reliability & Cloud Failover", "DEVOPS")
}

/**
 * Operational roles within the business assigned to autonomous agents.
 * Encompasses domain responsibility, standard deliverables, and human-in-the-loop gates.
 */
enum class OperationalRole(
    val title: String,
    val department: BusinessDepartment,
    val description: String,
    val coreResponsibilities: List<String>,
    val requiresHumanApproval: Boolean,
    val defaultPrompt: String
) {
    GOD_OVERSEER(
        title = "The Interceptor Overseer",
        department = BusinessDepartment.INTERCEPTOR_GATEWAY,
        description = "Ultimate, silent, and all-seeing overseer with absolute veto power monitoring outputs before live execution.",
        coreResponsibilities = listOf(
            "Passively intercept all code changes, commands, and deployments out-of-band",
            "Screen for prompt injection, hallucinated loops, and rogue security leaks",
            "Enforce absolute fail-safe halt and provide remediation fixes",
            "Guard zero-cost mandate and repository integrity without peer chatter"
        ),
        requiresHumanApproval = false,
        defaultPrompt = AgentPrompts.GOD_OVERSEER_SYSTEM_PROMPT
    ),

    EXECUTIVE_ORCHESTRATOR(
        title = "Executive Router & Goal Decomposition",
        department = BusinessDepartment.EXECUTIVE,
        description = "Decomposes high-level business goals into micro-tasks, manages priority routing, and oversees system approvals.",
        coreResponsibilities = listOf(
            "Parse primary business prompts into discrete execution graphs",
            "Route tasks to specialized autonomous agents across the swarm",
            "Coordinate multi-agent consensus and synthesize executive briefings",
            "Enforce human-in-the-loop gating before external mutations"
        ),
        requiresHumanApproval = false,
        defaultPrompt = AgentPrompts.CEO_PROMPT
    ),

    MARKET_RESEARCH_SCOUT(
        title = "Trend & Niche Scout",
        department = BusinessDepartment.MARKET_INTELLIGENCE,
        description = "Identifies rising zero-cost e-commerce niches, tracks competitor dynamics, and ranks opportunities by profit margins.",
        coreResponsibilities = listOf(
            "Scrape and score trending e-commerce search queries and buyer demand",
            "Benchmark competitor pricing matrices and identify saturated segments",
            "Calculate estimated unit profit margins across target categories",
            "Produce structured telemetry reports on emerging market gaps"
        ),
        requiresHumanApproval = false,
        defaultPrompt = AgentPrompts.RESEARCH_PROMPT
    ),

    BRAND_IDENTITY_ARCHITECT(
        title = "Brand & Voice Architect",
        department = BusinessDepartment.BRAND_STRATEGY,
        description = "Creates distinct, legally isolated brand identities per niche to guarantee storefront risk separation.",
        coreResponsibilities = listOf(
            "Formulate unique brand names, mission narratives, and target demographic personas",
            "Establish tone-of-voice rules and copywriting style manuals",
            "Generate color palettes and visual guidelines for store assets",
            "Ensure complete cross-store brand containment to insulate liability"
        ),
        requiresHumanApproval = false,
        defaultPrompt = "ROLE: Brand Agent. OBJECTIVE: You create distinct brands per niche to ensure complete risk isolation across storefronts."
    ),

    PRODUCT_DESIGNER_POD(
        title = "POD Design & Mockup Specialist",
        department = BusinessDepartment.PRODUCT_DESIGN,
        description = "Synthesizes high-resolution print-on-demand vector designs, mockups, and merchandise specifications.",
        coreResponsibilities = listOf(
            "Generate original vector artwork tailored for 300 DPI print zones",
            "Map designs to apparel, mug, tote, and poster mockup specifications",
            "Enforce printable color gamuts and bleed boundaries",
            "Output automated production-ready raster/vector bundles"
        ),
        requiresHumanApproval = false,
        defaultPrompt = AgentPrompts.PRODUCT_PROMPT
    ),

    CATALOG_SEO_COPYWRITER(
        title = "Listing & SEO Copywriter",
        department = BusinessDepartment.CATALOG_SEO,
        description = "Drafts high-converting product titles, 13 ETSY/Shopify SEO tags, bullet points, and marketing copy.",
        coreResponsibilities = listOf(
            "Write high-CTR listing titles adhering to algorithmic character limits",
            "Mine and integrate 13 high-volume, long-tail Etsy/Shopify search tags",
            "Author persuasive benefit-driven bullet points and full product descriptions",
            "Update catalog listings dynamically according to seasonal search trends"
        ),
        requiresHumanApproval = false,
        defaultPrompt = AgentPrompts.CONTENT_PROMPT
    ),

    SUPPLIER_PROCUREMENT(
        title = "Dropship Procurement Agent",
        department = BusinessDepartment.SUPPLY_CHAIN,
        description = "Vets and coordinates dropshipping suppliers with guaranteed ZERO upfront cost to the business owner.",
        coreResponsibilities = listOf(
            "Screen dropship suppliers for verified shipping reliability and stock reliability",
            "Enforce strict 0-cost onboarding (no upfront inventory or minimum order fees)",
            "Calculate COGS, supplier shipping surcharges, and gateway fees",
            "Maintain automated supplier failover queues for out-of-stock items"
        ),
        requiresHumanApproval = false,
        defaultPrompt = "ROLE: Dropship Sourcing Agent. OBJECTIVE: You find verified suppliers with guaranteed ZERO upfront cost to the user."
    ),

    ORGANIC_GROWTH_MARKETER(
        title = "Organic Growth & Content Marketer",
        department = BusinessDepartment.GROWTH_MARKETING,
        description = "Engineers viral organic discovery funnels across Pinterest, TikTok, and outbound email without paid ad spend.",
        coreResponsibilities = listOf(
            "Draft high-engagement Pinterest pin headlines and visual copy layouts",
            "Script 15-second viral short-form TikTok and Instagram Reels concepts",
            "Structure automated email welcome and abandoned-cart nurture sequences",
            "Monitor conversion ratios across organic referral channels"
        ),
        requiresHumanApproval = false,
        defaultPrompt = AgentPrompts.MARKETING_PROMPT
    ),

    CUSTOMER_SUCCESS_CRM(
        title = "Customer Service & Support Agent",
        department = BusinessDepartment.CUSTOMER_SUCCESS,
        description = "Resolves inbound customer inquiries empathetically and drafts support responses, escalating refunds for authorization.",
        coreResponsibilities = listOf(
            "Handle shipping status inquiries and tracking link lookups",
            "Draft empathetic resolutions for quality or fulfillment delays",
            "Maintain knowledge base vectors of recurring support questions",
            "CRITICAL: Never authorize refunds without human owner approval"
        ),
        requiresHumanApproval = true,
        defaultPrompt = AgentPrompts.SUPPORT_PROMPT
    ),

    FINANCIAL_CONTROLLER(
        title = "Finance & Ledger Controller",
        department = BusinessDepartment.FINANCE_LEDGER,
        description = "Maintains immutable double-entry ledger in whole cents, calculates unit economics, and compiles tax records.",
        coreResponsibilities = listOf(
            "Record all transactions in integer whole cents (debit/credit zero-variance audit)",
            "Track real-time gross revenue, platform processing fees, and net profit margins",
            "Prepare Schedule C tax categorization exports (income, COGS, deductions)",
            "CRITICAL: Gate all external payouts and bank movements behind human verification"
        ),
        requiresHumanApproval = true,
        defaultPrompt = AgentPrompts.FINANCE_PROMPT
    ),

    COMPLIANCE_LEGAL_AUDITOR(
        title = "Compliance & Trademark Auditor",
        department = BusinessDepartment.LEGAL_COMPLIANCE,
        description = "Screens merchandise, copy, and product titles against USPTO trademark registries and deceptive marketing policies.",
        coreResponsibilities = listOf(
            "Perform automated exact & phonetic trademark searches against USPTO databases",
            "Audit product claims for deceptive advertising or trademark infringements",
            "Flag intellectual property conflicts prior to storefront publication",
            "Archive verifiable audit trails for copyright clearance"
        ),
        requiresHumanApproval = false,
        defaultPrompt = "ROLE: Compliance Agent. OBJECTIVE: You screen designs and titles against USPTO trademark records and deceptive claims."
    ),

    INFRASTRUCTURE_RELIABILITY(
        title = "Reliability & Standby Pool Engineer",
        department = BusinessDepartment.INFRASTRUCTURE,
        description = "Monitors free-tier compute quotas, manages standby failover pools, and executes exponential backoff retries.",
        coreResponsibilities = listOf(
            "Track free cloud tier quotas across HuggingFace, Render, and GitHub Pages",
            "Execute automated 3x retry policies with exponential backoff on network failures",
            "Maintain hot-standby compute account rotation to avoid rate limits",
            "Verify endpoint health ping and trigger self-healing failovers"
        ),
        requiresHumanApproval = false,
        defaultPrompt = "ROLE: Reliability Agent. OBJECTIVE: You monitor free cloud tiers, handle 3x retries with backoff, and manage the standby pool."
    );

    companion object {
        fun fromRoleString(roleStr: String?): OperationalRole {
            if (roleStr.isNullOrBlank()) return EXECUTIVE_ORCHESTRATOR
            val normalized = roleStr.uppercase()
            return when {
                normalized.contains("GOD") || normalized.contains("OVERSEER") || normalized.contains("INTERCEPTOR") -> GOD_OVERSEER
                normalized.contains("ORCHESTRAT") || normalized.contains("EXECUTIVE") || normalized.contains("ROUTER") || normalized.contains("CEO") -> EXECUTIVE_ORCHESTRATOR
                normalized.contains("SCOUT") || normalized.contains("RESEARCH") || normalized.contains("TREND") || normalized.contains("NICHE") -> MARKET_RESEARCH_SCOUT
                normalized.contains("BRAND") || normalized.contains("VOICE") || normalized.contains("IDENTITY") -> BRAND_IDENTITY_ARCHITECT
                normalized.contains("DESIGN") || normalized.contains("MOCKUP") || normalized.contains("PRINT") || normalized.contains("POD") -> PRODUCT_DESIGNER_POD
                normalized.contains("LISTING") || normalized.contains("SEO") || normalized.contains("COPYWRIT") || normalized.contains("CONTENT") -> CATALOG_SEO_COPYWRITER
                normalized.contains("DROPSHIP") || normalized.contains("PROCUREMENT") || normalized.contains("SUPPLI") || normalized.contains("SOURC") -> SUPPLIER_PROCUREMENT
                normalized.contains("MARKET") || normalized.contains("GROWTH") || normalized.contains("ACQUISITION") -> ORGANIC_GROWTH_MARKETER
                normalized.contains("CUSTOMER") || normalized.contains("SUPPORT") || normalized.contains("CRM") || normalized.contains("SERVICE") -> CUSTOMER_SUCCESS_CRM
                normalized.contains("FINANCE") || normalized.contains("LEDGER") || normalized.contains("TAX") || normalized.contains("ACCOUNT") || normalized.contains("CFO") -> FINANCIAL_CONTROLLER
                normalized.contains("COMPLIANCE") || normalized.contains("TRADEMARK") || normalized.contains("LEGAL") || normalized.contains("RISK") -> COMPLIANCE_LEGAL_AUDITOR
                normalized.contains("RELIABILITY") || normalized.contains("STANDBY") || normalized.contains("FAILOVER") || normalized.contains("INFRA") -> INFRASTRUCTURE_RELIABILITY
                else -> EXECUTIVE_ORCHESTRATOR
            }
        }
    }
}

/**
 * Comprehensive Kotlin Data Model representing an Autonomous Agent within the enterprise swarm.
 *
 * @param id Unique identifier of the agent (e.g. "1", "agent-finance")
 * @param name Professional name of the autonomous agent
 * @param role Operational business role with domain responsibilities and safety constraints
 * @param status Operational status indicating whether the agent is ACTIVE or in STANDBY reserve
 * @param department Business department the agent reports to
 * @param modelEngine Underlying LLM engine / runtime model (e.g., "gemini-1.5-flash", "llama3:8b")
 * @param systemPrompt Core operating system prompt dictating behavior and output schemas
 * @param currentTask Currently assigned active or queued operational task description
 * @param completedTasksCount Number of successful tasks executed by this agent
 * @param errorCount Number of failed or retried tasks
 * @param lastActiveTimestamp Epoch timestamp of the last heartbeat or operational action
 * @param successRate Historical reliability ratio between 0.0 and 1.0
 * @param capabilities List of concrete capabilities (e.g. "USPTO_CHECK", "LEDGER_WRITE")
 * @param requiresHumanApproval Whether critical actions by this agent require human approval
 * @param uptimeSeconds Total elapsed operational runtime in seconds
 * @param metadata Additional runtime key-value attributes
 */
data class AutonomousAgent(
    val id: String,
    val name: String,
    val role: OperationalRole,
    val status: AgentStatus = AgentStatus.ACTIVE,
    val department: BusinessDepartment = role.department,
    val modelEngine: String = "llama3:8b",
    val systemPrompt: String = role.defaultPrompt,
    val currentTask: String? = null,
    val completedTasksCount: Int = 0,
    val errorCount: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val successRate: Float = 1.0f,
    val capabilities: List<String> = emptyList(),
    val requiresHumanApproval: Boolean = role.requiresHumanApproval,
    val uptimeSeconds: Long = 0L,
    val metadata: Map<String, String> = emptyMap()
) {
    /** Whether the agent is actively processing or monitoring */
    val isActive: Boolean get() = status == AgentStatus.ACTIVE

    /** Whether the agent is in standby ready-reserve */
    val isStandby: Boolean get() = status == AgentStatus.STANDBY

    /** Human-readable status indicator badge text */
    val statusBadge: String get() = when (status) {
        AgentStatus.ACTIVE -> "ACTIVE"
        AgentStatus.STANDBY -> "STANDBY"
        AgentStatus.PAUSED -> "PAUSED"
        AgentStatus.ERROR -> "ERROR"
    }

    /**
     * Converts this rich domain model into a Room AgentEntity for local database persistence.
     */
    fun toEntity(): AgentEntity {
        return AgentEntity(
            id = id,
            name = name,
            role = role.title,
            systemPrompt = systemPrompt,
            isActive = (status == AgentStatus.ACTIVE)
        )
    }

    /**
     * Converts this rich domain model into a SwarmAgentStatus for live network console streaming.
     */
    fun toSwarmStatus(): SwarmAgentStatus {
        return SwarmAgentStatus(
            id = id,
            name = name,
            role = role.title,
            status = status.name,
            model = modelEngine,
            currentTask = currentTask ?: if (status == AgentStatus.ACTIVE) "Monitoring operations" else "Standby reserve",
            lastPingTimestamp = lastActiveTimestamp,
            latencyMs = if (status == AgentStatus.ACTIVE) 14L else 0L
        )
    }

    companion object {
        /**
         * Reconstructs an AutonomousAgent domain model from a Room database AgentEntity.
         */
        fun fromEntity(
            entity: AgentEntity,
            statusOverride: AgentStatus? = null
        ): AutonomousAgent {
            val role = OperationalRole.fromRoleString(entity.role)
            val resolvedStatus = statusOverride ?: if (entity.isActive) AgentStatus.ACTIVE else AgentStatus.STANDBY
            return AutonomousAgent(
                id = entity.id,
                name = entity.name,
                role = role,
                status = resolvedStatus,
                department = role.department,
                systemPrompt = entity.systemPrompt.ifBlank { role.defaultPrompt }
            )
        }

        /**
         * Reconstructs an AutonomousAgent domain model from a network SwarmAgentStatus item.
         */
        fun fromSwarmStatus(swarm: SwarmAgentStatus): AutonomousAgent {
            val role = OperationalRole.fromRoleString(swarm.role)
            val status = AgentStatus.fromString(swarm.status)
            return AutonomousAgent(
                id = swarm.id,
                name = swarm.name,
                role = role,
                status = status,
                department = role.department,
                modelEngine = swarm.model,
                currentTask = swarm.currentTask,
                lastActiveTimestamp = swarm.lastPingTimestamp
            )
        }

        /**
         * The standard autonomous enterprise swarm with GOD Overseer and specialized agents.
         */
        val DEFAULT_SWARM: List<AutonomousAgent> = listOf(
            AutonomousAgent(
                id = "0",
                name = "GOD",
                role = OperationalRole.GOD_OVERSEER,
                status = AgentStatus.ACTIVE,
                modelEngine = "gemini-2.5-pro (Interceptor)",
                currentTask = "Silent surveillance: Intercepting live actions before execution",
                capabilities = listOf("ABSOLUTE_VETO", "PROMPT_INJECTION_SHIELD", "FAIL_SAFE_HALT", "ROGUE_KEY_BLOCK", "ZERO_COST_GUARD")
            ),
            AutonomousAgent(
                id = "1",
                name = "Orchestrator Agent",
                role = OperationalRole.EXECUTIVE_ORCHESTRATOR,
                status = AgentStatus.ACTIVE,
                modelEngine = "llama3:8b",
                currentTask = "Monitoring swarm task queue & router",
                capabilities = listOf("TASK_ROUTING", "BRIEFING_GENERATION", "DECISION_ORCHESTRATION")
            ),

            AutonomousAgent(
                id = "2",
                name = "Trend & Niche Scout",
                role = OperationalRole.MARKET_RESEARCH_SCOUT,
                status = AgentStatus.ACTIVE,
                modelEngine = "llama3:8b",
                currentTask = "Tracking trending zero-cost niches",
                capabilities = listOf("MARKET_SCRAPING", "NICHE_SCORING", "COMPETITOR_BENCHMARKING")
            ),
            AutonomousAgent(
                id = "3",
                name = "Brand Agent",
                role = OperationalRole.BRAND_IDENTITY_ARCHITECT,
                status = AgentStatus.STANDBY,
                modelEngine = "llama3:8b",
                currentTask = "Standby: Awaiting new niche pipeline",
                capabilities = listOf("BRAND_NAMING", "VOICE_GUIDELINES", "RISK_CONTAINMENT")
            ),
            AutonomousAgent(
                id = "4",
                name = "POD Design Agent",
                role = OperationalRole.PRODUCT_DESIGNER_POD,
                status = AgentStatus.ACTIVE,
                modelEngine = "mistral:7b",
                currentTask = "Generating 300 DPI vector artwork mockups",
                capabilities = listOf("VECTOR_SYNTHESIS", "MOCKUP_MAPPING", "BLEED_VALIDATION")
            ),
            AutonomousAgent(
                id = "5",
                name = "Listing & SEO Agent",
                role = OperationalRole.CATALOG_SEO_COPYWRITER,
                status = AgentStatus.ACTIVE,
                modelEngine = "llama3:8b",
                currentTask = "Optimizing Etsy/Shopify metadata & 13 SEO tags",
                capabilities = listOf("SEO_TAG_MINING", "TITLE_COPYWRITING", "BULLET_OPTIMIZATION")
            ),
            AutonomousAgent(
                id = "6",
                name = "Dropship Sourcing Agent",
                role = OperationalRole.SUPPLIER_PROCUREMENT,
                status = AgentStatus.STANDBY,
                modelEngine = "mistral:7b",
                currentTask = "Standby: Verified supplier catalog synced",
                capabilities = listOf("SUPPLIER_VETTING", "COGS_CALCULATION", "ZERO_UPFRONT_VALIDATION")
            ),
            AutonomousAgent(
                id = "7",
                name = "Marketing Agent",
                role = OperationalRole.ORGANIC_GROWTH_MARKETER,
                status = AgentStatus.ACTIVE,
                modelEngine = "llama3:8b",
                currentTask = "Producing organic Pinterest pins & TikTok scripts",
                capabilities = listOf("PIN_CREATION", "REEL_SCRIPTING", "OUTBOUND_EMAIL_FUNNELS")
            ),
            AutonomousAgent(
                id = "8",
                name = "Customer Service Agent",
                role = OperationalRole.CUSTOMER_SUCCESS_CRM,
                status = AgentStatus.STANDBY,
                modelEngine = "mistral:7b",
                currentTask = "Standby: Listening for customer inquiry webhooks",
                capabilities = listOf("SUPPORT_TRIAGE", "EMPATHETIC_REPLY", "REFUND_GATEWAY")
            ),
            AutonomousAgent(
                id = "9",
                name = "Finance Agent",
                role = OperationalRole.FINANCIAL_CONTROLLER,
                status = AgentStatus.ACTIVE,
                modelEngine = "deepseek-coder:6.7b",
                currentTask = "Auditing double-entry ledger & whole cent balances",
                capabilities = listOf("LEDGER_AUDITING", "UNIT_ECONOMICS", "SCHEDULE_C_TAX")
            ),
            AutonomousAgent(
                id = "10",
                name = "Compliance Agent",
                role = OperationalRole.COMPLIANCE_LEGAL_AUDITOR,
                status = AgentStatus.ACTIVE,
                modelEngine = "open-source-ip-filter",
                currentTask = "Screening merchandise against USPTO records",
                capabilities = listOf("USPTO_TRADEMARK_LOOKUP", "DECEPTIVE_CLAIM_AUDIT", "IP_CLEARANCE")
            ),
            AutonomousAgent(
                id = "11",
                name = "Reliability Agent",
                role = OperationalRole.INFRASTRUCTURE_RELIABILITY,
                status = AgentStatus.ACTIVE,
                modelEngine = "local-open-weights",
                currentTask = "Monitoring free-tier quotas & failover pool",
                capabilities = listOf("QUOTA_MONITORING", "BACKOFF_RETRY", "FAILOVER_ROTATION")
            )
        )
    }
}
