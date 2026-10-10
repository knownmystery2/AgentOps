package com.example.agentops.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val client = OkHttpClient()

    private val _baseUrl = MutableStateFlow("http://127.0.0.1:8080")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    private val _daemonState = MutableStateFlow("ACTIVE")
    val daemonState: StateFlow<String> = _daemonState.asStateFlow()

    private val _cronInterval = MutableStateFlow("Every 5 min")
    val cronInterval: StateFlow<String> = _cronInterval.asStateFlow()

    private val _concurrencyThreads = MutableStateFlow(2)
    val concurrencyThreads: StateFlow<Int> = _concurrencyThreads.asStateFlow()

    private val _pingResult = MutableStateFlow<String>("Not Tested")
    val pingResult: StateFlow<String> = _pingResult.asStateFlow()

    fun updateBaseUrl(newUrl: String) { _baseUrl.value = newUrl }
    fun toggleDeveloperMode(enabled: Boolean) { _isDeveloperMode.value = enabled }
    fun updateDaemonState(state: String) { _daemonState.value = state }
    fun updateCronInterval(interval: String) { _cronInterval.value = interval }
    fun updateConcurrency(threads: Int) { _concurrencyThreads.value = threads }

    fun runLivePingTest() {
        viewModelScope.launch {
            _pingResult.value = "Pinging..."
            val startTime = System.currentTimeMillis()
            val request = Request.Builder().url("${_baseUrl.value}/api/status").get().build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    _pingResult.value = "Failed: ${e.localizedMessage}"
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (it.isSuccessful) {
                            val duration = System.currentTimeMillis() - startTime
                            _pingResult.value = "Success: ${duration}ms"
                        } else { _pingResult.value = "Error: ${it.code}" }
                    }
                }
            })
        }
    }
}
