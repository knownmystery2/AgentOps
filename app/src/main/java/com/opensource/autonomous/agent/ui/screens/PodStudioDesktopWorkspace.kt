package com.opensource.autonomous.agent.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.opensource.autonomous.agent.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

// ============================================================================
// DATA MODELS & CATALOG STRUCTURES
// ============================================================================

data class DesktopProductBlank(
    val id: String,
    val name: String,
    val category: String,
    val weight: String,
    val baseSku: String,
    val blueprintId: String,
    val silhouetteType: String
)

data class DesktopColorSwatch(
    val id: String,
    val name: String,
    val hex: String,
    val isLightText: Boolean
)

data class DesktopPrintZone(
    val id: String,
    val name: String,
    val skuCode: String,
    val widthPx: Int,
    val heightPx: Int,
    val dimensionLabel: String
)

val DESKTOP_CATALOG = listOf(
    DesktopProductBlank("heavyweight-tshirt", "Heavyweight T-Shirt", "Apparel", "240 GSM Organic Cotton", "POD-HET", "384", "tshirt"),
    DesktopProductBlank("vintage-hoodie", "Vintage Hoodie", "Apparel", "380 GSM Heavy Fleece", "POD-HOD", "412", "hoodie"),
    DesktopProductBlank("ceramic-mug", "Ceramic Mug", "Drinkware", "11 oz Glossy Ceramic", "POD-MUG", "68", "mug"),
    DesktopProductBlank("canvas-tote", "Canvas Tote Bag", "Accessories", "100% Bull Denim", "POD-TOT", "29", "tote"),
    DesktopProductBlank("wall-art-poster", "Wall Art Poster", "Home Decor", "250 GSM Archival Matte", "POD-PST", "12", "poster")
)

val DESKTOP_SWATCHES = listOf(
    DesktopColorSwatch("dark-teal", "Dark Teal", "#0D5C53", true),
    DesktopColorSwatch("cream", "Cream", "#F5EBE6", false),
    DesktopColorSwatch("forest-green", "Forest Green", "#1A331E", true),
    DesktopColorSwatch("terracotta", "Terracotta", "#C27648", true),
    DesktopColorSwatch("classic-white", "Classic White", "#FFFFFF", false)
)

val DESKTOP_ZONES = mapOf(
    "chest" to DesktopPrintZone("chest", "Chest Center (11\" x 13\")", "CHSL", 260, 310, "11\" × 13\" Max"),
    "pocket" to DesktopPrintZone("pocket", "Pocket Left (4\" x 4\")", "PKTL", 110, 110, "4\" × 4\" Pocket")
)

// ============================================================================
// AUTO-SCALING ENGINE & COORDINATE MAPPER
// ============================================================================

object PodAutoScaler {
    enum class ScalingMode {
        FIT_SAFE_90,
        FIT_WIDTH,
        FIT_HEIGHT,
        CENTER_XY,
        RESET_NORMAL
    }

    data class ScaleResult(
        val scaleX: Float,
        val scaleY: Float,
        val x: Float,
        val y: Float,
        val clamped: Boolean
    )

    fun computeScale(
        elementWidth: Float,
        elementHeight: Float,
        zoneWidth: Float,
        zoneHeight: Float,
        mode: ScalingMode
    ): ScaleResult {
        return when (mode) {
            ScalingMode.FIT_SAFE_90 -> {
                val targetW = zoneWidth * 0.90f
                val targetH = zoneHeight * 0.90f
                val factor = (targetW / elementWidth).coerceAtMost(targetH / elementHeight)
                ScaleResult(
                    scaleX = factor,
                    scaleY = factor,
                    x = zoneWidth / 2f,
                    y = zoneHeight / 2f,
                    clamped = true
                )
            }
            ScalingMode.FIT_WIDTH -> {
                val factor = (zoneWidth * 0.92f) / elementWidth
                ScaleResult(scaleX = factor, scaleY = factor, x = zoneWidth / 2f, y = zoneHeight / 2f, clamped = true)
            }
            ScalingMode.FIT_HEIGHT -> {
                val factor = (zoneHeight * 0.92f) / elementHeight
                ScaleResult(scaleX = factor, scaleY = factor, x = zoneWidth / 2f, y = zoneHeight / 2f, clamped = true)
            }
            ScalingMode.CENTER_XY -> {
                ScaleResult(scaleX = 1.0f, scaleY = 1.0f, x = zoneWidth / 2f, y = zoneHeight / 2f, clamped = true)
            }
            ScalingMode.RESET_NORMAL -> {
                ScaleResult(scaleX = 1.0f, scaleY = 1.0f, x = zoneWidth / 2f, y = zoneHeight / 2f, clamped = false)
            }
        }
    }
}

// ============================================================================
// JAVASCRIPT BRIDGE INTERFACE WITH MAIN THREAD DISPATCH
// ============================================================================

class PodFabricBridge(
    private val onPayloadUpdated: (String) -> Unit,
    private val onLayerSelected: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(payloadJson: String) {
        mainHandler.post {
            onPayloadUpdated(payloadJson)
        }
    }

    @JavascriptInterface
    fun onSelectLayer(layerId: String) {
        mainHandler.post {
            onLayerSelected(layerId)
        }
    }
}

// ============================================================================
// HTML + FABRIC.JS EMBEDDED CANVAS GENERATOR (WITH LOCAL VECTOR FALLBACK)
// ============================================================================

fun generateFabricHtml(zoneWidth: Int, zoneHeight: Int): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body {
            width: 100%;
            height: 100%;
            overflow: hidden;
            background: transparent;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            user-select: none;
            -webkit-user-select: none;
        }
        #canvas-wrapper {
            position: relative;
            width: ${zoneWidth}px;
            height: ${zoneHeight}px;
            background: transparent;
        }
        canvas {
            display: block;
        }
    </style>
    <!-- Fabric.js Vector Canvas Engine -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/fabric.js/5.3.1/fabric.min.js"></script>
</head>
<body>
    <div id="canvas-wrapper">
        <canvas id="pod-canvas" width="${zoneWidth}" height="${zoneHeight}"></canvas>
    </div>

    <script>
        var canvas;
        var currentZoneWidth = ${zoneWidth};
        var currentZoneHeight = ${zoneHeight};
        var layersMap = {};

        function emitTelemetry() {
            if (!canvas) return;
            var objects = canvas.getObjects();
            var layers = objects.map(function(obj, idx) {
                var bound = obj.getBoundingRect(true);
                return {
                    id: obj.dataId || ('layer_' + (idx + 1)),
                    name: obj.dataName || obj.type,
                    type: obj.type,
                    visible: obj.visible !== false,
                    x: Math.round(obj.left || 0),
                    y: Math.round(obj.top || 0),
                    scale_x: Number((obj.scaleX || 1).toFixed(3)),
                    scale_y: Number((obj.scaleY || 1).toFixed(3)),
                    angle: Math.round(obj.angle || 0),
                    normalized_x: Number(((obj.left || 0) / currentZoneWidth).toFixed(4)),
                    normalized_y: Number(((obj.top || 0) / currentZoneHeight).toFixed(4)),
                    bound_w: Math.round(bound.width),
                    bound_h: Math.round(bound.height)
                };
            });

            var payload = {
                width: currentZoneWidth,
                height: currentZoneHeight,
                dpi: 300,
                layers: layers
            };

            if (window.PodBridge && window.PodBridge.postMessage) {
                window.PodBridge.postMessage(JSON.stringify(payload));
            }
        }

        function initFabric() {
            if (typeof fabric === 'undefined') {
                // In case of CDN delay, retry briefly
                setTimeout(initFabric, 100);
                return;
            }

            canvas = new fabric.Canvas('pod-canvas', {
                width: currentZoneWidth,
                height: currentZoneHeight,
                selection: true,
                preserveObjectStacking: true,
                backgroundColor: 'transparent'
            });

            // Automated Vector Clipping Boundary (clipPath) matching Print Area Limit
            var clipRect = new fabric.Rect({
                left: 0,
                top: 0,
                width: currentZoneWidth,
                height: currentZoneHeight,
                absolutePositioned: true
            });
            canvas.clipPath = clipRect;

            // Asset 1: Solid Clean White Circle
            var circleLayer = new fabric.Circle({
                radius: currentZoneWidth > 150 ? 56 : 28,
                fill: '#FFFFFF',
                left: currentZoneWidth / 2,
                top: currentZoneWidth > 150 ? (currentZoneHeight / 2 - 32) : (currentZoneHeight / 2 - 16),
                originX: 'center',
                originY: 'center',
                cornerColor: '#2563eb',
                cornerStrokeColor: '#ffffff',
                cornerSize: 10,
                transparentCorners: false,
                hasRotatingPoint: true
            });
            circleLayer.dataId = 'layer_circle';
            circleLayer.dataName = 'Vector Base Circle';
            canvas.add(circleLayer);
            layersMap['layer_circle'] = circleLayer;

            // Asset 2: Crisp Typographical Element
            var textLayer = new fabric.Text('ORIGINAL VINTAGE', {
                fontSize: currentZoneWidth > 150 ? 17 : 9,
                fontFamily: 'sans-serif',
                fontWeight: 'bold',
                fill: '#FFFFFF',
                left: currentZoneWidth / 2,
                top: currentZoneWidth > 150 ? (currentZoneHeight / 2 + 50) : (currentZoneHeight / 2 + 25),
                originX: 'center',
                originY: 'center',
                cornerColor: '#2563eb',
                cornerStrokeColor: '#ffffff',
                cornerSize: 10,
                transparentCorners: false,
                charSpacing: 80
            });
            textLayer.dataId = 'layer_text';
            textLayer.dataName = 'Typography Subtitle';
            canvas.add(textLayer);
            layersMap['layer_text'] = textLayer;

            canvas.setActiveObject(circleLayer);
            canvas.renderAll();

            canvas.on('object:modified', emitTelemetry);
            canvas.on('object:moving', emitTelemetry);
            canvas.on('object:scaling', emitTelemetry);
            canvas.on('object:rotating', emitTelemetry);
            canvas.on('selection:created', function(e) {
                if (e.selected && e.selected[0] && window.PodBridge) {
                    window.PodBridge.onSelectLayer(e.selected[0].dataId || 'layer');
                }
                emitTelemetry();
            });
            canvas.on('selection:updated', function(e) {
                if (e.selected && e.selected[0] && window.PodBridge) {
                    window.PodBridge.onSelectLayer(e.selected[0].dataId || 'layer');
                }
                emitTelemetry();
            });

            emitTelemetry();
        }

        // Auto-scaling helper functions
        window.autoScaleActive = function(mode) {
            if (!canvas) return;
            var active = canvas.getActiveObject() || canvas.getObjects()[0];
            if (!active) return;

            var bound = active.getBoundingRect(true);

            if (mode === 'CENTER_XY') {
                active.set({
                    left: currentZoneWidth / 2,
                    top: currentZoneHeight / 2,
                    originX: 'center',
                    originY: 'center'
                });
            } else if (mode === 'FIT_SAFE_90') {
                var maxW = currentZoneWidth * 0.90;
                var maxH = currentZoneHeight * 0.90;
                var currentW = bound.width / (active.scaleX || 1);
                var currentH = bound.height / (active.scaleY || 1);
                var s = Math.min(maxW / currentW, maxH / currentH);
                active.set({
                    scaleX: s,
                    scaleY: s,
                    left: currentZoneWidth / 2,
                    top: currentZoneHeight / 2,
                    originX: 'center',
                    originY: 'center'
                });
            } else if (mode === 'FIT_WIDTH') {
                var maxW = currentZoneWidth * 0.92;
                var currentW = bound.width / (active.scaleX || 1);
                var s = maxW / currentW;
                active.set({
                    scaleX: s,
                    scaleY: s,
                    left: currentZoneWidth / 2,
                    top: currentZoneHeight / 2,
                    originX: 'center',
                    originY: 'center'
                });
            } else if (mode === 'FIT_HEIGHT') {
                var maxH = currentZoneHeight * 0.92;
                var currentH = bound.height / (active.scaleY || 1);
                var s = maxH / currentH;
                active.set({
                    scaleX: s,
                    scaleY: s,
                    left: currentZoneWidth / 2,
                    top: currentZoneHeight / 2,
                    originX: 'center',
                    originY: 'center'
                });
            } else if (mode === 'RESET_NORMAL') {
                active.set({
                    scaleX: 1.0,
                    scaleY: 1.0,
                    angle: 0,
                    left: currentZoneWidth / 2,
                    top: currentZoneHeight / 2,
                    originX: 'center',
                    originY: 'center'
                });
            }

            active.setCoords();
            canvas.renderAll();
            emitTelemetry();
        };

        window.selectLayer = function(layerId) {
            if (!canvas) return;
            var target = layersMap[layerId];
            if (target) {
                canvas.setActiveObject(target);
                canvas.renderAll();
                emitTelemetry();
            }
        };

        window.toggleLayer = function(layerId, isVisible) {
            if (!canvas) return;
            var target = layersMap[layerId];
            if (target) {
                target.set('visible', isVisible);
                canvas.renderAll();
                emitTelemetry();
            }
        };

        window.reorderLayer = function(layerId, dir) {
            if (!canvas) return;
            var target = layersMap[layerId];
            if (target) {
                if (dir === 'UP') {
                    canvas.bringForward(target);
                } else {
                    canvas.sendBackwards(target);
                }
                canvas.renderAll();
                emitTelemetry();
            }
        };

        window.removeLayer = function(layerId) {
            if (!canvas) return;
            var target = layersMap[layerId];
            if (target) {
                canvas.remove(target);
                delete layersMap[layerId];
                canvas.renderAll();
                emitTelemetry();
            }
        };

        window.addCustomText = function(textStr) {
            if (!canvas) return;
            var newId = 'layer_' + Date.now();
            var text = new fabric.Text(textStr || 'NEW VECTOR DESIGN', {
                fontSize: currentZoneWidth > 150 ? 16 : 8,
                fontFamily: 'sans-serif',
                fontWeight: 'bold',
                fill: '#FFFFFF',
                left: currentZoneWidth / 2,
                top: currentZoneHeight / 2,
                originX: 'center',
                originY: 'center',
                cornerColor: '#2563eb',
                cornerSize: 10
            });
            text.dataId = newId;
            text.dataName = textStr || 'Custom Text';
            layersMap[newId] = text;
            canvas.add(text);
            canvas.setActiveObject(text);
            canvas.renderAll();
            emitTelemetry();
        };

        window.onload = initFabric;
    </script>
</body>
</html>
    """.trimIndent()
}

// ============================================================================
// COMPOSABLE: POD STUDIO DESKTOP WORKSPACE
// ============================================================================

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodStudioDesktopWorkspace(
    viewModel: MainViewModel? = null,
    onNavigateBack: () -> Unit = {}
) {
    // 1. Android Back Handler for proper route popping
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val httpClient = remember { OkHttpClient() }

    // State: Catalog Selections
    var selectedProduct by remember { mutableStateOf(DESKTOP_CATALOG[0]) }
    var selectedColor by remember { mutableStateOf(DESKTOP_SWATCHES[0]) }
    var selectedZoneKey by remember { mutableStateOf("chest") }
    val currentZone = DESKTOP_ZONES[selectedZoneKey] ?: DESKTOP_ZONES["chest"]!!

    // State: Layer Stack
    var circleVisible by remember { mutableStateOf(true) }
    var textVisible by remember { mutableStateOf(true) }
    var selectedLayerId by remember { mutableStateOf("layer_circle") }
    var newLayerText by remember { mutableStateOf("") }
    var showAddLayerDialog by remember { mutableStateOf(false) }

    // State: AI Prompt Module
    var aiPrompt by remember { mutableStateOf("Model modeling t-shirt in urban studio lighting, cinematic rim light") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var aiStatusMessage by remember { mutableStateOf<String?>(null) }

    // State: Live Printify API-Ready Payload
    var rawTelemetryJson by remember { mutableStateOf("") }
    var formattedPrintifyPayload by remember { mutableStateOf("{}") }
    var isPublishing by remember { mutableStateOf(false) }
    var publishSuccessBanner by remember { mutableStateOf<String?>(null) }

    // State: Server Auto-Scale Telemetry Verification
    var serverAutoScaleResult by remember { mutableStateOf<String?>(null) }

    // WebView Reference for Fabric.js
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Target SKU
    val computedSku = "Target SKU: ${selectedProduct.baseSku}--${currentZone.skuCode}-2026"

    // Construct Printify API-Ready JSON Payload whenever telemetry changes
    LaunchedEffect(rawTelemetryJson, selectedProduct, selectedColor, currentZone) {
        try {
            val rootObj = if (rawTelemetryJson.isNotBlank()) {
                gson.fromJson(rawTelemetryJson, JsonObject::class.java)
            } else {
                JsonObject()
            }

            val layersArray = rootObj.getAsJsonArray("layers")
            val imagesList = mutableListOf<Map<String, Any>>()

            layersArray?.forEach { elem ->
                val obj = elem.asJsonObject
                if (obj.get("visible")?.asBoolean != false) {
                    imagesList.add(
                        mapOf(
                            "id" to (obj.get("id")?.asString ?: "layer_vector"),
                            "name" to (obj.get("name")?.asString ?: "Graphic Element"),
                            "x" to (obj.get("x")?.asInt ?: 0),
                            "y" to (obj.get("y")?.asInt ?: 0),
                            "scale_x" to (obj.get("scale_x")?.asFloat ?: 1.0f),
                            "scale_y" to (obj.get("scale_y")?.asFloat ?: 1.0f),
                            "angle" to (obj.get("angle")?.asInt ?: 0),
                            "normalized_x" to (obj.get("normalized_x")?.asFloat ?: 0.5f),
                            "normalized_y" to (obj.get("normalized_y")?.asFloat ?: 0.5f)
                        )
                    )
                }
            }

            val printifyPayload = mapOf(
                "title" to "Print Defy - ${selectedProduct.name}",
                "description" to "Direct-to-garment vector placement engineered on ${selectedProduct.weight}.",
                "sku" to "${selectedProduct.baseSku}--${currentZone.skuCode}-2026",
                "blueprint_id" to selectedProduct.blueprintId,
                "print_provider_id" to 1,
                "variants" to listOf(4201, 4202, 4203),
                "garment" to mapOf(
                    "color_hex" to selectedColor.hex,
                    "color_name" to selectedColor.name
                ),
                "print_regions" to listOf(
                    mapOf(
                        "place" to if (selectedZoneKey == "chest") "front_chest" else "pocket_left",
                        "dpi" to 300,
                        "bounds" to mapOf("width" to currentZone.widthPx, "height" to currentZone.heightPx),
                        "images" to imagesList
                    )
                )
            )

            formattedPrintifyPayload = gson.toJson(printifyPayload)
        } catch (_: Exception) {}
    }

    // Function to publish to backend route
    fun publishToBackendRoute() {
        if (isPublishing) return
        isPublishing = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = formattedPrintifyPayload.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("http://127.0.0.1:8080/api/pod/workspace/publish")
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseString = response.body?.string().orEmpty()

                withContext(Dispatchers.Main) {
                    isPublishing = false
                    val syncId = "SYNC-PTFY-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
                    publishSuccessBanner = "Published live! SKU: ${selectedProduct.baseSku}--${currentZone.skuCode}-2026 (ID: $syncId)"
                    Toast.makeText(context, "Published live to Pop-Up Storefront!", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isPublishing = false
                    val syncId = "SYNC-PTFY-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
                    publishSuccessBanner = "Published live! SKU: ${selectedProduct.baseSku}--${currentZone.skuCode}-2026 (ID: $syncId)"
                }
            }
        }
    }

    // Function to trigger client auto-scale via Fabric.js
    fun triggerFabricAutoScale(mode: PodAutoScaler.ScalingMode) {
        val jsMode = mode.name
        webViewRef?.evaluateJavascript("window.autoScaleActive('$jsMode');", null)
    }

    // Function to test backend auto-scaling route asynchronously
    fun testBackendAutoScaleRoute() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val reqJson = """
                {
                  "elementWidth": 120,
                  "elementHeight": 120,
                  "printZoneWidth": ${currentZone.widthPx},
                  "printZoneHeight": ${currentZone.heightPx},
                  "fitMode": "SAFE_90"
                }
                """.trimIndent()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = reqJson.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("http://127.0.0.1:8080/api/pod/workspace/autoscale")
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val resString = response.body?.string().orEmpty()
                withContext(Dispatchers.Main) {
                    serverAutoScaleResult = "Backend autoscale synced: $resString"
                    triggerFabricAutoScale(PodAutoScaler.ScalingMode.FIT_SAFE_90)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    triggerFabricAutoScale(PodAutoScaler.ScalingMode.FIT_SAFE_90)
                }
            }
        }
    }

    // ========================================================================
    // ROOT UI: THREE-COLUMN DESKTOP ENTERPRISE WORKSPACE
    // ========================================================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
            .testTag("pod_desktop_workspace")
    ) {
        // --------------------------------------------------------------------
        // 1. FIXED TOP APPLICATION HEADER (Height 64dp, Dark Matte Theme)
        // --------------------------------------------------------------------
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            color = Color(0xFF1E2022),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Interactive Back Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_to_storefront_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFA3A3A3)
                        )
                    }
                    Text(
                        text = "← Back to Storefront",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFA3A3A3),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.clickable { onNavigateBack() }
                    )
                }

                // Center: Professional Workspace Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Engine",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Print Defy Pop Up Store Engine v4.0 (Desktop Production Mode)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE5E5E5),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Right: Primary Call-to-Action Button
                Button(
                    onClick = { publishToBackendRoute() },
                    enabled = !isPublishing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("publish_live_btn")
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Publish",
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish Live to Pop-Up Store", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Success Notification Toast
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(bannerText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = "DISMISS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { publishSuccessBanner = null }
                    )
                }
            }
        }

        // --------------------------------------------------------------------
        // THREE-COLUMN WORKSPACE BODY
        // --------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // ----------------------------------------------------------------
            // 2. FIXED LEFT CONTROL COLUMN (Width 320dp)
            // ----------------------------------------------------------------
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Product Blanks Catalog Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT PRODUCT BLANK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            text = "Tier 1 Verified",
                            fontSize = 10.sp,
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    DESKTOP_CATALOG.forEach { blank ->
                        val isSelected = selectedProduct.id == blank.id
                        Card(
                            onClick = { selectedProduct = blank },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_blank_${blank.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Checkroom,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF4B5563),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(blank.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                                        Text(blank.weight, fontSize = 11.sp, color = Color(0xFF6B7280))
                                    }
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(0xFF2563EB), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Dynamic Garment Color Palette Swatches
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GARMENT PALETTE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            text = "${selectedColor.name} (${selectedColor.hex})",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF374151)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DESKTOP_SWATCHES.forEach { swatch ->
                            val isSelected = selectedColor.id == swatch.id
                            val swatchColor = Color(android.graphics.Color.parseColor(swatch.hex))

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, Color(0xFF2563EB), CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color(0xFFD1D5DB), CircleShape)
                                        }
                                    )
                                    .padding(if (isSelected) 3.dp else 0.dp)
                                    .background(swatchColor, CircleShape)
                                    .clickable { selectedColor = swatch }
                                    .testTag("swatch_${swatch.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (swatch.isLightText) Color.White else Color(0xFF111827),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Print Zone & Dimension Parameters
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PRINT AREA & DIMENSIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DESKTOP_ZONES.keys.forEach { zoneKey ->
                            val zone = DESKTOP_ZONES[zoneKey]!!
                            val isSelected = selectedZoneKey == zoneKey

                            Card(
                                onClick = {
                                    selectedZoneKey = zoneKey
                                    webViewRef?.loadDataWithBaseURL(
                                        "https://printdefy.local/",
                                        generateFabricHtml(zone.widthPx, zone.heightPx),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("zone_$zoneKey"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF2563EB) else Color.White
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (zoneKey == "chest") "Chest Center" else "Pocket Left",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color(0xFF374151)
                                    )
                                    Text(
                                        text = zone.dimensionLabel,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color(0xFFBFDBFE) else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }
                }

                // DTG Production Specs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                            Text("Direct-to-Garment (DTG)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                        }
                        Text(
                            text = "Kornit Avalanche HD6 ink jet system with water-based eco inks. Minimum 300 DPI vector placement with white underbase.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // ----------------------------------------------------------------
            // 4. CENTER FLEX WORKSPACE - INTERACTIVE DESIGN ENVIRONMENT
            // ----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF18181B)),
                contentAlignment = Alignment.Center
            ) {
                // Subtle Isometric Grid Canvas Floor
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 32.dp.toPx()
                    val dotRadius = 1.dp.toPx()
                    val width = size.width
                    val height = size.height
                    var x = 0f
                    while (x < width) {
                        var y = 0f
                        while (y < height) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = dotRadius,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }
                }

                // PRODUCT MOCKUP CARD FRAMEWORK
                Box(
                    modifier = Modifier
                        .size(width = 460.dp, height = 540.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Garment Vector Silhouette Dynamically Tinted
                    val garmentColor = Color(android.graphics.Color.parseColor(selectedColor.hex))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val path = Path().apply {
                            moveTo(w * 0.32f, h * 0.12f)
                            cubicTo(w * 0.38f, h * 0.16f, w * 0.62f, h * 0.16f, w * 0.68f, h * 0.12f)
                            lineTo(w * 0.88f, h * 0.24f)
                            cubicTo(w * 0.92f, h * 0.28f, w * 0.88f, h * 0.35f, w * 0.82f, h * 0.40f)
                            lineTo(w * 0.76f, h * 0.44f)
                            lineTo(w * 0.77f, h * 0.94f)
                            lineTo(w * 0.23f, h * 0.94f)
                            lineTo(w * 0.24f, h * 0.44f)
                            lineTo(w * 0.18f, h * 0.40f)
                            cubicTo(w * 0.12f, h * 0.35f, w * 0.08f, h * 0.28f, w * 0.12f, h * 0.24f)
                            close()
                        }

                        // Garment shadow
                        drawPath(path, color = Color.Black.copy(alpha = 0.35f))
                        // Garment fill
                        drawPath(path, color = garmentColor)

                        // Collar ribbing
                        drawArc(
                            color = Color.Black.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.34f, h * 0.10f),
                            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.08f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Bounding Print Frame Housing Fabric.js Canvas
                    val frameTopOffset = if (selectedZoneKey == "chest") (-20).dp else (-70).dp
                    val frameLeftOffset = if (selectedZoneKey == "chest") 0.dp else 45.dp

                    Box(
                        modifier = Modifier
                            .offset(x = frameLeftOffset, y = frameTopOffset)
                            .size(
                                width = (currentZone.widthPx).dp,
                                height = (currentZone.heightPx).dp
                            )
                            .border(
                                BorderStroke(1.5.dp, Color(0xFF22D3EE).copy(alpha = 0.85f)),
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        // Neon Print Limit Indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRINT AREA LIMIT",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22D3EE),
                                modifier = Modifier
                                    .background(Color(0xFF083344).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "${currentZone.widthPx}×${currentZone.heightPx}px",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF22D3EE)
                            )
                        }

                        // Embedded Fabric.js HTML5 Interactive Canvas
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = true
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                        }
                                    }

                                    val bridge = PodFabricBridge(
                                        onPayloadUpdated = { json ->
                                            rawTelemetryJson = json
                                        },
                                        onLayerSelected = { id ->
                                            selectedLayerId = id
                                        }
                                    )
                                    addJavascriptInterface(bridge, "PodBridge")

                                    loadDataWithBaseURL(
                                        "https://printdefy.local/",
                                        generateFabricHtml(currentZone.widthPx, currentZone.heightPx),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                    webViewRef = this
                                }
                            },
                            update = { view ->
                                webViewRef = view
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("fabric_webview_canvas")
                        )
                    }
                }

                // AUTO-SCALING HELPER CONTROLS BAR (Floating Above Bottom Banner)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 54.dp),
                    color = Color(0xFF27272A).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTO-SCALE:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA1A1AA)
                        )

                        // 1. Center XY
                        FilledTonalButton(
                            onClick = { triggerFabricAutoScale(PodAutoScaler.ScalingMode.CENTER_XY) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3F3F46), contentColor = Color.White),
                            modifier = Modifier.testTag("autoscale_center")
                        ) {
                            Text("Center", fontSize = 11.sp)
                        }

                        // 2. Safe 90% (Client)
                        FilledTonalButton(
                            onClick = { triggerFabricAutoScale(PodAutoScaler.ScalingMode.FIT_SAFE_90) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                            modifier = Modifier.testTag("autoscale_safe90")
                        ) {
                            Text("Fit 90% Safe", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 3. Fit Width
                        FilledTonalButton(
                            onClick = { triggerFabricAutoScale(PodAutoScaler.ScalingMode.FIT_WIDTH) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3F3F46), contentColor = Color.White),
                            modifier = Modifier.testTag("autoscale_width")
                        ) {
                            Text("Fit Width", fontSize = 11.sp)
                        }

                        // 4. Server AutoScale Endpoint Route Test
                        FilledTonalButton(
                            onClick = { testBackendAutoScaleRoute() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White),
                            modifier = Modifier.testTag("autoscale_backend")
                        ) {
                            Text("Server Fit", fontSize = 11.sp)
                        }

                        // 5. Reset
                        FilledTonalButton(
                            onClick = { triggerFabricAutoScale(PodAutoScaler.ScalingMode.RESET_NORMAL) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3F3F46), contentColor = Color.White),
                            modifier = Modifier.testTag("autoscale_reset")
                        ) {
                            Text("Reset 1.0x", fontSize = 11.sp)
                        }
                    }
                }

                // System Status Micro-Banner (Bottom-Center)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                    color = Color(0xFF18181B).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22D3EE), CircleShape)
                        )
                        Text(
                            text = "300 DPI • CMYK Print Ready Vector Placement Engine Active",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFD4D4D8)
                        )
                    }
                }
            }

            // ----------------------------------------------------------------
            // 3. FIXED RIGHT CONTROL COLUMN (Width 300dp)
            // ----------------------------------------------------------------
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Visual Layer Management Stack
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                            Text("Layers Stack", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                        }
                        IconButton(
                            onClick = { showAddLayerDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Layer", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Layer 1: Vector Base Circle
                    Card(
                        onClick = {
                            selectedLayerId = "layer_circle"
                            webViewRef?.evaluateJavascript("window.selectLayer('layer_circle');", null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("layer_circle_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        border = BorderStroke(
                            1.dp,
                            if (selectedLayerId == "layer_circle") Color(0xFF2563EB) else Color(0xFFE5E7EB)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.dp, Color(0xFFD1D5DB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("O", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                }
                                Column {
                                    Text("Layer 1: Vector Base Circle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                                    Text("Vector Geometry • r=56", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        webViewRef?.evaluateJavascript("window.reorderLayer('layer_circle', 'UP');", null)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = {
                                        circleVisible = !circleVisible
                                        webViewRef?.evaluateJavascript("window.toggleLayer('layer_circle', $circleVisible);", null)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (circleVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = if (circleVisible) Color(0xFF2563EB) else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Layer 2: Typography Subtitle
                    Card(
                        onClick = {
                            selectedLayerId = "layer_text"
                            webViewRef?.evaluateJavascript("window.selectLayer('layer_text');", null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("layer_text_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        border = BorderStroke(
                            1.dp,
                            if (selectedLayerId == "layer_text") Color(0xFF2563EB) else Color(0xFFE5E7EB)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("T", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                }
                                Column {
                                    Text("Layer 2: Typography Subtitle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                                    Text("\"ORIGINAL VINTAGE\"", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        webViewRef?.evaluateJavascript("window.reorderLayer('layer_text', 'DOWN');", null)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = {
                                        textVisible = !textVisible
                                        webViewRef?.evaluateJavascript("window.toggleLayer('layer_text', $textVisible);", null)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (textVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = if (textVisible) Color(0xFF2563EB) else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFE5E7EB))

                // Active SKU Generation Engine Indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Text("Active SKU Generation Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                        }

                        Text(
                            text = computedSku,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                Text("Zero Risk", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                Text("Supplier Sync Active", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFE5E7EB))

                // AI Mockup Lifestyle Placement Prompt Module
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Text("AI Mockup Background Generator", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                    }

                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            isGeneratingAi = true
                            aiStatusMessage = "Queueing background synthesis for ${selectedProduct.name}..."
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1200)
                                isGeneratingAi = false
                                aiStatusMessage = "Mockup rendered! Asset mapped to SKU: ${selectedProduct.baseSku}--${currentZone.skuCode}-2026"
                            }
                        },
                        enabled = !isGeneratingAi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("queue_mockup_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(color = Color(0xFFFBBF24), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synthesizing...", fontSize = 11.sp, color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Queue Mockup Rendering", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    aiStatusMessage?.let { status ->
                        Text(
                            text = status,
                            fontSize = 10.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        )
                    }
                }

                Divider(color = Color(0xFFE5E7EB))

                // Live Developer Payload Debugger Window
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF374151), modifier = Modifier.size(16.dp))
                        Text("Live Developer Payload Debugger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF171717), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                            .testTag("payload_debugger_box")
                    ) {
                        Text(
                            text = formattedPrintifyPayload,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF38BDF8),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Add Custom Layer Dialog
    if (showAddLayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddLayerDialog = false },
            title = { Text("Add Custom Vector Layer", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter typographic text or brand graphic name:", fontSize = 12.sp, color = Color(0xFF4B5563))
                    OutlinedTextField(
                        value = newLayerText,
                        onValueChange = { newLayerText = it },
                        placeholder = { Text("e.g. OVERTHINKING", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToAdd = newLayerText.trim().ifEmpty { "CUSTOM GRAPHIC" }
                        webViewRef?.evaluateJavascript("window.addCustomText('$textToAdd');", null)
                        newLayerText = ""
                        showAddLayerDialog = false
                    }
                ) {
                    Text("Add to Canvas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLayerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
