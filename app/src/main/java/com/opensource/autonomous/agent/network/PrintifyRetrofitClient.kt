package com.opensource.autonomous.agent.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * High-performance Retrofit client service for interacting with the Printify REST API v1.
 * Provides clean domain methods with robust error handling and coroutines dispatching.
 */
class PrintifyRetrofitClient(
    private val apiToken: String,
    val shopDomain: String = "kriticallimitz.printify.me",
    val defaultShopId: String = "26650832"
) {
    private val authHeader: String
        get() = if (apiToken.startsWith("Bearer ")) apiToken else "Bearer ${apiToken.trim()}"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AutonomousPrintifyStudio/2.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    val apiService: PrintifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.printify.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrintifyApiService::class.java)
    }

    /**
     * Verifies live connectivity with Printify shops endpoint.
     */
    suspend fun testConnection(): Result<PrintifyConnectionStatus> = withContext(Dispatchers.IO) {
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Printify API Token is empty. Please configure in Settings."))
        }
        try {
            val shops = apiService.getShops(authHeader)
            val primary = shops.firstOrNull()
            Result.success(
                PrintifyConnectionStatus(
                    isConnected = true,
                    message = "Live Printify Connection Verified! Connected to ${primary?.title ?: "kriticallimitz"} ($shopDomain).",
                    shopCount = shops.size,
                    primaryShopTitle = primary?.title ?: "kriticallimitz",
                    domain = shopDomain
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves all catalog blueprints from Printify.
     */
    suspend fun getBlueprints(): Result<List<Blueprint>> = withContext(Dispatchers.IO) {
        try {
            val list = apiService.getBlueprints(authHeader)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves detailed blueprint specification including dimensions and images.
     */
    suspend fun getBlueprintDetails(blueprintId: String): Result<Blueprint> = withContext(Dispatchers.IO) {
        try {
            val bp = apiService.getBlueprintDetails(authHeader, blueprintId)
            Result.success(bp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists print providers fulfilling a given blueprint.
     */
    suspend fun getPrintProviders(blueprintId: String): Result<List<PrintProvider>> = withContext(Dispatchers.IO) {
        try {
            val providers = apiService.getPrintProviders(authHeader, blueprintId)
            Result.success(providers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves available variant specifications (sizes, colors, wholesale prices).
     */
    suspend fun getVariants(blueprintId: String, providerId: Int = 1): Result<List<Variant>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getVariants(authHeader, blueprintId, providerId)
            Result.success(resp.variants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches products currently created/published in the target shop.
     */
    suspend fun getProducts(shopId: String = defaultShopId, page: Int = 1): Result<List<PrintifyProduct>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProducts(authHeader, shopId, page)
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates and stages a product in the Printify shop.
     */
    suspend fun createProduct(
        productRequest: PrintifyProductRequest,
        shopId: String = defaultShopId
    ): Result<PrintifyProductResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.publishProduct(authHeader, shopId, productRequest)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Publishes a product live to the storefront sales channels.
     */
    suspend fun publishProductLive(
        productId: String,
        shopId: String = defaultShopId
    ): Result<PrintifyProductResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.publishProductLive(authHeader, shopId, productId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads an artwork image asset (URL or Base64 encoded) to Printify media library.
     */
    suspend fun uploadArtwork(
        fileName: String,
        urlOrBase64: String,
        isUrl: Boolean = true
    ): Result<ImageUploadResponse> = withContext(Dispatchers.IO) {
        try {
            val req = if (isUrl) {
                ImageUploadRequest(file_name = fileName, url = urlOrBase64)
            } else {
                ImageUploadRequest(file_name = fileName, contents = urlOrBase64)
            }
            val response = apiService.uploadImage(authHeader, req)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun create(token: String, domain: String = "kriticallimitz.printify.me", shopId: String = "26650832"): PrintifyRetrofitClient {
            return PrintifyRetrofitClient(token, domain, shopId)
        }
    }
}
