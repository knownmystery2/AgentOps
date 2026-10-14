package com.opensource.autonomous.agent.network

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class LicenseKeyMapping(
    val licenseKey: String,
    val productId: String,
    val sku: String,
    val customerEmail: String,
    val downloadUrl: String,
    val signedToken: String,
    val issuedAt: Long = System.currentTimeMillis()
)

/**
 * GitHub connection wrapper that authenticates via GitHub Apps / OAuth / Personal Access Tokens,
 * providing secure remote asset fetching and cryptographic license key mapping when digital products are purchased.
 */
class GitHubConnectionWrapper(
    private val tokenProvider: () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()

    /**
     * Authenticates and verifies the current GitHub App / OAuth credential.
     */
    suspend fun verifyConnection(): Result<String> = withContext(Dispatchers.IO) {
        val token = tokenProvider().trim()
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No GitHub OAuth or App token provided."))
        }

        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val login = json.get("login")?.asString ?: "connected_user"
                    Result.success(login)
                } else {
                    Result.failure(IOException("GitHub OAuth Verification Error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Securely fetches digital product assets (raw files, templates, vectors) from repository.
     */
    suspend fun fetchAsset(
        owner: String,
        repo: String,
        path: String,
        ref: String = "main"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val token = tokenProvider().trim()
        val builder = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$ref")
            .header("Accept", "application/vnd.github.raw+json")
            .header("X-GitHub-Api-Version", "2022-11-28")

        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        try {
            client.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes() ?: ByteArray(0)
                    Result.success(bytes)
                } else {
                    val errorStr = response.body?.string().orEmpty()
                    Result.failure(IOException("Failed to fetch asset from $owner/$repo/$path (${response.code}): $errorStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates and maps a unique license key for a customer purchase.
     */
    fun generateLicenseMapping(
        productId: String,
        sku: String,
        customerEmail: String,
        assetDownloadUrl: String
    ): LicenseKeyMapping {
        val now = System.currentTimeMillis()
        val raw = "$productId:$sku:$customerEmail:$now"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02X".format(it) }

        val licenseKey = "LIC-${hash.take(4)}-${hash.substring(4, 8)}-${hash.substring(8, 12)}"
        val signedToken = Base64.encodeToString("$licenseKey:$customerEmail:$now".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        return LicenseKeyMapping(
            licenseKey = licenseKey,
            productId = productId,
            sku = sku,
            customerEmail = customerEmail,
            downloadUrl = assetDownloadUrl,
            signedToken = signedToken,
            issuedAt = now
        )
    }
}
