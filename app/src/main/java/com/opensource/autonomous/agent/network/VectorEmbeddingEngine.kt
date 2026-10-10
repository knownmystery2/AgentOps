package com.opensource.autonomous.agent.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Leverages the unmetered, open-source local Ollama infrastructure configuration
 * layer to generate math vector embeddings dynamically on-device, bypassing closed-source external tracking.
 */
class VectorEmbeddingEngine(private val baseUrl: String = "http://10.0.2.2:11434") {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Converts a text string directly into an open-source mathematical FloatArray model space mapping.
     */
    fun computeEmbedding(textInput: String): Result<FloatArray> {
        val endpoint = "$baseUrl/api/embeddings"
        val jsonBody = JsonObject().apply {
            addProperty("model", "all-minilm") // Pulled locally via: ollama pull all-minilm
            addProperty("prompt", textInput)
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(gson.toJson(jsonBody).toRequestBody(mediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(IOException("Ollama Embedding Fault: Code ${response.code}"))
                } else {
                    val rawBody = response.body?.string() ?: ""
                    val rootObj = gson.fromJson(rawBody, JsonObject::class.java)
                    val embeddingArray = rootObj.getAsJsonArray("embedding")
                    val floatArray = FloatArray(embeddingArray.size())
                    for (i in 0 until embeddingArray.size()) {
                        floatArray[i] = embeddingArray.get(i).asFloat
                    }
                    Result.success(floatArray)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
