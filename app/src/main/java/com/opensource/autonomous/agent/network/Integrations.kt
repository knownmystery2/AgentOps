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
