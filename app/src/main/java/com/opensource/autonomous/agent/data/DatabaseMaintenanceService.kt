package com.opensource.autonomous.agent.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntegrityCheckResult(
    val isValid: Boolean,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class VacuumResult(
    val success: Boolean,
    val bytesBefore: Long,
    val bytesAfter: Long,
    val bytesSaved: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class DatabaseMaintenanceState(
    val isRunning: Boolean = false,
    val lastCheckTimestamp: Long = 0L,
    val isIntegrityValid: Boolean = true,
    val lastIntegrityReport: String = "Not performed yet",
    val lastVacuumTimestamp: Long = 0L,
    val bytesSavedOnLastVacuum: Long = 0L,
    val databaseSizeKb: Long = 0L,
    val maintenanceLogs: List<String> = emptyList()
)

/**
 * DatabaseMaintenanceService
 *
 * Performs periodic integrity checks and vacuum operations on the Room database
 * to prevent data corruption and ensure efficient local storage.
 */
class DatabaseMaintenanceService(
    private val context: Context? = null,
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(DatabaseMaintenanceState())
    val state: StateFlow<DatabaseMaintenanceState> = _state.asStateFlow()

    private val dbFile: File?
        get() = context?.getDatabasePath("agent_biz.db")

    init {
        // Measure initial database size
        measureDatabaseSize()
    }

    private fun measureDatabaseSize() {
        val sizeKb = if (dbFile?.exists() == true) dbFile!!.length() / 1024 else 128L
        _state.value = _state.value.copy(databaseSizeKb = sizeKb)
    }

    /**
     * Executes SQLite PRAGMA integrity_check to verify database health
     */
    suspend fun runIntegrityCheck(): IntegrityCheckResult = withContext(Dispatchers.IO) {
        logMessage("Starting SQLite PRAGMA integrity_check...")
        try {
            val database = db.openHelper.writableDatabase
            val cursor = database.query("PRAGMA integrity_check")
            val results = mutableListOf<String>()

            cursor.use {
                while (it.moveToNext()) {
                    results.add(it.getString(0))
                }
            }

            val isValid = results.size == 1 && results[0].equals("ok", ignoreCase = true)
            val report = if (isValid) "Database integrity OK (0 corruption markers)" else "Integrity anomalies: ${results.joinToString(", ")}"
            
            logMessage("Integrity check result: $report")

            _state.value = _state.value.copy(
                lastCheckTimestamp = System.currentTimeMillis(),
                isIntegrityValid = isValid,
                lastIntegrityReport = report
            )
            measureDatabaseSize()

            IntegrityCheckResult(isValid, report)
        } catch (e: Exception) {
            val errorReport = "Integrity check failed: ${e.message}"
            logMessage("❌ $errorReport")
            _state.value = _state.value.copy(
                lastCheckTimestamp = System.currentTimeMillis(),
                isIntegrityValid = false,
                lastIntegrityReport = errorReport
            )
            IntegrityCheckResult(false, errorReport)
        }
    }

    /**
     * Executes SQLite VACUUM to reclaim free space, defragment, and rebuild the database file
     */
    suspend fun runVacuum(): VacuumResult = withContext(Dispatchers.IO) {
        logMessage("Starting SQLite VACUUM maintenance...")
        val bytesBefore = if (dbFile?.exists() == true) dbFile?.length() ?: 0L else 0L

        try {
            val database = db.openHelper.writableDatabase
            // Checkpoint WAL first
            database.execSQL("PRAGMA wal_checkpoint(FULL)")
            // Reclaim fragmented space
            database.execSQL("VACUUM")

            val bytesAfter = if (dbFile?.exists() == true) dbFile?.length() ?: 0L else 0L
            val saved = (bytesBefore - bytesAfter).coerceAtLeast(0L)

            val msg = "VACUUM completed: Before=${bytesBefore / 1024}KB, After=${bytesAfter / 1024}KB, Reclaimed=${saved} bytes"
            logMessage(msg)

            _state.value = _state.value.copy(
                lastVacuumTimestamp = System.currentTimeMillis(),
                bytesSavedOnLastVacuum = saved
            )
            measureDatabaseSize()

            VacuumResult(true, bytesBefore, bytesAfter, saved)
        } catch (e: Exception) {
            val errorMsg = "VACUUM operation failed: ${e.message}"
            logMessage("❌ $errorMsg")
            VacuumResult(false, bytesBefore, bytesBefore, 0L)
        }
    }

    /**
     * Comprehensive Maintenance Routine: Integrity Check + Vacuum
     */
    fun runFullMaintenance() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)

        scope.launch(Dispatchers.IO) {
            try {
                runIntegrityCheck()
                runVacuum()
                logMessage("Full database maintenance cycle finished successfully.")
            } catch (e: Exception) {
                logMessage("Error during full maintenance: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
            }
        }
    }

    private fun logMessage(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $msg"
        Log.i("DbMaintenance", entry)
        val logs = (_state.value.maintenanceLogs + entry).takeLast(20)
        _state.value = _state.value.copy(maintenanceLogs = logs)
    }
}
