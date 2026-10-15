package com.opensource.autonomous.agent.network

import com.google.gson.annotations.SerializedName

// ============================================================================
// PRINTIFY CATALOG BLUEPRINTS & PROVIDER MODELS
// ============================================================================

data class Blueprint(
    val id: String,
    val title: String,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val images: List<String> = emptyList()
)

data class PrintProvider(
    val id: Int,
    val title: String,
    val location: Map<String, Any>? = null
)

data class VariantResponse(
    val id: Int? = null,
    val title: String? = null,
    val variants: List<Variant> = emptyList()
)

data class Variant(
    val id: Int,
    val title: String,
    val colors: List<String> = emptyList(),
    val options: Map<String, Any>? = null,
    val placeholders: List<VariantPlaceholder> = emptyList(),
    val price: Int? = null,
    val cost: Int? = null,
    val is_enabled: Boolean = true
)

data class VariantPlaceholder(
    val position: String,
    val height: Int,
    val width: Int
)

// ============================================================================
// PRINTIFY PRODUCT MANAGEMENT & PUBLISHING MODELS
// ============================================================================

data class PrintifyProductRequest(
    val title: String,
    val description: String,
    val blueprint_id: String,
    val print_provider_id: Int,
    val variants: List<Int>,
    val print_regions: List<PrintifyPrintRegion>,
    val tags: List<String> = listOf("kritical-limitz", "streetwear", "dtg-apparel")
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

data class PrintifyProductResponse(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val blueprint_id: Int? = null,
    val print_provider_id: Int? = null,
    val created_at: String? = null
)

data class PrintifyProduct(
    val id: String,
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val options: List<Map<String, Any>> = emptyList(),
    val variants: List<Map<String, Any>> = emptyList(),
    val images: List<Map<String, Any>> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null,
    val visible: Boolean = true,
    val is_locked: Boolean = false,
    val blueprint_id: Int? = null,
    val user_id: Long? = null,
    val shop_id: Long? = null,
    val print_provider_id: Int? = null,
    val print_areas: List<Map<String, Any>> = emptyList(),
    val sales_channel_properties: List<Any> = emptyList()
)

data class ProductListResponse(
    val current_page: Int = 1,
    val data: List<PrintifyProduct> = emptyList(),
    val last_page: Int = 1,
    val total: Int = 0
)

data class PublishRequest(
    val title: Boolean = true,
    val description: Boolean = true,
    val images: Boolean = true,
    val variants: Boolean = true,
    val tags: Boolean = true,
    @SerializedName("keyFeatures")
    val keyFeatures: Boolean = true,
    val shipping_template: Boolean = true
)

data class PublishStatusResponse(
    val id: String,
    val status: String? = null
)

// ============================================================================
// IMAGE UPLOAD & ARTWORK ASSET MODELS
// ============================================================================

data class ImageUploadRequest(
    val file_name: String,
    val url: String? = null,
    val contents: String? = null
)

data class ImageUploadResponse(
    val id: String,
    val file_name: String,
    val height: Int = 0,
    val width: Int = 0,
    val size: Long = 0L,
    val mime_type: String = "image/png",
    val preview_url: String? = null,
    val upload_time: String? = null
)

data class PrintifyShopResponse(
    val id: Long,
    val title: String,
    val sales_channel: String? = null
)
