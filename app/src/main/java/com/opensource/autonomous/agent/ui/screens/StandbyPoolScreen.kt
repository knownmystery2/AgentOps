package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.AccountTaskEntity
import com.opensource.autonomous.agent.data.StandbyProviderEntity
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun StandbyPoolScreen(viewModel: MainViewModel) {
    val tasks by viewModel.accountTasks.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val daemonState by viewModel.daemonState.collectAsState()
    val cronSchedule by viewModel.cronSchedule.collectAsState()
    val workerConcurrency by viewModel.workerConcurrency.collectAsState()
    val memoryThrottleMb by viewModel.memoryThrottleMb.collectAsState()

    val pendingTasks = tasks.filter { !it.isCompleted }
    val healthyProviders = providers.count { it.status == "ACTIVE" || it.status == "STANDBY" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Daemon Control & Standby Pool",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "True daemon sleep/wake control, cron schedule dispatch, resource throttling, and compute failover registry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. True Daemon Sleep/Wake Control
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("daemon_control_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Master Daemon Execution State",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (daemonState == "ACTIVE") ProfitGreen else Color(0xFFF59E0B)).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = daemonState,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (daemonState == "ACTIVE") ProfitGreen else Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Control background thread polling and heartbeat frequency inside Termux Linux runtime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = daemonState == "ACTIVE",
                            onClick = { viewModel.setDaemonState("ACTIVE") },
                            label = { Text("ACTIVE (5s)") },
                            modifier = Modifier.weight(1f).testTag("daemon_state_active")
                        )
                        FilterChip(
                            selected = daemonState == "STANDBY",
                            onClick = { viewModel.setDaemonState("STANDBY") },
                            label = { Text("STANDBY (Saver)") },
                            modifier = Modifier.weight(1f).testTag("daemon_state_standby")
                        )
                        FilterChip(
                            selected = daemonState == "SLEEP",
                            onClick = { viewModel.setDaemonState("SLEEP") },
                            label = { Text("SLEEP (Cron)") },
                            modifier = Modifier.weight(1f).testTag("daemon_state_sleep")
                        )
                    }
                }
            }
        }

        // 2. Cron Schedule Dispatch
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("cron_dispatch_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cron Execution Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.triggerCronDispatchNow() },
                            modifier = Modifier.testTag("trigger_cron_dispatch_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dispatch Now", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "Automated execution interval for agent pipeline cycles and ledger audits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Every 5 min", "Every 15 min", "Hourly").forEach { sched ->
                            FilterChip(
                                selected = cronSchedule == sched,
                                onClick = { viewModel.setCronSchedule(sched) },
                                label = { Text(sched) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Resource Throttling Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("resource_throttling_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Resource Throttling & Limits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Worker Concurrency: $workerConcurrency threads",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 4, 8).forEach { count ->
                            FilterChip(
                                selected = workerConcurrency == count,
                                onClick = { viewModel.workerConcurrency.value = count },
                                label = { Text("$count Threads") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Heap Memory Throttle: $memoryThrottleMb MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(256, 512, 1024).forEach { mb ->
                            FilterChip(
                                selected = memoryThrottleMb == mb,
                                onClick = { viewModel.memoryThrottleMb.value = mb },
                                label = { Text("$mb MB") }
                            )
                        }
                    }
                }
            }
        }

        // 4. Phone tasks section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("phone_tasks_header_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actions Requiring Your Phone (${pendingTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "Agents prepare credentials and branding. You complete 2FA, W-9, and CAPTCHAs directly on your phone to maintain top admin control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(tasks) { task ->
            AccountTaskCard(task, onToggle = { viewModel.toggleAccountTask(task.id, !task.isCompleted) })
        }

        // 5. Standby Pool & Provider Registry Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("standby_pool_header_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Standby Free-Tier Compute Pool",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ProfitGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$healthyProviders Healthy",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Automatic failover with exponential backoff. If Colab or Kaggle free quota runs low, the orchestrator routes to the next healthy account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(providers) { provider ->
            ProviderRegistryCard(provider)
        }
    }
}

@Composable
fun AccountTaskCard(task: AccountTaskEntity, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().testTag("account_task_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.platform,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandEmerald
                    )
                    Text(
                        text = task.priority,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.priority == "HIGH") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProviderRegistryCard(provider: StandbyProviderEntity) {
    val isHealthy = provider.status == "ACTIVE" || provider.status == "STANDBY"
    val badgeColor = if (isHealthy) ProfitGreen else Color(0xFFF59E0B)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("provider_card_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${provider.serviceName} (${provider.tierType})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = provider.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Alias: ${provider.accountAlias} • ${provider.endpointUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quota Used: ${provider.quotaPercentUsed}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (provider.quotaPercentUsed > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { provider.quotaPercentUsed / 100f },
                    modifier = Modifier.width(140.dp).height(6.dp),
                    color = if (provider.quotaPercentUsed > 80) MaterialTheme.colorScheme.error else BrandEmerald
                )
            }
        }
    }
}
