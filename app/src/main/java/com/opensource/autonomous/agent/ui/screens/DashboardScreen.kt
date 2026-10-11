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
    val autonomousAgents by viewModel.autonomousAgents.collectAsState()
    var inspectingAgent by remember { mutableStateOf<AutonomousAgent?>(null) }
    var selectedDepartmentFilter by remember { mutableStateOf("ALL") }
    var showAdvancedSwarmHub by remember { mutableStateOf(false) }

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
