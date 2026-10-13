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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.AgentStatus
import com.opensource.autonomous.agent.data.AutonomousAgent
import com.opensource.autonomous.agent.ui.AdvancedWorkspaceDashboard
import com.opensource.autonomous.agent.ui.AutonomousAgentInspectorDialog
import com.opensource.autonomous.agent.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToApprovals: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToStandby: () -> Unit,
    onNavigateToMockupStudio: () -> Unit = {}
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
    val autonomousAgents by viewModel.autonomousAgents.collectAsState()
    val isSwarmRunning by viewModel.isSwarmRunning.collectAsState()
    val swarmStatusMessage by viewModel.swarmStatusMessage.collectAsState()
    val activeStoreTheme by viewModel.activeStoreTheme.collectAsState()
    val marketingPackages by viewModel.marketingPackages.collectAsState()
    var inspectingAgent by remember { mutableStateOf<AutonomousAgent?>(null) }
    var selectedDepartmentFilter by remember { mutableStateOf("ALL") }
    var showAdvancedSwarmHub by remember { mutableStateOf(false) }

    val activeAlert by viewModel.activeSecurityAlert.collectAsState()
    val godHaltedCount by viewModel.godHaltedCount.collectAsState()
    val businessAnalysis by viewModel.businessAnalysis.collectAsState()
    val isProcessingBusinessPrompt by viewModel.isProcessingBusinessPrompt.collectAsState()
    val businessOperationStatusMessage by viewModel.businessOperationStatusMessage.collectAsState()

    val salesList = ledger.filter { it.category == "SALE" }
    val grossCents = salesList.sumOf { it.amountCents }
    val cogsCents = ledger.filter { it.category == "COGS" }.sumOf { it.amountCents }
    val feeCents = ledger.filter { it.category == "FEE" }.sumOf { it.amountCents }
    val netProfitCents = grossCents - cogsCents - feeCents
    val pendingDecisionsCount = decisions.size
    val pendingPhoneTasksCount = accountTasks.count { !it.isCompleted }

    // Print-On-Demand Business & Order Metrics
    val podQueue by viewModel.podQueue.collectAsState()
    val activePodOrdersCount = podQueue.count { it.status == "PRINTING" || it.status == "QUEUED" || it.status == "RENDERING" || it.status == "COMPILED" }
    val readyPodOrdersCount = podQueue.count { it.status == "READY" }

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
        // Persistent Real-Time God Security Anomaly / System Halt Banner
        if (activeAlert != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("god_security_halt_banner")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Security Alert",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🚨 GOD OVERSEER SECURITY HALT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissSecurityAlert() },
                                modifier = Modifier.size(28.dp).testTag("dismiss_security_halt_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss Banner", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A critical security anomaly or unauthorized rogue action was intercepted and halted by the God Agent before live execution.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Source: ${activeAlert?.sourceAgent} • Target: ${activeAlert?.targetResource}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hazard: ${activeAlert?.hazardDetected}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remediation: ${activeAlert?.remediation}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.dismissSecurityAlert() },
                                modifier = Modifier.testTag("dismiss_halt_btn")
                            ) {
                                Text("Dismiss Banner")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateToApprovals,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("view_approval_register_btn")
                            ) {
                                Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Approval Register")
                            }
                        }
                    }
                }
            }
        }

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
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLedger,
                            testTag = "metric_gross_revenue"
                        )
                        MetricBox(
                            title = "Net Profit",
                            value = "$${String.format("%.2f", netProfitCents / 100.0)}",
                            sub = "Whole cents ledger",
                            valueColor = ProfitGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLedger,
                            testTag = "metric_net_profit"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Badges (Row 1: Approvals & Phone 2FA)
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

                    // Navigation Badges (Row 2: Ledger Balance & POD Studio Workspace)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onNavigateToLedger,
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ProfitGreen) },
                            label = { Text("Audit Ledger ($${String.format("%.2f", netProfitCents / 100.0)})") },
                            modifier = Modifier.weight(1f).testTag("nav_to_ledger_chip")
                        )
                        AssistChip(
                            onClick = onNavigateToMockupStudio,
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            label = { Text("POD Mockup Studio") },
                            modifier = Modifier.weight(1f).testTag("nav_to_mockup_studio_chip")
                        )
                    }
                }
            }
        }

        // High-Level Print-On-Demand Business Operations Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pod_business_dashboard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BrandEmerald.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "POD",
                                        tint = BrandEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Print-On-Demand Operations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Direct-to-Garment • Printify & Stripe Live Pipeline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandEmerald.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "300 DPI Vector",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // POD High-Level Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Active Orders",
                            value = "$activePodOrdersCount in production",
                            sub = "${podQueue.size} lifetime orders",
                            valueColor = if (activePodOrdersCount > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            testTag = "pod_metric_active_orders"
                        )
                        MetricBox(
                            title = "Ready to Ship",
                            value = "$readyPodOrdersCount completed",
                            sub = "Print QC verified",
                            valueColor = ProfitGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "pod_metric_ready_orders"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary POD Metric Row: Ledger Balance & Catalog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Recent Ledger Balance",
                            value = "$${String.format("%.2f", netProfitCents / 100.0)}",
                            sub = "Gross $${String.format("%.2f", grossCents / 100.0)} • Whole cents",
                            valueColor = ProfitGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLedger,
                            testTag = "pod_metric_ledger_balance"
                        )
                        MetricBox(
                            title = "Print Catalogs",
                            value = "5 Core Blanks",
                            sub = "Tee, Hoodie, Mug, Tote, Art",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMockupStudio,
                            testTag = "pod_metric_catalogs"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Route Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToMockupStudio,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.2f).testTag("dashboard_open_pod_studio_btn")
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open POD Studio", maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.placeCustomerPodOrder(
                                    productName = "Heavyweight T-Shirt",
                                    color = "Dark Teal",
                                    placement = "Chest Center (11\" x 13\")",
                                    grossPriceCents = 2800L,
                                    supplierCostCents = 1050L,
                                    platformFeeCents = 182L
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("dashboard_place_pod_order_btn")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Test Order", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active POD Orders List
                    Text(
                        text = "Active Print Queue & Orders (${podQueue.take(4).size} of ${podQueue.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (podQueue.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No active print orders queued. Tap '+ Test Order' to trigger a live DTG order.",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            podQueue.take(4).forEach { order ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToMockupStudio() }
                                        .testTag("pod_order_card_${order.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = order.id,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = order.sku,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (order.status) {
                                                    "READY" -> ProfitGreen.copy(alpha = 0.15f)
                                                    "PRINTING" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                }
                                            ) {
                                                Text(
                                                    text = order.status,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (order.status) {
                                                        "READY" -> ProfitGreen
                                                        "PRINTING" -> Color(0xFFF59E0B)
                                                        else -> MaterialTheme.colorScheme.primary
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "${order.product} • ${order.color}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Zone: ${order.placement} • ${order.vectorDpi} DPI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (order.status == "PRINTING" || order.progressPercent < 100) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = { order.progressPercent / 100f },
                                                    modifier = Modifier.weight(1f).height(6.dp),
                                                    color = if (order.status == "PRINTING") Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${order.progressPercent}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // The Interceptor Architecture: God Overseer Sentinel
        item {
            val godIntercepts by viewModel.godTotalIntercepts.collectAsState()
            val godHalted by viewModel.godHaltedCount.collectAsState()
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (godHalted > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("god_sentinel_banner_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (godHalted > 0) MaterialTheme.colorScheme.error else BrandEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "God: Silent Interceptor Overseer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = (if (godHalted > 0) MaterialTheme.colorScheme.error else BrandEmerald).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (godHalted > 0) "$godHalted Hazards Halted" else "$godIntercepts Intercepted • Safe",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (godHalted > 0) MaterialTheme.colorScheme.error else BrandEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Silent out-of-band monitor with absolute veto power reviewing all agent actions before live execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Multi-Agent Swarm Cluster (Who Talks to Whom)
        item {

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("multi_agent_swarm_cluster_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).background(BrandEmerald.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = BrandEmerald)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Multi-Agent Swarm Cluster",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "4-Role Autonomous Collaboration Loop",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandEmerald
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = (if (isSwarmRunning) Color(0xFFF59E0B) else ProfitGreen).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(6.dp)
                                        .background(if (isSwarmRunning) Color(0xFFF59E0B) else ProfitGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSwarmRunning) "Swarm Active" else "Swarm Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSwarmRunning) Color(0xFFF59E0B) else ProfitGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pop-Up Store Theme Banner
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Active Pop-Up Store Theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = activeStoreTheme ?: "Pop-Up Store Awaiting Directive", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // The 4 Swarm Roles (Who Talks to Whom)
                    Text(text = "Swarm Handoff Pipeline:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwarmRolePill("1. Administrator AI", "The Boss / Orchestrator", Icons.Default.AdminPanelSettings, BrandEmerald)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        SwarmRolePill("2. Trend & Identity", "Theme & 5 Concepts", Icons.Default.TrendingUp, Color(0xFF38BDF8))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        SwarmRolePill("3. Creative Design", "Visual Prompts & Copy", Icons.Default.Palette, Color(0xFFA855F7))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        SwarmRolePill("4. Legal Compliance", "Strict Trademark Filter", Icons.Default.Gavel, ProfitGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swarm Status Banner
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = swarmStatusMessage ?: "Swarm Cluster Online",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: Run Swarm Loop & Reset Demo Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.autoStartSwarmCluster() },
                            enabled = !isSwarmRunning,
                            modifier = Modifier.weight(1f).testTag("trigger_swarm_loop_button")
                        ) {
                            if (isSwarmRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Collaborating...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Swarm Loop", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearAllDemoData() },
                            modifier = Modifier.testTag("purge_workspace_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purge Workspace Cache", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
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

        // Autonomous Swarm Operations Fleet Card
        item {
            val activeAgentsCount = autonomousAgents.count { it.isActive }
            val standbyAgentsCount = autonomousAgents.count { it.isStandby }
            val filteredSwarm = remember(autonomousAgents, selectedDepartmentFilter) {
                if (selectedDepartmentFilter == "ALL") {
                    autonomousAgents
                } else {
                    autonomousAgents.filter { it.department.code == selectedDepartmentFilter }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("autonomous_swarm_fleet_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BrandEmerald.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = BrandEmerald)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Autonomous Swarm Operations Fleet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "11 Business Roles • Active / Standby State Matrix",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ProfitGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$activeAgentsCount Active • $standbyAgentsCount Standby",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { showAdvancedSwarmHub = true },
                                modifier = Modifier.size(32.dp).testTag("open_advanced_swarm_hub_btn")
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open Full Swarm Hub",
                                    tint = BrandEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bulk Swarm Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setAllAgentsStatus(AgentStatus.ACTIVE) },
                            modifier = Modifier.weight(1f).testTag("bulk_activate_swarm_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = ProfitGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Activate All", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setAllAgentsStatus(AgentStatus.STANDBY) },
                            modifier = Modifier.weight(1f).testTag("bulk_standby_swarm_btn")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Standby All", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Department Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL", "EXEC", "INTEL", "BRAND", "PROD", "SALES", "SUPPLY", "GROWTH", "CRM", "FIN", "LEGAL", "DEVOPS").forEach { deptCode ->
                            FilterChip(
                                selected = selectedDepartmentFilter == deptCode,
                                onClick = { selectedDepartmentFilter = deptCode },
                                label = { Text(deptCode, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("swarm_dept_filter_$deptCode")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrolling Agent Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredSwarm.forEach { agent ->
                            val isAgentActive = agent.isActive
                            val statusColor = if (isAgentActive) ProfitGreen else Color(0xFFF59E0B)

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(260.dp)
                                    .testTag("dashboard_agent_card_${agent.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = agent.department.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandEmerald
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = statusColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (isAgentActive) "ACTIVE" else "STANDBY",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = agent.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = agent.role.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = agent.role.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { inspectingAgent = agent },
                                            modifier = Modifier.testTag("inspect_agent_${agent.id}")
                                        ) {
                                            Text("Inspect Role", fontSize = 11.sp)
                                        }

                                        Switch(
                                            checked = isAgentActive,
                                            onCheckedChange = { checked ->
                                                viewModel.setAgentStatus(agent.id, if (checked) AgentStatus.ACTIVE else AgentStatus.STANDBY)
                                            },
                                            modifier = Modifier.testTag("toggle_agent_switch_${agent.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Executive Operations Summary
        item {
            val podPending = decisions.count { it.agentSource.contains("POD") || it.description.contains("Tee") || it.description.contains("Hoodie") }
            val otherPending = (decisions.size - podPending).coerceAtLeast(0)

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
                        desc = "$pendingDecisionsCount items awaiting human authorization ($podPending POD, $otherPending operations)."
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

        // Autonomous Zero-Cost Storefront Deployment Card
        item {
            var targetCompanyName by remember { mutableStateOf("") }
            var targetBusinessNiche by remember { mutableStateOf("") }
            val isDeployingStorefront by viewModel.isDeployingStorefront.collectAsState()
            val storefrontDeploymentStatus by viewModel.storefrontDeploymentStatus.collectAsState()
            val storefronts by viewModel.storefronts.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("zero_cost_storefront_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ProfitGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = ProfitGreen)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Autonomous Storefront Creator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zero Cost Deployment • Multi-Agent Automation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProfitGreen
                                )
                            }
                        }
                        AssistChip(
                            onClick = { },
                            label = { Text("${storefronts.size} Live Stores") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Instruct the AI swarm to launch a turn-key storefront for any company or business. The agents code the repo on GitHub, link Stripe checkouts, generate zero-cost POD items, and pre-verify trademarks before your final approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetCompanyName,
                        onValueChange = { targetCompanyName = it },
                        label = { Text("Company or Business Name") },
                        placeholder = { Text("e.g. Solaris Living Studio") },
                        modifier = Modifier.fillMaxWidth().testTag("storefront_company_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetBusinessNiche,
                        onValueChange = { targetBusinessNiche = it },
                        label = { Text("Business Niche & Products") },
                        placeholder = { Text("e.g. Eco-Minimalist Ceramic Decor & Cotton Apparel") },
                        modifier = Modifier.fillMaxWidth().testTag("storefront_niche_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (storefrontDeploymentStatus != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = storefrontDeploymentStatus ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (targetCompanyName.isNotBlank()) {
                                viewModel.requestZeroCostStorefront(targetCompanyName, targetBusinessNiche)
                                targetCompanyName = ""
                                targetBusinessNiche = ""
                            }
                        },
                        enabled = !isDeployingStorefront && targetCompanyName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("deploy_storefront_button")
                    ) {
                        if (isDeployingStorefront) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assembling Swarm & Verifying Trademarks...")
                        } else {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Zero-Cost Storefront For Me")
                        }
                    }
                }
            }
        }

        // Multi-Agent Pitch & Trademark Protection Studio Card
        item {
            val isPitchingIdeas by viewModel.isPitchingIdeas.collectAsState()
            val trademarkChecks by viewModel.trademarkChecks.collectAsState()
            val agentMessages by viewModel.agentMessages.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("multi_agent_pitch_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Multi-Agent Idea Pitches & TM Shield",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "100% Non-Infringing • Pre-Screened Approval",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        AssistChip(
                            onClick = { },
                            label = { Text("Zero Risk") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Autonomous agents (Trend Scout, POD Designer, and Compliance) collaborate across an open message bus to invent new merchandise concepts. Every placement is checked against USPTO and copyright databases BEFORE reaching your approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.pitchNewProductIdeas() },
                            enabled = !isPitchingIdeas,
                            modifier = Modifier.weight(1f).testTag("swarm_pitch_ideas_button")
                        ) {
                            if (isPitchingIdeas) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Brainstorming...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pitch New Ideas", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToApprovals,
                            modifier = Modifier.weight(1f).testTag("view_pitches_approvals_button")
                        ) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pending Approvals", fontSize = 12.sp)
                        }
                    }

                    if (trademarkChecks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recent Trademark Clearance Audits",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        trademarkChecks.take(2).forEach { tm ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = tm.assetTitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(text = tm.trademarkCollisions, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(tm.clearanceHash.take(12), fontSize = 10.sp) },
                                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(12.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Autonomous Digital Products Studio ($0 Cost • 100% Net Profit)
        item {
            val digitalProducts by viewModel.digitalProducts.collectAsState()
            val isGeneratingDigitalProduct by viewModel.isGeneratingDigitalProduct.collectAsState()
            val digitalProductStatusMessage by viewModel.digitalProductStatusMessage.collectAsState()
            val secretStatus by viewModel.secretStatus.collectAsState()
            var selectedCategory by remember { mutableStateOf("PROMPT_GUIDE") }
            var customNicheInput by remember { mutableStateOf("") }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("digital_products_studio_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ProfitGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = ProfitGreen)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Digital Products Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\$0 COGS • 100% Pure Profit • Instant Download",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProfitGreen
                                )
                            }
                        }
                        AssistChip(
                            onClick = { },
                            label = { Text("${digitalProducts.size} Products") },
                            leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Instruct the AI swarm to formulate high-margin digital products (AI prompt books, Notion operating systems, vector asset kits, and micro-SaaS boilerplates). The agents write the files, create Stripe payment links, host on GitHub Pages, and audit trademarks before asking your approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Gateway Status Pills
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (secretStatus.isGitHubConfigured) ProfitGreen else Color(0xFFF59E0B), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GitHub: ${if (secretStatus.isGitHubConfigured) secretStatus.githubTokenMasked else "Blueprint Mode"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (secretStatus.isStripeConfigured) ProfitGreen else Color(0xFFF59E0B), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stripe: ${if (secretStatus.isStripeConfigured) secretStatus.stripeSecretKeyMasked else "Sandbox Links"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Digital Product Blueprint:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PROMPT_GUIDE" to "AI Prompts",
                            "NOTION_SYSTEM" to "Notion OS",
                            "GRAPHIC_PACK" to "Vector SVG",
                            "EBOOK" to "E-Book",
                            "CODE_TEMPLATE" to "Micro-SaaS"
                        ).forEach { (catKey, label) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = catKey },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customNicheInput,
                        onValueChange = { customNicheInput = it },
                        label = { Text("Target Topic or Audience") },
                        placeholder = { Text("e.g. AI Content Creation, Solopreneur Finance, Cyberpunk Art") },
                        modifier = Modifier.fillMaxWidth().testTag("digital_niche_input"),
                        singleLine = true
                    )

                    if (digitalProductStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = digitalProductStatusMessage ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.generateDigitalProduct(
                                niche = customNicheInput.ifBlank { "Solopreneur & AI Automation" },
                                category = selectedCategory
                            )
                            customNicheInput = ""
                        },
                        enabled = !isGeneratingDigitalProduct,
                        modifier = Modifier.fillMaxWidth().testTag("generate_digital_product_button")
                    ) {
                        if (isGeneratingDigitalProduct) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Formulating & Uploading Digital Product...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create & Upload Digital Product ($0 COGS)")
                        }
                    }

                    if (digitalProducts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Active Digital Products Catalog (100% Margin)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        digitalProducts.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("digital_product_item_${item.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(text = "${item.category} • ${item.fileFormat}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(
                                            text = "$${"%.2f".format(item.priceCents / 100.0)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sales: ${item.salesCount} (Earned: $${"%.2f".format(item.totalRevenueCents / 100.0)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        AssistChip(
                                            onClick = { },
                                            label = { Text("USPTO Cleared", fontSize = 10.sp) },
                                            leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(12.dp)) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.recordDigitalProductSale(item.id) },
                                            modifier = Modifier.weight(1f).testTag("test_sale_button_${item.id}")
                                        ) {
                                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Record Sale (+$${"%.2f".format(item.priceCents / 100.0)})", fontSize = 11.sp)
                                        }

                                        if (item.status != "PUBLISHED_LIVE") {
                                            FilledTonalButton(
                                                onClick = { viewModel.publishDigitalProduct(item.id) },
                                                modifier = Modifier.testTag("publish_live_button_${item.id}")
                                            ) {
                                                Text("Publish Live", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Autonomous Execution Entry Console
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("gemini_business_operations_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Business Operations Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "GEMINI 2.5/3.5",
                                color = BrandEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Process natural language founder inputs into structured business operations and prioritized agent task suggestions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Quick Prompt Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Launch Q4 Vintage Apparel on Printify",
                            "Viral Organic TikTok & Pinterest Strategy",
                            "Double-Entry Ledger Fee & Profit Audit",
                            "USPTO Trademark Clearance for New Brand"
                        ).forEach { quickPrompt ->
                            SuggestionChip(
                                onClick = { businessPrompt = quickPrompt },
                                label = { Text(quickPrompt, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = businessPrompt,
                        onValueChange = { businessPrompt = it },
                        placeholder = { Text("Enter business idea, scaling directive, or market inquiry...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("business_objective_input"),
                        maxLines = 4,
                        supportingText = {
                            if (businessOperationStatusMessage != null) {
                                Text(businessOperationStatusMessage!!, color = BrandEmerald, fontSize = 11.sp)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (businessPrompt.isNotBlank()) {
                                    viewModel.processBusinessPrompt(businessPrompt)
                                }
                            },
                            enabled = !isProcessingBusinessPrompt && businessPrompt.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("gemini_analyze_button")
                        ) {
                            if (isProcessingBusinessPrompt) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyzing...")
                            } else {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Operations")
                            }
                        }

                        if (businessAnalysis != null) {
                            OutlinedButton(
                                onClick = { viewModel.clearBusinessAnalysis() },
                                modifier = Modifier.testTag("clear_analysis_btn")
                            ) {
                                Text("Reset")
                            }
                        }
                    }

                    // Display Generated Analysis & Task Suggestions
                    if (businessAnalysis != null) {
                        val analysis = businessAnalysis!!
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Executive Strategy Summary",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ProfitGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = analysis.modelUsed,
                                            color = ProfitGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = analysis.executiveSummary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Niche: ${analysis.nicheFocus}", fontSize = 11.sp) }
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(analysis.riskAssessment.take(28) + "...", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Generated Task Suggestions (${analysis.taskSuggestions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        analysis.taskSuggestions.forEach { suggestion ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("suggestion_card_${suggestion.id.take(6)}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = suggestion.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BrandEmerald.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${suggestion.estimatedMarginPercent}% Margin",
                                                color = BrandEmerald,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Assignee: ${suggestion.assignedAgentRole}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "• Priority: ${suggestion.priority}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (suggestion.priority == "HIGH") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "• $0 COGS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ProfitGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = suggestion.detailedInstruction,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.enqueueSuggestedTask(suggestion) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Enqueue", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.enqueueSuggestionToStandby(suggestion) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Standby", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.sendSuggestionToApprovals(suggestion) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approval", fontSize = 11.sp)
                                        }
                                    }
                                }
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

    inspectingAgent?.let { agent ->
        AutonomousAgentInspectorDialog(
            agent = agent,
            onDismiss = { inspectingAgent = null },
            onToggleStatus = { nextStatus ->
                viewModel.setAgentStatus(agent.id, nextStatus)
                inspectingAgent = agent.copy(status = nextStatus)
            },
            onDispatchDirective = { directive ->
                viewModel.dispatchAdHocTask(agent.id, "Direct Console Dispatch", directive)
            }
        )
    }

    if (showAdvancedSwarmHub) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAdvancedSwarmHub = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedWorkspaceDashboard(
                    viewModel = viewModel,
                    onClose = { showAdvancedSwarmHub = false }
                )
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
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }
    val finalModifier = if (testTag != null) clickableModifier.testTag(testTag) else clickableModifier

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = finalModifier
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

@Composable
fun SwarmRolePill(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

