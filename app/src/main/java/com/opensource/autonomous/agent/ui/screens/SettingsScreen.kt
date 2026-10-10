package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agentops.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val isDevMode by viewModel.isDeveloperMode.collectAsState()
    val daemonState by viewModel.daemonState.collectAsState()
    val cronInterval by viewModel.cronInterval.collectAsState()
    val threads by viewModel.concurrencyThreads.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()

    var hostInput by remember { mutableStateOf(baseUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("System Control Panel", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Advanced Developer Diagnostics", modifier = Modifier.weight(1fr))
            Switch(
                checked = isDevMode,
                onCheckedChange = { viewModel.toggleDeveloperMode(it) }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        if (!isDevMode) {
            Text("General Configurations", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Master Automation Lifecycle Daemon:")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("ACTIVE", "STANDBY", "SLEEP").forEach { state ->
                    FilterChip(
                        selected = daemonState == state,
                        onClick = { viewModel.updateDaemonState(state) },
                        label = { Text(state) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Script Cron Trigger Frequency:")
            var expandedCron by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expandedCron = true }) { Text(cronInterval) }
                DropdownMenu(expanded = expandedCron, onDismissRequest = { expandedCron = false }) {
                    listOf("Every 5 min", "Every 15 min", "Hourly").forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval) },
                            onClick = {
                                viewModel.updateCronInterval(interval)
                                expandedCron = false
                            }
                        )
                    }
                }
            }
        } else {
            Text("Advanced Micro-Orchestrator Enclaves", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = hostInput,
                onValueChange = { hostInput = it; viewModel.updateBaseUrl(it) },
                label = { Text("Server Host Base URL Hook") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    hostInput = "http://127.0.0.1:8080"
                    viewModel.updateBaseUrl(hostInput)
                }, modifier = Modifier.weight(1fr)) {
                    Text("Termux Local")
                }
                Button(onClick = { 
                    hostInput = "http://10.0.2.2:8080"
                    viewModel.updateBaseUrl(hostInput)
                }, modifier = Modifier.weight(1fr)) {
                    Text("Emulator")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Interactive Runtime Host Ping Tool")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { viewModel.runLivePingTest() }) { Text("Run Latency Diagnostics") }
                Spacer(modifier = Modifier.width(16.dp))
                Text(pingResult, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Worker Concurrency Allocations: ($threads Threads)")
            Slider(
                value = threads.toFloat(),
                onValueChange = { viewModel.updateConcurrency(it.toInt()) },
                valueRange = 1f..8f,
                steps = 6
            )

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dangerous Operations", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.refreshLocalQueues() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Flush Local Task Queue Caches")
                    }
                }
            }
        }
    }
}
