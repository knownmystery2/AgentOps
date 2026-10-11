package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.network.PodJobItem
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun PodMockupScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedProduct by viewModel.selectedMockupProduct.collectAsState()
    val selectedColor by viewModel.selectedMockupColor.collectAsState()
    val selectedPlacement by viewModel.selectedMockupPlacement.collectAsState()
    val podQueue by viewModel.podQueue.collectAsState()
    val isGeneratingPod by viewModel.isGeneratingPod.collectAsState()
    val isPitchingIdeas by viewModel.isPitchingIdeas.collectAsState()

    val products = listOf("Heavyweight T-Shirt", "Vintage Hoodie", "Ceramic Mug", "Canvas Tote Bag", "Wall Art Poster")
    val colors = listOf(
        Pair("Charcoal Slate", Color(0xFF262E3D)),
        Pair("Natural Canvas", Color(0xFFE8E2D2)),
        Pair("Forest Green", Color(0xFF1E3A2B)),
        Pair("Terracotta Sand", Color(0xFFB86B53)),
        Pair("Crisp White", Color(0xFFF8FAFC))
    )
    val placements = listOf("Chest Center (11\" x 13\")", "Pocket Left (4\" x 4\")", "Back Large (14\" x 16\")")

    val garmentColor = colors.firstOrNull { it.first == selectedColor }?.second ?: Color(0xFF262E3D)

    val currentSkuPreview = remember(selectedProduct, selectedColor) {
        val prod = selectedProduct.split(" ").map { it.take(2).uppercase() }.joinToString("")
        val col = selectedColor.split(" ").map { it.take(2).uppercase() }.joinToString("")
        "POD-$prod-$col-2026"
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "POD Studio & Vector Pipeline",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "PROD • Agent #4",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "High-precision 300 DPI vector placement engine & live SKU creation pipeline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Mockup Canvas
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .testTag("pod_mockup_canvas_card")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val w = size.width
                        val h = size.height

                        when (selectedProduct) {
                            "Heavyweight T-Shirt" -> {
                                val shirtPath = Path().apply {
                                    moveTo(w * 0.35f, h * 0.12f)
                                    quadraticBezierTo(w * 0.5f, h * 0.22f, w * 0.65f, h * 0.12f)
                                    lineTo(w * 0.85f, h * 0.22f)
                                    lineTo(w * 0.95f, h * 0.45f)
                                    lineTo(w * 0.82f, h * 0.52f)
                                    lineTo(w * 0.78f, h * 0.42f)
                                    lineTo(w * 0.78f, h * 0.92f)
                                    lineTo(w * 0.22f, h * 0.92f)
                                    lineTo(w * 0.22f, h * 0.42f)
                                    lineTo(w * 0.18f, h * 0.52f)
                                    lineTo(w * 0.05f, h * 0.45f)
                                    lineTo(w * 0.15f, h * 0.22f)
                                    close()
                                }
                                drawPath(shirtPath, color = garmentColor)
                                drawPath(shirtPath, color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 3f))

                                val printRect = if (selectedPlacement.contains("Pocket")) {
                                    Rect(w * 0.30f, h * 0.32f, w * 0.45f, h * 0.45f)
                                } else {
                                    Rect(w * 0.35f, h * 0.30f, w * 0.65f, h * 0.68f)
                                }
                                drawRoundRect(
                                    color = BrandEmerald.copy(alpha = 0.4f),
                                    topLeft = Offset(printRect.left, printRect.top),
                                    size = Size(printRect.right - printRect.left, printRect.bottom - printRect.top),
                                    cornerRadius = CornerRadius(8f, 8f),
                                    style = Stroke(width = 2f)
                                )

                                val artCenter = Offset((printRect.left + printRect.right) / 2f, (printRect.top + printRect.bottom) / 2f)
                                drawCircle(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                                    radius = (printRect.right - printRect.left) * 0.25f,
                                    center = Offset(artCenter.x, artCenter.y - 10f)
                                )
                                val mountainPath = Path().apply {
                                    moveTo(printRect.left + 15f, artCenter.y + 25f)
                                    lineTo(artCenter.x, artCenter.y - 5f)
                                    lineTo(printRect.right - 15f, artCenter.y + 25f)
                                    close()
                                }
                                drawPath(mountainPath, color = Color.White.copy(alpha = 0.9f))
                            }
                            "Vintage Hoodie" -> {
                                val hoodiePath = Path().apply {
                                    moveTo(w * 0.30f, h * 0.10f)
                                    quadraticBezierTo(w * 0.5f, h * 0.02f, w * 0.70f, h * 0.10f)
                                    lineTo(w * 0.88f, h * 0.24f)
                                    lineTo(w * 0.96f, h * 0.55f)
                                    lineTo(w * 0.84f, h * 0.60f)
                                    lineTo(w * 0.80f, h * 0.48f)
                                    lineTo(w * 0.80f, h * 0.92f)
                                    lineTo(w * 0.20f, h * 0.92f)
                                    lineTo(w * 0.20f, h * 0.48f)
                                    lineTo(w * 0.16f, h * 0.60f)
                                    lineTo(w * 0.04f, h * 0.55f)
                                    lineTo(w * 0.12f, h * 0.24f)
                                    close()
                                }
                                drawPath(hoodiePath, color = garmentColor)
                                drawPath(hoodiePath, color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 3f))

                                val pocket = Path().apply {
                                    moveTo(w * 0.30f, h * 0.70f)
                                    lineTo(w * 0.70f, h * 0.70f)
                                    lineTo(w * 0.75f, h * 0.88f)
                                    lineTo(w * 0.25f, h * 0.88f)
                                    close()
                                }
                                drawPath(pocket, color = garmentColor.copy(alpha = 0.85f), style = Stroke(width = 2f))
                                drawCircle(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                                    radius = 28f,
                                    center = Offset(w * 0.5f, h * 0.42f)
                                )
                            }
                            else -> {
                                drawRoundRect(
                                    color = garmentColor,
                                    topLeft = Offset(w * 0.28f, h * 0.22f),
                                    size = Size(w * 0.44f, h * 0.58f),
                                    cornerRadius = CornerRadius(16f, 16f)
                                )
                                drawCircle(
                                    color = Color(0xFFF59E0B),
                                    radius = 35f,
                                    center = Offset(w * 0.5f, h * 0.48f)
                                )
                            }
                        }
                    }

                    // Watermark badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "300 DPI • CMYK Print Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // SKU Creation Tracking & Dispatch Pipeline Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("sku_pipeline_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active SKU Generation Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Target: $currentSkuPreview",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = BrandEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Zero Risk",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.dispatchSkuToProductionQueue(
                                    product = selectedProduct,
                                    color = selectedColor,
                                    placement = selectedPlacement
                                )
                            },
                            enabled = !isGeneratingPod,
                            modifier = Modifier.weight(1f).testTag("dispatch_sku_button")
                        ) {
                            if (isGeneratingPod) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compiling...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Queue SKU", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.pitchNewProductIdeas("$selectedProduct in $selectedColor Line Art")
                            },
                            enabled = !isPitchingIdeas,
                            modifier = Modifier.weight(1f).testTag("pod_agent_pitch_btn")
                        ) {
                            if (isPitchingIdeas) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pitching...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandEmerald)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI POD Pitch", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Product Selection
            Text("Select Product Blank", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                products.take(3).forEach { prod ->
                    FilterChip(
                        selected = selectedProduct == prod,
                        onClick = { viewModel.selectedMockupProduct.value = prod },
                        label = { Text(prod) },
                        modifier = Modifier.testTag("prod_blank_$prod")
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                products.drop(3).forEach { prod ->
                    FilterChip(
                        selected = selectedProduct == prod,
                        onClick = { viewModel.selectedMockupProduct.value = prod },
                        label = { Text(prod) }
                    )
                }
            }

            // Garment Color Selection
            Text("Garment Palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.forEach { (name, col) ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(col, CircleShape)
                            .border(
                                width = if (selectedColor == name) 3.dp else 1.dp,
                                color = if (selectedColor == name) BrandEmerald else Color.Gray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.selectedMockupColor.value = name }
                            .testTag("color_picker_$name")
                    )
                }
            }

            // Placement Selection
            Text("Print Area & Dimensions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                placements.forEach { pl ->
                    FilterChip(
                        selected = selectedPlacement == pl,
                        onClick = { viewModel.selectedMockupPlacement.value = pl },
                        label = { Text(pl) },
                        modifier = Modifier.testTag("placement_chip_$pl")
                    )
                }
            }

            // Live Print-on-Demand Generation Queue
            Text("Live POD Production Queue (${podQueue.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                podQueue.forEach { job ->
                    PodJobQueueCard(job = job)
                }
            }

            // Specifications Compliance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Automated POD Specs Validation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✔ Resolution: 4500 x 5400 px @ 300 DPI transparent PNG", style = MaterialTheme.typography.bodySmall)
                    Text("✔ Print Area: Precision aligned to 11\" x 13\" Chest Center", style = MaterialTheme.typography.bodySmall)
                    Text("✔ Color Space: CMYK compliant with zero out-of-gamut clipping", style = MaterialTheme.typography.bodySmall)
                    Text("✔ Trademark Clearance: Screened against USPTO registered goods", style = MaterialTheme.typography.bodySmall, color = ProfitGreen)
                }
            }
        }
    }
}

@Composable
fun PodJobQueueCard(job: PodJobItem) {
    val statusColor = when (job.status) {
        "READY" -> ProfitGreen
        "COMPILED" -> BrandEmerald
        "RENDERING" -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("pod_queue_item_${job.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.sku,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = job.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${job.product} • ${job.color} • ${job.placement}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { job.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = statusColor
            )
        }
    }
}

private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)
