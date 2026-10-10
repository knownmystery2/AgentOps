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
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.AccountTaskEntity
import com.opensource.autonomous.agent.data.StandbyProviderEntity
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun StandbyPoolScreen(viewModel: MainViewModel) {
    val tasks by viewModel.accountTasks.collectAsState()
    val providers by viewModel.providers.collectAsState()

    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
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
                text = "Account Tasks & Standby Pool",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Top admin phone verification tasks & standby free-tier compute failover registry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Phone tasks section
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

        // Standby Pool & Provider Registry Section
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
                    text = provider.serviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (provider.status) {
                        "ACTIVE" -> ProfitGreen.copy(alpha = 0.15f)
                        "STANDBY" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = provider.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (provider.status) {
                            "ACTIVE" -> ProfitGreen
                            "STANDBY" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${provider.tierType} • Alias: ${provider.accountAlias}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Free Quota Consumption:", style = MaterialTheme.typography.bodySmall)
                Text("${provider.quotaPercentUsed}% Used", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { provider.quotaPercentUsed / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (provider.quotaPercentUsed > 80) MaterialTheme.colorScheme.error else BrandEmerald,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
