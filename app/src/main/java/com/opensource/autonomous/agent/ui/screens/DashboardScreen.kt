package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToApprovals: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToStandby: () -> Unit
) {
    var businessPrompt by remember { mutableStateOf("") }
    val decisions by viewModel.decisions.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val ledger by viewModel.ledgerEntries.collectAsState()
    val accountTasks by viewModel.accountTasks.collectAsState()
    val swarmTasks by viewModel.tasks.collectAsState()
    val isRunningCycle by viewModel.isRunningCycle.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedFilter by viewModel.selectedLogFilter.collectAsState()

    val salesList = ledger.filter { it.category == "SALE" }
    val grossCents = salesList.sumOf { it.amountCents }
    val cogsCents = ledger.filter { it.category == "COGS" }.sumOf { it.amountCents }
    val feeCents = ledger.filter { it.category == "FEE" }.sumOf { it.amountCents }
    val netProfitCents = grossCents - cogsCents - feeCents
    val pendingDecisionsCount = decisions.size
    val pendingPhoneTasksCount = accountTasks.count { !it.isCompleted }

    // Worker Queue Counters
    val pendingWorkerTasks = swarmTasks.count { it.status == "PENDING" }
    val runningWorkerTasks = swarmTasks.count { it.status == "RUNNING" }
    val completedWorkerTasks = swarmTasks.count { it.status == "COMPLETED" }
    val failedWorkerTasks = swarmTasks.count { it.status == "FAILED" }

    val currentDateStr = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    val (totalMem, freeMem) = viewModel.getMemoryStats()

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "STDOUT" -> logs.filter { !it.message.contains("Error", ignoreCase = true) && !it.message.contains("Denied", ignoreCase = true) }
            "STDERR" -> logs.filter { it.message.contains("Error", ignoreCase = true) || it.message.contains("Denied", ignoreCase = true) || it.message.contains("offline", ignoreCase = true) }
            "WORKER" -> logs.filter { it.agentId.contains("Orchestrator") || it.agentId.contains("Reliability") }
            else -> logs
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Executive Header Card with Real System Date & Live Telemetry
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("executive_pulse_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE CONSOLE • $currentDateStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandEmerald
                            )
                            Text(
                                text = "Autonomous Swarm Runtime",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = (if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B)).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isLiveConnected) "Termux :8080 Live" else "Room Local Engine",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live System Uptime & Heap Telemetry Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Uptime: ${viewModel.getSystemUptimeFormatted()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "JVM Heap: ${freeMem}MB free / ${totalMem}MB total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Money metrics ticker computed directly from live Room ledger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricBox(
                            title = "Gross Revenue",
                            value = "$${String.format("%.2f", grossCents / 100.0)}",
                            sub = "${salesList.size} live sales settled",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Net Profit",
                            value = "$${String.format("%.2f", netProfitCents / 100.0)}",
                            sub = "Whole cents ledger",
                            valueColor = ProfitGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onNavigateToApprovals,
                            leadingIcon = { Icon(Icons.Default.PendingActions, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                            label = { Text("$pendingDecisionsCount Approvals Queue") },
                            modifier = Modifier.weight(1f).testTag("pending_approvals_chip")
                        )
                        AssistChip(
                            onClick = onNavigateToStandby,
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            label = { Text("$pendingPhoneTasksCount Phone 2FA") },
                            modifier = Modifier.weight(1f).testTag("phone_tasks_chip")
                        )
                    }
                }
            }
        }

        // Live Worker Queue Counters
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("worker_queue_counters_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Worker Queue Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QueuePill(label = "Pending", count = pendingWorkerTasks, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                        QueuePill(label = "Running", count = runningWorkerTasks, color = BrandEmerald, modifier = Modifier.weight(1f))
                        QueuePill(label = "Completed", count = completedWorkerTasks, color = ProfitGreen, modifier = Modifier.weight(1f))
                        QueuePill(label = "Failed", count = failedWorkerTasks, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Executive Operations Summary
        item {
            val podPending = decisions.count { it.agentSource.contains("POD") || it.description.contains("Tee") || it.description.contains("Hoodie") }
            val dsPending = decisions.count { it.agentSource.contains("Dropship") || it.description.contains("Shortlist") }
            val otherPending = (decisions.size - podPending - dsPending).coerceAtLeast(0)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Executive Operations Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BriefingBullet(
                        icon = Icons.Default.CheckCircle,
                        iconColor = ProfitGreen,
                        label = "What Sold",
                        desc = "${salesList.size} orders recorded in double-entry ledger ($${String.format("%.2f", grossCents / 100.0)} gross). COGS covered directly from buyer checkouts."
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BriefingBullet(
                        icon = Icons.Default.HourglassTop,
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        label = "What is Pending",
                        desc = "$pendingDecisionsCount items awaiting human authorization ($podPending POD, $dsPending dropship, $otherPending operations)."
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BriefingBullet(
                        icon = Icons.Default.Shield,
                        iconColor = MaterialTheme.colorScheme.primary,
                        label = "What is Blocked",
                        desc = if (pendingPhoneTasksCount > 0) "$pendingPhoneTasksCount phone actions pending user SMS/W-9 input. Zero accounts shut down or flagged." else "Zero blocked accounts. All stores operating normally."
                    )
                }
            }
        }

        // Autonomous Execution Entry Console
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("objective_input_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Autonomous Directive Dispatch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Specify an e-commerce expansion directive. The 11-agent team will decompose it into compliant, zero-upfront tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = businessPrompt,
                        onValueChange = { businessPrompt = it },
                        placeholder = { Text("e.g. Minimalist Retro Coffee POD line on Etsy / CJ Dropshipping desk accessories") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("business_objective_input"),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.initializeBusiness(businessPrompt)
                                businessPrompt = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("initialize_engine_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch Directive")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.triggerAgentCycle() },
                            enabled = !isRunningCycle,
                            modifier = Modifier.testTag("trigger_agent_cycle_button")
                        ) {
                            if (isRunningCycle) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Autorenew, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        // Decision Register Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inbound Approvals Register",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToApprovals) {
                    Text("View Full Queue (${decisions.size})")
                }
            }
        }

        if (decisions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "All proposed items reviewed. Tap 'Trigger Agent Cycle' or submit a directive to generate new items.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(decisions.take(3), key = { it.id }) { decision ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_decision_card_${decision.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandEmerald.copy(alpha = 0.15f),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = decision.agentSource,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandEmerald
                                )
                            }
                            Text(
                                text = viewModel.formatRelativeTime(decision.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = decision.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.rejectDecision(decision.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("dashboard_deny_button_${decision.id}")
                            ) {
                                Text("DENY")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.approveDecision(decision.id) },
                                modifier = Modifier.testTag("dashboard_approve_button_${decision.id}")
                            ) {
                                Text("APPROVE INTERACTION")
                            }
                        }
                    }
                }
            }
        }

        // Live stdout / stderr Terminal Stream
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Terminal Log Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.syncWithLiveBackend() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "STDOUT", "STDERR", "WORKER").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.selectedLogFilter.value = filter },
                            label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("log_filter_$filter")
                        )
                    }
                }
            }
        }

        items(filteredLogs.take(20), key = { it.id }) { log ->
            val isError = log.message.contains("Error", ignoreCase = true) || log.message.contains("Denied", ignoreCase = true)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[${log.agentId}]",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.formatLiveTimestamp(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QueuePill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BriefingBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
