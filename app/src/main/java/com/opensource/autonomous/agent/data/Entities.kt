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

// User Written Design Specs & Autonomous Settings
@Entity(tableName = "business_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val allowedStyles: String = "Minimalist line art, Vintage botanical, Retro typographic 70s, Cyber-grid geometric, Cottagecore watercolor",
    val forbiddenItems: String = "Trademarked logos, Disney/Marvel characters, celebrity names, copyrighted song lyrics, brand slogans",
    val preferredPalettes: String = "Earthy terracotta, Charcoal slate, Pastel sage, Amber gold, Monochrome ink",
    val ollamaEndpoint: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "llama3",
    val minMarginPercent: Int = 35
)
