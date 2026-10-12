package com.opensource.autonomous.agent.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.opensource.autonomous.agent.data.GodAuditVerdictEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SecurityAlert(
    val alertId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceAgent: String,
    val actionType: String,
    val targetResource: String,
    val hazardDetected: String,
    val remediation: String,
    val severity: String = "CRITICAL",
    val isDismissed: Boolean = false
)

/**
 * Real-time notification service that triggers a persistent banner on the dashboard
 * and dispatches system notifications when the God agent detects a security anomaly or system halt.
 */
class GodNotificationService(
    private val context: Context? = null,
    private val scope: CoroutineScope
) {
    companion object {
        const val CHANNEL_ID = "god_security_anomalies"
        const val CHANNEL_NAME = "God Overseer Security Alerts"
        const val NOTIFICATION_ID = 9001
    }

    private val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val _activeAlert = MutableStateFlow<SecurityAlert?>(null)
    val activeAlert: StateFlow<SecurityAlert?> = _activeAlert.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<SecurityAlert>>(emptyList())
    val alertHistory: StateFlow<List<SecurityAlert>> = _alertHistory.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for prompt injections, rogue logic, and system halts"
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Triggers a real-time security anomaly alert from a God halt verdict
     */
    fun triggerSecurityHalt(verdict: GodAuditVerdictEntity) {
        val alert = SecurityAlert(
            alertId = verdict.id,
            timestamp = verdict.timestamp,
            sourceAgent = verdict.sourceAgent,
            actionType = verdict.actionType,
            targetResource = verdict.targetResource,
            hazardDetected = verdict.hazardDetected ?: "Security anomaly detected by God Overseer.",
            remediation = verdict.remediation ?: "System halted pending human review."
        )

        _activeAlert.value = alert
        _alertHistory.value = (_alertHistory.value + alert).takeLast(20)

        // Post system notification
        dispatchSystemNotification(alert)
    }

    private fun dispatchSystemNotification(alert: SecurityAlert) {
        val ctx = context ?: return
        try {
            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("🚨 [GOD OVERSEER HALT] ${alert.sourceAgent}")
                .setContentText(alert.hazardDetected)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.hazardDetected}\n\nAction: ${alert.actionType} on ${alert.targetResource}\nRemediation: ${alert.remediation}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Notification permission might be off or test environment
        }
    }

    fun dismissBanner() {
        _activeAlert.value = null
    }

    fun acknowledgeAlert(alertId: String) {
        if (_activeAlert.value?.alertId == alertId) {
            _activeAlert.value = null
        }
        _alertHistory.value = _alertHistory.value.map {
            if (it.alertId == alertId) it.copy(isDismissed = true) else it
        }
    }

    fun clearAll() {
        _activeAlert.value = null
        _alertHistory.value = emptyList()
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
