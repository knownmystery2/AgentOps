package com.opensource.autonomous.agent.ui.pod

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun PodVectorCanvas(
    product: PrintifyProductBlank,
    color: PrintifyColorSwatch,
    zone: PrintifyPrintZone,
    layers: List<PodVectorLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onUpdateLayer: (PodVectorLayer) -> Unit,
    onDuplicateLayer: (PodVectorLayer) -> Unit,
    onDeleteLayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    // Zoom & Pan state for overall canvas inspection
    var canvasZoom by remember { mutableFloatStateOf(1.0f) }
    var canvasPanX by remember { mutableFloatStateOf(0f) }
    var canvasPanY by remember { mutableFloatStateOf(0f) }

    // Snapping state
    var isSnappedCenterX by remember { mutableStateOf(false) }
    var isSnappedCenterY by remember { mutableStateOf(false) }

    val garmentColor = remember(color.hex) {
        try {
            Color(android.graphics.Color.parseColor(color.hex))
        } catch (_: Exception) {
            Color(0xFF1E293B)
        }
    }

    val selectedLayer = layers.find { it.id == selectedLayerId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) Color(0xFF090D16) else Color(0xFFF1F5F9))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    canvasZoom = (canvasZoom * zoom).coerceIn(0.5f, 3.5f)
                    canvasPanX += pan.x
                    canvasPanY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (canvasZoom > 1.2f) {
                            canvasZoom = 1.0f
                            canvasPanX = 0f
                            canvasPanY = 0f
                        } else {
                            canvasZoom = 1.75f
                        }
                    }
                )
            }
            .testTag("pod_masterclass_canvas"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Isometric Canvas Grid Pattern & Measurement Rulers
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 24.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }

            // Outer Canvas Rulers
            val rulerBg = if (isDarkTheme) Color(0xFF111827) else Color(0xFFE2E8F0)
            val rulerMark = if (isDarkTheme) Color(0xFF475569) else Color(0xFF94A3B8)
            // Top Ruler
            drawRect(color = rulerBg.copy(alpha = 0.7f), topLeft = Offset.Zero, size = Size(size.width, 16.dp.toPx()))
            var rx = 20.dp.toPx()
            while (rx < size.width) {
                drawLine(
                    color = rulerMark,
                    start = Offset(rx, 10.dp.toPx()),
                    end = Offset(rx, 16.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                rx += 20.dp.toPx()
            }
            // Left Ruler
            drawRect(color = rulerBg.copy(alpha = 0.7f), topLeft = Offset.Zero, size = Size(16.dp.toPx(), size.height))
            var ry = 20.dp.toPx()
            while (ry < size.height) {
                drawLine(
                    color = rulerMark,
                    start = Offset(10.dp.toPx(), ry),
                    end = Offset(16.dp.toPx(), ry),
                    strokeWidth = 1.dp.toPx()
                )
                ry += 20.dp.toPx()
            }
        }

        // 2. Garment Mockup Frame with Silhouette Architecture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = canvasZoom
                    scaleY = canvasZoom
                    translationX = canvasPanX
                    translationY = canvasPanY
                },
            contentAlignment = Alignment.Center
        ) {
            // Silhouette vector rendering
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                val w = size.width
                val h = size.height

                when (product.silhouetteType) {
                    "hoodie" -> drawHoodieSilhouette(w, h, garmentColor)
                    "crewneck" -> drawCrewneckSilhouette(w, h, garmentColor)
                    "mug" -> drawMugSilhouette(w, h, garmentColor)
                    "tumbler" -> drawTumblerSilhouette(w, h, garmentColor)
                    "tote" -> drawToteSilhouette(w, h, garmentColor)
                    "sticker" -> drawStickerSilhouette(w, h, garmentColor)
                    "phonecase" -> drawPhoneCaseSilhouette(w, h, garmentColor)
                    "poster" -> drawPosterSilhouette(w, h, garmentColor)
                    "canvas" -> drawCanvasWrapSilhouette(w, h, garmentColor)
                    "blanket" -> drawBlanketSilhouette(w, h, garmentColor)
                    else -> drawTShirtSilhouette(w, h, garmentColor) // default tshirt
                }
            }

            // 3. Print Zone Boundary Frame
            val zoneWidthDp = (zone.widthPx * 0.82f).dp
            val zoneHeightDp = (zone.heightPx * 0.82f).dp

            val frameTopOffset = when (zone.id) {
                "back" -> (-8).dp
                "pocket" -> (-40).dp
                "sleeve_left" -> 10.dp
                "sleeve_right" -> 10.dp
                else -> (-14).dp // chest
            }
            val frameLeftOffset = when (zone.id) {
                "pocket" -> 40.dp
                "sleeve_left" -> (-80).dp
                "sleeve_right" -> 80.dp
                else -> 0.dp
            }

            Box(
                modifier = Modifier
                    .offset(x = frameLeftOffset, y = frameTopOffset)
                    .size(zoneWidthDp, zoneHeightDp)
                    .border(
                        BorderStroke(1.5.dp, Color(0xFF06B6D4).copy(alpha = 0.85f)),
                        RoundedCornerShape(4.dp)
                    )
                    .testTag("print_zone_bounds"),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Vector Canvas Area
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedLayerId) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                selectedLayer?.let { layer ->
                                    if (!layer.isLocked) {
                                        val newX = layer.offsetX + pan.x
                                        val newY = layer.offsetY + pan.y
                                        // Snapping detection
                                        isSnappedCenterX = abs(newX) < 12f
                                        isSnappedCenterY = abs(newY) < 12f
                                        val finalX = if (isSnappedCenterX) 0f else newX
                                        val finalY = if (isSnappedCenterY) 0f else newY

                                        val newScale = (layer.scale * zoom).coerceIn(0.2f, 3.5f)
                                        val newRot = (layer.rotation + rotation) % 360f

                                        onUpdateLayer(
                                            layer.copy(
                                                offsetX = finalX,
                                                offsetY = finalY,
                                                scale = newScale,
                                                rotation = newRot
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                // Select nearest layer or cycle
                                val hit = layers.lastOrNull { layer ->
                                    val layerPos = Offset(center.x + layer.offsetX, center.y + layer.offsetY)
                                    val dist = (tapOffset - layerPos).getDistance()
                                    dist < (50f * layer.scale)
                                }
                                if (hit != null) {
                                    onSelectLayer(hit.id)
                                }
                            }
                        }
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Snapping Guides
                    if (isSnappedCenterX) {
                        drawLine(
                            color = Color(0xFF06B6D4),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                    }
                    if (isSnappedCenterY) {
                        drawLine(
                            color = Color(0xFF06B6D4),
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                    }

                    // Render each layer from bottom to top
                    layers.filter { it.isVisible }.forEach { layer ->
                        val isSelected = layer.id == selectedLayerId
                        val layerCenter = Offset(center.x + layer.offsetX, center.y + layer.offsetY)

                        withTransform({
                            translate(layerCenter.x, layerCenter.y)
                            rotate(layer.rotation)
                            scale(
                                scaleX = layer.scale * (if (layer.isFlippedH) -1f else 1f),
                                scaleY = layer.scale * (if (layer.isFlippedV) -1f else 1f)
                            )
                        }) {
                            drawVectorLayerContent(layer)

                            // Render selection bounding box & transform handles
                            if (isSelected) {
                                val boxRadius = 40.dp.toPx()
                                drawRoundRect(
                                    color = Color(0xFF22D3EE),
                                    topLeft = Offset(-boxRadius, -boxRadius),
                                    size = Size(boxRadius * 2, boxRadius * 2),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                    )
                                )
                                // Corner scale anchors
                                val handleSize = 7.dp.toPx()
                                val corners = listOf(
                                    Offset(-boxRadius, -boxRadius),
                                    Offset(boxRadius, -boxRadius),
                                    Offset(-boxRadius, boxRadius),
                                    Offset(boxRadius, boxRadius)
                                )
                                corners.forEach { pt ->
                                    drawCircle(color = Color(0xFF06B6D4), radius = handleSize / 2, center = pt)
                                    drawCircle(color = Color.White, radius = handleSize / 3, center = pt)
                                }
                                // Top Rotation Pin
                                drawLine(
                                    color = Color(0xFF06B6D4),
                                    start = Offset(0f, -boxRadius),
                                    end = Offset(0f, -boxRadius - 16.dp.toPx()),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawCircle(
                                    color = Color(0xFF10B981),
                                    radius = 5.dp.toPx(),
                                    center = Offset(0f, -boxRadius - 16.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Zone dimension corner tag
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "${zone.name} • 300 DPI",
                        color = Color(0xFF22D3EE),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 4. Floating On-Canvas Quick Actions Toolbar
        selectedLayer?.let { layer ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .testTag("floating_canvas_toolbar"),
                shape = RoundedCornerShape(24.dp),
                color = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Center Alignment
                    IconButton(
                        onClick = {
                            onUpdateLayer(layer.copy(offsetX = 0f, offsetY = 0f))
                            isSnappedCenterX = true
                            isSnappedCenterY = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FilterCenterFocus, contentDescription = "Center", tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
                    }

                    // Fit 90% Safe Zone
                    IconButton(
                        onClick = {
                            onUpdateLayer(layer.copy(offsetX = 0f, offsetY = 0f, scale = 1.35f, rotation = 0f))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Fit 90%", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }

                    // Rotate 90
                    IconButton(
                        onClick = {
                            onUpdateLayer(layer.copy(rotation = (layer.rotation + 90f) % 360f))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    }

                    // Flip Horizontal
                    IconButton(
                        onClick = {
                            onUpdateLayer(layer.copy(isFlippedH = !layer.isFlippedH))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = "Flip", tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                    }

                    // Duplicate
                    IconButton(
                        onClick = { onDuplicateLayer(layer) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }

                    // Delete
                    IconButton(
                        onClick = { onDeleteLayer(layer.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 5. Floating Live DPI Status Pill at Top Right
        selectedLayer?.let { layer ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, if (layer.liveDpi >= 300) Color(0xFF10B981) else Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (layer.liveDpi >= 300) Color(0xFF10B981) else Color(0xFFF59E0B),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = "${layer.liveDpi} DPI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 6. Floating Canvas Zoom & Pan Navigation Control HUD
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .testTag("canvas_zoom_pan_hud"),
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF131B2E).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Zoom Out
                IconButton(
                    onClick = { canvasZoom = (canvasZoom - 0.25f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(28.dp).testTag("canvas_zoom_out_btn")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }

                // Zoom Level
                Text(
                    text = "${(canvasZoom * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canvasZoom != 1.0f) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier
                        .clickable {
                            canvasZoom = 1.0f
                            canvasPanX = 0f
                            canvasPanY = 0f
                        }
                        .padding(horizontal = 4.dp)
                        .testTag("canvas_zoom_label")
                )

                // Zoom In
                IconButton(
                    onClick = { canvasZoom = (canvasZoom + 0.25f).coerceAtMost(3.5f) },
                    modifier = Modifier.size(28.dp).testTag("canvas_zoom_in_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }

                // Reset Pan / Fit View
                if (canvasZoom != 1.0f || canvasPanX != 0f || canvasPanY != 0f) {
                    VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 2.dp), color = Color(0xFF334155))
                    IconButton(
                        onClick = {
                            canvasZoom = 1.0f
                            canvasPanX = 0f
                            canvasPanY = 0f
                        },
                        modifier = Modifier.size(28.dp).testTag("canvas_zoom_reset_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset View", tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================
// VECTOR LAYER DRAWING LOGIC (TEXT, STREETWEAR CLIPART, GEOMETRIC EMBLEMS)
// ============================================================================

private fun DrawScope.drawVectorLayerContent(layer: PodVectorLayer) {
    val tint = try {
        Color(android.graphics.Color.parseColor(layer.tintHex)).copy(alpha = layer.opacity)
    } catch (_: Exception) {
        Color.White.copy(alpha = layer.opacity)
    }

    when (layer.type) {
        PodLayerType.TEXT -> {
            // High-detail Gothic Chicano Vector Typography Emulation
            drawGothicTextGraphic(layer.textContent, tint)
        }
        PodLayerType.AI_ART -> {
            drawStreetwearArtworkGraphic(layer.textContent, tint)
        }
        PodLayerType.SHAPE -> {
            drawShieldCrestGraphic(tint)
        }
        PodLayerType.VECTOR -> {
            drawStreetwearArtworkGraphic(layer.name, tint)
        }
    }
}

private fun DrawScope.drawGothicTextGraphic(text: String, tint: Color) {
    // Outer drop shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.4f),
        topLeft = Offset(-36.dp.toPx() + 2f, -12.dp.toPx() + 2f),
        size = Size(72.dp.toPx(), 24.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Fill
    )
    // Main banner badge
    drawRoundRect(
        color = tint.copy(alpha = 0.15f),
        topLeft = Offset(-36.dp.toPx(), -12.dp.toPx()),
        size = Size(72.dp.toPx(), 24.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Fill
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(-36.dp.toPx(), -12.dp.toPx()),
        size = Size(72.dp.toPx(), 24.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(width = 1.8.dp.toPx())
    )
    // Inner decorative flourish cross lines
    drawLine(
        color = tint,
        start = Offset(-30.dp.toPx(), 0f),
        end = Offset(30.dp.toPx(), 0f),
        strokeWidth = 1.2.dp.toPx()
    )
    drawCircle(color = tint, radius = 2.5.dp.toPx(), center = Offset(0f, 0f))
}

private fun DrawScope.drawStreetwearArtworkGraphic(name: String, tint: Color) {
    val r = 28.dp.toPx()

    // Outer intricate octagon / crest
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

    drawPath(path, color = Color.Black.copy(alpha = 0.35f))
    drawPath(path, color = tint.copy(alpha = 0.2f), style = Fill)
    drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx()))

    // Inner Chicano Payasa / Scarab fine-line graphic motifs
    drawCircle(color = tint, radius = r * 0.5f, style = Stroke(width = 1.5.dp.toPx()))
    drawCircle(color = tint, radius = 3.dp.toPx(), center = Offset(0f, 0f))

    // Stippling and radial spikes
    val spikeCount = 8
    for (i in 0 until spikeCount) {
        val angle = (i * 2 * PI / spikeCount).toFloat()
        val sx = cos(angle) * r * 0.5f
        val sy = sin(angle) * r * 0.5f
        val ex = cos(angle) * r * 0.9f
        val ey = sin(angle) * r * 0.9f
        drawLine(color = tint, start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = 1.2.dp.toPx())
    }
}

private fun DrawScope.drawShieldCrestGraphic(tint: Color) {
    val w = 30.dp.toPx()
    val h = 36.dp.toPx()

    val path = Path().apply {
        moveTo(-w, -h * 0.8f)
        lineTo(w, -h * 0.8f)
        lineTo(w, h * 0.1f)
        cubicTo(w, h * 0.6f, 0f, h, 0f, h)
        cubicTo(0f, h, -w, h * 0.6f, -w, h * 0.1f)
        close()
    }

    drawPath(path, color = tint.copy(alpha = 0.25f), style = Fill)
    drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx()))
}

// ============================================================================
// REALISTIC GARMENT & PRODUCT SILHOUETTE DRAWING FUNCTIONS
// ============================================================================

private fun DrawScope.drawTShirtSilhouette(w: Float, h: Float, garmentColor: Color) {
    val path = Path().apply {
        moveTo(w * 0.32f, h * 0.08f)
        cubicTo(w * 0.38f, h * 0.13f, w * 0.62f, h * 0.13f, w * 0.68f, h * 0.08f)
        lineTo(w * 0.88f, h * 0.20f)
        cubicTo(w * 0.93f, h * 0.25f, w * 0.89f, h * 0.33f, w * 0.83f, h * 0.38f)
        lineTo(w * 0.77f, h * 0.42f)
        lineTo(w * 0.78f, h * 0.95f)
        lineTo(w * 0.22f, h * 0.95f)
        lineTo(w * 0.23f, h * 0.42f)
        lineTo(w * 0.17f, h * 0.38f)
        cubicTo(w * 0.11f, h * 0.33f, w * 0.07f, h * 0.25f, w * 0.12f, h * 0.20f)
        close()
    }
    // Shadow & Garment Fill
    drawPath(path, color = Color.Black.copy(alpha = 0.35f))
    drawPath(path, color = garmentColor)
    // Collar Ribbing
    drawArc(
        color = Color.Black.copy(alpha = 0.25f),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.34f, h * 0.07f),
        size = Size(w * 0.32f, h * 0.07f),
        style = Stroke(width = 2.5.dp.toPx())
    )
}

private fun DrawScope.drawHoodieSilhouette(w: Float, h: Float, garmentColor: Color) {
    // Outer Torso & Sleeves
    val path = Path().apply {
        moveTo(w * 0.30f, h * 0.14f)
        lineTo(w * 0.88f, h * 0.23f)
        lineTo(w * 0.82f, h * 0.44f)
        lineTo(w * 0.78f, h * 0.46f)
        lineTo(w * 0.78f, h * 0.94f)
        lineTo(w * 0.22f, h * 0.94f)
        lineTo(w * 0.22f, h * 0.46f)
        lineTo(w * 0.18f, h * 0.44f)
        lineTo(w * 0.12f, h * 0.23f)
        close()
    }
    drawPath(path, color = Color.Black.copy(alpha = 0.35f))
    drawPath(path, color = garmentColor)

    // Pullover Hood
    val hoodPath = Path().apply {
        moveTo(w * 0.30f, h * 0.14f)
        cubicTo(w * 0.28f, h * 0.02f, w * 0.72f, h * 0.02f, w * 0.70f, h * 0.14f)
        close()
    }
    drawPath(hoodPath, color = garmentColor.copy(alpha = 0.9f))
    drawPath(hoodPath, color = Color.Black.copy(alpha = 0.2f), style = Stroke(width = 2.dp.toPx()))

    // Kangaroo Pocket Pouch
    val pocketPath = Path().apply {
        moveTo(w * 0.32f, h * 0.65f)
        lineTo(w * 0.68f, h * 0.65f)
        lineTo(w * 0.72f, h * 0.88f)
        lineTo(w * 0.28f, h * 0.88f)
        close()
    }
    drawPath(pocketPath, color = Color.Black.copy(alpha = 0.15f))
    drawPath(pocketPath, color = Color.Black.copy(alpha = 0.25f), style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawCrewneckSilhouette(w: Float, h: Float, garmentColor: Color) {
    val path = Path().apply {
        moveTo(w * 0.33f, h * 0.09f)
        cubicTo(w * 0.38f, h * 0.13f, w * 0.62f, h * 0.13f, w * 0.67f, h * 0.09f)
        lineTo(w * 0.89f, h * 0.22f)
        lineTo(w * 0.84f, h * 0.44f)
        lineTo(w * 0.78f, h * 0.46f)
        lineTo(w * 0.78f, h * 0.94f)
        lineTo(w * 0.22f, h * 0.94f)
        lineTo(w * 0.22f, h * 0.46f)
        lineTo(w * 0.16f, h * 0.44f)
        lineTo(w * 0.11f, h * 0.22f)
        close()
    }
    drawPath(path, color = Color.Black.copy(alpha = 0.35f))
    drawPath(path, color = garmentColor)
    // Ribbed Waistband
    drawRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.22f, h * 0.89f),
        size = Size(w * 0.56f, h * 0.05f)
    )
}

private fun DrawScope.drawMugSilhouette(w: Float, h: Float, garmentColor: Color) {
    // Mug Cylindrical Body
    val body = Path().apply {
        moveTo(w * 0.26f, h * 0.18f)
        lineTo(w * 0.68f, h * 0.18f)
        cubicTo(w * 0.70f, h * 0.82f, w * 0.68f, h * 0.84f, w * 0.64f, h * 0.86f)
        lineTo(w * 0.30f, h * 0.86f)
        cubicTo(w * 0.26f, h * 0.84f, w * 0.24f, h * 0.82f, w * 0.26f, h * 0.18f)
        close()
    }
    drawPath(body, color = Color.Black.copy(alpha = 0.3f))
    drawPath(body, color = garmentColor)

    // Handle
    val handle = Path().apply {
        moveTo(w * 0.68f, h * 0.28f)
        cubicTo(w * 0.86f, h * 0.28f, w * 0.86f, h * 0.74f, w * 0.68f, h * 0.74f)
        cubicTo(w * 0.78f, h * 0.70f, w * 0.78f, h * 0.32f, w * 0.68f, h * 0.28f)
        close()
    }
    drawPath(handle, color = garmentColor)
    drawPath(handle, color = Color.Black.copy(alpha = 0.25f), style = Stroke(width = 2.dp.toPx()))

    // Glossy Ceramic Lighting Sheen
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
        ),
        topLeft = Offset(w * 0.30f, h * 0.19f),
        size = Size(w * 0.08f, h * 0.66f)
    )
}

private fun DrawScope.drawTumblerSilhouette(w: Float, h: Float, garmentColor: Color) {
    val path = Path().apply {
        moveTo(w * 0.30f, h * 0.12f)
        lineTo(w * 0.70f, h * 0.12f)
        lineTo(w * 0.65f, h * 0.88f)
        lineTo(w * 0.35f, h * 0.88f)
        close()
    }
    drawPath(path, color = Color.Black.copy(alpha = 0.3f))
    drawPath(path, color = garmentColor)

    // Stainless Steel Rim & Clear Lid
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(w * 0.28f, h * 0.09f),
        size = Size(w * 0.44f, h * 0.04f),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    // Metallic Highlight
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)),
        topLeft = Offset(w * 0.33f, h * 0.13f),
        size = Size(w * 0.06f, h * 0.74f)
    )
}

private fun DrawScope.drawToteSilhouette(w: Float, h: Float, garmentColor: Color) {
    // Straps
    drawArc(
        color = Color.Black.copy(alpha = 0.3f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.34f, h * 0.04f),
        size = Size(w * 0.32f, h * 0.35f),
        style = Stroke(width = 6.dp.toPx())
    )

    // Bag Body
    val bag = Path().apply {
        moveTo(w * 0.22f, h * 0.28f)
        lineTo(w * 0.78f, h * 0.28f)
        lineTo(w * 0.75f, h * 0.90f)
        lineTo(w * 0.25f, h * 0.90f)
        close()
    }
    drawPath(bag, color = Color.Black.copy(alpha = 0.35f))
    drawPath(bag, color = garmentColor)
}

private fun DrawScope.drawStickerSilhouette(w: Float, h: Float, garmentColor: Color) {
    val r = 70.dp.toPx()
    val center = Offset(w / 2f, h / 2f)

    // Peel away backing
    drawCircle(color = Color.Black.copy(alpha = 0.2f), radius = r + 8f, center = center)
    drawCircle(color = Color.White, radius = r + 4f, center = center)
    drawCircle(color = garmentColor, radius = r, center = center)
}

private fun DrawScope.drawPhoneCaseSilhouette(w: Float, h: Float, garmentColor: Color) {
    val rect = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.26f,
                top = h * 0.10f,
                right = w * 0.74f,
                bottom = h * 0.90f,
                cornerRadius = CornerRadius(24.dp.toPx())
            )
        )
    }
    drawPath(rect, color = Color.Black.copy(alpha = 0.35f))
    drawPath(rect, color = garmentColor)

    // Camera Cutout
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.31f, h * 0.14f),
        size = Size(w * 0.18f, h * 0.18f),
        cornerRadius = CornerRadius(10.dp.toPx())
    )
}

private fun DrawScope.drawPosterSilhouette(w: Float, h: Float, garmentColor: Color) {
    // Outer Frame
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(w * 0.20f, h * 0.08f),
        size = Size(w * 0.60f, h * 0.84f)
    )
    // White Matting Board
    drawRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(w * 0.23f, h * 0.11f),
        size = Size(w * 0.54f, h * 0.78f)
    )
    // Printable Canvas Area
    drawRect(
        color = garmentColor,
        topLeft = Offset(w * 0.26f, h * 0.14f),
        size = Size(w * 0.48f, h * 0.72f)
    )
}

private fun DrawScope.drawCanvasWrapSilhouette(w: Float, h: Float, garmentColor: Color) {
    // 3D Stretched Canvas Wrap
    val path = Path().apply {
        moveTo(w * 0.18f, h * 0.14f)
        lineTo(w * 0.78f, h * 0.14f)
        lineTo(w * 0.82f, h * 0.18f)
        lineTo(w * 0.82f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.86f)
        lineTo(w * 0.18f, h * 0.86f)
        close()
    }
    drawPath(path, color = Color.Black.copy(alpha = 0.4f))
    drawPath(path, color = garmentColor)
}

private fun DrawScope.drawBlanketSilhouette(w: Float, h: Float, garmentColor: Color) {
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(w * 0.16f, h * 0.10f),
        size = Size(w * 0.68f, h * 0.80f),
        cornerRadius = CornerRadius(16.dp.toPx())
    )
    drawRoundRect(
        color = garmentColor,
        topLeft = Offset(w * 0.16f, h * 0.10f),
        size = Size(w * 0.68f, h * 0.80f),
        cornerRadius = CornerRadius(16.dp.toPx())
    )
    // Faux sherpa border stitching
    drawRoundRect(
        color = Color(0xFFF5EBE6).copy(alpha = 0.7f),
        topLeft = Offset(w * 0.19f, h * 0.13f),
        size = Size(w * 0.62f, h * 0.74f),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )
    )
}
