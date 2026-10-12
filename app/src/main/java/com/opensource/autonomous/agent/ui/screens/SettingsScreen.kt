package com.opensource.autonomous.agent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.AgentStatus
import com.opensource.autonomous.agent.data.AutonomousAgent
import com.opensource.autonomous.agent.data.SettingsEntity
import com.opensource.autonomous.agent.ui.AutonomousAgentInspectorDialog
import com.opensource.autonomous.agent.ui.MainViewModel

enum class SettingsCategory(val label: String, val icon: ImageVector) {
    ALL("All Options", Icons.Default.Tune),
    ACCOUNT("Account & Brand", Icons.Default.AccountCircle),
    APPEARANCE("Appearance", Icons.Default.Palette),
    AGENT_SWARM("Agent Swarm", Icons.Default.SmartToy),
    STOREFRONT("Storefront & eCom", Icons.Default.Storefront),
    TRADEMARK("Legal & Copyright", Icons.Default.VerifiedUser),
    CREDENTIALS("Secrets & Keys", Icons.Default.Key),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    PRIVACY("Privacy & Storage", Icons.Default.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentSettings by viewModel.settings.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val rawJsonSchema by viewModel.rawJsonSchemaDialogContent.collectAsState()
    val pingStatus by viewModel.pingStatus.collectAsState()
    val pingLatency by viewModel.pingLatencyMs.collectAsState()
    val isTestingPing by viewModel.isTestingPing.collectAsState()
    val secretStatus by viewModel.secretStatus.collectAsState()
    val dbMaintenanceState by viewModel.databaseMaintenanceState.collectAsState()
    val printifyConnectionStatus by viewModel.printifyConnectionStatus.collectAsState()
    val isTestingPrintify by viewModel.isTestingPrintify.collectAsState()

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var showResetDialog by remember { mutableStateOf(false) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val autonomousAgents by viewModel.autonomousAgents.collectAsState()
    var inspectingAgentInSettings by remember { mutableStateOf<AutonomousAgent?>(null) }

    // 1. Account & Profile State
    var businessName by remember(currentSettings) { mutableStateOf(currentSettings?.businessName ?: "Autonomous Studio") }
    var operatorAlias by remember(currentSettings) { mutableStateOf(currentSettings?.operatorAlias ?: "Lead Founder") }
    var supportEmail by remember(currentSettings) { mutableStateOf(currentSettings?.supportEmail ?: "ops@autonomous.studio") }
    var storefrontProvider by remember(currentSettings) { mutableStateOf(currentSettings?.storefrontProvider ?: "GitHub Pages + Stripe") }
    var requireBiometricPin by remember(currentSettings) { mutableStateOf(currentSettings?.requireBiometricPin ?: false) }

    // 2. Appearance & Display State
    var themeMode by remember(currentSettings) { mutableStateOf(currentSettings?.themeMode ?: "Dark") }
    var amoledMode by remember(currentSettings) { mutableStateOf(currentSettings?.amoledMode ?: true) }
    var accentColor by remember(currentSettings) { mutableStateOf(currentSettings?.accentColor ?: "Emerald") }
    var compactDensity by remember(currentSettings) { mutableStateOf(currentSettings?.compactDensity ?: false) }
    var currencySymbol by remember(currentSettings) { mutableStateOf(currentSettings?.currencySymbol ?: "USD ($)") }
    var uiAnimationsEnabled by remember(currentSettings) { mutableStateOf(currentSettings?.uiAnimationsEnabled ?: true) }

    // 3. Agent Swarm State
    var globalAutomationActive by remember(currentSettings) { mutableStateOf(currentSettings?.globalAutomationActive ?: true) }
    var productFrequency by remember(currentSettings) { mutableStateOf(currentSettings?.productFrequency ?: "Daily") }
    var riskLevel by remember(currentSettings) { mutableStateOf(currentSettings?.riskLevel ?: "Balanced") }
    var minMarginPercent by remember(currentSettings) { mutableStateOf(currentSettings?.minMarginPercent ?: 35) }
    var autoPitchGeneration by remember(currentSettings) { mutableStateOf(currentSettings?.autoPitchGeneration ?: true) }
    var maxDailyCreations by remember(currentSettings) { mutableStateOf(currentSettings?.maxDailyCreations ?: 8) }
    var enableAgentChatLogs by remember(currentSettings) { mutableStateOf(currentSettings?.enableAgentChatLogs ?: true) }
    var bypassApprovals by remember(currentSettings) { mutableStateOf(currentSettings?.bypassApprovals ?: false) }
    var bypassRateLimits by remember(currentSettings) { mutableStateOf(currentSettings?.bypassRateLimits ?: false) }
    var offlineSimulationMode by remember(currentSettings) { mutableStateOf(currentSettings?.offlineSimulationMode ?: false) }

    // Per-Agent Models
    var modelCeo by remember(currentSettings) { mutableStateOf(currentSettings?.modelCeo ?: "gemini-2.5-flash") }
    var modelResearch by remember(currentSettings) { mutableStateOf(currentSettings?.modelResearch ?: "llama-3-70b") }
    var modelProduct by remember(currentSettings) { mutableStateOf(currentSettings?.modelProduct ?: "mistral-large") }
    var modelMarketing by remember(currentSettings) { mutableStateOf(currentSettings?.modelMarketing ?: "llama-3-8b") }
    var modelContent by remember(currentSettings) { mutableStateOf(currentSettings?.modelContent ?: "groq-mixtral") }
    var modelSupport by remember(currentSettings) { mutableStateOf(currentSettings?.modelSupport ?: "gemini-2.5-flash") }
    var modelFinance by remember(currentSettings) { mutableStateOf(currentSettings?.modelFinance ?: "deepseek-coder") }

    // 4. Storefront & eCommerce State
    var autoDeployOnApproval by remember(currentSettings) { mutableStateOf(currentSettings?.autoDeployOnApproval ?: true) }
    var instantStripeCheckout by remember(currentSettings) { mutableStateOf(currentSettings?.instantStripeCheckout ?: true) }
    var githubRepoVisibility by remember(currentSettings) { mutableStateOf(currentSettings?.githubRepoVisibility ?: "public") }
    var digitalProductFormats by remember(currentSettings) {
        mutableStateOf(currentSettings?.digitalProductFormats ?: "Markdown Guides, SVG Vectors, Code Snippets, Presets, Boilerplates")
    }
    var podProviderSandbox by remember(currentSettings) { mutableStateOf(currentSettings?.podProviderSandbox ?: "Direct JAMStack Mock") }

    // 5. Trademark & Copyright State
    var strictCopyrightCheck by remember(currentSettings) { mutableStateOf(currentSettings?.strictCopyrightCheck ?: true) }
    var trademarkConfidenceMin by remember(currentSettings) { mutableStateOf(currentSettings?.trademarkConfidenceMin ?: 98) }
    var autoClearanceCertificate by remember(currentSettings) { mutableStateOf(currentSettings?.autoClearanceCertificate ?: true) }
    var forbiddenItems by remember(currentSettings) {
        mutableStateOf(currentSettings?.forbiddenItems ?: "Trademarked logos, Disney/Marvel characters, celebrity names, copyrighted song lyrics, brand slogans")
    }
    var allowedStyles by remember(currentSettings) {
        mutableStateOf(currentSettings?.allowedStyles ?: "Minimalist line art, Vintage botanical, Retro typographic 70s, Cyber-grid geometric, Cottagecore watercolor")
    }
    var preferredPalettes by remember(currentSettings) {
        mutableStateOf(currentSettings?.preferredPalettes ?: "Earthy terracotta, Charcoal slate, Pastel sage, Amber gold, Monochrome ink")
    }

    // 6. SecretManager & Credentials State
    var backendServerEndpoint by remember(currentSettings) { mutableStateOf(currentSettings?.backendServerEndpoint ?: "http://10.0.2.2:8080") }
    var ollamaEndpoint by remember(currentSettings) { mutableStateOf(currentSettings?.ollamaEndpoint ?: "http://10.0.2.2:11434") }
    var ollamaModel by remember(currentSettings) { mutableStateOf(currentSettings?.ollamaModel ?: "llama3") }
    var keyGithub by remember(currentSettings) { mutableStateOf(currentSettings?.keyGithub ?: "") }
    var keyStripe by remember(currentSettings) { mutableStateOf(currentSettings?.keyStripe ?: "") }
    var keyStripePublishable by remember(currentSettings) { mutableStateOf(currentSettings?.keyStripePublishable ?: "") }
    var keyGroq by remember(currentSettings) { mutableStateOf(currentSettings?.keyGroq ?: "") }
    var keyHuggingFace by remember(currentSettings) { mutableStateOf(currentSettings?.keyHuggingFace ?: "") }
    var keyOpenRouter by remember(currentSettings) { mutableStateOf(currentSettings?.keyOpenRouter ?: "") }
    var keyGemini by remember(currentSettings) { mutableStateOf(currentSettings?.keyGemini ?: "") }
    var keyPrintify by remember(currentSettings) { mutableStateOf(currentSettings?.keyPrintify ?: "") }
    var printifyShopDomain by remember(currentSettings) { mutableStateOf(currentSettings?.printifyShopDomain ?: "kriticallimitz.printify.me") }
    var printifyShopId by remember(currentSettings) { mutableStateOf(currentSettings?.printifyShopId ?: "26650832") }

    var showGithubKey by remember { mutableStateOf(false) }
    var showStripeKey by remember { mutableStateOf(false) }
    var showStripePubKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }
    var showHfKey by remember { mutableStateOf(false) }
    var showOrKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showPrintifyKey by remember { mutableStateOf(false) }

    // 7. Notifications State
    var pushNotificationsActive by remember(currentSettings) { mutableStateOf(currentSettings?.pushNotificationsActive ?: true) }
    var notificationLevel by remember(currentSettings) { mutableStateOf(currentSettings?.notificationLevel ?: "All Alerts") }
    var notifyPitches by remember(currentSettings) { mutableStateOf(currentSettings?.notifyPitches ?: true) }
    var notifySales by remember(currentSettings) { mutableStateOf(currentSettings?.notifySales ?: true) }
    var notifyTrademarkAlerts by remember(currentSettings) { mutableStateOf(currentSettings?.notifyTrademarkAlerts ?: true) }
    var soundEnabled by remember(currentSettings) { mutableStateOf(currentSettings?.soundEnabled ?: true) }
    var vibrationEnabled by remember(currentSettings) { mutableStateOf(currentSettings?.vibrationEnabled ?: true) }

    // 8. Privacy & Data State
    var analyticsTelemetry by remember(currentSettings) { mutableStateOf(currentSettings?.analyticsTelemetry ?: false) }
    var localOnlyStorage by remember(currentSettings) { mutableStateOf(currentSettings?.localOnlyStorage ?: true) }
    var autoBackupEnabled by remember(currentSettings) { mutableStateOf(currentSettings?.autoBackupEnabled ?: true) }

    fun commitSettings() {
        val updated = SettingsEntity(
            businessName = businessName,
            operatorAlias = operatorAlias,
            supportEmail = supportEmail,
            storefrontProvider = storefrontProvider,
            requireBiometricPin = requireBiometricPin,
            themeMode = themeMode,
            amoledMode = amoledMode,
            accentColor = accentColor,
            compactDensity = compactDensity,
            currencySymbol = currencySymbol,
            uiAnimationsEnabled = uiAnimationsEnabled,
            globalAutomationActive = globalAutomationActive,
            productFrequency = productFrequency,
            riskLevel = riskLevel,
            minMarginPercent = minMarginPercent,
            autoPitchGeneration = autoPitchGeneration,
            maxDailyCreations = maxDailyCreations,
            enableAgentChatLogs = enableAgentChatLogs,
            bypassApprovals = bypassApprovals,
            bypassRateLimits = bypassRateLimits,
            offlineSimulationMode = offlineSimulationMode,
            modelCeo = modelCeo,
            modelResearch = modelResearch,
            modelProduct = modelProduct,
            modelMarketing = modelMarketing,
            modelContent = modelContent,
            modelSupport = modelSupport,
            modelFinance = modelFinance,
            autoDeployOnApproval = autoDeployOnApproval,
            instantStripeCheckout = instantStripeCheckout,
            githubRepoVisibility = githubRepoVisibility,
            digitalProductFormats = digitalProductFormats,
            podProviderSandbox = podProviderSandbox,
            strictCopyrightCheck = strictCopyrightCheck,
            trademarkConfidenceMin = trademarkConfidenceMin,
            autoClearanceCertificate = autoClearanceCertificate,
            allowedStyles = allowedStyles,
            forbiddenItems = forbiddenItems,
            preferredPalettes = preferredPalettes,
            backendServerEndpoint = backendServerEndpoint,
            ollamaEndpoint = ollamaEndpoint,
            ollamaModel = ollamaModel,
            keyGithub = keyGithub,
            keyStripe = keyStripe,
            keyStripePublishable = keyStripePublishable,
            keyGroq = keyGroq,
            keyHuggingFace = keyHuggingFace,
            keyOpenRouter = keyOpenRouter,
            keyGemini = keyGemini,
            keyPrintify = keyPrintify,
            printifyShopDomain = printifyShopDomain,
            printifyShopId = printifyShopId,
            pushNotificationsActive = pushNotificationsActive,
            notificationLevel = notificationLevel,
            notifyPitches = notifyPitches,
            notifySales = notifySales,
            notifyTrademarkAlerts = notifyTrademarkAlerts,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            analyticsTelemetry = analyticsTelemetry,
            localOnlyStorage = localOnlyStorage,
            autoBackupEnabled = autoBackupEnabled
        )
        viewModel.updateSettings(updated)
        saveFeedbackMessage = "All function settings successfully saved!"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "Function Settings Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Granular configuration options for every autonomous function, store deployment, copyright engine & API key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Endpoint Status & Quick Actions
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLiveConnected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("live_server_status_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    text = if (isLiveConnected) "Termux & Local Core Connected" else "Local DB Engine Standby",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Endpoint: $backendServerEndpoint",
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
                            Text("Ping", fontSize = 12.sp)
                        }
                    }

                    pingStatus?.let { status ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
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

                    // Save / Reset / Schema Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { commitSettings() },
                            modifier = Modifier.weight(1f).testTag("save_all_settings_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save All", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { viewModel.viewRawJsonSchema() },
                            modifier = Modifier.weight(1f).testTag("view_schema_button")
                        ) {
                            Icon(Icons.Default.Schema, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f).testTag("reset_defaults_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }

        // Save Feedback Banner
        saveFeedbackMessage?.let { feedback ->
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandEmerald.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feedback, style = MaterialTheme.typography.bodySmall, color = BrandEmerald, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { saveFeedbackMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Category Filter Chips Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "FUNCTION CATEGORIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(cat.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.testTag("category_chip_${cat.name}")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 1. ACCOUNT & BUSINESS PROFILE SECTION
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.ACCOUNT) {
            item {
                SettingsSectionCard(
                    title = "1. Account & Business Profile",
                    icon = Icons.Default.AccountCircle,
                    testTag = "settings_account_card"
                ) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it; commitSettings() },
                        label = { Text("Business / Brand Name") },
                        supportingText = { Text("Used across generated storefront banners & receipts") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_business_name")
                    )

                    OutlinedTextField(
                        value = operatorAlias,
                        onValueChange = { operatorAlias = it; commitSettings() },
                        label = { Text("Founder / Operator Alias") },
                        supportingText = { Text("Used for human approval signatures in ledger") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_operator_alias")
                    )

                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it; commitSettings() },
                        label = { Text("Customer Support Email") },
                        supportingText = { Text("Embedded in Jamstack storefront footer") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_support_email")
                    )

                    Column {
                        Text("Default Zero-Cost Storefront Provider", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("GitHub Pages + Stripe", "Netlify + Stripe", "Vercel + Stripe").forEach { prov ->
                                FilterChip(
                                    selected = storefrontProvider == prov,
                                    onClick = { storefrontProvider = prov; commitSettings() },
                                    label = { Text(prov, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Require Biometric / PIN Lock",
                        description = "Protect financial ledger actions and API keys with device credentials",
                        checked = requireBiometricPin,
                        onCheckedChange = { requireBiometricPin = it; commitSettings() },
                        testTag = "setting_biometric_lock"
                    )
                }
            }
        }

        // ==========================================
        // 2. APPEARANCE & DISPLAY SECTION
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.APPEARANCE) {
            item {
                SettingsSectionCard(
                    title = "2. Appearance & Display",
                    icon = Icons.Default.Palette,
                    testTag = "settings_appearance_card"
                ) {
                    Column {
                        Text("App Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Dark", "Light", "System").forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { themeMode = mode; commitSettings() },
                                    label = { Text(mode) },
                                    modifier = Modifier.testTag("theme_mode_$mode")
                                )
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = "AMOLED High-Efficiency Black",
                        description = "Enforces pure #000000 black canvas for maximum OLED battery savings",
                        checked = amoledMode,
                        onCheckedChange = { amoledMode = it; commitSettings() },
                        testTag = "setting_amoled_mode"
                    )

                    HorizontalDivider()

                    Column {
                        Text("Primary Color Accent", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Emerald", "Cyber Blue", "Neon Violet", "Amber Gold").forEach { col ->
                                FilterChip(
                                    selected = accentColor == col,
                                    onClick = { accentColor = col; commitSettings() },
                                    label = { Text(col, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("accent_color_$col")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Compact Telemetry Density",
                        description = "Reduces card paddings to show more agent data and ledger lines on small screens",
                        checked = compactDensity,
                        onCheckedChange = { compactDensity = it; commitSettings() },
                        testTag = "setting_compact_density"
                    )

                    HorizontalDivider()

                    Column {
                        Text("Active Currency Format", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("USD ($)", "EUR (€)", "GBP (£)", "CAD (C$)", "AUD (A$)").forEach { curr ->
                                FilterChip(
                                    selected = currencySymbol == curr,
                                    onClick = { currencySymbol = curr; commitSettings() },
                                    label = { Text(curr, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("currency_$curr")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "UI Animation & Micro-Interactions",
                        description = "Enables smooth ripple effects, progress meters, and transition animations",
                        checked = uiAnimationsEnabled,
                        onCheckedChange = { uiAnimationsEnabled = it; commitSettings() },
                        testTag = "setting_animations_enabled"
                    )
                }
            }
        }

        // ==========================================
        // 3. AUTONOMOUS AGENT SWARM & ORCHESTRATION
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.AGENT_SWARM) {
            item {
                SettingsSectionCard(
                    title = "3. Autonomous Agent Swarm & Orchestration",
                    icon = Icons.Default.SmartToy,
                    testTag = "settings_agent_swarm_card"
                ) {
                    SettingsToggleRow(
                        title = "Global Automation Loop",
                        description = "Master killswitch for autonomous 11-agent background execution cycle",
                        checked = globalAutomationActive,
                        onCheckedChange = { globalAutomationActive = it; commitSettings() },
                        testTag = "global_automation_switch"
                    )

                    HorizontalDivider()

                    Column {
                        Text("Autonomous Dispatch Frequency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("How often agents brainstorm, audit, and pitch digital products", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Every 1h", "Every 4h", "Every 12h", "Daily", "Weekly").forEach { freq ->
                                FilterChip(
                                    selected = productFrequency == freq,
                                    onClick = { productFrequency = freq; commitSettings() },
                                    label = { Text(freq, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("freq_$freq")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Column {
                        Text("Risk Tolerance & Autonomy Threshold", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Conservative", "Balanced", "Aggressive").forEach { risk ->
                                FilterChip(
                                    selected = riskLevel == risk,
                                    onClick = { riskLevel = risk; commitSettings() },
                                    label = { Text(risk) },
                                    modifier = Modifier.testTag("risk_level_$risk")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Minimum Required Net Profit Margin", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("$minMarginPercent%", fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                        Text("Decline proposals with margin below this threshold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = minMarginPercent.toFloat(),
                            onValueChange = { minMarginPercent = it.toInt(); commitSettings() },
                            valueRange = 10f..90f,
                            steps = 15,
                            modifier = Modifier.testTag("slider_min_margin")
                        )
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Auto-Pitch Brainstorming",
                        description = "Agents proactively debate niche ideas and queue pitches for human approval",
                        checked = autoPitchGeneration,
                        onCheckedChange = { autoPitchGeneration = it; commitSettings() },
                        testTag = "setting_auto_pitch"
                    )

                    HorizontalDivider()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Max Products Created Per Cycle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("$maxDailyCreations listings", fontWeight = FontWeight.Bold, color = BrandEmerald)
                        }
                        Slider(
                            value = maxDailyCreations.toFloat(),
                            onValueChange = { maxDailyCreations = it.toInt(); commitSettings() },
                            valueRange = 1f..30f,
                            steps = 28,
                            modifier = Modifier.testTag("slider_max_creations")
                        )
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Agent Deliberation Chat & Audit Logs",
                        description = "Log inter-agent messaging and reasoning transcripts in local database",
                        checked = enableAgentChatLogs,
                        onCheckedChange = { enableAgentChatLogs = it; commitSettings() },
                        testTag = "setting_agent_chat_logs"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Bypass Human Approval Gates (Sandbox Mode)",
                        description = "Auto-approves validated pitches without waiting for user confirmation (Warning: Test use only)",
                        checked = bypassApprovals,
                        onCheckedChange = { bypassApprovals = it; commitSettings() },
                        testTag = "setting_bypass_approvals"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Bypass Rate Limits & Retries",
                        description = "Skips exponential backoff and immediately switches to Standby Provider pool",
                        checked = bypassRateLimits,
                        onCheckedChange = { bypassRateLimits = it; commitSettings() },
                        testTag = "setting_bypass_ratelimits"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Offline Simulation Mode",
                        description = "Simulates all agent inference safely without live network calls or LLM tokens",
                        checked = offlineSimulationMode,
                        onCheckedChange = { offlineSimulationMode = it; commitSettings() },
                        testTag = "setting_offline_simulation"
                    )

                    HorizontalDivider()

                    HorizontalDivider()

                    // Operational Roles & Status Management Section
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Autonomous Fleet Operational Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Set individual agents to Active or Standby reserve mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setAllAgentsStatus(AgentStatus.ACTIVE) },
                                modifier = Modifier.weight(1f).testTag("settings_activate_all_agents")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = ProfitGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Activate All", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.setAllAgentsStatus(AgentStatus.STANDBY) },
                                modifier = Modifier.weight(1f).testTag("settings_standby_all_agents")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Standby All", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        autonomousAgents.forEach { agent ->
                            val isAgentActive = agent.isActive
                            val statusColor = if (isAgentActive) ProfitGreen else Color(0xFFF59E0B)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = agent.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusColor.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (isAgentActive) "ACTIVE" else "STANDBY",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${agent.role.title} • ${agent.department.departmentName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (agent.requiresHumanApproval) {
                                            Text(
                                                text = "🔒 Human Verification Gate: Required",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { inspectingAgentInSettings = agent },
                                            modifier = Modifier.size(32.dp).testTag("inspect_settings_agent_${agent.id}")
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "Inspect Role", modifier = Modifier.size(18.dp))
                                        }

                                        Switch(
                                            checked = isAgentActive,
                                            onCheckedChange = { checked ->
                                                viewModel.setAgentStatus(agent.id, if (checked) AgentStatus.ACTIVE else AgentStatus.STANDBY)
                                            },
                                            modifier = Modifier.testTag("settings_agent_switch_${agent.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Per-Agent Model Matrix Sub-section
                    Text("Per-Agent Free-Tier AI Model Matrix", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Assign specific open-source or free-tier models to each specialized agent role.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    ModelConfigField("CEO / Strategy Agent", modelCeo) { modelCeo = it; commitSettings() }
                    ModelConfigField("Research & Trends Agent", modelResearch) { modelResearch = it; commitSettings() }
                    ModelConfigField("Product Architect", modelProduct) { modelProduct = it; commitSettings() }
                    ModelConfigField("Marketing Copywriter", modelMarketing) { modelMarketing = it; commitSettings() }
                    ModelConfigField("Content & Code Agent", modelContent) { modelContent = it; commitSettings() }
                    ModelConfigField("Customer Support Agent", modelSupport) { modelSupport = it; commitSettings() }
                    ModelConfigField("Finance & Double-Entry Ledger", modelFinance) { modelFinance = it; commitSettings() }
                }
            }
        }

        // ==========================================
        // 4. STOREFRONT & ZERO-COST ECOMMERCE SECTION
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.STOREFRONT) {
            item {
                SettingsSectionCard(
                    title = "4. Storefront & Zero-Cost eCommerce",
                    icon = Icons.Default.Storefront,
                    testTag = "settings_storefront_card"
                ) {
                    SettingsToggleRow(
                        title = "Instant Deployment on Human Approval",
                        description = "Automatically commit static Jamstack site to GitHub Pages when approved",
                        checked = autoDeployOnApproval,
                        onCheckedChange = { autoDeployOnApproval = it; commitSettings() },
                        testTag = "setting_auto_deploy"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Instant Stripe Checkout Integration",
                        description = "Automatically generate Stripe Product, Price, and direct Buy button link for \$0 COGS products",
                        checked = instantStripeCheckout,
                        onCheckedChange = { instantStripeCheckout = it; commitSettings() },
                        testTag = "setting_instant_stripe"
                    )

                    HorizontalDivider()

                    Column {
                        Text("Created GitHub Storefront Repo Visibility", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("public", "private").forEach { vis ->
                                FilterChip(
                                    selected = githubRepoVisibility == vis,
                                    onClick = { githubRepoVisibility = vis; commitSettings() },
                                    label = { Text(vis.replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.testTag("repo_vis_$vis")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = digitalProductFormats,
                        onValueChange = { digitalProductFormats = it; commitSettings() },
                        label = { Text("Allowed Digital Product Formats") },
                        supportingText = { Text("Comma-separated list of zero-cost digital downloads") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_digital_formats")
                    )

                    HorizontalDivider()

                    Column {
                        Text("Print-On-Demand / Dropship Fulfillment Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Direct JAMStack Mock", "Printify Live (kriticallimitz.printify.me)", "Printful API").forEach { pod ->
                                FilterChip(
                                    selected = podProviderSandbox == pod,
                                    onClick = { podProviderSandbox = pod; commitSettings() },
                                    label = { Text(pod, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Printify Storefront Live Connection Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("printify_settings_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Printify Storefront (Live)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ProfitGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "JWT ACTIVE",
                                        color = ProfitGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = printifyConnectionStatus ?: "Connected: $printifyShopDomain",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = printifyShopDomain,
                                onValueChange = {
                                    printifyShopDomain = it
                                    commitSettings()
                                    viewModel.updatePrintifyShop(it, printifyShopId)
                                },
                                label = { Text("Printify Shop Domain") },
                                modifier = Modifier.fillMaxWidth().testTag("setting_printify_domain")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = printifyShopId,
                                onValueChange = {
                                    printifyShopId = it
                                    commitSettings()
                                    viewModel.updatePrintifyShop(printifyShopDomain, it)
                                },
                                label = { Text("Printify Shop ID") },
                                modifier = Modifier.fillMaxWidth().testTag("setting_printify_shop_id")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.testPrintifyConnection() },
                                    enabled = !isTestingPrintify,
                                    modifier = Modifier.weight(1f).testTag("test_printify_btn")
                                ) {
                                    if (isTestingPrintify) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Connection", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. TRADEMARK, COPYRIGHT & LEGAL ENGINE
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.TRADEMARK) {
            item {
                SettingsSectionCard(
                    title = "5. Trademark, Copyright & Legal Protection",
                    icon = Icons.Default.VerifiedUser,
                    testTag = "settings_trademark_card"
                ) {
                    SettingsToggleRow(
                        title = "Strict USPTO & Copyright Pre-Screening",
                        description = "Mandatory zero-tolerance pre-screening before any design or product is submitted for approval",
                        checked = strictCopyrightCheck,
                        onCheckedChange = { strictCopyrightCheck = it; commitSettings() },
                        testTag = "setting_strict_copyright"
                    )

                    HorizontalDivider()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Clearance Confidence Safety Threshold", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("$trademarkConfidenceMin%", fontWeight = FontWeight.Bold, color = BrandEmerald)
                        }
                        Text("Reject any phrase with similarity index above acceptable variance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = trademarkConfidenceMin.toFloat(),
                            onValueChange = { trademarkConfidenceMin = it.toInt(); commitSettings() },
                            valueRange = 90f..99f,
                            steps = 8,
                            modifier = Modifier.testTag("slider_tm_confidence")
                        )
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Generate Cryptographic Clearance Certificate",
                        description = "Creates immutable SHA-256 proof hash verifying 0 trademark collisions found",
                        checked = autoClearanceCertificate,
                        onCheckedChange = { autoClearanceCertificate = it; commitSettings() },
                        testTag = "setting_auto_certificate"
                    )

                    HorizontalDivider()

                    OutlinedTextField(
                        value = forbiddenItems,
                        onValueChange = { forbiddenItems = it; commitSettings() },
                        label = { Text("Strict Banned Keywords & Brands") },
                        supportingText = { Text("Zero-tolerance keywords immediately filtered out of product pitches") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_forbidden_items"),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = allowedStyles,
                        onValueChange = { allowedStyles = it; commitSettings() },
                        label = { Text("Whitelisted Creative Art Styles") },
                        supportingText = { Text("Styles verified to be in the public domain and safe for commercial use") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_allowed_styles"),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = preferredPalettes,
                        onValueChange = { preferredPalettes = it; commitSettings() },
                        label = { Text("Preferred Aesthetic Palettes") },
                        supportingText = { Text("Recommended color palettes for digital artwork & mockups") },
                        modifier = Modifier.fillMaxWidth().testTag("setting_preferred_palettes")
                    )
                }
            }
        }

        // ==========================================
        // 6. SECRETMANAGER & CREDENTIALS SECTION
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.CREDENTIALS) {
            item {
                SettingsSectionCard(
                    title = "6. SecretManager & API Keys",
                    icon = Icons.Default.Key,
                    testTag = "settings_secrets_card"
                ) {
                    // SecretManager live status summary
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Active Credential Security Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GitHub Token:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${secretStatus.githubTokenMasked} (${secretStatus.githubSource})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (secretStatus.isGitHubConfigured) ProfitGreen else Color(0xFFF59E0B)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Stripe Secret Key:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${secretStatus.stripeSecretKeyMasked} (${secretStatus.stripeSource})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (secretStatus.isStripeConfigured) ProfitGreen else Color(0xFFF59E0B)
                                )
                            }
                        }
                    }

                    SecureKeyField(
                        label = "GitHub Personal Access Token (repo, pages)",
                        value = keyGithub,
                        isVisible = showGithubKey,
                        onValueChange = { keyGithub = it; commitSettings(); viewModel.updateGitHubToken(it) },
                        onToggleVisibility = { showGithubKey = !showGithubKey }
                    )

                    SecureKeyField(
                        label = "Stripe Secret API Key (sk_test_... / sk_live_...)",
                        value = keyStripe,
                        isVisible = showStripeKey,
                        onValueChange = { keyStripe = it; commitSettings(); viewModel.updateStripeSecretKey(it) },
                        onToggleVisibility = { showStripeKey = !showStripeKey }
                    )

                    SecureKeyField(
                        label = "Stripe Publishable Key (pk_test_... / pk_live_...)",
                        value = keyStripePublishable,
                        isVisible = showStripePubKey,
                        onValueChange = { keyStripePublishable = it; commitSettings() },
                        onToggleVisibility = { showStripePubKey = !showStripePubKey }
                    )

                    SecureKeyField(
                        label = "Google Gemini AI API Key",
                        value = keyGemini,
                        isVisible = showGeminiKey,
                        onValueChange = { keyGemini = it; commitSettings() },
                        onToggleVisibility = { showGeminiKey = !showGeminiKey }
                    )

                    SecureKeyField(
                        label = "Printify API Bearer Token (JWT / kriticallimitz)",
                        value = keyPrintify,
                        isVisible = showPrintifyKey,
                        onValueChange = { keyPrintify = it; commitSettings(); viewModel.updatePrintifyToken(it) },
                        onToggleVisibility = { showPrintifyKey = !showPrintifyKey }
                    )

                    SecureKeyField(
                        label = "Groq Cloud API Key (High-Speed LPU)",
                        value = keyGroq,
                        isVisible = showGroqKey,
                        onValueChange = { keyGroq = it; commitSettings() },
                        onToggleVisibility = { showGroqKey = !showGroqKey }
                    )

                    SecureKeyField(
                        label = "HuggingFace User Token",
                        value = keyHuggingFace,
                        isVisible = showHfKey,
                        onValueChange = { keyHuggingFace = it; commitSettings() },
                        onToggleVisibility = { showHfKey = !showHfKey }
                    )

                    SecureKeyField(
                        label = "OpenRouter API Key",
                        value = keyOpenRouter,
                        isVisible = showOrKey,
                        onValueChange = { keyOpenRouter = it; commitSettings() },
                        onToggleVisibility = { showOrKey = !showOrKey }
                    )

                    HorizontalDivider()

                    Text("Local & Background Endpoints", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { backendServerEndpoint = "http://127.0.0.1:8080"; commitSettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Termux (127.0.0.1)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { backendServerEndpoint = "http://10.0.2.2:8080"; commitSettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Emulator (10.0.2.2)", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = backendServerEndpoint,
                        onValueChange = { backendServerEndpoint = it; commitSettings() },
                        label = { Text("Backend Server Endpoint (Port 8080)") },
                        modifier = Modifier.fillMaxWidth().testTag("backend_endpoint_input")
                    )

                    OutlinedTextField(
                        value = ollamaEndpoint,
                        onValueChange = { ollamaEndpoint = it; commitSettings() },
                        label = { Text("Ollama Local Endpoint (Port 11434)") },
                        modifier = Modifier.fillMaxWidth().testTag("ollama_endpoint_input")
                    )

                    OutlinedTextField(
                        value = ollamaModel,
                        onValueChange = { ollamaModel = it; commitSettings() },
                        label = { Text("Default Ollama LLM Model") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ==========================================
        // 7. PUSH & IN-APP NOTIFICATIONS SECTION
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.NOTIFICATIONS) {
            item {
                SettingsSectionCard(
                    title = "7. Push & In-App Notifications",
                    icon = Icons.Default.Notifications,
                    testTag = "settings_notifications_card"
                ) {
                    SettingsToggleRow(
                        title = "Push Notifications Master Toggle",
                        description = "Receive operating notifications and revenue alerts on device",
                        checked = pushNotificationsActive,
                        onCheckedChange = { pushNotificationsActive = it; commitSettings() },
                        testTag = "setting_push_master"
                    )

                    HorizontalDivider()

                    Column {
                        Text("Notification Filter Level", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All Alerts", "Approvals Only", "Silent").forEach { level ->
                                FilterChip(
                                    selected = notificationLevel == level,
                                    onClick = { notificationLevel = level; commitSettings() },
                                    label = { Text(level) },
                                    modifier = Modifier.testTag("notif_level_$level")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Notify on Validated Agent Pitches",
                        description = "Alert when agents have vetted and queued a new product ready for approval",
                        checked = notifyPitches,
                        onCheckedChange = { notifyPitches = it; commitSettings() },
                        testTag = "setting_notify_pitches"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Notify on Customer Sales & Revenue",
                        description = "Instant alert when Stripe registers customer checkout payment",
                        checked = notifySales,
                        onCheckedChange = { notifySales = it; commitSettings() },
                        testTag = "setting_notify_sales"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Notify on Trademark / Copyright Warnings",
                        description = "Urgent alert if an idea or supplier title generates legal collisions",
                        checked = notifyTrademarkAlerts,
                        onCheckedChange = { notifyTrademarkAlerts = it; commitSettings() },
                        testTag = "setting_notify_tm_warnings"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Notification Sound Chimes",
                        description = "Play audio chime on incoming high-priority business events",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it; commitSettings() },
                        testTag = "setting_sound_enabled"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Haptic Vibration Feedback",
                        description = "Vibrate on decision approvals and payment alerts",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it; commitSettings() },
                        testTag = "setting_vibration_enabled"
                    )
                }
            }
        }

        // ==========================================
        // 8. PRIVACY, SECURITY & DATA MANAGEMENT
        // ==========================================
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.PRIVACY) {
            item {
                SettingsSectionCard(
                    title = "8. Privacy, Security & Data Management",
                    icon = Icons.Default.Security,
                    testTag = "settings_privacy_card"
                ) {
                    SettingsToggleRow(
                        title = "Anonymous Usage Telemetry",
                        description = "Send crash reports and anonymous performance metrics to improve the agent swarm",
                        checked = analyticsTelemetry,
                        onCheckedChange = { analyticsTelemetry = it; commitSettings() },
                        testTag = "setting_analytics_telemetry"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Strictly Local On-Device Storage",
                        description = "Keep all product databases, ledgers, and API keys 100% on this Android device in SQLite",
                        checked = localOnlyStorage,
                        onCheckedChange = { localOnlyStorage = it; commitSettings() },
                        testTag = "setting_local_storage_only"
                    )

                    HorizontalDivider()

                    SettingsToggleRow(
                        title = "Automatic Database Snapshots",
                        description = "Create automatic ledger backup checkpoints after every registered sale",
                        checked = autoBackupEnabled,
                        onCheckedChange = { autoBackupEnabled = it; commitSettings() },
                        testTag = "setting_auto_backup"
                    )

                    HorizontalDivider()

                    Text("Room Database Maintenance & Integrity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Periodic SQLite integrity validation and VACUUM defragmentation to prevent data corruption and reclaim free storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Database telemetry chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Storage Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${dbMaintenanceState.databaseSizeKb} KB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Integrity Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (dbMaintenanceState.isIntegrityValid) "Healthy" else "Warning",
                                    color = if (dbMaintenanceState.isIntegrityValid) ProfitGreen else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Last Reclaimed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${dbMaintenanceState.bytesSavedOnLastVacuum} B", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Report snippet
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = dbMaintenanceState.lastIntegrityReport,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.runDatabaseIntegrityCheck() },
                            modifier = Modifier.weight(1f).testTag("run_integrity_check_btn")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Integrity Check", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.runDatabaseVacuum() },
                            modifier = Modifier.weight(1f).testTag("run_vacuum_btn")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VACUUM", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.runFullDatabaseMaintenance() },
                            enabled = !dbMaintenanceState.isRunning,
                            modifier = Modifier.weight(1f).testTag("run_full_maintenance_btn")
                        ) {
                            if (dbMaintenanceState.isRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Full Cycle", fontSize = 11.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("Database Management & Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.viewRawJsonSchema() },
                            modifier = Modifier.weight(1f).testTag("export_schema_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f).testTag("factory_reset_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset All", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings to Default?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will restore all function settings, agent parameters, and creative rules back to their initial factory baselines. Your product inventory and ledger transactions will remain safe.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSettingsToDefault()
                        showResetDialog = false
                        saveFeedbackMessage = "All settings successfully restored to factory defaults!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Raw JSON Schema & Preferences Dialog
    rawJsonSchema?.let { schemaContent ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissJsonSchemaDialog() },
            title = { Text("Exported JSON Preferences & Schema", fontWeight = FontWeight.Bold) },
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
                    Text("Done")
                }
            }
        )
    }

    inspectingAgentInSettings?.let { agent ->
        AutonomousAgentInspectorDialog(
            agent = agent,
            onDismiss = { inspectingAgentInSettings = null },
            onToggleStatus = { nextStatus ->
                viewModel.setAgentStatus(agent.id, nextStatus)
                inspectingAgentInSettings = agent.copy(status = nextStatus)
            },
            onDispatchDirective = { directive ->
                viewModel.dispatchAdHocTask(agent.id, "Settings Directive", directive)
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BrandEmerald.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
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
        label = { Text(label, fontSize = 12.sp) },
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
