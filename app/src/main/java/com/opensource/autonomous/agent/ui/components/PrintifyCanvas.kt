package com.opensource.autonomous.agent.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.opensource.autonomous.agent.ui.pod.*

@Composable
fun PrintifyCanvas(
    product: PrintifyProductBlank = PRINTIFY_CATALOG[0],
    color: PrintifyColorSwatch = PRINTIFY_SWATCHES[0],
    zone: PrintifyPrintZone = PRINTIFY_ZONES["chest"]!!
) {
    var layers by remember {
        mutableStateOf(
            listOf(
                PodVectorLayer(
                    id = "layer_base_crest",
                    name = "KL Signature Crest",
                    type = PodLayerType.VECTOR,
                    textContent = "Kritical Limitz",
                    scale = 1.0f,
                    tintHex = "#FFFFFF"
                )
            )
        )
    }
    var selectedLayerId by remember { mutableStateOf<String?>("layer_base_crest") }

    Box(modifier = Modifier.fillMaxSize()) {
        PodVectorCanvas(
            product = product,
            color = color,
            zone = zone,
            layers = layers,
            selectedLayerId = selectedLayerId,
            onSelectLayer = { selectedLayerId = it },
            onUpdateLayer = { updated ->
                layers = layers.map { if (it.id == updated.id) updated else it }
            },
            onDuplicateLayer = { orig ->
                val copy = orig.copy(
                    id = "layer_${System.currentTimeMillis() % 10000}",
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
            isDarkTheme = true
        )
    }
}
