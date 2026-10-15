package com.opensource.autonomous.agent.ui.pod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Pod3dMockupView(
    product: PrintifyProductBlank,
    color: PrintifyColorSwatch,
    zone: PrintifyPrintZone,
    layers: List<PodVectorLayer>,
    onPublish: () -> Unit,
    isPublishing: Boolean,
    isDarkTheme: Boolean = true
) {
    var previewStyle by remember { mutableStateOf("Studio Model") } // "Studio Model", "Flat Lay", "Lookbook Hanger"
    var adjustedRetailPriceCents by remember(product.retailPriceCents) { mutableLongStateOf(product.retailPriceCents) }

    val garmentColor = remember(color.hex) {
        try {
            Color(android.graphics.Color.parseColor(color.hex))
        } catch (_: Exception) {
            Color(0xFF1E293B)
        }
    }

    val profitDollars = (adjustedRetailPriceCents - product.cogsCents).coerceAtLeast(0L) / 100.0
    val marginPercent = if (adjustedRetailPriceCents > 0) {
        (((adjustedRetailPriceCents - product.cogsCents).toDouble() / adjustedRetailPriceCents) * 100).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("pod_3d_mockup_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Lifestyle Perspective Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Studio Model", "Flat Lay", "Lookbook Hanger").forEach { style ->
                FilterChip(
                    selected = previewStyle == style,
                    onClick = { previewStyle = style },
                    label = { Text(style, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("preview_style_$style")
                )
            }
        }

        // 2. High-Fidelity Studio Mockup Composite Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF131B2E) else Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Studio backdrop lighting & floor reflection
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Radial Studio Spotlight Gradient
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)).copy(alpha = 0.8f),
                                if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            ),
                            center = Offset(w * 0.5f, h * 0.35f),
                            radius = w * 0.7f
                        )
                    )

                    // Studio Table / Ground Line
                    drawLine(
                        color = if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFCBD5E1),
                        start = Offset(0f, h * 0.88f),
                        end = Offset(w, h * 0.88f),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Ambient Shadow under garment
                    drawOval(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(w * 0.22f, h * 0.85f),
                        size = Size(w * 0.56f, h * 0.08f)
                    )

                    // Render Perspective-Specific Garment
                    drawLifestyleComposite(w, h, garmentColor, previewStyle)

                    // Render Applied Vector Artworks Composited on Garment Chest/Placement
                    drawCompositedArtwork(w, h, layers)
                }

                // Studio Badge at top-left
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "DPI", tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                        Text(
                            text = "Photorealistic 300 DPI Lifestyle Render",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Provider Badge at bottom-end
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "${product.providerName} • ${product.turnaroundTime}",
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 3. Live E-Commerce Profit Margin Calculator & Price Adjuster
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Live Storefront Margin Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Double-entry whole-cents revenue vs Monster Digital COGS",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$marginPercent% Gross Margin",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Interactive Retail Price Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Suggested Retail Price:", fontSize = 12.sp, color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B))
                        Text(
                            "$${String.format("%.2f", adjustedRetailPriceCents / 100.0)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = adjustedRetailPriceCents.toFloat(),
                        onValueChange = { adjustedRetailPriceCents = (it / 100).toLong() * 100 },
                        valueRange = (product.cogsCents + 500L).toFloat()..9000f,
                        steps = 70,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2563EB),
                            activeTrackColor = Color(0xFF2563EB)
                        )
                    )
                }

                // 3-Metric KPI Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(
                        title = "Retail Price",
                        value = "$${String.format("%.2f", adjustedRetailPriceCents / 100.0)}",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        isDark = isDarkTheme
                    )
                    MetricChip(
                        title = "Printify COGS",
                        value = "$${String.format("%.2f", product.cogsCents / 100.0)}",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        isDark = isDarkTheme
                    )
                    MetricChip(
                        title = "Net Profit / Unit",
                        value = "+$${String.format("%.2f", profitDollars)}",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        isDark = isDarkTheme
                    )
                }

                // Stage & Publish Action Button
                Button(
                    onClick = onPublish,
                    enabled = !isPublishing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("publish_mockup_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Staging Product in Register...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Publish", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Stage & Sync to kriticallimitz.printify.me (+$${String.format("%.2f", profitDollars)})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun DrawScope.drawLifestyleComposite(w: Float, h: Float, garmentColor: Color, style: String) {
    when (style) {
        "Lookbook Hanger" -> {
            // Stainless Steel Hanger
            val hangerPath = Path().apply {
                moveTo(w * 0.50f, h * 0.05f)
                cubicTo(w * 0.53f, h * 0.02f, w * 0.47f, h * 0.02f, w * 0.50f, h * 0.05f)
                lineTo(w * 0.50f, h * 0.09f)
                lineTo(w * 0.72f, h * 0.17f)
                lineTo(w * 0.28f, h * 0.17f)
                close()
            }
            drawPath(hangerPath, color = Color(0xFF94A3B8), style = Stroke(width = 3.dp.toPx()))
        }
        "Flat Lay" -> {
            // Wood Grain Table Plank Line
            drawLine(
                color = Color.Black.copy(alpha = 0.1f),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }

    // Realistic draped garment silhouette with natural folds
    val garment = Path().apply {
        moveTo(w * 0.32f, h * 0.10f)
        cubicTo(w * 0.38f, h * 0.15f, w * 0.62f, h * 0.15f, w * 0.68f, h * 0.10f)
        lineTo(w * 0.88f, h * 0.22f)
        cubicTo(w * 0.93f, h * 0.27f, w * 0.89f, h * 0.35f, w * 0.83f, h * 0.40f)
        lineTo(w * 0.77f, h * 0.44f)
        lineTo(w * 0.78f, h * 0.92f)
        lineTo(w * 0.22f, h * 0.92f)
        lineTo(w * 0.23f, h * 0.44f)
        lineTo(w * 0.17f, h * 0.40f)
        cubicTo(w * 0.11f, h * 0.35f, w * 0.07f, h * 0.27f, w * 0.12f, h * 0.22f)
        close()
    }

    drawPath(garment, color = Color.Black.copy(alpha = 0.30f))
    drawPath(garment, color = garmentColor)

    // Realistic Fabric Weave Highlights & Shadow Creases
    drawLine(
        color = Color.Black.copy(alpha = 0.18f),
        start = Offset(w * 0.25f, h * 0.44f),
        end = Offset(w * 0.38f, h * 0.75f),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.18f),
        start = Offset(w * 0.75f, h * 0.44f),
        end = Offset(w * 0.62f, h * 0.75f),
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawCompositedArtwork(w: Float, h: Float, layers: List<PodVectorLayer>) {
    val chestCenter = Offset(w * 0.50f, h * 0.42f)

    layers.filter { it.isVisible }.forEach { layer ->
        val layerPos = Offset(chestCenter.x + (layer.offsetX * 0.6f), chestCenter.y + (layer.offsetY * 0.6f))
        val tint = try {
            Color(android.graphics.Color.parseColor(layer.tintHex)).copy(alpha = layer.opacity)
        } catch (_: Exception) {
            Color.White.copy(alpha = layer.opacity)
        }

        withTransform({
            translate(layerPos.x, layerPos.y)
            rotate(layer.rotation)
            scale(layer.scale * 0.75f, layer.scale * 0.75f)
        }) {
            val r = 26.dp.toPx()
            // High-detail octagonal Chicano Streetwear graphic
            val path = Path().apply {
                moveTo(0f, -r)
                lineTo(r * 0.7f, -r * 0.7f)
                lineTo(r, 0f)
                lineTo(r * 0.7f, r * 0.7f)
                lineTo(0f, r)
                lineTo(-r * 0.7f, r * 0.7f)
                lineTo(-r, 0f)
                lineTo(-r * 0.7f, -r * 0.7f)
                close()
            }
            drawPath(path, color = Color.Black.copy(alpha = 0.25f))
            drawPath(path, color = tint.copy(alpha = 0.2f), style = Fill)
            drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx()))

            // Inner graphic
            drawCircle(color = tint, radius = r * 0.45f, style = Stroke(width = 1.2.dp.toPx()))
            drawCircle(color = tint, radius = 2.5.dp.toPx(), center = Offset(0f, 0f))
        }
    }
}
