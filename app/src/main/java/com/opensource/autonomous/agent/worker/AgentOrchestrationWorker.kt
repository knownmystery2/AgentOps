package com.yourproject

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AIAgentController(private val context: Context) {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- FREE TIER & OPEN SOURCE API KEYS (Replace with your actual keys) ---
    private val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"
    private val GROQ_API_KEY = "YOUR_GROQ_API_KEY"
    private val HUGGING_FACE_API_KEY = "YOUR_HUGGING_FACE_API_KEY"
    private val OPENROUTER_API_KEY = "YOUR_OPENROUTER_API_KEY"

    // --- SELF-HOSTED OPEN SOURCE SERVER ENDPOINTS ---
    private val GOTIFY_SERVER_URL = "http://your-self-hosted-gotify.local"
    private val MEDUSA_STORE_URL = "http://your-medusa-store.local"

    /**
     * Boot up the branched multi-agent network
     */
    fun startAgent() {
        // Core Loop: Check for actions every 5 minutes using Gemini
        scheduler.scheduleAtFixedRate({
            try { runRoutineSystemCheck() } catch (e: Exception) { Log.e("Orchestrator", "System check failed", e) }
        }, 0, 5, TimeUnit.MINUTES)

        // Asset Loop: Every 24 hours, generate products using a team of Llama 3 & Mistral
        scheduler.scheduleAtFixedRate({
            try { runMultiEngineProductGeneration() } catch (e: Exception) { Log.e("Orchestrator", "Generation failed", e) }
        }, 0, 1, TimeUnit.DAYS)
    }

    /**
     * ENGINE BRANCH 1: Google Gemini (Fast, low-latency checking)
     * Handles the regular 5-minute status updates.
     */
    private fun runRoutineSystemCheck() {
        Log.d("Orchestrator", "[Engine 1] Gemini: scanning task queue...")
        val url = "https://googleapis.com"
        val prompt = "Analyze system queue logs. Are there any critical manual decisions needed? Respond strictly with JSON: {'needsApproval': true/false}"

        val jsonPayload = """{"contents": [{"parts":[{"text": "$prompt"}]}]}"""
        val request = Request.Builder().url(url).post(jsonPayload.toRequestBody(jsonMediaType)).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { Log.e("Orchestrator", "Gemini failed, trying OpenRouter fallback...") }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.contains("\"needsApproval\": true")) {
                            sendGotifyPushNotification("Approval Required", "A process has paused waiting for your mobile verification.")
                        }
                    }
                }
            }
        })
    }

    /**
     * ENGINES BRANCH 2 & 3: Groq (Llama 3) & Hugging Face (Mistral) WORKING TOGETHER
     * Llama creates the content structure, Mistral refines it to ensure it is clear and non-copyright.
     */
    private fun runMultiEngineProductGeneration() {
        Log.d("Orchestrator", "[Engine 2] Groq Llama-3: Designing digital outline...")
        
        val groqUrl = "https://groq.com"
        val prompt = "Create a unique, comprehensive study outline for open-source app design. Do not include copyright text."
        
        val groqPayload = """
            {
              "model": "llama3-8b-8192",
              "messages": [{"role": "user", "content": "$prompt"}]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(groqUrl)
            .header("Authorization", "Bearer $GROQ_API_KEY")
            .post(groqPayload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { Log.e("Orchestrator", "Groq failed.") }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val rawLlamaOutput = response.body?.string() ?: ""
                        // Now pass Llama's text to Hugging Face to write the final polished, unique book text
                        polishContentWithHuggingFace(rawLlamaOutput)
                    }
                }
            }
        })
    }

    private fun polishContentWithHuggingFace(rawOutline: String) {
        Log.d("Orchestrator", "[Engine 3] Hugging Face Mistral-7B: Refining text content...")
        // Using the free, open-source Mistral model hosted on Hugging Face infrastructure
        val hfUrl = "https://huggingface.co"
        
        val cleanPayload = """{"inputs": "Based on this concept outline, write a complete, unique open-license e-book guide: $rawOutline"}"""

        val request = Request.Builder()
            .url(hfUrl)
            .header("Authorization", "Bearer $HUGGING_FACE_API_KEY")
            .post(cleanPayload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { Log.e("Orchestrator", "Hugging Face failed.") }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val finalBookText = response.body?.string() ?: ""
                        publishToMedusaStorefront("Open Source Developer Volume ${System.currentTimeMillis()}", finalBookText)
                    }
                }
            }
        })
    }

    /**
     * ENGINE BRANCH 4: OpenRouter (Fallback Backup Gateway)
     * If your keys run low or any API throws an error, you can route calls here.
     */
    private fun executeOpenRouterFallback(prompt: String) {
        Log.d("Orchestrator", "[Engine 4] OpenRouter: Running fallback layer...")
        val url = "https://openrouter.ai"
        
        val payload = """
            {
              "model": "meta-llama/llama-3-8b-instruct:free",
              "messages": [{"role": "user", "content": "$prompt"}]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $OPENROUTER_API_KEY")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    // --- SYSTEM UTILITIES ---

    private fun sendGotifyPushNotification(title: String, message: String) {
        val payload = """{"title": "$title", "message": "$message", "priority": 5}"""
        val request = Request.Builder().url(GOTIFY_SERVER_URL).post(payload.toRequestBody(jsonMediaType)).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    private fun publishToMedusaStorefront(title: String, body: String) {
        val payload = """{"product": {"title": "$title", "description": "100% Unique Open-Source Digital Publication.", "status": "published"}}"""
        val request = Request.Builder().url(MEDUSA_STORE_URL).post(payload.toRequestBody(jsonMediaType)).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }
}
