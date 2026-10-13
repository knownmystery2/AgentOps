package com.opensource.autonomous.agent.network

import retrofit2.http.*

interface PrintifyApiService {
    @GET("v1/catalog/blueprints.json")
    suspend fun getBlueprints(@Header("Authorization") token: String): List<Blueprint>

    @GET("v1/catalog/blueprints/{blueprint_id}/variants.json")
    suspend fun getVariants(
        @Header("Authorization") token: String,
        @Path("blueprint_id") blueprintId: String
    ): VariantResponse

    @POST("v1/shops/{shop_id}/products.json")
    suspend fun publishProduct(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Body product: PrintifyProductRequest
    ): PrintifyProductResponse

    @POST("v1/shops/{shop_id}/products/{product_id}/publish.json")
    suspend fun publishProductLive(
        @Header("Authorization") token: String,
        @Path("shop_id") shopId: String,
        @Path("product_id") productId: String
    ): PrintifyProductResponse
}
