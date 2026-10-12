package com.opensource.autonomous.agent.network

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * GitHubService manages repository deployments, code writing, and commits.
 */
class GitHubService(private val personalAccessToken: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Creates a file or updates content inside a GitHub repository autonomously.
     */
    fun commitFile(owner: String, repo: String, path: String, message: String, content: String): Result<String> {
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path"

        // GitHub expects base64 encoded strings for contents
        val encodedContent = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)

        val jsonBody = JsonObject().apply {
            addProperty("message", message)
            addProperty("content", encodedContent)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $personalAccessToken")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .put(gson.toJson(jsonBody).toRequestBody(mediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("File successfully committed to GitHub repository pathway.")
                } else {
                    Result.failure(IOException("GitHub Error: ${response.code} - ${response.body?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * StripeService manages checkout setups, customers, and invoice billing loops.
 */
class StripeService(private val apiKey: String) {
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Creates a formal payment link for subscription setups or product sales.
     */
    fun createPaymentLink(priceId: String, quantity: Int = 1): Result<String> {
        val url = "https://api.stripe.com/v1/payment_links"

        // Stripe uses standard x-www-form-urlencoded format for raw platform actions
        val formBody = FormBody.Builder()
            .add("line_items[0][price]", priceId)
            .add("line_items[0][quantity]", quantity.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonResponse = gson.fromJson(bodyString, JsonObject::class.java)
                    val generatedUrl = jsonResponse.get("url")?.asString ?: ""
                    Result.success(generatedUrl)
                } else {
                    Result.failure(IOException("Stripe Gateway Error: ${response.code} - $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data structures for Printify API integration
 */
data class PrintifyShop(
    val id: Long,
    val title: String,
    val salesChannel: String? = null
)

data class PrintifyProductItem(
    val id: String,
    val title: String,
    val description: String,
    val blueprintId: Int = 0,
    val printProviderId: Int = 0,
    val visible: Boolean = true
)

data class PrintifyConnectionStatus(
    val isConnected: Boolean,
    val message: String,
    val shopCount: Int = 0,
    val primaryShopTitle: String = "",
    val domain: String = "kriticallimitz.printify.me"
)

/**
 * PrintifyService manages print-on-demand catalog integration, product generation,
 * and storefront fulfillment for kriticallimitz.printify.me.
 */
class PrintifyService(
    private val apiToken: String,
    val shopDomain: String = "kriticallimitz.printify.me",
    val shopId: String = "26650832"
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Tests live authentication with the Printify REST API using the provided token.
     */
    fun testConnection(): Result<PrintifyConnectionStatus> {
        val trimmedToken = apiToken.trim()
        if (trimmedToken.isBlank()) {
            return Result.failure(IllegalStateException("Printify API Token is empty. Please configure token in Settings."))
        }

        val request = Request.Builder()
            .url("https://api.printify.com/v1/shops.json")
            .header("Authorization", "Bearer $trimmedToken")
            .header("User-Agent", "AutonomousAgentStudio/2.0")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonArray = gson.fromJson(body, com.google.gson.JsonArray::class.java)
                    val count = jsonArray.size()
                    val primaryTitle = if (count > 0) {
                        jsonArray.get(0).asJsonObject.get("title")?.asString ?: "kriticallimitz"
                    } else {
                        "Shop #$shopId"
                    }
                    Result.success(
                        PrintifyConnectionStatus(
                            isConnected = true,
                            message = "Live Printify Connection Verified! Connected to $primaryTitle ($shopDomain).",
                            shopCount = count,
                            primaryShopTitle = primaryTitle,
                            domain = shopDomain
                        )
                    )
                } else {
                    Result.failure(IOException("Printify API Rejected (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves all connected shops on the user's Printify account.
     */
    fun getShops(): Result<List<PrintifyShop>> {
        val trimmedToken = apiToken.trim()
        if (trimmedToken.isBlank()) return Result.failure(IllegalStateException("No Printify API token"))

        val request = Request.Builder()
            .url("https://api.printify.com/v1/shops.json")
            .header("Authorization", "Bearer $trimmedToken")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonArray = gson.fromJson(body, com.google.gson.JsonArray::class.java)
                    val list = jsonArray.map { elem ->
                        val obj = elem.asJsonObject
                        PrintifyShop(
                            id = obj.get("id")?.asLong ?: 0L,
                            title = obj.get("title")?.asString ?: "Printify Shop",
                            salesChannel = obj.get("sales_channel")?.asString
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(IOException("Printify Shops Error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves products published under a target shop on Printify.
     */
    fun getProducts(targetShopId: String = shopId): Result<List<PrintifyProductItem>> {
        val trimmedToken = apiToken.trim()
        if (trimmedToken.isBlank()) return Result.failure(IllegalStateException("No Printify API token"))

        val request = Request.Builder()
            .url("https://api.printify.com/v1/shops/$targetShopId/products.json")
            .header("Authorization", "Bearer $trimmedToken")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonRoot = gson.fromJson(body, JsonObject::class.java)
                    val dataArray = jsonRoot.getAsJsonArray("data") ?: com.google.gson.JsonArray()
                    val list = dataArray.map { elem ->
                        val obj = elem.asJsonObject
                        PrintifyProductItem(
                            id = obj.get("id")?.asString ?: "",
                            title = obj.get("title")?.asString ?: "Product",
                            description = obj.get("description")?.asString ?: "",
                            blueprintId = obj.get("blueprint_id")?.asInt ?: 0,
                            printProviderId = obj.get("print_provider_id")?.asInt ?: 0,
                            visible = obj.get("visible")?.asBoolean ?: true
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(IOException("Printify Products Error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

