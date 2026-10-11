package com.opensource.autonomous.agent.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.opensource.autonomous.agent.data.SecretManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class StripeProductResult(
    val productId: String,
    val name: String,
    val active: Boolean = true
)

data class PaymentLinkResult(
    val paymentLinkId: String,
    val paymentUrl: String,
    val priceId: String,
    val isLivePayment: Boolean,
    val unitAmountCents: Long
)

/**
 * Service interface for automated payment gateway operations, price generation,
 * digital product checkout links, and webhook integrity verification.
 */
interface PaymentProcessingService {
    suspend fun createProduct(
        name: String,
        description: String
    ): Result<StripeProductResult>

    suspend fun createPrice(
        productId: String,
        unitAmountCents: Long,
        currency: String = "usd"
    ): Result<String>

    suspend fun createPaymentLink(
        priceId: String,
        quantity: Int = 1
    ): Result<String>

    suspend fun createDigitalProductCheckout(
        productTitle: String,
        description: String,
        priceInCents: Long,
        successRedirectUrl: String = ""
    ): Result<PaymentLinkResult>

    suspend fun verifyWebhookSignature(
        payload: String,
        signatureHeader: String,
        endpointSecret: String
    ): Boolean
}

/**
 * Production-ready implementation of PaymentProcessingService communicating directly
 * with the open Stripe API standard.
 */
class StripePaymentService(
    private val secretManager: SecretManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : PaymentProcessingService {

    private val gson = Gson()

    override suspend fun createProduct(name: String, description: String): Result<StripeProductResult> = withContext(Dispatchers.IO) {
        val apiKey = secretManager.getStripeSecretKey()
        if (apiKey.isBlank()) {
            val fallbackId = "prod_sandbox_" + System.currentTimeMillis() % 1000000
            return@withContext Result.success(StripeProductResult(productId = fallbackId, name = name))
        }

        val formBody = FormBody.Builder()
            .add("name", name)
            .add("description", description)
            .build()

        val request = Request.Builder()
            .url("https://api.stripe.com/v1/products")
            .header("Authorization", "Bearer $apiKey")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val id = json.get("id")?.asString ?: "prod_unknown"
                    Result.success(StripeProductResult(productId = id, name = name))
                } else {
                    Result.failure(IOException("Stripe createProduct error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPrice(
        productId: String,
        unitAmountCents: Long,
        currency: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = secretManager.getStripeSecretKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success("price_sandbox_${unitAmountCents}_${System.currentTimeMillis() % 10000}")
        }

        val formBody = FormBody.Builder()
            .add("product", productId)
            .add("unit_amount", unitAmountCents.toString())
            .add("currency", currency.lowercase())
            .build()

        val request = Request.Builder()
            .url("https://api.stripe.com/v1/prices")
            .header("Authorization", "Bearer $apiKey")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val priceId = json.get("id")?.asString ?: "price_unknown"
                    Result.success(priceId)
                } else {
                    Result.failure(IOException("Stripe createPrice error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPaymentLink(priceId: String, quantity: Int): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = secretManager.getStripeSecretKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success("https://buy.stripe.com/test_zero_cost_${priceId.take(12)}")
        }

        val formBody = FormBody.Builder()
            .add("line_items[0][price]", priceId)
            .add("line_items[0][quantity]", quantity.toString())
            .build()

        val request = Request.Builder()
            .url("https://api.stripe.com/v1/payment_links")
            .header("Authorization", "Bearer $apiKey")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val url = json.get("url")?.asString ?: ""
                    Result.success(url)
                } else {
                    Result.failure(IOException("Stripe createPaymentLink error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDigitalProductCheckout(
        productTitle: String,
        description: String,
        priceInCents: Long,
        successRedirectUrl: String
    ): Result<PaymentLinkResult> = withContext(Dispatchers.IO) {
        val apiKey = secretManager.getStripeSecretKey()
        val isLive = apiKey.isNotBlank()

        if (!isLive) {
            // Generate clean simulated sandbox payment link
            val slug = productTitle.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(16)
            val fallbackUrl = "https://buy.stripe.com/test_$slug"
            val fallbackPriceId = "price_sandbox_${priceInCents}"
            return@withContext Result.success(
                PaymentLinkResult(
                    paymentLinkId = "plink_sandbox_${System.currentTimeMillis() % 100000}",
                    paymentUrl = fallbackUrl,
                    priceId = fallbackPriceId,
                    isLivePayment = false,
                    unitAmountCents = priceInCents
                )
            )
        }

        // Live flow: Create Product -> Create Price -> Create Payment Link
        val productResult = createProduct(productTitle, description)
        if (productResult.isFailure) {
            return@withContext Result.failure(productResult.exceptionOrNull()!!)
        }
        val productId = productResult.getOrNull()!!.productId

        val priceResult = createPrice(productId, priceInCents)
        if (priceResult.isFailure) {
            return@withContext Result.failure(priceResult.exceptionOrNull()!!)
        }
        val priceId = priceResult.getOrNull()!!

        val linkResult = createPaymentLink(priceId, 1)
        if (linkResult.isFailure) {
            return@withContext Result.failure(linkResult.exceptionOrNull()!!)
        }
        val paymentUrl = linkResult.getOrNull()!!

        Result.success(
            PaymentLinkResult(
                paymentLinkId = "plink_" + System.currentTimeMillis(),
                paymentUrl = paymentUrl,
                priceId = priceId,
                isLivePayment = true,
                unitAmountCents = priceInCents
            )
        )
    }

    override suspend fun verifyWebhookSignature(
        payload: String,
        signatureHeader: String,
        endpointSecret: String
    ): Boolean = withContext(Dispatchers.Default) {
        if (endpointSecret.isBlank() || signatureHeader.isBlank()) return@withContext false

        try {
            // Stripe signature format: t=timestamp,v1=signature
            var timestamp = ""
            var signature = ""
            for (part in signatureHeader.split(",")) {
                val pair = part.split("=", limit = 2)
                if (pair.size == 2) {
                    when (pair[0].trim()) {
                        "t" -> timestamp = pair[1].trim()
                        "v1" -> signature = pair[1].trim()
                    }
                }
            }

            if (timestamp.isBlank() || signature.isBlank()) return@withContext false

            val signedPayload = "$timestamp.$payload"
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(endpointSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val expectedHash = mac.doFinal(signedPayload.toByteArray(Charsets.UTF_8))
            val computedHex = expectedHash.joinToString("") { "%02x".format(it) }

            computedHex.equals(signature, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
