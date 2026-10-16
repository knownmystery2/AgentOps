package com.example

import com.opensource.autonomous.agent.ui.pod.*
import com.opensource.autonomous.agent.ui.screens.CLIPART_PRESETS
import com.opensource.autonomous.agent.ui.screens.PodAutoScaler
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests validating the POD Pop Up Store Ultra-Grade Workspace Architecture:
 * - Printify Catalog Blueprint integrity & provider data
 * - Live DPI and resolution compliance
 * - Gross profit and margin computations
 * - Vector layer transformations and autoscaling algorithms
 */
class PodPopUpStoreArchitectureTest {

    @Test
    fun testPrintifyCatalogBlueprintsIntegrity() {
        assertFalse("Printify catalog must not be empty", PRINTIFY_CATALOG.isEmpty())
        assertTrue("Printify catalog should have multiple high-quality blanks", PRINTIFY_CATALOG.size >= 5)

        // Verify key blueprints exist
        val blueprintIds = PRINTIFY_CATALOG.map { it.blueprintId }
        assertTrue("Must include Bella+Canvas 3001 (Blueprint 12)", blueprintIds.contains("12"))
        assertTrue("Must include Gildan 18500 Hoodie (Blueprint 412)", blueprintIds.contains("412"))
        assertTrue("Must include Ceramic Mug 11oz (Blueprint 68)", blueprintIds.contains("68"))

        PRINTIFY_CATALOG.forEach { blank ->
            assertTrue("Product name should not be blank", blank.name.isNotBlank())
            assertTrue("Base SKU should not be blank", blank.baseSku.isNotBlank())
            assertTrue("Retail price must exceed COGS", blank.retailPriceCents > blank.cogsCents)
            assertTrue("Profit margin must be positive", blank.profitMarginPercent > 0)
            assertTrue("Available sizes must not be empty", blank.availableSizes.isNotEmpty())
            assertTrue("Supported zones must not be empty", blank.supportedZones.isNotEmpty())
        }
    }

    @Test
    fun testPrintifyColorSwatchesAndZones() {
        assertFalse("Color swatches must not be empty", PRINTIFY_SWATCHES.isEmpty())
        assertTrue("Must contain standard apparel colors", PRINTIFY_SWATCHES.any { it.name == "Pitch Black" })
        assertTrue("Must contain Crisp White", PRINTIFY_SWATCHES.any { it.name == "Crisp White" })

        assertFalse("Print zones must not be empty", PRINTIFY_ZONES.isEmpty())
        val chestZone = PRINTIFY_ZONES["chest"]
        assertNotNull("Chest zone must exist", chestZone)
        assertEquals(240, chestZone?.widthPx)
        assertEquals(280, chestZone?.heightPx)

        val backZone = PRINTIFY_ZONES["back"]
        assertNotNull("Back zone must exist", backZone)
        assertEquals(240, backZone?.widthPx)
        assertEquals(310, backZone?.heightPx)
    }

    @Test
    fun testLayerDpiCalculationAndResolutionStatus() {
        val defaultLayer = PodVectorLayer(scale = 1.0f)
        assertEquals(300, defaultLayer.liveDpi)
        assertEquals("300 DPI • Print Ready", defaultLayer.dpiStatus)

        val scaledDownLayer = PodVectorLayer(scale = 0.5f)
        assertEquals(600, scaledDownLayer.liveDpi)
        assertEquals("300 DPI • Print Ready", scaledDownLayer.dpiStatus)

        val largeLayer = PodVectorLayer(scale = 1.35f)
        assertTrue("Should be between 200 and 299 DPI", largeLayer.liveDpi in 200..299)
        assertEquals("200 DPI • Good Quality", largeLayer.dpiStatus)

        val oversizedLayer = PodVectorLayer(scale = 2.5f)
        assertTrue("Should be under 200 DPI", oversizedLayer.liveDpi < 200)
        assertEquals("Low Resolution Warning", oversizedLayer.dpiStatus)
    }

    @Test
    fun testProfitAndCogsCalculations() {
        val tShirt = PRINTIFY_CATALOG.first { it.blueprintId == "12" }
        assertEquals(3200L, tShirt.retailPriceCents)
        assertEquals(1250L, tShirt.cogsCents)
        assertEquals(1950L, tShirt.grossProfitCents)
        // Profit margin: 1950 / 3200 * 100 = 60.9% -> 60%
        assertEquals(60, tShirt.profitMarginPercent)

        val hoodie = PRINTIFY_CATALOG.first { it.blueprintId == "412" }
        assertEquals(4800L, hoodie.retailPriceCents)
        assertEquals(2250L, hoodie.cogsCents)
        assertEquals(2550L, hoodie.grossProfitCents)
        // Profit margin: 2550 / 4800 * 100 = 53.1% -> 53%
        assertEquals(53, hoodie.profitMarginPercent)
    }

    @Test
    fun testPodAutoScalerUtilities() {
        val layer = PodVectorLayer(scale = 1.8f, rotation = 45f, offsetX = 100f, offsetY = -80f)
        val centered = PodAutoScaler.centerLayer(layer)
        assertEquals(0f, centered.offsetX)
        assertEquals(0f, centered.offsetY)
        assertEquals(1.8f, centered.scale)

        val safeLayer = PodAutoScaler.fitLayerToSafeZone(layer, PodAutoScaler.ScalingMode.FIT_SAFE_90)
        assertEquals(0.9f, safeLayer.scale)
        assertEquals(0f, safeLayer.offsetX)
        assertEquals(0f, safeLayer.offsetY)
    }

    @Test
    fun testClipartPresets() {
        assertFalse("Clipart presets must be available for instant vector placement", CLIPART_PRESETS.isEmpty())
        assertTrue("Must contain Payasa preset", CLIPART_PRESETS.any { it.id == "kl_payasa" })
        assertTrue("Must contain Vintage Skull preset", CLIPART_PRESETS.any { it.id == "vintage_skull" })
        assertTrue("Must contain Gothic Monogram preset", CLIPART_PRESETS.any { it.id == "kl_gothic" })
        CLIPART_PRESETS.forEach { preset ->
            assertTrue("Preset title should be valid", preset.title.isNotBlank())
            assertTrue("Preset category should be valid", preset.category.isNotBlank())
        }
    }
}
