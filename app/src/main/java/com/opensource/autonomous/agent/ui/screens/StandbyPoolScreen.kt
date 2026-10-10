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
fun StandbyPoolScreen(viewModel: MainViewModel) {
    val daemonState by viewModel.daemonState.collectAsState()
    val cronInterval by viewModel.cronInterval.collectAsState()
    val threads by viewModel.concurrencyThreads.collectAsState()
    val isDevMode by viewModel.isDeveloperMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Standby & Daemon Studio", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Manage resource allocations and background loop scheduling intervals for your local Termux server environment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // --- SECTION 1: MASTER STATE CONTROL ---
        Text("Master Daemon State Control", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ACTIVE", "STANDBY", "SLEEP").forEach { state ->
                ElevatedFilterChip(
                    selected = daemonState == state,
                    onClick = { viewModel.updateDaemonState(state) },
                    label = { Text(state) },
                    modifier = Modifier.weight(1fr)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: CRON INTERVAL SCHEDULER ---
        Text("Cron Schedule Dispatch Interval", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        var expandedInterval by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedInterval = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Interval: $cronInterval")
            }
            DropdownMenu(
                expanded = expandedInterval,
                onDismissRequest = { expandedInterval = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Every 5 min", "Every 15 min", "Hourly").forEach { interval ->
                    DropdownMenuItem(
                        text = { Text(interval) },
                        onClick = {
                            viewModel.updateCronInterval(interval)
                            expandedInterval = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.runLivePingTest() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trigger Manual Dispatch Loop Now")
        }

        // --- SECTION 3: ADVANCED RUNTIME THROTTLING ---
        if (isDevMode) {
            Divider(modifier = Modifier.padding(vertical = 24.dp))
            Text("Advanced Resource Throttling", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Worker Concurrency Limit: ($threads Threads)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = threads.toFloat(),
                        onValueChange = { viewModel.updateConcurrency(it.toInt()) },
                        valueRange = 1f..8f,
                        steps = 6
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Heap Memory Limit Optimization", style = MaterialTheme.typography.labelLarge)
                    Text("Controlled via internal Termux runtime environment configuration parameters.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
