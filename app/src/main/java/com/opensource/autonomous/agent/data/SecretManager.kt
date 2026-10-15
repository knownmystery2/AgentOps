package com.opensource.autonomous.agent.data

import com.example.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * SecretStatus represents the configuration state and safe masked preview of active API credentials.
 */
data class SecretStatus(
    val isGitHubConfigured: Boolean = false,
    val isStripeConfigured: Boolean = false,
    val isStripePublishableConfigured: Boolean = false,
    val isGeminiConfigured: Boolean = false,
    val isPrintifyConfigured: Boolean = true,
    val githubTokenMasked: String = "Not Configured",
    val stripeSecretKeyMasked: String = "Not Configured",
    val stripePublishableKeyMasked: String = "Not Configured",
    val geminiApiKeyMasked: String = "Not Configured",
    val printifyTokenMasked: String = "Not Configured",
    val printifyShopDomain: String = "kriticallimitz.printify.me",
    val printifyShopId: String = "26650832",
    val githubSource: String = "None", // "AI Studio Secrets / BuildConfig" or "Device Local Settings"
    val stripeSource: String = "None",
    val geminiSource: String = "None",
    val printifySource: String = "None"
)

/**
 * Result of implementing Stripe and GitHub keys into the store secrets.
 */
data class StoreSecretsImplementationResult(
    val isGitHubConfigured: Boolean,
    val isStripeSecretConfigured: Boolean,
    val isStripePublishableConfigured: Boolean,
    val githubMasked: String,
    val stripeSecretMasked: String,
    val stripePublishableMasked: String,
    val message: String
)

/**
 * SecretManager is a secure repository module responsible for managing and retrieving
 * credentials required for automated deployments (GitHub PAT), payment processing (Stripe),
 * and generative AI reasoning (Gemini / open source endpoints).
 */
class SecretManager(
    private val settingsDao: SettingsDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _secretStatus = MutableStateFlow(SecretStatus())
    val secretStatus: StateFlow<SecretStatus> = _secretStatus.asStateFlow()

    // In-memory cache for ultra-fast, non-blocking synchronous queries
    @Volatile
    private var cachedGithubToken: String = ""

    @Volatile
    private var cachedStripeSecretKey: String = ""

    @Volatile
    private var cachedStripePublishableKey: String = ""

    @Volatile
    private var cachedGeminiApiKey: String = ""

    @Volatile
    private var cachedPrintifyToken: String = ""

    @Volatile
    private var cachedPrintifyShopDomain: String = "kriticallimitz.printify.me"

    @Volatile
    private var cachedPrintifyShopId: String = "26650832"

    init {
        // Observe settings changes reactively from Room and sync BuildConfig secrets into Room
        scope.launch {
            syncBuildConfigToRoom()
            settingsDao.getSettings().collect { settings ->
                refreshSecrets(settings)
            }
        }
    }

    private fun refreshSecrets(settings: SettingsEntity?) {
        // Resolve GitHub Token
        val localGithub = settings?.keyGithub?.trim().orEmpty()
        val buildConfigGithub = try {
            BuildConfig.GITHUB_TOKEN.trim()
        } catch (_: Throwable) {
            ""
        }

        val effectiveGithub: String
        val githubSource: String
        when {
            localGithub.isNotBlank() -> {
                effectiveGithub = localGithub
                githubSource = "Device Local Settings"
            }
            buildConfigGithub.isNotBlank() && !buildConfigGithub.startsWith("placeholder_") -> {
                effectiveGithub = buildConfigGithub
                githubSource = "AI Studio Secrets / BuildConfig"
            }
            else -> {
                effectiveGithub = ""
                githubSource = "Unconfigured"
            }
        }

        // Resolve Stripe Secret Key
        val localStripe = settings?.keyStripe?.trim().orEmpty()
        val buildConfigStripe = try {
            BuildConfig.STRIPE_SECRET_KEY.trim()
        } catch (_: Throwable) {
            ""
        }

        val effectiveStripe: String
        val stripeSource: String
        when {
            localStripe.isNotBlank() -> {
                effectiveStripe = localStripe
                stripeSource = "Device Local Settings"
            }
            buildConfigStripe.isNotBlank() && !buildConfigStripe.startsWith("placeholder_") -> {
                effectiveStripe = buildConfigStripe
                stripeSource = "AI Studio Secrets / BuildConfig"
            }
            else -> {
                effectiveStripe = ""
                stripeSource = "Unconfigured"
            }
        }

        // Resolve Stripe Publishable Key
        val buildConfigStripePub = try {
            BuildConfig.STRIPE_PUBLISHABLE_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
        val effectiveStripePub = if (buildConfigStripePub.isNotBlank() && !buildConfigStripePub.startsWith("placeholder_")) {
            buildConfigStripePub
        } else {
            ""
        }

        // Resolve Gemini API Key
        val localGemini = settings?.keyGemini?.trim().orEmpty()
        val buildConfigGemini = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }

        val effectiveGemini: String
        val geminiSource: String
        when {
            localGemini.isNotBlank() -> {
                effectiveGemini = localGemini
                geminiSource = "Device Local Settings"
            }
            buildConfigGemini.isNotBlank() && !buildConfigGemini.startsWith("placeholder_") -> {
                effectiveGemini = buildConfigGemini
                geminiSource = "AI Studio Secrets / BuildConfig"
            }
            else -> {
                effectiveGemini = ""
                geminiSource = "Unconfigured"
            }
        }

        // Resolve Printify Token & Shop Domain
        val defaultPrintify = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIzN2Q0YmQzMDM1ZmUxMWU5YTgwM2FiN2VlYjNjY2M5NyIsImp0aSI6IjA4YmI2ZDE0YTI0ZjE5YjA4MGY4ZGJjMWY3NzgwNGNkZDQ4NGU5NjA2MDU5MGUxZmY2M2MyNjdlYTVmMWNkMmRlNTA5YWFiODJmMDU2ZWRiIiwiaWF0IjoxNzkxODE3NTI3LjkyMzUzMiwibmJmIjoxNzkxODE3NTI3LjkyMzUzNSwiZXhwIjoxODIzMzUzNTI3LjkxNDUyNiwic3ViIjoiMjY2NTA4MzIiLCJzY29wZXMiOlsic2hvcHMubWFuYWdlIiwic2hvcHMucmVhZCIsImNhdGFsb2cucmVhZCIsIm9yZGVycy5yZWFkIiwib3JkZXJzLndyaXRlIiwicHJvZHVjdHMucmVhZCIsInByb2R1Y3RzLndyaXRlIiwid2ViaG9va3MucmVhZCIsIndlYmhvb2tzLndyaXRlIiwidXBsb2Fkcy5yZWFkIiwidXBsb2Fkcy53cml0ZSIsInByaW50X3Byb3ZpZGVycy5yZWFkIiwidXNlci5pbmZvIl19.VM-97U2qhg5acAzIaGFER_vgWRvq5uQ0KEo83Id1QXrMqeJL74-q6fgD9wXFblhFpZPFGV3N0hups8cwUZ936Fvbx1mK0t-f7kWyor_KDoQsXGzHZ1AVm51d1KjE0iDnjY1m54ed4ZKQSfQBJglNBgBDsOmMywqua4NgEZa_BPTx5mS1AnZmjyMO8cN9vFehLwq430MNaSVPMzJ4Eod1wZ2yac167Buu-EI2q_-k0DtKV1rlEINO6bs9DT5AVEks7kdLVxP7g-DZf_ufzAnq7xcPh_ysuoX382WwOLChlVoBqiA_wmJGlNmHYblhtHF5fDF-XI-vmlqE4dN8FWmO7KvvbivsiTLjnJ9OnREhElAuE79uyJbc6qRTC_x6DeaytaT2fN9i3GlJueC9tj7ZAFAiWKteV6s0Rb8Nt5hGCclmU7LWgApkkrU-oEnpnP9Vx_monSyQovgtPDY3K2-5qTKoLtDBGlFNDGHRQ0rqCz_r5cisjfHkq_LKXbfziUNtFqJVn07GmunBUvlTwcgwvTuREnc2qZD2hlneETH35xTDT6cZHqcU2qs_h35xK1RbUR0sWV0ZoJYfyG-8fCFhB8sQnThoBpkPDnQ7jsd2sZ_NFyp3MNxetQ9rW_kmWlQv9141rUGuQus49fE1jBEvdwVNDqa3DkmadLMyjQkeits"
        val localPrintify = settings?.keyPrintify?.trim().orEmpty()
        val effectivePrintify = if (localPrintify.isNotBlank()) localPrintify else defaultPrintify
        val effectivePrintifyDomain = settings?.printifyShopDomain?.trim()?.ifBlank { "kriticallimitz.printify.me" } ?: "kriticallimitz.printify.me"
        val effectivePrintifyShopId = settings?.printifyShopId?.trim()?.ifBlank { "26650832" } ?: "26650832"

        cachedGithubToken = effectiveGithub
        cachedStripeSecretKey = effectiveStripe
        cachedStripePublishableKey = effectiveStripePub
        cachedGeminiApiKey = effectiveGemini
        cachedPrintifyToken = effectivePrintify
        cachedPrintifyShopDomain = effectivePrintifyDomain
        cachedPrintifyShopId = effectivePrintifyShopId

        _secretStatus.value = SecretStatus(
            isGitHubConfigured = effectiveGithub.isNotBlank(),
            isStripeConfigured = effectiveStripe.isNotBlank(),
            isStripePublishableConfigured = effectiveStripePub.isNotBlank(),
            isGeminiConfigured = effectiveGemini.isNotBlank(),
            isPrintifyConfigured = effectivePrintify.isNotBlank(),
            githubTokenMasked = maskSecret(effectiveGithub, prefixLength = 4, suffixLength = 4),
            stripeSecretKeyMasked = maskSecret(effectiveStripe, prefixLength = 7, suffixLength = 4),
            stripePublishableKeyMasked = maskSecret(effectiveStripePub, prefixLength = 7, suffixLength = 4),
            geminiApiKeyMasked = maskSecret(effectiveGemini, prefixLength = 4, suffixLength = 4),
            printifyTokenMasked = maskSecret(effectivePrintify, prefixLength = 8, suffixLength = 6),
            printifyShopDomain = effectivePrintifyDomain,
            printifyShopId = effectivePrintifyShopId,
            githubSource = githubSource,
            stripeSource = stripeSource,
            geminiSource = geminiSource,
            printifySource = if (localPrintify.isNotBlank()) "Device Local Settings" else "Verified Active JWT"
        )
    }

    /**
     * Retrieves active GitHub Personal Access Token.
     */
    fun getGitHubToken(): String = cachedGithubToken

    /**
     * Checks if a valid non-placeholder GitHub token is present.
     */
    fun hasValidGitHubToken(): Boolean = cachedGithubToken.isNotBlank()

    /**
     * Retrieves active Stripe Secret API Key.
     */
    fun getStripeSecretKey(): String = cachedStripeSecretKey

    /**
     * Checks if a valid non-placeholder Stripe secret key is present.
     */
    fun hasValidStripeSecretKey(): Boolean = cachedStripeSecretKey.isNotBlank()

    /**
     * Retrieves active Stripe Publishable Key (for client checkout embeds).
     */
    fun getStripePublishableKey(): String = cachedStripePublishableKey

    /**
     * Retrieves active Gemini API Key.
     */
    fun getGeminiApiKey(): String = cachedGeminiApiKey

    /**
     * Checks if a valid non-placeholder Gemini API Key is present.
     */
    fun hasValidGeminiApiKey(): Boolean = cachedGeminiApiKey.isNotBlank()

    /**
     * Saves or overrides the GitHub Personal Access Token on-device in Room.
     */
    suspend fun saveGitHubToken(token: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(keyGithub = token.trim())
        settingsDao.saveSettings(updated)
        cachedGithubToken = token.trim()
        refreshSecrets(updated)
    }

    /**
     * Saves or overrides the Stripe Secret Key on-device in Room.
     */
    suspend fun saveStripeSecretKey(secretKey: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(keyStripe = secretKey.trim())
        settingsDao.saveSettings(updated)
        cachedStripeSecretKey = secretKey.trim()
        refreshSecrets(updated)
    }

    /**
     * Saves or overrides the Gemini API Key on-device in Room.
     */
    suspend fun saveGeminiApiKey(apiKey: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(keyGemini = apiKey.trim())
        settingsDao.saveSettings(updated)
        cachedGeminiApiKey = apiKey.trim()
        refreshSecrets(updated)
    }

    /**
     * Retrieves active Printify API Token (JWT).
     */
    fun getPrintifyToken(): String = cachedPrintifyToken

    /**
     * Retrieves configured Printify Shop Domain (e.g. kriticallimitz.printify.me).
     */
    fun getPrintifyShopDomain(): String = cachedPrintifyShopDomain

    /**
     * Retrieves configured Printify Shop ID.
     */
    fun getPrintifyShopId(): String = cachedPrintifyShopId

    /**
     * Checks if a valid non-empty Printify token is available.
     */
    fun hasValidPrintifyToken(): Boolean = cachedPrintifyToken.isNotBlank()

    /**
     * Saves or overrides the Printify Token on-device in Room.
     */
    suspend fun savePrintifyToken(token: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(keyPrintify = token.trim())
        settingsDao.saveSettings(updated)
        cachedPrintifyToken = token.trim()
        refreshSecrets(updated)
    }

    /**
     * Saves Printify shop domain and ID on-device in Room.
     */
    suspend fun savePrintifyShop(domain: String, shopId: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(
            printifyShopDomain = domain.trim(),
            printifyShopId = shopId.trim()
        )
        settingsDao.saveSettings(updated)
        cachedPrintifyShopDomain = domain.trim()
        cachedPrintifyShopId = shopId.trim()
        refreshSecrets(updated)
    }

    /**
     * Synchronizes non-placeholder credentials from BuildConfig (.env) into Room database settings.
     * Ensures store secrets are stored in the underlying database for autonomous operations.
     */
    suspend fun syncBuildConfigToRoom() = withContext(Dispatchers.IO) {
        val current = settingsDao.getSettingsSync() ?: SettingsEntity()
        var updated = current

        val buildConfigGithub = try { BuildConfig.GITHUB_TOKEN.trim() } catch (_: Throwable) { "" }
        val buildConfigStripe = try { BuildConfig.STRIPE_SECRET_KEY.trim() } catch (_: Throwable) { "" }
        val buildConfigStripePub = try { BuildConfig.STRIPE_PUBLISHABLE_KEY.trim() } catch (_: Throwable) { "" }
        val buildConfigGemini = try { BuildConfig.GEMINI_API_KEY.trim() } catch (_: Throwable) { "" }

        var changed = false
        if (updated.keyGithub.isBlank() && buildConfigGithub.isNotBlank() && !buildConfigGithub.startsWith("placeholder_")) {
            updated = updated.copy(keyGithub = buildConfigGithub)
            changed = true
        }
        if (updated.keyStripe.isBlank() && buildConfigStripe.isNotBlank() && !buildConfigStripe.startsWith("placeholder_")) {
            updated = updated.copy(keyStripe = buildConfigStripe)
            changed = true
        }
        if (updated.keyStripePublishable.isBlank() && buildConfigStripePub.isNotBlank() && !buildConfigStripePub.startsWith("placeholder_")) {
            updated = updated.copy(keyStripePublishable = buildConfigStripePub)
            changed = true
        }
        if (updated.keyGemini.isBlank() && buildConfigGemini.isNotBlank() && !buildConfigGemini.startsWith("placeholder_")) {
            updated = updated.copy(keyGemini = buildConfigGemini)
            changed = true
        }

        if (changed) {
            settingsDao.saveSettings(updated)
            refreshSecrets(updated)
        }
    }

    /**
     * Implements the Stripe key and GitHub key directly into the Room database store secrets.
     * Extracts keys from BuildConfig / .env or accepts manual overrides, writing them to SettingsEntity.
     */
    suspend fun implementKeysIntoStoreSecrets(
        stripeSecret: String? = null,
        stripePublishable: String? = null,
        githubToken: String? = null
    ): StoreSecretsImplementationResult = withContext(Dispatchers.IO) {
        val current = settingsDao.getSettingsSync() ?: SettingsEntity()
        val buildConfigGithub = try { BuildConfig.GITHUB_TOKEN.trim() } catch (_: Throwable) { "" }
        val buildConfigStripe = try { BuildConfig.STRIPE_SECRET_KEY.trim() } catch (_: Throwable) { "" }
        val buildConfigStripePub = try { BuildConfig.STRIPE_PUBLISHABLE_KEY.trim() } catch (_: Throwable) { "" }

        val finalGithub = when {
            !githubToken.isNullOrBlank() -> githubToken.trim()
            current.keyGithub.isNotBlank() && !current.keyGithub.startsWith("placeholder_") -> current.keyGithub.trim()
            buildConfigGithub.isNotBlank() && !buildConfigGithub.startsWith("placeholder_") -> buildConfigGithub
            else -> current.keyGithub.trim()
        }

        val finalStripeSecret = when {
            !stripeSecret.isNullOrBlank() -> stripeSecret.trim()
            current.keyStripe.isNotBlank() && !current.keyStripe.startsWith("placeholder_") -> current.keyStripe.trim()
            buildConfigStripe.isNotBlank() && !buildConfigStripe.startsWith("placeholder_") -> buildConfigStripe
            else -> current.keyStripe.trim()
        }

        val finalStripePub = when {
            !stripePublishable.isNullOrBlank() -> stripePublishable.trim()
            current.keyStripePublishable.isNotBlank() && !current.keyStripePublishable.startsWith("placeholder_") -> current.keyStripePublishable.trim()
            buildConfigStripePub.isNotBlank() && !buildConfigStripePub.startsWith("placeholder_") -> buildConfigStripePub
            else -> current.keyStripePublishable.trim()
        }

        val updated = current.copy(
            keyGithub = finalGithub,
            keyStripe = finalStripeSecret,
            keyStripePublishable = finalStripePub
        )

        settingsDao.saveSettings(updated)
        cachedGithubToken = finalGithub
        cachedStripeSecretKey = finalStripeSecret
        cachedStripePublishableKey = finalStripePub
        refreshSecrets(updated)

        val isGhConfigured = finalGithub.isNotBlank() && !finalGithub.startsWith("placeholder_")
        val isStripeConfigured = finalStripeSecret.isNotBlank() && !finalStripeSecret.startsWith("placeholder_")
        val isPubConfigured = finalStripePub.isNotBlank() && !finalStripePub.startsWith("placeholder_")

        StoreSecretsImplementationResult(
            isGitHubConfigured = isGhConfigured,
            isStripeSecretConfigured = isStripeConfigured,
            isStripePublishableConfigured = isPubConfigured,
            githubMasked = maskSecret(finalGithub),
            stripeSecretMasked = maskSecret(finalStripeSecret),
            stripePublishableMasked = maskSecret(finalStripePub),
            message = "Store Secrets Updated: GitHub (${if (isGhConfigured) "Configured" else "Unconfigured"}), Stripe Secret (${if (isStripeConfigured) "Configured" else "Unconfigured"}), Stripe Pub (${if (isPubConfigured) "Configured" else "Unconfigured"})"
        )
    }

    /**
     * Verifies the live Stripe secret key against Stripe's API.
     */
    suspend fun verifyStripeSecretKey(): Result<String> = withContext(Dispatchers.IO) {
        val key = getStripeSecretKey()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Stripe Secret Key is empty"))

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.stripe.com/v1/balance")
            .header("Authorization", "Bearer $key")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Stripe Connected • Live API Authenticated (HTTP 200)")
                } else {
                    Result.failure(IOException("Stripe Auth Failure (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies the live GitHub access token against GitHub's API.
     */
    suspend fun verifyGitHubToken(): Result<String> = withContext(Dispatchers.IO) {
        val token = getGitHubToken()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("GitHub Token is empty"))

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

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
                    val json = Gson().fromJson(body, JsonObject::class.java)
                    val login = json.get("login")?.asString ?: "connected"
                    Result.success("GitHub Connected • Authenticated User: @$login")
                } else {
                    Result.failure(IOException("GitHub Auth Failure (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Utility method to safely mask credentials for UI display and audit logging.
     * e.g., "ghp_1234567890abcdef" -> "ghp_••••••••cdef"
     */
    fun maskSecret(secret: String, prefixLength: Int = 4, suffixLength: Int = 4): String {
        if (secret.isBlank()) return "Not Configured"
        if (secret.length <= (prefixLength + suffixLength)) {
            return "••••••••"
        }
        val prefix = secret.take(prefixLength)
        val suffix = secret.takeLast(suffixLength)
        return "$prefix••••••••$suffix"
    }
}
