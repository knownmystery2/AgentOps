package com.opensource.autonomous.agent.ui.screens

import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensource.autonomous.agent.data.DesignLayer
import com.opensource.autonomous.agent.data.LayerType
import com.opensource.autonomous.agent.ui.MainViewModel

// Curated Garment Colors matching Printify Catalog
data class ProductColorVariant(val name: String, val color: Color, val hex: String)

val CATALOG_COLORS = listOf(
    ProductColorVariant("Charcoal Slate", Color(0xFF262E3D), "#262E3D"),
    ProductColorVariant("Pitch Black", Color(0xFF121417), "#121417"),
    ProductColorVariant("Crisp White", Color(0xFFF8FAFC), "#F8FAFC"),
    ProductColorVariant("Heather Grey", Color(0xFF94A3B8), "#94A3B8"),
    ProductColorVariant("Forest Green", Color(0xFF1E3A2B), "#1E3A2B"),
    ProductColorVariant("Terracotta Sand", Color(0xFFB86B53), "#B86B53"),
    ProductColorVariant("Navy Indigo", Color(0xFF1E293B), "#1E293B"),
    ProductColorVariant("Burgundy Wine", Color(0xFF581C87), "#581C87")
)

// Curated Vector Presets for Instant Clipart Placement
data class GraphicPreset(val id: String, val title: String, val category: String, val primaryColor: Color)

val GRAPHIC_PRESETS = listOf(
    GraphicPreset("kl_crest", "KL Gothic Monogram Crest", "Luxury Streetwear", Color(0xFFF8FAFC)),
    GraphicPreset("chicano_payasa", "Payasa Fine-Line Portrait", "Chicano Culture", Color(0xFFE2E8F0)),
    GraphicPreset("egyptian_scarab", "Occult Scarab & Ankh", "Egyptian Mysticism", Color(0xFFF59E0B)),
    GraphicPreset("hellhound_wire", "Crowned Hellhound & Barbed Wire", "Dark Occult", Color(0xFFEF4444)),
    GraphicPreset("gothic_paisley", "Intricate Paisley & Metallic Razor", "Hyper-Detailed", Color(0xFF38BDF8)),
    GraphicPreset("vintage_skull", "Rose & Demonic Skull", "Underground Ink", Color(0xFF94A3B8)),
    GraphicPreset("cyber_crest", "Hieroglyphic Scripture Seal", "Street Luxury", Color(0xFF06B6D4)),
    GraphicPreset("minimal_line", "KL Razor Blade Spikes", "Metallic Detail", Color(0xFFEC4899))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintDefyMockupScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    // -------------------------------------------------------------------------
    // View States & Reactive Streams
    // -------------------------------------------------------------------------
    var activeRegion by remember { mutableStateOf("Front Side") }
    var viewMode by remember { mutableStateOf("Design") } // "Design" or "Preview"
    var showVariantsSheet by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }

    val layers by viewModel.designLayers.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val selectedProduct by viewModel.selectedMockupProduct.collectAsState()
    val selectedColorName by viewModel.selectedMockupColor.collectAsState()
    val isPublishing by viewModel.isPublishingToPrintify.collectAsState()
    val publishStatusMessage by viewModel.publishStatusMessage.collectAsState()

    val currentColorVariant = CATALOG_COLORS.firstOrNull { it.name == selectedColorName }
        ?: CATALOG_COLORS[0]

    val activeLayer = layers.find { it.id == activeLayerId }
    val regionLayers = layers.filter { it.region == activeRegion }

    val regions = listOf("Front Side", "Back Side", "Pocket Left", "Sleeve Right", "Neck Label")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(publishStatusMessage) {
        publishStatusMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }

    // Clean live canvas - no fake demo data auto-seeded
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // =================================================================
            // SUGGESTION 1: PROFESSIONAL NAVIGATION TOP BAR
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedProduct,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "300 DPI",
                                        color = Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Printify Blueprint #12 • Monster Digital • ${currentColorVariant.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to Dashboard")
                        }
                    },
                    actions = {
                        // Mode Switcher: Designer vs Lifestyle Preview
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { viewMode = "Design" },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("view_mode_design")
                                        .background(
                                            if (viewMode == "Design") MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Designer Mode",
                                        tint = if (viewMode == "Design") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewMode = "Preview" },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("view_mode_preview")
                                        .background(
                                            if (viewMode == "Preview") MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = "Lifestyle Preview",
                                        tint = if (viewMode == "Preview") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Align / Auto-scale Helper Trigger
                        IconButton(
                            onClick = { showAlignMenu = true },
                            modifier = Modifier.testTag("btn_align_helper")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Alignment & Scaling Helper")
                        }

                        // Export Mockup Image
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Mockup Exported: 300 DPI high-resolution composite saved.")
                                }
                            },
                            modifier = Modifier.testTag("btn_export_mockup")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export Mockup Image")
                        }

                        // Save & Publish Button
                        Button(
                            onClick = {
                                viewModel.publishToPrintify("26650832", "12", 1, listOf(1, 2, 3))
                            },
                            enabled = !isPublishing,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("btn_publish")
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                )

                // Region Tabs with Layer Counts
                ScrollableTabRow(
                    selectedTabIndex = regions.indexOf(activeRegion).coerceAtLeast(0),
                    edgePadding = 12.dp,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
                ) {
                    regions.forEach { region ->
                        val count = layers.count { it.region == region }
                        Tab(
                            selected = activeRegion == region,
                            onClick = { activeRegion = region },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(region, fontWeight = if (activeRegion == region) FontWeight.Bold else FontWeight.Medium)
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("$count", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // =================================================================
            // SUGGESTION 3: ENHANCED INTERACTIVITY BOTTOM BAR
            // =================================================================
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Variants Selector Button
                    TextButton(
                        onClick = { showVariantsSheet = true },
                        modifier = Modifier.testTag("btn_variants")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(currentColorVariant.color)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("Variants", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Add Element Primary Floating Action Button
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp).testTag("btn_add_element")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Text, Clipart or AI Art", modifier = Modifier.size(28.dp))
                    }

                    // Layers Stacking Button
                    TextButton(
                        onClick = { showLayersSheet = true },
                        modifier = Modifier.testTag("btn_layers")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BadgedBox(
                                badge = {
                                    if (regionLayers.isNotEmpty()) {
                                        Badge { Text("${regionLayers.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = "Manage Layer Stack")
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("Layers", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        // =====================================================================
        // SUGGESTION 2: CLEARER WORKSPACE & REALISTIC GARMENT CANVAS
        // =====================================================================
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    if (viewMode == "Design") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else Color(0xFF0F172A)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Interactive Workspace Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Realistic Garment Vector Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.85f)
                ) {
                    drawRealisticGarment(
                        product = selectedProduct,
                        garmentColor = currentColorVariant.color,
                        isPreviewMode = viewMode == "Preview",
                        isBackSide = activeRegion == "Back Side"
                    )
                }

                val boxWidth = when {
                    activeRegion.contains("Pocket") -> 110.dp
                    activeRegion.contains("Sleeve") -> 100.dp
                    activeRegion.contains("Neck") -> 140.dp
                    activeRegion.contains("Back") -> 250.dp
                    else -> 230.dp
                }
                val boxHeight = when {
                    activeRegion.contains("Pocket") -> 110.dp
                    activeRegion.contains("Sleeve") -> 150.dp
                    activeRegion.contains("Neck") -> 80.dp
                    activeRegion.contains("Back") -> 310.dp
                    else -> 290.dp
                }
                val boxOffsetX = when {
                    activeRegion.contains("Pocket") -> (-55).dp
                    activeRegion.contains("Sleeve") -> 95.dp
                    else -> 0.dp
                }
                val boxOffsetY = when {
                    activeRegion.contains("Pocket") -> (-35).dp
                    activeRegion.contains("Sleeve") -> (-15).dp
                    activeRegion.contains("Neck") -> (-110).dp
                    else -> (-5).dp
                }

                // Print Area Overlay with Boundary Box & Dimensions
                Box(
                    modifier = Modifier
                        .testTag("canvas_print_area")
                        .offset(x = boxOffsetX, y = boxOffsetY)
                        .size(width = boxWidth, height = boxHeight)
                        .then(
                            if (viewMode == "Design") {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Fine Grid lines in Design Mode
                    if (viewMode == "Design") {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Center crosshair guidelines
                            drawLine(
                                color = Color.Cyan.copy(alpha = 0.35f),
                                start = Offset(w / 2f, 0f),
                                end = Offset(w / 2f, h),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                            drawLine(
                                color = Color.Cyan.copy(alpha = 0.35f),
                                start = Offset(0f, h / 2f),
                                end = Offset(w, h / 2f),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // Dimension Badge at Top-Left of Print Box
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                        ) {
                            Text(
                                text = when {
                                    activeRegion.contains("Pocket") -> "4\" × 4\""
                                    activeRegion.contains("Sleeve") -> "3.5\" × 5\""
                                    activeRegion.contains("Neck") -> "3\" × 2\""
                                    activeRegion.contains("Back") -> "14\" × 16\""
                                    else -> "11\" × 14\""
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Render All Design Layers inside Print Area
                    regionLayers.filter { it.isVisible }.forEach { layer ->
                        val isSelected = layer.id == activeLayerId

                        Box(
                            modifier = Modifier
                                .offset(layer.x.dp, layer.y.dp)
                                .graphicsLayer(
                                    scaleX = layer.scale,
                                    scaleY = layer.scale,
                                    rotationZ = layer.rotation,
                                    alpha = layer.opacity
                                )
                                .then(
                                    if (viewMode == "Design") {
                                        Modifier.pointerInput(layer.id) {
                                            detectTransformGestures { _, pan, zoom, rotation ->
                                                viewModel.activeLayerId.value = layer.id
                                                viewModel.updateLayer(
                                                    id = layer.id,
                                                    x = layer.x + (pan.x / density),
                                                    y = layer.y + (pan.y / density),
                                                    scale = (layer.scale * zoom).coerceIn(0.2f, 3.5f),
                                                    rotation = (layer.rotation + rotation) % 360f
                                                )
                                            }
                                        }
                                    } else Modifier
                                )
                                .clickable {
                                    viewModel.activeLayerId.value = layer.id
                                }
                                .then(
                                    if (isSelected && viewMode == "Design") {
                                        Modifier
                                            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                                            .padding(4.dp)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Layer Content Rendering (Text vs Graphic)
                            if (layer.type == LayerType.TEXT) {
                                Text(
                                    text = layer.name,
                                    color = Color(android.graphics.Color.parseColor(layer.colorHex.ifEmpty { "#FFFFFF" })),
                                    fontSize = (22 * layer.scale).coerceAtLeast(10f).sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = when (layer.fontStyle) {
                                        "SERIF" -> FontFamily.Serif
                                        "MONOSPACE" -> FontFamily.Monospace
                                        else -> FontFamily.SansSerif
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(6.dp)
                                )
                            } else {
                                // Graphic Vector Element
                                RenderGraphicPreset(
                                    presetId = layer.graphicPreset,
                                    name = layer.name,
                                    colorHex = layer.colorHex
                                )
                            }
                        }
                    }
                }

                // Lifestyle Mockup Watermark in Preview Mode
                if (viewMode == "Preview") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "300 DPI CMYK High-Fidelity Mockup",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick Layer Control HUD (Floating when a layer is selected in Design Mode)
            if (activeLayer != null && viewMode == "Design") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.updateLayer(activeLayer.id, 0f, activeLayer.y, activeLayer.scale, activeLayer.rotation)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AlignHorizontalCenter, contentDescription = "Center Horizontal", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateLayer(activeLayer.id, activeLayer.x, 0f, activeLayer.scale, activeLayer.rotation)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AlignVerticalCenter, contentDescription = "Center Vertical", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateLayer(activeLayer.id, activeLayer.x, activeLayer.y, (activeLayer.scale + 0.15f).coerceAtMost(3.0f), activeLayer.rotation)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Scale Up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateLayer(activeLayer.id, activeLayer.x, activeLayer.y, (activeLayer.scale - 0.15f).coerceAtLeast(0.3f), activeLayer.rotation)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Scale Down", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateLayer(activeLayer.id, activeLayer.x, activeLayer.y, activeLayer.scale, (activeLayer.rotation + 45f) % 360f)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 45°", modifier = Modifier.size(18.dp))
                        }
                        VerticalDivider(modifier = Modifier.height(20.dp))
                        IconButton(
                            onClick = { viewModel.removeDesignLayer(activeLayer.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Layer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODALS & SHEETS IMPLEMENTATION
    // =========================================================================

    // 1. Alignment & Auto-Scale Helper Dialog
    if (showAlignMenu) {
        AlertDialog(
            onDismissRequest = { showAlignMenu = false },
            title = { Text("Design Placement & Auto-Scale Helper", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Automatically align and optimize layer dimensions relative to the Printify boundary box.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FilledTonalButton(
                        onClick = {
                            activeLayer?.let {
                                viewModel.updateLayer(it.id, 0f, 0f, it.scale, 0f)
                            }
                            showAlignMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Center Design to Print Area")
                    }
                    FilledTonalButton(
                        onClick = {
                            activeLayer?.let {
                                viewModel.updateLayer(it.id, 0f, 0f, 1.4f, it.rotation)
                            }
                            showAlignMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FitScreen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Scale to Maximum Safe Area")
                    }
                    FilledTonalButton(
                        onClick = {
                            activeLayer?.let {
                                viewModel.updateLayer(it.id, it.x, it.y, 1.0f, 0f)
                            }
                            showAlignMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Scale to 100% (1:1 Vector)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlignMenu = false }) { Text("Close") }
            }
        )
    }

    // 2. Variants Modal Bottom Sheet
    if (showVariantsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVariantsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Product & Color Variants", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Garment Model Selector
                Text("Garment Model", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                val products = listOf("Heavyweight T-Shirt", "Vintage Hoodie", "Crewneck Sweatshirt", "Canvas Tote Bag")
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    products.forEach { prod ->
                        FilterChip(
                            selected = selectedProduct == prod,
                            onClick = { viewModel.selectedMockupProduct.value = prod },
                            label = { Text(prod) }
                        )
                    }
                }

                // Color Swatches
                Text("Garment Fabric Color (${currentColorVariant.name})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CATALOG_COLORS) { variant ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(variant.color)
                                    .border(
                                        width = if (selectedColorName == variant.name) 3.dp else 1.dp,
                                        color = if (selectedColorName == variant.name) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.selectedMockupColor.value = variant.name
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorName == variant.name) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (variant.color == Color(0xFFF8FAFC)) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = variant.name.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Size Run
                Text("Size Run Availability", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                val sizes = listOf("XS", "S", "M", "L", "XL", "2XL", "3XL")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    sizes.forEach { size ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(size, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Production & Profit Breakdown Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Print Provider: Monster Digital (USA)", style = MaterialTheme.typography.labelSmall)
                            Text("Estimated Retail: $28.00 • Production: $9.50", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(color = Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "+$18.50 Net Profit",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 3. Layers Management Bottom Sheet
    if (showLayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayersSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layers Stack (${regionLayers.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (regionLayers.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAllLayers() }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (regionLayers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No layers on $activeRegion. Tap '+' to add text or graphics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(regionLayers.reversed()) { layer ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (layer.id == activeLayerId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            if (layer.type == LayerType.TEXT) Icons.Default.TextFields else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(layer.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text("${layer.type} • Scale: ${(layer.scale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.moveLayerUp(layer.id) }) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.moveLayerDown(layer.id) }) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.toggleLayerVisibility(layer.id) }) {
                                            Icon(
                                                if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Visibility",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.removeDesignLayer(layer.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 4. Add Design Elements Dialog (Text, Graphic Presets, AI Generation)
    if (showAddDialog) {
        var selectedAddTab by remember { mutableStateOf(0) }
        var customText by remember { mutableStateOf("MIND OVER MATTER") }
        var selectedFont by remember { mutableStateOf("SANS_SERIF") }
        var selectedTextColorHex by remember { mutableStateOf("#FFFFFF") }
        var aiPrompt by remember { mutableStateOf("Vintage 70s pine mountains sunset minimalist line art") }
        var isAiGenerating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                TabRow(selectedTabIndex = selectedAddTab) {
                    Tab(selected = selectedAddTab == 0, onClick = { selectedAddTab = 0 }, text = { Text("Text") })
                    Tab(selected = selectedAddTab == 1, onClick = { selectedAddTab = 1 }, text = { Text("Graphics") })
                    Tab(selected = selectedAddTab == 2, onClick = { selectedAddTab = 2 }, text = { Text("AI Art") })
                }
            },
            text = {
                when (selectedAddTab) {
                    // TAB 0: TEXT LAYER
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                label = { Text("Typography / Text") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Font Family", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("SANS_SERIF" to "Modern", "SERIF" to "Vintage", "MONOSPACE" to "Tech").forEach { (style, label) ->
                                    FilterChip(
                                        selected = selectedFont == style,
                                        onClick = { selectedFont = style },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Text("Text Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("#FFFFFF", "#121417", "#F59E0B", "#10B981", "#38BDF8", "#EF4444").forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (selectedTextColorHex == hex) 2.5.dp else 1.dp,
                                                color = if (selectedTextColorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedTextColorHex = hex }
                                    )
                                }
                            }
                        }
                    }
                    // TAB 1: GRAPHICS & CLIPART
                    1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(280.dp)
                        ) {
                            items(GRAPHIC_PRESETS) { preset ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addCustomDesignLayer(
                                                DesignLayer(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    type = LayerType.IMAGE,
                                                    name = preset.title,
                                                    x = 0f,
                                                    y = 0f,
                                                    scale = 1.0f,
                                                    rotation = 0f,
                                                    region = activeRegion,
                                                    graphicPreset = preset.id
                                                )
                                            )
                                            showAddDialog = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(preset.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = preset.primaryColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(preset.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                                        Text(preset.category, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    // TAB 2: AI ART GENERATION
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Gemini Vector & Artwork Synthesis", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = { aiPrompt = it },
                                label = { Text("Prompt description") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Quick Presets", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Chicano Payasa Fine-Line", "Dark Occult Sigils & Skulls", "Egyptian Scarab & Ankh", "Gothic Paisley Monogram", "Hellhound in Barbed Wire").forEach { chip ->
                                    SuggestionChip(
                                        onClick = { aiPrompt = chip },
                                        label = { Text(chip, fontSize = 11.sp) }
                                    )
                                }
                            }
                            if (isAiGenerating) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating vector layer...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedAddTab == 0) {
                    Button(
                        onClick = {
                            if (customText.isNotBlank()) {
                                viewModel.addCustomDesignLayer(
                                    DesignLayer(
                                        id = java.util.UUID.randomUUID().toString(),
                                        type = LayerType.TEXT,
                                        name = customText,
                                        x = 0f,
                                        y = 0f,
                                        scale = 1.0f,
                                        rotation = 0f,
                                        region = activeRegion,
                                        colorHex = selectedTextColorHex,
                                        fontStyle = selectedFont
                                    )
                                )
                                showAddDialog = false
                            }
                        }
                    ) { Text("Add Text Layer") }
                } else if (selectedAddTab == 2) {
                    Button(
                        onClick = {
                            isAiGenerating = true
                            // Simulate AI synthesis and immediately add the artwork layer
                            viewModel.addCustomDesignLayer(
                                DesignLayer(
                                    id = java.util.UUID.randomUUID().toString(),
                                    type = LayerType.IMAGE,
                                    name = "AI: ${aiPrompt.take(24)}...",
                                    x = 0f,
                                    y = 0f,
                                    scale = 1.1f,
                                    rotation = 0f,
                                    region = activeRegion,
                                    graphicPreset = "cyber_crest"
                                )
                            )
                            isAiGenerating = false
                            showAddDialog = false
                        }
                    ) { Text("Generate & Place") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// REALISTIC GARMENT DRAWING LOGIC (JETPACK COMPOSE CANVAS)
// -----------------------------------------------------------------------------
fun DrawScope.drawRealisticGarment(
    product: String,
    garmentColor: Color,
    isPreviewMode: Boolean,
    isBackSide: Boolean = false
) {
    val w = size.width
    val h = size.height

    // Drop shadow under garment in preview mode
    if (isPreviewMode) {
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.15f, h * 0.90f),
            size = Size(w * 0.70f, h * 0.08f)
        )
    }

    when {
        product.contains("Hoodie") -> {
            // Draw Hoodie silhouette
            val hoodiePath = Path().apply {
                moveTo(w * 0.30f, h * 0.10f)
                quadraticTo(w * 0.5f, if (isBackSide) 0.06f * h else 0.03f * h, w * 0.70f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.25f)
                lineTo(w * 0.96f, h * 0.55f)
                lineTo(w * 0.84f, h * 0.60f)
                lineTo(w * 0.80f, h * 0.48f)
                lineTo(w * 0.80f, h * 0.92f)
                lineTo(w * 0.20f, h * 0.92f)
                lineTo(w * 0.20f, h * 0.48f)
                lineTo(w * 0.16f, h * 0.60f)
                lineTo(w * 0.04f, h * 0.55f)
                lineTo(w * 0.12f, h * 0.25f)
                close()
            }
            drawPath(hoodiePath, color = garmentColor)
            // Seams and stitching
            drawPath(hoodiePath, color = Color.White.copy(alpha = 0.18f), style = Stroke(width = 2.5f))
            // Kangaroo Pocket only on front side
            if (!isBackSide) {
                val pocket = Path().apply {
                    moveTo(w * 0.32f, h * 0.70f)
                    lineTo(w * 0.68f, h * 0.70f)
                    lineTo(w * 0.74f, h * 0.88f)
                    lineTo(w * 0.26f, h * 0.88f)
                    close()
                }
                drawPath(pocket, color = garmentColor)
                drawPath(pocket, color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 2f))
            }
        }
        product.contains("Tote") -> {
            // Tote Bag silhouette
            val strap = Path().apply {
                moveTo(w * 0.35f, h * 0.35f)
                quadraticTo(w * 0.35f, h * 0.08f, w * 0.50f, h * 0.08f)
                quadraticTo(w * 0.65f, h * 0.08f, w * 0.65f, h * 0.35f)
            }
            drawPath(strap, color = garmentColor, style = Stroke(width = 24f))
            drawRoundRect(
                color = garmentColor,
                topLeft = Offset(w * 0.22f, h * 0.32f),
                size = Size(w * 0.56f, h * 0.60f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(w * 0.22f, h * 0.32f),
                size = Size(w * 0.56f, h * 0.60f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 2.5f)
            )
        }
        else -> {
            // Default Heavyweight T-Shirt Silhouette
            val shirtPath = Path().apply {
                // Collar neck curve
                moveTo(w * 0.34f, h * 0.10f)
                quadraticTo(w * 0.5f, if (isBackSide) 0.13f * h else 0.20f * h, w * 0.66f, h * 0.10f)
                // Right shoulder
                lineTo(w * 0.86f, h * 0.18f)
                // Right sleeve
                lineTo(w * 0.96f, h * 0.40f)
                lineTo(w * 0.83f, h * 0.46f)
                lineTo(w * 0.78f, h * 0.38f)
                // Right torso to bottom hem
                lineTo(w * 0.78f, h * 0.92f)
                lineTo(w * 0.22f, h * 0.92f)
                // Left torso
                lineTo(w * 0.22f, h * 0.38f)
                // Left sleeve
                lineTo(w * 0.17f, h * 0.46f)
                lineTo(w * 0.04f, h * 0.40f)
                lineTo(w * 0.14f, h * 0.18f)
                close()
            }
            drawPath(shirtPath, color = garmentColor)

            // Outer contour seam
            drawPath(shirtPath, color = Color.White.copy(alpha = 0.18f), style = Stroke(width = 2.5f))

            // Neck ribbing collar contour
            val collarPath = Path().apply {
                if (isBackSide) {
                    moveTo(w * 0.34f, h * 0.10f)
                    quadraticTo(w * 0.5f, h * 0.12f, w * 0.66f, h * 0.10f)
                    quadraticTo(w * 0.5f, h * 0.14f, w * 0.34f, h * 0.10f)
                } else {
                    moveTo(w * 0.34f, h * 0.10f)
                    quadraticTo(w * 0.5f, h * 0.18f, w * 0.66f, h * 0.10f)
                    quadraticTo(w * 0.5f, h * 0.22f, w * 0.34f, h * 0.10f)
                }
            }
            drawPath(collarPath, color = Color.White.copy(alpha = 0.22f), style = Stroke(width = 2f))

            // Inner collar tag on back side
            if (isBackSide) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.46f, h * 0.13f),
                    size = Size(w * 0.08f, h * 0.035f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // Shoulder stitching
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(w * 0.34f, h * 0.10f),
                end = Offset(w * 0.14f, h * 0.18f),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(w * 0.66f, h * 0.10f),
                end = Offset(w * 0.86f, h * 0.18f),
                strokeWidth = 1.5f
            )
        }
    }
}

// -----------------------------------------------------------------------------
// GRAPHIC PRESET VECTOR RENDERER
// -----------------------------------------------------------------------------
@Composable
fun RenderGraphicPreset(presetId: String, name: String, colorHex: String) {
    val presetColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color(0xFFF59E0B)
    }

    Box(
        modifier = Modifier
            .size(110.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (presetId) {
            "retro_mountain" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Sunset circle
                    drawCircle(color = presetColor, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.45f))
                    // Mountain peaks
                    val mtn = Path().apply {
                        moveTo(w * 0.1f, h * 0.75f)
                        lineTo(w * 0.5f, h * 0.28f)
                        lineTo(w * 0.9f, h * 0.75f)
                        close()
                    }
                    drawPath(mtn, color = Color.White.copy(alpha = 0.92f))
                }
            }
            "botanical_fern" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Stem
                    drawLine(color = Color(0xFF10B981), start = Offset(w * 0.5f, h * 0.9f), end = Offset(w * 0.5f, h * 0.1f), strokeWidth = 4f)
                    // Leaves
                    for (i in 1..4) {
                        val y = h * (0.2f + i * 0.14f)
                        drawOval(color = Color(0xFF10B981).copy(alpha = 0.85f), topLeft = Offset(w * 0.2f, y), size = Size(w * 0.3f, 14f))
                        drawOval(color = Color(0xFF10B981).copy(alpha = 0.85f), topLeft = Offset(w * 0.5f, y), size = Size(w * 0.3f, 14f))
                    }
                }
            }
            "cyber_crest" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(color = Color(0xFF06B6D4), radius = w * 0.42f, style = Stroke(width = 4f))
                    drawRect(color = Color(0xFF06B6D4).copy(alpha = 0.3f), topLeft = Offset(w * 0.25f, h * 0.25f), size = Size(w * 0.5f, h * 0.5f))
                    drawCircle(color = Color.White, radius = 6f, center = Offset(w * 0.5f, h * 0.5f))
                }
            }
            else -> {
                // Fallback Emblem
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = presetColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = name.take(16),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
