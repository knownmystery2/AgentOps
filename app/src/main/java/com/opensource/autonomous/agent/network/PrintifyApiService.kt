package com.opensource.autonomous.agent.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface defining the complete Printify REST API v1 specification
 * for e-commerce catalog blueprints, print providers, variants, and product management.
 */
interface PrintifyApiService {

    // ------------------------------------------------------------------------
    // SHOPS
    // ------------------------------------------------------------------------
    @GET("v1/shops.json")
    suspend fun getShops(
        @Header("Authorization") token: String
    ): List<PrintifyShopResponse>

    // ------------------------------------------------------------------------
    // CATALOG & BLUEPRINTS
    // ------------------------------------------------------------------------
    @GET("v1/catalog/blueprints.json")
    suspend fun getBlueprints(
        @Header("Authorization") token: String
    ): List<Blueprint>

    @GET("v1/catalog/blueprints/{blueprint_id}.json")
    suspend fun getBlueprintDetails(
        @Header("Authorization") token: String,
        @Path("blueprint_id") blueprintId: String
    ): Blueprint

    @GET("v1/catalog/blueprints/{blueprint_id}/print_providers.json")
    suspend fun getPrintProviders(
        @Header("Authorization") token: String,
        @Path("blueprint_id") blueprintId: String
    ): List<PrintProvider>

    @GET("v1/catalog/blueprints/{blueprint_id}/print_providers/{print_provider_id}/variants.json")
    suspend fun getVariants(
        @Header("Authorization") token: String,
        @Path("blueprint_id") blueprintId: String,
        @Path("print_provider_id") printProviderId: Int = 1
    ): VariantResponse

    // ------------------------------------------------------------------------
    // PRODUCT MANAGEMENT
    // ------------------------------------------------------------------------
    @GET("v1/shops/{shop_id}/products.json")
    suspend fun getProducts(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ProductListResponse

    @GET("v1/shops/{shop_id}/products/{product_id}.json")
    suspend fun getProduct(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Path("product_id") productId: String
    ): PrintifyProduct

    @POST("v1/shops/{shop_id}/products.json")
    suspend fun publishProduct(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Body product: PrintifyProductRequest
    ): PrintifyProductResponse

    @PUT("v1/shops/{shop_id}/products/{product_id}.json")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Path("product_id") productId: String,
        @Body product: PrintifyProductRequest
    ): PrintifyProduct

    @DELETE("v1/shops/{shop_id}/products/{product_id}.json")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Path("product_id") productId: String
    ): Response<Unit>

    // ------------------------------------------------------------------------
    // LIVE STOREFRONT PUBLISHING
    // ------------------------------------------------------------------------
    @POST("v1/shops/{shop_id}/products/{product_id}/publish.json")
    suspend fun publishProductLive(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Path("product_id") productId: String,
        @Body publishRequest: PublishRequest = PublishRequest()
    ): PrintifyProductResponse

    @POST("v1/shops/{shop_id}/products/{product_id}/publishing_succeeded.json")
    suspend fun notifyPublishingSucceeded(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Path("product_id") productId: String
    ): Response<Unit>

    // ------------------------------------------------------------------------
    // IMAGE & ARTWORK UPLOADS
    // ------------------------------------------------------------------------
    @POST("v1/uploads/images.json")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Body uploadRequest: ImageUploadRequest
    ): ImageUploadResponse
}
