package com.opensource.autonomous.agent.data

import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SecretStatus represents the configuration state and safe masked preview of active API credentials.
 */
data class SecretStatus(
    val isGitHubConfigured: Boolean = false,
    val isStripeConfigured: Boolean = false,
    val isStripePublishableConfigured: Boolean = false,
    val githubTokenMasked: String = "Not Configured",
    val stripeSecretKeyMasked: String = "Not Configured",
    val stripePublishableKeyMasked: String = "Not Configured",
    val githubSource: String = "None", // "AI Studio Secrets / BuildConfig" or "Device Local Settings"
    val stripeSource: String = "None"
)

/**
 * SecretManager is a secure repository module responsible for managing and retrieving
 * credentials required for automated deployments (GitHub PAT) and payment processing (Stripe).
 *
 * It provides:
 * 1. Dual-source resolution: Priority given to on-device explicit overrides, falling back to
 *    environment credentials injected via BuildConfig / Secrets Gradle plugin.
 * 2. Automatic placeholder sanitization: Ignores template placeholder values like "placeholder_...".
 * 3. Reactive state observation: UI components observe SecretStatus live.
 * 4. Masking utilities for zero-leak UI display and logging.
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

    init {
        // Observe settings changes reactively from Room
        scope.launch {
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

        cachedGithubToken = effectiveGithub
        cachedStripeSecretKey = effectiveStripe
        cachedStripePublishableKey = effectiveStripePub

        _secretStatus.value = SecretStatus(
            isGitHubConfigured = effectiveGithub.isNotBlank(),
            isStripeConfigured = effectiveStripe.isNotBlank(),
            isStripePublishableConfigured = effectiveStripePub.isNotBlank(),
            githubTokenMasked = maskSecret(effectiveGithub, prefixLength = 4, suffixLength = 4),
            stripeSecretKeyMasked = maskSecret(effectiveStripe, prefixLength = 7, suffixLength = 4),
            stripePublishableKeyMasked = maskSecret(effectiveStripePub, prefixLength = 7, suffixLength = 4),
            githubSource = githubSource,
            stripeSource = stripeSource
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
     * Saves or overrides the GitHub Personal Access Token on-device in Room.
     */
    suspend fun saveGitHubToken(token: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(keyGithub = token.trim())
        settingsDao.saveSettings(updated)
    }

    /**
     * Saves or overrides the Stripe Secret Key on-device in Room.
     */
    suspend fun saveStripeSecretKey(secretKey: String) = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()
        val updated = currentSettings.copy(keyStripe = secretKey.trim())
        settingsDao.saveSettings(updated)
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
