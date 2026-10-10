package com.opensource.autonomous.agent.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.ui.MainViewModel

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
    val tasks by viewModel.accountTasks.collectAsState()
    val isRunningCycle by viewModel.isRunningCycle.collectAsState()

    val grossCents = ledger.filter { it.category == "SALE" }.sumOf { it.amountCents }
    val cogsCents = ledger.filter { it.category == "COGS" }.sumOf { it.amountCents }
    val feeCents = ledger.filter { it.category == "FEE" }.sumOf { it.amountCents }
    val netProfitCents = grossCents - cogsCents - feeCents
    val pendingDecisionsCount = decisions.size
    val pendingTasksCount = tasks.count { !it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Executive Header Card
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
                                text = "DAILY BRIEFING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandEmerald
                            )
                            Text(
                                text = "AgentOps Executive Hub",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = BrandEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "100% Open Source",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Money metrics ticker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricBox(
                            title = "Gross Revenue",
                            value = "$${String.format("%.2f", grossCents / 100.0)}",
                            sub = "$0 upfront inventory",
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

                    // Pending badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onNavigateToApprovals,
                            leadingIcon = { Icon(Icons.Default.PendingActions, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                            label = { Text("$pendingDecisionsCount Pending Approvals") },
                            modifier = Modifier.weight(1f).testTag("pending_approvals_chip")
                        )
                        AssistChip(
                            onClick = onNavigateToStandby,
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            label = { Text("$pendingTasksCount Phone Tasks") },
                            modifier = Modifier.weight(1f).testTag("phone_tasks_chip")
                        )
                    }
                }
            }
        }

        // Executive Action Cards: What Sold, What is Pending, What is Blocked
        item {
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
                        desc = "2 customer orders fulfilled ($62.50 gross). Production COGS funded directly from customer checkouts. No out-of-pocket cost."
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BriefingBullet(
                        icon = Icons.Default.HourglassTop,
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        label = "What is Pending",
                        desc = "$pendingDecisionsCount items in Human Verification Register (1 POD apparel mockup, 1 dropship item, 1 customer inquiry)."
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BriefingBullet(
                        icon = Icons.Default.Shield,
                        iconColor = MaterialTheme.colorScheme.primary,
                        label = "What is Blocked",
                        desc = if (pendingTasksCount > 0) "$pendingTasksCount phone actions pending user SMS/W-9 input. Zero accounts shut down or flagged." else "Zero blocked accounts. All stores operating normally."
                    )
                }
            }
        }

        // Business Idea Generation Entry Input (from Master Prompt)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("objective_input_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AI Corporate Hub Control Console",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Describe an e-commerce goal or niche idea. Agents will decompose it into compliant, zero-upfront tasks.",
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
                            Text("Initialize Execution Engine")
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

        // Decision Register Preview (Awaiting Human Verification)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Decision Register (Human Override)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToApprovals) {
                    Text("View All (${decisions.size})")
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
                            text = "All proposed items reviewed. Tap 'Trigger Agent Cycle' or submit a business prompt to generate new designs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(decisions.take(3)) { decision ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_decision_card_${decision.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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

        // System Centralized Activity Logs
        item {
            Text(
                text = "System Centralized Activity Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(logs.take(15)) { log ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "[${log.agentId}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(130.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
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
