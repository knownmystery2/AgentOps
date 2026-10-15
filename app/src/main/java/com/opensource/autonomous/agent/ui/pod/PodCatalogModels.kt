package com.opensource.autonomous.agent.ui.pod

import java.util.UUID

// ============================================================================
// PRINTIFY CATALOG & PRODUCT BLUEPRINT DATA MODELS
// ============================================================================

enum class PodLayerType {
    VECTOR,
    TEXT,
    AI_ART,
    SHAPE
}

data class PodVectorLayer(
    val id: String = "layer_" + UUID.randomUUID().toString().take(6),
    val name: String = "Graphic Layer",
    val type: PodLayerType = PodLayerType.VECTOR,
    var textContent: String = "KRITICAL LIMITZ",
    var fontStyle: String = "Gothic Chicano",
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var scale: Float = 1.0f,
    var rotation: Float = 0f,
    var tintHex: String = "#FFFFFF",
    var opacity: Float = 1.0f,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false,
    var isFlippedH: Boolean = false,
    var isFlippedV: Boolean = false
) {
    val liveDpi: Int
        get() = (300f / scale.coerceAtLeast(0.1f)).toInt().coerceIn(72, 600)

    val dpiStatus: String
        get() = when {
            liveDpi >= 300 -> "300 DPI • Print Ready"
            liveDpi >= 200 -> "200 DPI • Good Quality"
            else -> "Low Resolution Warning"
        }
}

data class PrintifyProductBlank(
    val id: String,
    val name: String,
    val brandModel: String,
    val category: String,
    val weight: String,
    val baseSku: String,
    val blueprintId: String,
    val silhouetteType: String,
    val retailPriceCents: Long = 3200L,
    val cogsCents: Long = 1250L,
    val providerName: String = "Monster Digital",
    val providerLocation: String = "United States (FL / NV)",
    val turnaroundTime: String = "2-4 business days",
    val providerRating: Float = 9.8f,
    val availableSizes: List<String> = listOf("S", "M", "L", "XL", "2XL", "3XL"),
    val supportedZones: List<String> = listOf("chest", "back", "pocket"),
    val description: String = "Direct-to-garment underground luxury streetwear vector placement."
) {
    val grossProfitCents: Long
        get() = (retailPriceCents - cogsCents).coerceAtLeast(0L)

    val profitMarginPercent: Int
        get() = if (retailPriceCents > 0) ((grossProfitCents.toDouble() / retailPriceCents) * 100).toInt() else 0
}

data class PrintifyColorSwatch(
    val id: String,
    val name: String,
    val hex: String,
    val isLightText: Boolean
)

data class PrintifyPrintZone(
    val id: String,
    val name: String,
    val skuCode: String,
    val widthPx: Int,
    val heightPx: Int,
    val dimensionLabel: String,
    val printSpecs: String = "300 DPI • Max Printable Canvas"
)

data class PrintifyProviderInfo(
    val id: Int,
    val name: String,
    val location: String,
    val turnaround: String,
    val basePriceCents: Long,
    val score: Float,
    val badge: String
)

data class ClipartPreset(
    val id: String,
    val title: String,
    val category: String,
    val color: String,
    val previewIcon: String = "vector"
)

// ============================================================================
// AUTHENTIC PRINTIFY BLUEPRINT CATALOG (14 INDUSTRY-LEADING PRODUCTS)
// ============================================================================

val PRINTIFY_CATALOG: List<PrintifyProductBlank> = listOf(
    PrintifyProductBlank(
        id = "heavyweight-tshirt",
        name = "Unisex Heavy Cotton Tee",
        brandModel = "Gildan 5000",
        category = "Apparel",
        weight = "180 GSM 100% Pre-Shrunk Cotton",
        baseSku = "PRT-GIL5000",
        blueprintId = "384",
        silhouetteType = "tshirt",
        retailPriceCents = 2900L,
        cogsCents = 1025L,
        providerName = "Monster Digital",
        providerLocation = "United States (FL / NV)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.8f,
        availableSizes = listOf("S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL"),
        supportedZones = listOf("chest", "back", "pocket", "sleeve_left"),
        description = "Industry standard streetwear heavy cotton blank. Classic crew neck and tubular body."
    ),
    PrintifyProductBlank(
        id = "jersey-tshirt",
        name = "Unisex Jersey Short Sleeve Tee",
        brandModel = "Bella+Canvas 3001",
        category = "Apparel",
        weight = "142 GSM Ringspun Combed Cotton",
        baseSku = "PRT-BC3001",
        blueprintId = "12",
        silhouetteType = "tshirt",
        retailPriceCents = 3200L,
        cogsCents = 1250L,
        providerName = "SwiftPOD",
        providerLocation = "United States (CA / FL)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.7f,
        availableSizes = listOf("XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL"),
        supportedZones = listOf("chest", "back", "pocket", "sleeve_left"),
        description = "Premium retail-fitting retail tee. Ultra-soft feel, side-seamed construction, fitted bicep."
    ),
    PrintifyProductBlank(
        id = "garment-dyed-tshirt",
        name = "Garment-Dyed Heavyweight Tee",
        brandModel = "Comfort Colors 1717",
        category = "Apparel",
        weight = "207 GSM 100% Ring-Spun Cotton",
        baseSku = "PRT-CC1717",
        blueprintId = "1042",
        silhouetteType = "tshirt",
        retailPriceCents = 3800L,
        cogsCents = 1780L,
        providerName = "Awkward Styles",
        providerLocation = "United States (CA)",
        turnaroundTime = "3-5 business days",
        providerRating = 9.6f,
        availableSizes = listOf("S", "M", "L", "XL", "2XL", "3XL"),
        supportedZones = listOf("chest", "back", "pocket"),
        description = "Vintage wash luxury streetwear heavyweight tee. Soft-washed garment-dyed fabric."
    ),
    PrintifyProductBlank(
        id = "vintage-hoodie",
        name = "Heavy Blend Hooded Sweatshirt",
        brandModel = "Gildan 18500",
        category = "Apparel",
        weight = "271 GSM 50/50 Cotton Fleece",
        baseSku = "PRT-GIL18500",
        blueprintId = "412",
        silhouetteType = "hoodie",
        retailPriceCents = 4800L,
        cogsCents = 2250L,
        providerName = "Monster Digital",
        providerLocation = "United States (FL / NV)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.8f,
        availableSizes = listOf("S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL"),
        supportedZones = listOf("chest", "back", "pocket"),
        description = "Heavy plush fleece pullover with color-matched drawcord, pouch pocket, and double-needle stitching."
    ),
    PrintifyProductBlank(
        id = "crewneck-sweatshirt",
        name = "Heavy Blend Crewneck",
        brandModel = "Gildan 18000",
        category = "Apparel",
        weight = "271 GSM Brushed Fleece",
        baseSku = "PRT-GIL18000",
        blueprintId = "29",
        silhouetteType = "crewneck",
        retailPriceCents = 4200L,
        cogsCents = 1890L,
        providerName = "SwiftPOD",
        providerLocation = "United States (CA / FL)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.7f,
        availableSizes = listOf("S", "M", "L", "XL", "2XL", "3XL"),
        supportedZones = listOf("chest", "back", "sleeve_left"),
        description = "Classic relaxed crewneck sweater with 1x1 ribbed spandex cuffs and collar."
    ),
    PrintifyProductBlank(
        id = "all-over-hoodie",
        name = "All-Over Print Unisex Hoodie",
        brandModel = "Sublimation Custom",
        category = "Apparel",
        weight = "290 GSM Poly-Spandex Microfleece",
        baseSku = "PRT-AOPHOD",
        blueprintId = "3",
        silhouetteType = "hoodie",
        retailPriceCents = 6400L,
        cogsCents = 3450L,
        providerName = "Drive Fulfillment",
        providerLocation = "United States (UT)",
        turnaroundTime = "3-5 business days",
        providerRating = 9.5f,
        availableSizes = listOf("XS", "S", "M", "L", "XL", "2XL"),
        supportedZones = listOf("chest", "back"),
        description = "Full 360-degree all-over dye sublimation cut & sew hoodie with vivid color permanence."
    ),
    PrintifyProductBlank(
        id = "ceramic-mug",
        name = "Ceramic Mug 11oz",
        brandModel = "ORCA Coatings",
        category = "Drinkware",
        weight = "330 ml High-Gloss Ceramic",
        baseSku = "PRT-MUG11",
        blueprintId = "68",
        silhouetteType = "mug",
        retailPriceCents = 1800L,
        cogsCents = 650L,
        providerName = "District Photo",
        providerLocation = "United States (MD)",
        turnaroundTime = "2-3 business days",
        providerRating = 9.9f,
        availableSizes = listOf("11 oz", "15 oz"),
        supportedZones = listOf("chest"),
        description = "Dishwasher and microwave safe high-gloss ceramic mug with durable sublimation wrap."
    ),
    PrintifyProductBlank(
        id = "stainless-tumbler",
        name = "Insulated Tumbler with Straw 20oz",
        brandModel = "Double Wall Vacuum",
        category = "Drinkware",
        weight = "20 oz 304 Stainless Steel",
        baseSku = "PRT-TUMB20",
        blueprintId = "440",
        silhouetteType = "tumbler",
        retailPriceCents = 2800L,
        cogsCents = 1420L,
        providerName = "Monster Digital",
        providerLocation = "United States (FL)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.7f,
        availableSizes = listOf("20 oz"),
        supportedZones = listOf("chest"),
        description = "Double-wall stainless steel vacuum insulated tumbler with clear splash-proof lid."
    ),
    PrintifyProductBlank(
        id = "canvas-tote",
        name = "Heavy Cotton Canvas Tote Bag",
        brandModel = "Port Authority B150",
        category = "Accessories",
        weight = "100% Spun Poly / Heavy Canvas",
        baseSku = "PRT-TOTE",
        blueprintId = "52",
        silhouetteType = "tote",
        retailPriceCents = 2400L,
        cogsCents = 950L,
        providerName = "SwiftPOD",
        providerLocation = "United States (CA)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.6f,
        availableSizes = listOf("One Size (15\" × 16\")"),
        supportedZones = listOf("chest"),
        description = "Durable reinforced cotton canvas tote with 22\" handles for everyday carry."
    ),
    PrintifyProductBlank(
        id = "die-cut-stickers",
        name = "Die-Cut Vinyl Decal Stickers",
        brandModel = "Weatherproof UV 3M",
        category = "Accessories",
        weight = "Heavy-Duty 6 mil Calendered Vinyl",
        baseSku = "PRT-STKDC",
        blueprintId = "393",
        silhouetteType = "sticker",
        retailPriceCents = 800L,
        cogsCents = 240L,
        providerName = "Spoke Custom",
        providerLocation = "United States (GA)",
        turnaroundTime = "1-3 business days",
        providerRating = 9.8f,
        availableSizes = listOf("2\" × 2\"", "3\" × 3\"", "4\" × 4\"", "6\" × 6\""),
        supportedZones = listOf("chest"),
        description = "Precision die-cut stickers with satin laminate. Scratch, water, and sunlight resistant."
    ),
    PrintifyProductBlank(
        id = "tough-phone-case",
        name = "Tough Dual-Layer Phone Case",
        brandModel = "iPhone / Galaxy Tough Case",
        category = "Accessories",
        weight = "Dual Polycarbonate + TPU Liner",
        baseSku = "PRT-CASING",
        blueprintId = "120",
        silhouetteType = "phonecase",
        retailPriceCents = 2900L,
        cogsCents = 1350L,
        providerName = "WOKN",
        providerLocation = "United States (TX)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.6f,
        availableSizes = listOf("iPhone 15 Pro", "iPhone 16 Pro", "Galaxy S24 Ultra", "Galaxy S10e"),
        supportedZones = listOf("chest"),
        description = "Dual-layer impact protection with high-gloss wrap around print."
    ),
    PrintifyProductBlank(
        id = "wall-art-poster",
        name = "Fine Art Matte Vertical Poster",
        brandModel = "Museum Grade Archival",
        category = "Home Decor",
        weight = "250 GSM Matte Fine Art Paper",
        baseSku = "PRT-PST599",
        blueprintId = "599",
        silhouetteType = "poster",
        retailPriceCents = 2200L,
        cogsCents = 700L,
        providerName = "Sensaria",
        providerLocation = "United States (WI)",
        turnaroundTime = "2-3 business days",
        providerRating = 9.8f,
        availableSizes = listOf("12\" × 18\"", "18\" × 24\"", "24\" × 36\""),
        supportedZones = listOf("chest"),
        description = "Archival museum-grade paper with pigment inks for lifelike gallery color reproduction."
    ),
    PrintifyProductBlank(
        id = "canvas-gallery-wrap",
        name = "Stretched Canvas Gallery Wrap",
        brandModel = "1.25\" Solid Face Wood Frame",
        category = "Home Decor",
        weight = "100% Cotton Artist Grade Canvas",
        baseSku = "PRT-CNV281",
        blueprintId = "281",
        silhouetteType = "canvas",
        retailPriceCents = 5800L,
        cogsCents = 2400L,
        providerName = "Sensaria",
        providerLocation = "United States (WI)",
        turnaroundTime = "2-4 business days",
        providerRating = 9.8f,
        availableSizes = listOf("12\" × 16\"", "16\" × 20\"", "24\" × 30\""),
        supportedZones = listOf("chest"),
        description = "High-definition giclée print on stretched cotton canvas with sealed backing and pre-installed hardware."
    ),
    PrintifyProductBlank(
        id = "sherpa-blanket",
        name = "Ultra-Plush Sherpa Fleece Blanket",
        brandModel = "Microfiber + Faux Sherpa",
        category = "Home Decor",
        weight = "480 GSM Ultra-Warm Dual Layer",
        baseSku = "PRT-BLNK77",
        blueprintId = "77",
        silhouetteType = "blanket",
        retailPriceCents = 6200L,
        cogsCents = 2850L,
        providerName = "MWW On Demand",
        providerLocation = "United States (NC)",
        turnaroundTime = "3-5 business days",
        providerRating = 9.7f,
        availableSizes = listOf("50\" × 60\"", "60\" × 80\""),
        supportedZones = listOf("chest"),
        description = "Ultra-soft printed polyester micro-fleece face with fluffy cream faux sherpa fleece backing."
    )
)

// Swatches with accurate Pantone / Printify representation
val PRINTIFY_SWATCHES: List<PrintifyColorSwatch> = listOf(
    PrintifyColorSwatch("pitch-black", "Pitch Black", "#121417", false),
    PrintifyColorSwatch("dark-slate", "Charcoal Slate", "#262E3D", false),
    PrintifyColorSwatch("classic-white", "Crisp White", "#FFFFFF", true),
    PrintifyColorSwatch("cream", "Vintage Natural Cream", "#F5EBE6", true),
    PrintifyColorSwatch("forest-green", "Forest Pine Green", "#1A331E", false),
    PrintifyColorSwatch("terracotta", "Terracotta Sand", "#C27648", false),
    PrintifyColorSwatch("navy-indigo", "Navy Indigo", "#1E293B", false),
    PrintifyColorSwatch("dark-teal", "Dark Teal Cyan", "#0D5C53", false),
    PrintifyColorSwatch("maroon", "Maroon Burgundy", "#4A0E17", false),
    PrintifyColorSwatch("heather-grey", "Vintage Heather Grey", "#8E96A0", false)
)

// Print zones mapped by key
val PRINTIFY_ZONES: Map<String, PrintifyPrintZone> = mapOf(
    "chest" to PrintifyPrintZone("chest", "Front Chest", "CHST", 240, 280, "11\" × 13\" Print Area", "3300 × 3900 px @ 300 DPI"),
    "back" to PrintifyPrintZone("back", "Back Large", "BCKL", 240, 310, "14\" × 16\" Large Back", "4200 × 4800 px @ 300 DPI"),
    "pocket" to PrintifyPrintZone("pocket", "Left Pocket", "PKTL", 100, 100, "4\" × 4\" Pocket Chest", "1200 × 1200 px @ 300 DPI"),
    "sleeve_left" to PrintifyPrintZone("sleeve_left", "Left Sleeve", "SLVL", 90, 220, "3.5\" × 12\" Sleeve", "1050 × 3600 px @ 300 DPI"),
    "sleeve_right" to PrintifyPrintZone("sleeve_right", "Right Sleeve", "SLVR", 90, 220, "3.5\" × 12\" Sleeve", "1050 × 3600 px @ 300 DPI")
)

// Printify Providers
val PRINTIFY_PROVIDERS: List<PrintifyProviderInfo> = listOf(
    PrintifyProviderInfo(1, "Monster Digital", "United States (FL / NV)", "2-4 days", 1025L, 9.8f, "Best Overall DTG"),
    PrintifyProviderInfo(2, "SwiftPOD", "United States (CA / FL)", "2-4 days", 1250L, 9.7f, "Top Reliability"),
    PrintifyProviderInfo(3, "Awkward Styles", "United States (CA)", "3-5 days", 1190L, 9.6f, "Eco Packaging"),
    PrintifyProviderInfo(4, "Drive Fulfillment", "United States (UT)", "3-5 days", 1320L, 9.5f, "AOP Specialist"),
    PrintifyProviderInfo(5, "Textildruck Europa", "Germany (EU)", "3-6 days", 1450L, 9.4f, "EU Fast Ship")
)

// Underground Streetwear Clipart Presets
val STREETWEAR_CLIPART_PRESETS: List<ClipartPreset> = listOf(
    ClipartPreset("kl_payasa", "KL Fine-Line Payasa", "Chicano Streetwear", "#F8FAFC"),
    ClipartPreset("kl_scarab", "Egyptian Scarab & Ankh", "Occult Luxury", "#F59E0B"),
    ClipartPreset("kl_sigil", "Esoteric Satanic Sigil", "Dark Occult", "#EF4444"),
    ClipartPreset("kl_paisley", "Dark Paisley & Barbed Wire", "Luxury Texture", "#A855F7"),
    ClipartPreset("kl_hellhound", "Crowned Hellhound", "Underground Street", "#10B981"),
    ClipartPreset("kl_gothic", "Kritical Limitz Gothic Monogram", "Signature Luxury", "#38BDF8"),
    ClipartPreset("vintage_skull", "Rose & Vintage Skull", "Tattoo Fine-Line", "#EF4444"),
    ClipartPreset("cyber_crest", "Cyberpunk Hex Crest", "Techwear", "#06B6D4"),
    ClipartPreset("sacred_heart", "Sacred Heart & Daggers", "Chicano Heritage", "#F43F5E"),
    ClipartPreset("chrome_impala", "Lowrider Impala Script", "West Coast Chrome", "#E2E8F0"),
    ClipartPreset("barbed_wire", "Barbed Wire Ring Border", "Street Texture", "#94A3B8"),
    ClipartPreset("barcode_logo", "Underground Luxury Barcode", "Signature Monogram", "#FFFFFF")
)
