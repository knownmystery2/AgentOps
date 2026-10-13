package com.opensource.autonomous.agent.network

data class Blueprint(val id: String, val title: String)

data class VariantResponse(val variants: List<Variant>)
data class Variant(val id: Int, val title: String, val colors: List<String>)

data class PrintifyProductRequest(
    val title: String,
    val description: String,
    val blueprint_id: String,
    val print_provider_id: Int,
    val variants: List<Int>,
    val print_regions: List<PrintifyPrintRegion>
)

data class PrintifyPrintRegion(
    val place: String,
    val images: List<PrintifyImage>
)

data class PrintifyImage(
    val id: String,
    val x: Int,
    val y: Int,
    val scale_x: Float,
    val scale_y: Float,
    val angle: Int
)

data class PrintifyProductResponse(val id: String)
