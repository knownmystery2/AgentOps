package com.opensource.autonomous.agent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.SettingsEntity
import com.opensource.autonomous.agent.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentSettings by viewModel.settings.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTimestamp.collectAsState()
    val rawJsonSchema by viewModel.rawJsonSchemaDialogContent.collectAsState()
    val pingStatus by viewModel.pingStatus.collectAsState()
    val pingLatency by viewModel.pingLatencyMs.collectAsState()
    val isTestingPing by viewModel.isTestingPing.collectAsState()

    // 0 = General Settings, 1 = Advanced Developer Settings
    var settingsLayerIndex by remember { mutableStateOf(0) }

    // General state bindings
    var globalAutomationActive by remember(currentSettings) {
        mutableStateOf(currentSettings?.globalAutomationActive ?: true)
    }
    var notificationLevel by remember(currentSettings) {
        mutableStateOf(currentSettings?.notificationLevel ?: "All Alerts")
    }
    var productFrequency by remember(currentSettings) {
        mutableStateOf(currentSettings?.productFrequency ?: "Daily")
    }
    var riskLevel by remember(currentSettings) {
        mutableStateOf(currentSettings?.riskLevel ?: "Balanced")
    }

    // Advanced state bindings
    var backendServerEndpoint by remember(currentSettings) {
        mutableStateOf(currentSettings?.backendServerEndpoint ?: "http://127.0.0.1:8080")
    }
    var ollamaEndpoint by remember(currentSettings) {
        mutableStateOf(currentSettings?.ollamaEndpoint ?: "http://127.0.0.1:11434")
    }

    // Database Credentials state (MariaDB / PostgreSQL on Termux)
    var dbType by remember { mutableStateOf("MariaDB (Termux Localhost)") }
    var dbHost by remember { mutableStateOf("127.0.0.1") }
    var dbPort by remember { mutableStateOf("3306") }
    var dbName by remember { mutableStateOf("agent_biz") }
    var dbUser by remember { mutableStateOf("root") }

    // Per-Agent Models
    var modelCeo by remember(currentSettings) { mutableStateOf(currentSettings?.modelCeo ?: "llama3") }
    var modelResearch by remember(currentSettings) { mutableStateOf(currentSettings?.modelResearch ?: "llama3") }
    var modelProduct by remember(currentSettings) { mutableStateOf(currentSettings?.modelProduct ?: "llama3") }
    var modelMarketing by remember(currentSettings) { mutableStateOf(currentSettings?.modelMarketing ?: "llama3") }
    var modelContent by remember(currentSettings) { mutableStateOf(currentSettings?.modelContent ?: "llama3") }
    var modelSupport by remember(currentSettings) { mutableStateOf(currentSettings?.modelSupport ?: "llama3") }
    var modelFinance by remember(currentSettings) { mutableStateOf(currentSettings?.modelFinance ?: "llama3") }

    // Raw Key Storage
    var keyGithub by remember(currentSettings) { mutableStateOf(currentSettings?.keyGithub ?: "") }
    var keyStripe by remember(currentSettings) { mutableStateOf(currentSettings?.keyStripe ?: "") }
    var keyGroq by remember(currentSettings) { mutableStateOf(currentSettings?.keyGroq ?: "") }
    var keyHuggingFace by remember(currentSettings) { mutableStateOf(currentSettings?.keyHuggingFace ?: "") }
    var keyOpenRouter by remember(currentSettings) { mutableStateOf(currentSettings?.keyOpenRouter ?: "") }
    var keyGemini by remember(currentSettings) { mutableStateOf(currentSettings?.keyGemini ?: "") }

    // Bypass Flags
    var bypassApprovals by remember(currentSettings) { mutableStateOf(currentSettings?.bypassApprovals ?: false) }
    var bypassRateLimits by remember(currentSettings) { mutableStateOf(currentSettings?.bypassRateLimits ?: false) }
    var offlineSimulationMode by remember(currentSettings) { mutableStateOf(currentSettings?.offlineSimulationMode ?: false) }

    // Key visibility toggles
    var showGithubKey by remember { mutableStateOf(false) }
    var showStripeKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }
    var showHfKey by remember { mutableStateOf(false) }
    var showOrKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }

    fun commitSettings() {
        val updated = SettingsEntity(
            globalAutomationActive = globalAutomationActive,
            notificationLevel = notificationLevel,
            productFrequency = productFrequency,
            riskLevel = riskLevel,
            backendServerEndpoint = backendServerEndpoint,
            ollamaEndpoint = ollamaEndpoint,
            modelCeo = modelCeo,
            modelResearch = modelResearch,
            modelProduct = modelProduct,
            modelMarketing = modelMarketing,
            modelContent = modelContent,
            modelSupport = modelSupport,
            modelFinance = modelFinance,
            keyGithub = keyGithub,
            keyStripe = keyStripe,
            keyGroq = keyGroq,
            keyHuggingFace = keyHuggingFace,
            keyOpenRouter = keyOpenRouter,
            keyGemini = keyGemini,
            bypassApprovals = bypassApprovals,
            bypassRateLimits = bypassRateLimits,
            offlineSimulationMode = offlineSimulationMode
        )
        viewModel.updateSettings(updated)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Column {
                Text(
                    text = "System Configuration Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Open-source Linux Termux host configuration, database credentials & latency healthcheck.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Endpoint Health Status Banner with Healthcheck Ping Test
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLiveConnected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("live_server_status_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isLiveConnected) "Termux Live Endpoint Connected" else "Local DB Engine Standby",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Target: $backendServerEndpoint",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.testPingConnection(backendServerEndpoint) },
                            enabled = !isTestingPing,
                            modifier = Modifier.testTag("ping_test_button")
                        ) {
                            if (isTestingPing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping Test", fontSize = 12.sp)
                        }
                    }

                    pingStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B)).copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                pingLatency?.let { lat ->
                                    Text(
                                        text = "${lat}ms round-trip",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dual-Layer Switch Tab Row (General vs Advanced Developer)
        item {
            TabRow(
                selectedTabIndex = settingsLayerIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = settingsLayerIndex == 0,
                    onClick = { settingsLayerIndex = 0 },
                    text = { Text("General Settings", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                )
                Tab(
                    selected = settingsLayerIndex == 1,
                    onClick = { settingsLayerIndex = 1 },
                    text = { Text("Developer Settings", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) }
                )
            }
        }

        // LAYER 1: GENERAL SETTINGS
        if (settingsLayerIndex == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("general_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = BrandEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Primary Operating Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        // 1. Global Master Automation Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Global Automation Loop", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Master switch to pause/resume background agent swarm tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = globalAutomationActive,
                                onCheckedChange = {
                                    globalAutomationActive = it
                                    commitSettings()
                                },
                                modifier = Modifier.testTag("global_automation_switch")
                            )
                        }

                        HorizontalDivider()

                        // 2. Notification Level
                        Column {
                            Text("Notification Alert Level", fontWeight = FontWeight.SemiBold)
                            Text("Controls phone alerts during autonomous cycles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("All Alerts", "Approvals Only", "Silent").forEach { level ->
                                    FilterChip(
                                        selected = notificationLevel == level,
                                        onClick = {
                                            notificationLevel = level
                                            commitSettings()
                                        },
                                        label = { Text(level) },
                                        modifier = Modifier.testTag("notif_level_$level")
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 3. Product Frequency
                        Column {
                            Text("Listing Generation Frequency", fontWeight = FontWeight.SemiBold)
                            Text("How frequently POD and dropshipping designs are proposed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Every 12h", "Daily", "Weekly").forEach { freq ->
                                    FilterChip(
                                        selected = productFrequency == freq,
                                        onClick = {
                                            productFrequency = freq
                                            commitSettings()
                                        },
                                        label = { Text(freq) },
                                        modifier = Modifier.testTag("freq_$freq")
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 4. Risk Level Safety Gauge
                        Column {
                            Text("Risk Level Safety Gauge", fontWeight = FontWeight.SemiBold)
                            Text("Controls trend velocity versus trademark risk thresholds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Conservative", "Balanced", "Aggressive").forEach { risk ->
                                    FilterChip(
                                        selected = riskLevel == risk,
                                        onClick = {
                                            riskLevel = risk
                                            commitSettings()
                                        },
                                        label = { Text(risk) },
                                        modifier = Modifier.testTag("risk_level_$risk")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Core Function Modules Overview
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("All 8 Modules Live Integration Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        ModuleQuickRow("Console", "Live stdout/stderr stream, uptime counter & queue telemetry")
                        ModuleQuickRow("Swarm", "11 autonomous agents with live heartbeat & direct directive dispatch")
                        ModuleQuickRow("Approvals", "Zero-Trust HMAC SHA-256 verification with immediate REST dispatch")
                        ModuleQuickRow("POD Studio", "300 DPI vector rendering pipeline & live SKU creation tracking")
                        ModuleQuickRow("Dropship", "Real-time supplier feed sync, inventory stock & $0 order routing")
                        ModuleQuickRow("Ledger", "Double-entry whole-cent audit trail with cryptographic hash tokens")
                        ModuleQuickRow("Standby", "Master daemon sleep/wake control & cron schedule dispatch")
                        ModuleQuickRow("Settings", "Custom Termux localhost port & MariaDB/PostgreSQL credentials")
                    }
                }
            }
        }

        // LAYER 2: ADVANCED DEVELOPER SETTINGS
        if (settingsLayerIndex == 1) {
            // 1. Termux Host & Port Configuration Presets
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("endpoint_overrides_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lan, contentDescription = null, tint = BrandEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Termux Host & Port Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Configure the background server URL. Select quick presets for phone Termux or Android emulator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    backendServerEndpoint = "http://127.0.0.1:8080"
                                    commitSettings()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Termux (127.0.0.1)", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    backendServerEndpoint = "http://10.0.2.2:8080"
                                    commitSettings()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Emulator (10.0.2.2)", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = backendServerEndpoint,
                            onValueChange = {
                                backendServerEndpoint = it
                                commitSettings()
                            },
                            label = { Text("Background Server Endpoint (Port 8080)") },
                            modifier = Modifier.fillMaxWidth().testTag("backend_endpoint_input"),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.testPingConnection(backendServerEndpoint) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save & Test")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = ollamaEndpoint,
                            onValueChange = {
                                ollamaEndpoint = it
                                commitSettings()
                            },
                            label = { Text("Ollama Local Inference Server (Port 11434)") },
                            modifier = Modifier.fillMaxWidth().testTag("ollama_endpoint_input")
                        )
                    }
                }
            }

            // 2. Open-Source Linux Database Credentials (Termux MariaDB / PostgreSQL)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("database_credentials_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = ProfitGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Database Credentials (Termux Linux)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Connects to open-source MariaDB or PostgreSQL running on Termux port 8080/3306.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = dbType.contains("MariaDB"),
                                onClick = { dbType = "MariaDB (Termux Localhost)"; dbPort = "3306" },
                                label = { Text("MariaDB (Port 3306)") }
                            )
                            FilterChip(
                                selected = dbType.contains("PostgreSQL"),
                                onClick = { dbType = "PostgreSQL (Termux Localhost)"; dbPort = "5432" },
                                label = { Text("PostgreSQL (Port 5432)") }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dbHost,
                                onValueChange = { dbHost = it },
                                label = { Text("Host") },
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = dbPort,
                                onValueChange = { dbPort = it },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dbName,
                                onValueChange = { dbName = it },
                                label = { Text("Database") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dbUser,
                                onValueChange = { dbUser = it },
                                label = { Text("Username") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = { viewModel.forceDatabaseSync() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Database Connection")
                        }
                    }
                }
            }

            // 3. Model Customization Per Agent
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("model_customization_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agent Model Customization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Customize open-source LLM model strings per agent (e.g. llama3-8b, mistral-7b).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ModelConfigField("Agent 1 (CEO Router):", modelCeo) { modelCeo = it; commitSettings() }
                        ModelConfigField("Agent 2 (Research):", modelResearch) { modelResearch = it; commitSettings() }
                        ModelConfigField("Agent 3 (Product/Dev):", modelProduct) { modelProduct = it; commitSettings() }
                        ModelConfigField("Agent 4 (Marketing):", modelMarketing) { modelMarketing = it; commitSettings() }
                        ModelConfigField("Agent 5 (Content Copy):", modelContent) { modelContent = it; commitSettings() }
                        ModelConfigField("Agent 6 (Support CRM):", modelSupport) { modelSupport = it; commitSettings() }
                        ModelConfigField("Agent 7 (Finance CFO):", modelFinance) { modelFinance = it; commitSettings() }
                    }
                }
            }

            // 4. Raw Key Storage (Secure mask fields)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("raw_keys_storage_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Raw Key Storage (Secure On-Device)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Stored directly in your local SQLite Room database. Never sent to third-party telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SecureKeyField("GitHub Personal Access Token", keyGithub, showGithubKey, { keyGithub = it; commitSettings() }) { showGithubKey = !showGithubKey }
                        SecureKeyField("Stripe Secret API Key", keyStripe, showStripeKey, { keyStripe = it; commitSettings() }) { showStripeKey = !showStripeKey }
                        SecureKeyField("Groq Cloud API Key", keyGroq, showGroqKey, { keyGroq = it; commitSettings() }) { showGroqKey = !showGroqKey }
                        SecureKeyField("HuggingFace User Token", keyHuggingFace, showHfKey, { keyHuggingFace = it; commitSettings() }) { showHfKey = !showHfKey }
                        SecureKeyField("OpenRouter API Key", keyOpenRouter, showOrKey, { keyOpenRouter = it; commitSettings() }) { showOrKey = !showOrKey }
                        SecureKeyField("Gemini API Key", keyGemini, showGeminiKey, { keyGemini = it; commitSettings() }) { showGeminiKey = !showGeminiKey }
                    }
                }
            }

            // 5. Database Schema Inspector
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Local Schema Architecture", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Room SQLite schema version 3 with 10 tables: agents, tasks, decisions, logs, ledger_entries, dropship_catalog, account_tasks, standby_providers, business_settings, knowledge_base.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { viewModel.viewRawJsonSchema() }) {
                            Icon(Icons.Default.Schema, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Raw JSON Schema")
                        }
                    }
                }
            }

            // 6. Bypass System Flags
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("bypass_flags_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bypass System Flags (Developer Testing)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bypass Human Approval Gates", fontWeight = FontWeight.SemiBold)
                                Text("Force-auto-approves low-risk listings without prompt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = bypassApprovals,
                                onCheckedChange = {
                                    bypassApprovals = it
                                    commitSettings()
                                }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bypass Rate Limits & Retries", fontWeight = FontWeight.SemiBold)
                                Text("Disables 3-step exponential backoff wait", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = bypassRateLimits,
                                onCheckedChange = {
                                    bypassRateLimits = it
                                    commitSettings()
                                }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Offline Simulation Mode", fontWeight = FontWeight.SemiBold)
                                Text("Simulate all LLM inference without calling Ollama", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = offlineSimulationMode,
                                onCheckedChange = {
                                    offlineSimulationMode = it
                                    commitSettings()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Raw JSON Schema Dialog
    rawJsonSchema?.let { schemaContent ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissJsonSchemaDialog() },
            title = { Text("Raw Database JSON Schema", fontWeight = FontWeight.Bold) },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = schemaContent,
                        color = Color(0xFF00FF66),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissJsonSchemaDialog() }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ModuleQuickRow(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = BrandEmerald.copy(alpha = 0.2f),
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ModelConfigField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(160.dp),
            singleLine = true
        )
    }
}

@Composable
fun SecureKeyField(
    label: String,
    value: String,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Visibility"
                )
            }
        }
    )
}
