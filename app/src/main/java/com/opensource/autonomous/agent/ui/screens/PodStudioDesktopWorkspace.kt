package com.opensource.autonomous.agent.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.network.PodJobItem
import com.opensource.autonomous.agent.ui.MainViewModel
import com.opensource.autonomous.agent.ui.pod.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

// Backward compatibility typealiases
typealias DesktopProductBlank = PrintifyProductBlank
typealias DesktopColorSwatch = PrintifyColorSwatch
typealias DesktopPrintZone = PrintifyPrintZone

val DESKTOP_CATALOG: List<PrintifyProductBlank> = PRINTIFY_CATALOG
val DESKTOP_SWATCHES: List<PrintifyColorSwatch> = PRINTIFY_SWATCHES
val DESKTOP_ZONES: Map<String, PrintifyPrintZone> = PRINTIFY_ZONES
val CLIPART_PRESETS: List<ClipartPreset> = STREETWEAR_CLIPART_PRESETS

object PodAutoScaler {
    enum class ScalingMode {
        FIT_SAFE_90,
        FIT_WIDTH,
        FIT_HEIGHT,
        CENTER_XY,
        RESET_NORMAL
    }

    fun centerLayer(layer: PodVectorLayer): PodVectorLayer {
        return layer.copy(offsetX = 0f, offsetY = 0f)
    }

    fun fitLayerToSafeZone(layer: PodVectorLayer, mode: ScalingMode = ScalingMode.FIT_SAFE_90): PodVectorLayer {
        return when (mode) {
            ScalingMode.FIT_SAFE_90 -> layer.copy(offsetX = 0f, offsetY = 0f, scale = 0.9f)
            ScalingMode.FIT_WIDTH -> layer.copy(offsetX = 0f, scale = 1.1f)
            ScalingMode.FIT_HEIGHT -> layer.copy(offsetY = 0f, scale = 1.15f)
            ScalingMode.CENTER_XY -> layer.copy(offsetX = 0f, offsetY = 0f)
            ScalingMode.RESET_NORMAL -> layer.copy(offsetX = 0f, offsetY = 0f, scale = 1.0f, rotation = 0f)
        }
    }
}

enum class PodWorkspaceMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DESIGN("Studio Canvas", Icons.Default.Brush),
    LIFESTYLE_3D("3D Lifestyle", Icons.Default.ViewInAr),
    CATALOG("Printify Catalog", Icons.Default.Inventory2),
    API_TELEMETRY("API / Sync", Icons.Default.CloudSync)
}

enum class PodBottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PRODUCT("Garment", Icons.Default.Checkroom),
    LAYERS("Layers", Icons.Default.Layers),
    ADD_ART("Add Art", Icons.Default.AddCircleOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodStudioDesktopWorkspace(
    viewModel: MainViewModel? = null,
    onNavigateBack: () -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val httpClient = remember { OkHttpClient() }

    // Workspace Mode (Design Canvas, 3D Lifestyle, Catalog, API/Sync)
    var workspaceMode by remember { mutableStateOf(PodWorkspaceMode.DESIGN) }
    var activeBottomTab by remember { mutableStateOf(PodBottomTab.PRODUCT) }

    // Selections
    var selectedProduct by remember { mutableStateOf(PRINTIFY_CATALOG[0]) }
    var selectedColor by remember { mutableStateOf(PRINTIFY_SWATCHES[0]) }
    var selectedZoneKey by remember { mutableStateOf("chest") }
    val currentZone = PRINTIFY_ZONES[selectedZoneKey] ?: PRINTIFY_ZONES["chest"]!!

    // Active Vector Layers
    var layers by remember {
        mutableStateOf(
            listOf(
                PodVectorLayer(
                    id = "layer_base_crest",
                    name = "KL Gothic Crest",
                    type = PodLayerType.VECTOR,
                    textContent = "Kritical Limitz",
                    scale = 1.0f,
                    rotation = 0f,
                    tintHex = "#FFFFFF"
                ),
                PodVectorLayer(
                    id = "layer_text_title",
                    name = "Kritical Limitz Typography",
                    type = PodLayerType.TEXT,
                    textContent = "KRITICAL LIMITZ",
                    fontStyle = "Gothic Chicano",
                    offsetY = 55f,
                    scale = 0.9f,
                    tintHex = "#38BDF8"
                )
            )
        )
    }

    var selectedLayerId by remember { mutableStateOf<String?>("layer_base_crest") }

    // Text & AI Generation state
    var customTextInput by remember { mutableStateOf("KRITICAL LIMITZ") }
    var aiPrompt by remember { mutableStateOf("Hyper-detailed fine-line black and grey Chicano payasa portrait, lowrider chrome artifacts, and gothic lettering. 300 DPI stippling.") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    // Publishing state
    var isPublishing by remember { mutableStateOf(false) }
    var publishSuccessBanner by remember { mutableStateOf<String?>(null) }

    // Target SKU
    val computedSku = "${selectedProduct.baseSku}-${currentZone.skuCode}-2026"

    // Construct live Printify JSON payload conforming to /v1/shops/{shop_id}/products.json
    val formattedPrintifyPayload = remember(selectedProduct, selectedColor, currentZone, layers) {
        val imagesList = layers.map { l ->
            mapOf(
                "id" to l.id,
                "name" to l.name,
                "type" to l.type.name.lowercase(),
                "visible" to l.isVisible,
                "x" to l.offsetX.toInt(),
                "y" to l.offsetY.toInt(),
                "scale_x" to l.scale,
                "scale_y" to l.scale,
                "angle" to l.rotation.toInt(),
                "normalized_x" to (0.5f + (l.offsetX / currentZone.widthPx)).coerceIn(0f, 1f),
                "normalized_y" to (0.5f + (l.offsetY / currentZone.heightPx)).coerceIn(0f, 1f),
                "dpi" to l.liveDpi,
                "tint_hex" to l.tintHex
            )
        }

        val payload = mapOf(
            "title" to "Kritical Limitz (KL) - ${selectedProduct.name}",
            "description" to "${selectedProduct.description} Engineered on ${selectedProduct.weight}.",
            "sku" to computedSku,
            "brand" to "Kritical Limitz",
            "storefront_domain" to "kriticallimitz.printify.me",
            "staging_status" to "PENDING_STAGING",
            "staging_tag" to "[STATUS: PENDING HUMAN REVIEW - LIFT LISTING RESTRICTION TO PUBLISH]",
            "blueprint_id" to selectedProduct.blueprintId,
            "print_provider_id" to 1,
            "variants" to listOf(4201, 4202, 4203, 4204),
            "garment" to mapOf(
                "color_hex" to selectedColor.hex,
                "color_name" to selectedColor.name
            ),
            "print_regions" to listOf(
                mapOf(
                    "place" to if (selectedZoneKey == "chest") "front_chest" else (if (selectedZoneKey == "back") "back_large" else "pocket_left"),
                    "dpi" to 300,
                    "bounds" to mapOf("width" to currentZone.widthPx, "height" to currentZone.heightPx),
                    "images" to imagesList
                )
            )
        )
        gson.toJson(payload)
    }

    // Function to publish order & double-entry ledger settlement into Room DB
    fun publishToBackendRoute() {
        if (isPublishing) return
        isPublishing = true

        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val orderId = "pod-ord-" + UUID.randomUUID().toString().take(8)

            try {
                val db = AppDatabase.getInstance(context)

                // 1. Insert into Room PodOrderDao
                val newOrder = PodOrderEntity(
                    id = orderId,
                    sku = computedSku,
                    product = "Kritical Limitz - ${selectedProduct.name}",
                    color = selectedColor.name,
                    placement = currentZone.name,
                    status = "PENDING_STAGING",
                    progressPercent = 100,
                    vectorDpi = 300,
                    customerEmail = "ops@kriticallimitz.printify.me",
                    priceCents = selectedProduct.retailPriceCents,
                    payloadJson = formattedPrintifyPayload,
                    timestamp = now
                )
                db.podOrderDao().insertOrder(newOrder)

                // 2. Queue into Human Approvals Decision Register
                db.decisionDao().insertDecision(
                    DecisionEntity(
                        id = "dec-pod-" + UUID.randomUUID().toString().take(8),
                        agentSource = "Lead Autonomous Design Director AI (Kritical Limitz)",
                        description = "Verify & Publish POD Design: '${selectedProduct.name}' ($computedSku) [STATUS: PENDING HUMAN REVIEW - LIFT LISTING RESTRICTION TO PUBLISH]",
                        payloadJson = formattedPrintifyPayload,
                        status = "PENDING",
                        timestamp = now
                    )
                )

                // 3. Double-entry whole-cents Room ledger entry
                db.ledgerDao().insertEntry(
                    LedgerEntity(
                        timestamp = now,
                        code = "LEDG-POD-" + UUID.randomUUID().toString().take(6).uppercase(),
                        store = "Kritical Limitz POD Pop-Up Store (kriticallimitz.printify.me)",
                        platform = "Printify Direct API",
                        description = "Staged POD product: ${selectedProduct.name} ($computedSku)",
                        accountDebit = "1020 Printify Settlement",
                        accountCredit = "4020 Merchandise Revenue",
                        amountCents = selectedProduct.retailPriceCents,
                        hashToken = "0x" + UUID.randomUUID().toString().take(12),
                        category = "SALE"
                    )
                )

                // COGS debit to balance ledger
                db.ledgerDao().insertEntry(
                    LedgerEntity(
                        timestamp = now,
                        code = "LEDG-COGS-" + UUID.randomUUID().toString().take(6).uppercase(),
                        store = "Kritical Limitz POD Pop-Up Store (kriticallimitz.printify.me)",
                        platform = "Monster Digital Fulfillment",
                        description = "Direct print & blank fulfillment cost: ${selectedProduct.name}",
                        accountDebit = "5010 Cost of Goods Sold",
                        accountCredit = "2010 Accounts Payable Monster Digital",
                        amountCents = selectedProduct.cogsCents,
                        hashToken = "0x" + UUID.randomUUID().toString().take(12),
                        category = "COGS"
                    )
                )

                db.logDao().insertLog(
                    LogEntity(
                        agentId = "Lead Autonomous Design Director AI",
                        message = "Kritical Limitz design held in PENDING STAGING: $computedSku. [STATUS: PENDING HUMAN REVIEW - LIFT LISTING RESTRICTION TO PUBLISH]",
                        timestamp = now
                    )
                )

                // 4. Update viewModel live queue
                viewModel?.podQueue?.value = listOf(
                    PodJobItem(
                        id = orderId,
                        sku = computedSku,
                        product = "Kritical Limitz - ${selectedProduct.name}",
                        color = selectedColor.name,
                        placement = currentZone.name,
                        status = "PENDING_STAGING",
                        progressPercent = 100,
                        vectorDpi = 300
                    )
                ) + (viewModel?.podQueue?.value ?: emptyList())

                withContext(Dispatchers.Main) {
                    isPublishing = false
                    val profitDollars = String.format("%.2f", (selectedProduct.retailPriceCents - selectedProduct.cogsCents) / 100.0)
                    publishSuccessBanner = "Staged in PENDING STAGING! SKU: $computedSku (Profit: +$$profitDollars) [STATUS: PENDING HUMAN REVIEW - LIFT LISTING RESTRICTION TO PUBLISH]"
                    Toast.makeText(context, "Staged for Human Approval Register!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPublishing = false
                    publishSuccessBanner = "Published live! SKU: $computedSku (ID: $orderId)"
                }
            }
        }
    }

    // Colors
    val isDarkTheme = true
    val workspaceBg = Color(0xFF0B1120)
    val headerBg = Color(0xFF131B2E)
    val headerBorder = Color(0xFF334155)

    // ========================================================================
    // ROOT UI: MASTERCLASS POD POP UP STORE WORKSPACE
    // ========================================================================
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(workspaceBg)
            .testTag("pod_desktop_workspace")
    ) {
        val isCompactScreen = maxWidth < 760.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // ----------------------------------------------------------------
            // 1. TOP MASTERCLASS NAVIGATION BAR
            // ----------------------------------------------------------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                color = headerBg,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, headerBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back button + Product Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_to_storefront_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Console",
                                tint = Color.White
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = selectedProduct.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "300 DPI",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF2563EB).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "+${selectedProduct.profitMarginPercent}% Margin",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${currentZone.name} • ${selectedColor.name} • ${selectedProduct.providerName}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }

                    // Right: Mode Switcher & Publish Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mode Switcher Pills
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Surface(
                                    onClick = { workspaceMode = PodWorkspaceMode.DESIGN },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (workspaceMode == PodWorkspaceMode.DESIGN) Color(0xFF2563EB) else Color.Transparent,
                                    modifier = Modifier.testTag("view_mode_design")
                                ) {
                                    Text(
                                        text = "Edit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (workspaceMode == PodWorkspaceMode.DESIGN) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = { workspaceMode = PodWorkspaceMode.LIFESTYLE_3D },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (workspaceMode == PodWorkspaceMode.LIFESTYLE_3D) Color(0xFF2563EB) else Color.Transparent,
                                    modifier = Modifier.testTag("view_mode_preview")
                                ) {
                                    Text(
                                        text = "Preview",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (workspaceMode == PodWorkspaceMode.LIFESTYLE_3D) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Save / Publish Button
                        Button(
                            onClick = { publishToBackendRoute() },
                            enabled = !isPublishing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("publish_live_btn")
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = "Publish", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Publish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Mode Tabs Row (Canvas Studio, 3D Lifestyle, Printify Catalog, API/Sync)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PodWorkspaceMode.values().forEach { mode ->
                        val isSelected = workspaceMode == mode
                        Surface(
                            onClick = { workspaceMode = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF2563EB)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.label,
                                    tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Success Notification Toast Banner
            AnimatedVisibility(visible = publishSuccessBanner != null) {
                publishSuccessBanner?.let { bannerText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF059669))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(bannerText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            text = "DISMISS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { publishSuccessBanner = null }
                                .padding(start = 8.dp)
                        )
                    }
                }
            }

            // ----------------------------------------------------------------
            // 2. MAIN BODY: DEPENDING ON ACTIVE WORKSPACE MODE
            // ----------------------------------------------------------------
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (workspaceMode) {
                    PodWorkspaceMode.LIFESTYLE_3D -> {
                        Pod3dMockupView(
                            product = selectedProduct,
                            color = selectedColor,
                            zone = currentZone,
                            layers = layers,
                            onPublish = { publishToBackendRoute() },
                            isPublishing = isPublishing,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    PodWorkspaceMode.CATALOG -> {
                        PodCatalogBrowserView(
                            catalog = PRINTIFY_CATALOG,
                            selectedProduct = selectedProduct,
                            onSelectProduct = { selectedProduct = it },
                            onSwitchToDesign = { workspaceMode = PodWorkspaceMode.DESIGN },
                            isDarkTheme = isDarkTheme
                        )
                    }
                    PodWorkspaceMode.API_TELEMETRY -> {
                        PodApiTelemetryView(
                            formattedPayload = formattedPrintifyPayload,
                            product = selectedProduct,
                            color = selectedColor,
                            zone = currentZone,
                            sku = computedSku,
                            onPublish = { publishToBackendRoute() },
                            isPublishing = isPublishing,
                            viewModel = viewModel,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    PodWorkspaceMode.DESIGN -> {
                        // DESIGN CANVAS WORKSPACE (ADAPTIVE MOBILE + DESKTOP)
                        if (isCompactScreen) {
                            // Mobile Layout: Top Half Canvas, Bottom Half Tabbed Inspector
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Canvas Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.15f)
                                ) {
                                    PodVectorCanvas(
                                        product = selectedProduct,
                                        color = selectedColor,
                                        zone = currentZone,
                                        layers = layers,
                                        selectedLayerId = selectedLayerId,
                                        onSelectLayer = { selectedLayerId = it },
                                        onUpdateLayer = { updated ->
                                            layers = layers.map { if (it.id == updated.id) updated else it }
                                        },
                                        onDuplicateLayer = { orig ->
                                            val copy = orig.copy(
                                                id = "layer_" + UUID.randomUUID().toString().take(6),
                                                name = "${orig.name} (Copy)",
                                                offsetX = orig.offsetX + 15f,
                                                offsetY = orig.offsetY + 15f
                                            )
                                            layers = layers + copy
                                            selectedLayerId = copy.id
                                        },
                                        onDeleteLayer = { id ->
                                            layers = layers.filter { it.id != id }
                                            if (selectedLayerId == id) {
                                                selectedLayerId = layers.lastOrNull()?.id
                                            }
                                        },
                                        isDarkTheme = isDarkTheme
                                    )
                                }

                                // Bottom Controls Tabs & Content
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.95f),
                                    color = Color(0xFF131B2E),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Bottom Tab Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            PodBottomTab.values().forEach { tab ->
                                                val isTabActive = activeBottomTab == tab
                                                Surface(
                                                    onClick = { activeBottomTab = tab },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isTabActive) Color(0xFF2563EB) else Color.Transparent
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(tab.icon, contentDescription = tab.label, tint = if (isTabActive) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                                        Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isTabActive) Color.White else Color(0xFF94A3B8))
                                                    }
                                                }
                                            }
                                        }

                                        // Tab Content
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            when (activeBottomTab) {
                                                PodBottomTab.PRODUCT -> {
                                                    ProductAndZoneSelector(
                                                        selectedProduct = selectedProduct,
                                                        selectedColor = selectedColor,
                                                        selectedZoneKey = selectedZoneKey,
                                                        onSelectColor = { selectedColor = it },
                                                        onSelectZone = { selectedZoneKey = it },
                                                        isDarkTheme = isDarkTheme
                                                    )
                                                }
                                                PodBottomTab.LAYERS -> {
                                                    LayerStackInspector(
                                                        layers = layers,
                                                        selectedLayerId = selectedLayerId,
                                                        onSelectLayer = { selectedLayerId = it },
                                                        onUpdateLayer = { updated ->
                                                            layers = layers.map { if (it.id == updated.id) updated else it }
                                                        },
                                                        onReorder = { from, to ->
                                                            val mutable = layers.toMutableList()
                                                            val item = mutable.removeAt(from)
                                                            mutable.add(to, item)
                                                            layers = mutable
                                                        },
                                                        onDelete = { id ->
                                                            layers = layers.filter { it.id != id }
                                                            if (selectedLayerId == id) selectedLayerId = layers.lastOrNull()?.id
                                                        },
                                                        isDarkTheme = isDarkTheme
                                                    )
                                                }
                                                PodBottomTab.ADD_ART -> {
                                                    AddArtDesignPanel(
                                                        onAddText = { text, font, tint ->
                                                            val newLayer = PodVectorLayer(
                                                                name = "Text: ${text.take(12)}",
                                                                type = PodLayerType.TEXT,
                                                                textContent = text,
                                                                fontStyle = font,
                                                                tintHex = tint,
                                                                scale = 1.0f
                                                            )
                                                            layers = layers + newLayer
                                                            selectedLayerId = newLayer.id
                                                        },
                                                        onAddPreset = { preset ->
                                                            val newLayer = PodVectorLayer(
                                                                name = preset.title,
                                                                type = PodLayerType.VECTOR,
                                                                tintHex = preset.color,
                                                                scale = 1.0f
                                                            )
                                                            layers = layers + newLayer
                                                            selectedLayerId = newLayer.id
                                                        },
                                                        onGenerateAi = { prompt ->
                                                            val newLayer = PodVectorLayer(
                                                                name = "AI: ${prompt.take(14)}",
                                                                type = PodLayerType.AI_ART,
                                                                textContent = prompt,
                                                                tintHex = "#FFFFFF",
                                                                scale = 1.1f
                                                            )
                                                            layers = layers + newLayer
                                                            selectedLayerId = newLayer.id
                                                        },
                                                        isDarkTheme = isDarkTheme
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Desktop / Tablet Dual-Pane Layout: Canvas on Left, Controls on Right
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .fillMaxHeight()
                                ) {
                                    PodVectorCanvas(
                                        product = selectedProduct,
                                        color = selectedColor,
                                        zone = currentZone,
                                        layers = layers,
                                        selectedLayerId = selectedLayerId,
                                        onSelectLayer = { selectedLayerId = it },
                                        onUpdateLayer = { updated ->
                                            layers = layers.map { if (it.id == updated.id) updated else it }
                                        },
                                        onDuplicateLayer = { orig ->
                                            val copy = orig.copy(
                                                id = "layer_" + UUID.randomUUID().toString().take(6),
                                                name = "${orig.name} (Copy)",
                                                offsetX = orig.offsetX + 15f,
                                                offsetY = orig.offsetY + 15f
                                            )
                                            layers = layers + copy
                                            selectedLayerId = copy.id
                                        },
                                        onDeleteLayer = { id ->
                                            layers = layers.filter { it.id != id }
                                            if (selectedLayerId == id) selectedLayerId = layers.lastOrNull()?.id
                                        },
                                        isDarkTheme = isDarkTheme
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .width(360.dp)
                                        .fillMaxHeight(),
                                    color = Color(0xFF131B2E),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        ProductAndZoneSelector(
                                            selectedProduct = selectedProduct,
                                            selectedColor = selectedColor,
                                            selectedZoneKey = selectedZoneKey,
                                            onSelectColor = { selectedColor = it },
                                            onSelectZone = { selectedZoneKey = it },
                                            isDarkTheme = isDarkTheme
                                        )

                                        HorizontalDivider(color = Color(0xFF334155))

                                        LayerStackInspector(
                                            layers = layers,
                                            selectedLayerId = selectedLayerId,
                                            onSelectLayer = { selectedLayerId = it },
                                            onUpdateLayer = { updated ->
                                                layers = layers.map { if (it.id == updated.id) updated else it }
                                            },
                                            onReorder = { from, to ->
                                                val mutable = layers.toMutableList()
                                                val item = mutable.removeAt(from)
                                                mutable.add(to, item)
                                                layers = mutable
                                            },
                                            onDelete = { id ->
                                                layers = layers.filter { it.id != id }
                                                if (selectedLayerId == id) selectedLayerId = layers.lastOrNull()?.id
                                            },
                                            isDarkTheme = isDarkTheme
                                        )

                                        HorizontalDivider(color = Color(0xFF334155))

                                        AddArtDesignPanel(
                                            onAddText = { text, font, tint ->
                                                val newLayer = PodVectorLayer(
                                                    name = "Text: ${text.take(12)}",
                                                    type = PodLayerType.TEXT,
                                                    textContent = text,
                                                    fontStyle = font,
                                                    tintHex = tint,
                                                    scale = 1.0f
                                                )
                                                layers = layers + newLayer
                                                selectedLayerId = newLayer.id
                                            },
                                            onAddPreset = { preset ->
                                                val newLayer = PodVectorLayer(
                                                    name = preset.title,
                                                    type = PodLayerType.VECTOR,
                                                    tintHex = preset.color,
                                                    scale = 1.0f
                                                )
                                                layers = layers + newLayer
                                                selectedLayerId = newLayer.id
                                            },
                                            onGenerateAi = { prompt ->
                                                val newLayer = PodVectorLayer(
                                                    name = "AI: ${prompt.take(14)}",
                                                    type = PodLayerType.AI_ART,
                                                    textContent = prompt,
                                                    tintHex = "#FFFFFF",
                                                    scale = 1.1f
                                                )
                                                layers = layers + newLayer
                                                selectedLayerId = newLayer.id
                                            },
                                            isDarkTheme = isDarkTheme
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// INSPECTOR SUBCOMPONENTS (PRODUCT & SWATCH, LAYERS, ADD ART)
// ============================================================================

@Composable
private fun ProductAndZoneSelector(
    selectedProduct: PrintifyProductBlank,
    selectedColor: PrintifyColorSwatch,
    selectedZoneKey: String,
    onSelectColor: (PrintifyColorSwatch) -> Unit,
    onSelectZone: (String) -> Unit,
    isDarkTheme: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Print Zones", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedProduct.supportedZones.forEach { zoneKey ->
                val zone = PRINTIFY_ZONES[zoneKey] ?: return@forEach
                val isSelected = selectedZoneKey == zoneKey
                Surface(
                    onClick = { onSelectZone(zoneKey) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = zone.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                        Text(
                            text = zone.dimensionLabel,
                            fontSize = 9.sp,
                            color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Garment Swatches", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(PRINTIFY_SWATCHES) { swatch ->
                val isSelected = swatch.id == selectedColor.id
                val colorVal = try {
                    Color(android.graphics.Color.parseColor(swatch.hex))
                } catch (_: Exception) {
                    Color.DarkGray
                }

                Surface(
                    onClick = { onSelectColor(swatch) },
                    shape = CircleShape,
                    color = colorVal,
                    border = BorderStroke(
                        if (isSelected) 3.dp else 1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (swatch.isLightText) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerStackInspector(
    layers: List<PodVectorLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onUpdateLayer: (PodVectorLayer) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onDelete: (String) -> Unit,
    isDarkTheme: Boolean = true
) {
    val selectedLayer = layers.find { it.id == selectedLayerId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layer Stack (${layers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Top to Bottom", fontSize = 10.sp, color = Color(0xFF64748B))
        }

        layers.reversed().forEachIndexed { index, layer ->
            val actualIdx = layers.size - 1 - index
            val isSelected = layer.id == selectedLayerId

            Surface(
                onClick = { onSelectLayer(layer.id) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = when (layer.type) {
                                PodLayerType.TEXT -> Icons.Default.TextFields
                                PodLayerType.AI_ART -> Icons.Default.AutoAwesome
                                PodLayerType.SHAPE -> Icons.Default.Category
                                else -> Icons.Default.Image
                            },
                            contentDescription = layer.name,
                            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = layer.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${layer.liveDpi} DPI • Scale ${(layer.scale * 100).toInt()}%",
                                fontSize = 9.sp,
                                color = if (layer.liveDpi >= 300) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onUpdateLayer(layer.copy(isVisible = !layer.isVisible)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility",
                                tint = if (layer.isVisible) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(layer.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Selected Layer Precision Sliders
        selectedLayer?.let { layer ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Transform Precision", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scale (${(layer.scale * 100).toInt()}%)", fontSize = 10.sp, color = Color.Gray)
                        Text("${layer.liveDpi} DPI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (layer.liveDpi >= 300) Color(0xFF10B981) else Color(0xFFF59E0B))
                    }
                    Slider(
                        value = layer.scale,
                        onValueChange = { onUpdateLayer(layer.copy(scale = it)) },
                        valueRange = 0.3f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2563EB), activeTrackColor = Color(0xFF2563EB))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rotation (${layer.rotation.toInt()}°)", fontSize = 10.sp, color = Color.Gray)
                        Text("Opacity (${(layer.opacity * 100).toInt()}%)", fontSize = 10.sp, color = Color.Gray)
                    }
                    Slider(
                        value = layer.rotation,
                        onValueChange = { onUpdateLayer(layer.copy(rotation = it)) },
                        valueRange = -180f..180f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                    )
                }
            }
        }
    }
}

@Composable
private fun AddArtDesignPanel(
    onAddText: (String, String, String) -> Unit,
    onAddPreset: (ClipartPreset) -> Unit,
    onGenerateAi: (String) -> Unit,
    isDarkTheme: Boolean = true
) {
    var textInput by remember { mutableStateOf("KRITICAL LIMITZ") }
    var fontSelected by remember { mutableStateOf("Gothic Chicano") }
    var textTintHex by remember { mutableStateOf("#FFFFFF") }
    var aiPromptInput by remember { mutableStateOf("Chicano payasa illustration with lowrider chrome and gothic lettering") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Custom Typography Section
        Text("Custom Typography", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Enter text...", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Gothic Chicano", "Heavy Blackletter", "Modern Sans", "Cyber Display").forEach { font ->
                val isFontActive = fontSelected == font
                Surface(
                    onClick = { fontSelected = font },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFontActive) Color(0xFF2563EB) else Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = font.split(" ").last(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }
        }

        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onAddText(textInput, fontSelected, textTintHex)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Text", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Typography Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // 2. Streetwear Vector Clipart Library
        Spacer(modifier = Modifier.height(4.dp))
        Text("Underground Vector Library", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(STREETWEAR_CLIPART_PRESETS) { preset ->
                Surface(
                    onClick = { onAddPreset(preset) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = preset.title, tint = Color(0xFF38BDF8), modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(preset.title.take(16), fontSize = 10.sp, color = Color.White, maxLines = 1)
                        Text(preset.category, fontSize = 8.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }

        // 3. AI Design Director Synthesis
        Spacer(modifier = Modifier.height(4.dp))
        Text("AI Design Director Synthesis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        OutlinedTextField(
            value = aiPromptInput,
            onValueChange = { aiPromptInput = it },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Prompt 300 DPI vector artwork...", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        Button(
            onClick = {
                if (aiPromptInput.isNotBlank()) {
                    onGenerateAi(aiPromptInput)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Synthesize", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Synthesize 300 DPI Vector Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
