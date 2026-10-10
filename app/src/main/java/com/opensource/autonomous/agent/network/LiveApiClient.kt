package com.opensource.autonomous.agent.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.opensource.autonomous.agent.data.DecisionEntity
import com.opensource.autonomous.agent.data.DropshipEntity
import com.opensource.autonomous.agent.data.LedgerEntity
import com.opensource.autonomous.agent.data.LogEntity
import com.opensource.autonomous.agent.data.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SwarmAgentStatus(
    val id: String,
    val name: String,
    val role: String,
    val status: String, // "ACTIVE", "RUNNING", "STANDBY", "PROCESSING", "IDLE"
    val model: String = "llama3",
    val currentTask: String = "Awaiting directive",
    val lastPingTimestamp: Long = System.currentTimeMillis(),
    val latencyMs: Long = 18
)

data class LiveConsoleStats(
    val status: String = "ONLINE",
    val serverTime: Long = System.currentTimeMillis(),
    val activeAgentsCount: Int = 11,
    val pendingDecisionsCount: Int = 0,
    val grossRevenueCents: Long = 0,
    val netProfitCents: Long = 0,
    val uptimeSeconds: Long = 0,
    val totalMemoryMb: Long = 0,
    val freeMemoryMb: Long = 0
)

data class PodJobItem(
    val id: String,
    val sku: String,
    val product: String,
    val color: String,
    val placement: String,
    val status: String, // QUEUED, RENDERING, COMPILED, READY
    val progressPercent: Int = 0,
    val vectorDpi: Int = 300,
    val timestamp: Long = System.currentTimeMillis()
)

class LiveApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Smart resolved baseUrl cache
    @Volatile
    private var resolvedBaseUrl: String? = null

    /**
     * Resolves the working base URL between 127.0.0.1 and 10.0.2.2 on Android.
     */
    suspend fun resolveActiveBaseUrl(configuredUrl: String): String = withContext(Dispatchers.IO) {
        val cached = resolvedBaseUrl
        if (cached != null && checkUrlHealth(cached)) {
            return@withContext cached
        }

        val cleanConfigured = configuredUrl.trimEnd('/')
        if (checkUrlHealth(cleanConfigured)) {
            resolvedBaseUrl = cleanConfigured
            return@withContext cleanConfigured
        }

        // Alternative host candidates:
        // When on phone Termux: 127.0.0.1
        // When on Android emulator: 10.0.2.2
        val candidate = when {
            cleanConfigured.contains("127.0.0.1") -> cleanConfigured.replace("127.0.0.1", "10.0.2.2")
            cleanConfigured.contains("localhost") -> cleanConfigured.replace("localhost", "10.0.2.2")
            cleanConfigured.contains("10.0.2.2") -> cleanConfigured.replace("10.0.2.2", "127.0.0.1")
            else -> null
        }

        if (candidate != null && checkUrlHealth(candidate)) {
            resolvedBaseUrl = candidate
            return@withContext candidate
        }

        cleanConfigured
    }

    private fun checkUrlHealth(baseUrl: String): Boolean {
        return try {
            val url = "$baseUrl/api/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkServerHealth(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            checkUrlHealth(resolved)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Executes real healthcheck ping test and returns (isSuccess, latencyMs).
     */
    suspend fun testServerPing(baseUrl: String): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
                Pair(response.isSuccessful, latency)
            }
        } catch (_: Exception) {
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
            Pair(false, latency)
        }
    }

    suspend fun fetchLiveSwarm(baseUrl: String): Result<List<SwarmAgentStatus>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/swarm"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code} from /api/swarm"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<SwarmAgentStatus>>() {}.type
                    val list: List<SwarmAgentStatus> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLiveDecisions(baseUrl: String): Result<List<DecisionEntity>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/decisions"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<DecisionEntity>>() {}.type
                    val list: List<DecisionEntity> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postDecisionAction(baseUrl: String, decisionId: String, action: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/decisions/$decisionId/action"
            val payload = JsonObject().apply {
                addProperty("decisionId", decisionId)
                addProperty("action", action)
                addProperty("timestamp", System.currentTimeMillis())
            }
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code} processing $action for decision $decisionId"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLiveTasks(baseUrl: String): Result<List<TaskEntity>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/tasks"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<TaskEntity>>() {}.type
                    val list: List<TaskEntity> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLiveLogs(baseUrl: String): Result<List<LogEntity>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/logs"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<LogEntity>>() {}.type
                    val list: List<LogEntity> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLiveLedger(baseUrl: String): Result<List<LedgerEntity>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/ledger"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<LedgerEntity>>() {}.type
                    val list: List<LedgerEntity> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncDropshipSuppliers(baseUrl: String): Result<List<DropshipEntity>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/dropship/suppliers"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<DropshipEntity>>() {}.type
                    val list: List<DropshipEntity> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postDropshipOrder(baseUrl: String, dropshipId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/dropship/order"
            val payload = JsonObject().apply {
                addProperty("dropshipId", dropshipId)
                addProperty("timestamp", System.currentTimeMillis())
            }
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPodQueue(baseUrl: String): Result<List<PodJobItem>> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/pod/queue"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}"))
                } else {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<PodJobItem>>() {}.type
                    val list: List<PodJobItem> = gson.fromJson(body, type)
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postPodJob(baseUrl: String, sku: String, product: String, color: String, placement: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/pod/generate"
            val payload = JsonObject().apply {
                addProperty("sku", sku)
                addProperty("product", product)
                addProperty("color", color)
                addProperty("placement", placement)
                addProperty("timestamp", System.currentTimeMillis())
            }
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postDaemonState(baseUrl: String, state: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/daemon/state"
            val payload = JsonObject().apply {
                addProperty("state", state)
                addProperty("timestamp", System.currentTimeMillis())
            }
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerCronDispatch(baseUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveActiveBaseUrl(baseUrl)
            val url = "$resolved/api/cron/trigger"
            val payload = JsonObject().apply {
                addProperty("action", "CRON_DISPATCH")
                addProperty("timestamp", System.currentTimeMillis())
            }
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
