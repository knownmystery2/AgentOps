package com.opensource.autonomous.agent.ui

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
import com.opensource.autonomous.agent.data.*
import com.opensource.autonomous.agent.network.SwarmAgentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWorkspaceDashboard(
    viewModel: MainViewModel,
    onClose: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Live Swarm Hub", "Decision Engine", "Centralized Logs")

    val decisions by viewModel.decisions.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var rawInputPrompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Hub")
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "LIVE AGENT SWARM CORE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 16.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncWithLiveBackend() },
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("swarm_sync_btn")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Swarm")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LiveSwarmControlCenterView(
                    viewModel = viewModel,
                    prompt = rawInputPrompt,
                    onPromptChange = { rawInputPrompt = it },
                    onDeploy = {
                        viewModel.initializeBusiness(rawInputPrompt)
                        rawInputPrompt = ""
                    }
                )
                1 -> DecisionEngineView(viewModel, decisions, viewModel::approveDecision, viewModel::rejectDecision)
                2 -> CentralizedLogsView(viewModel, logs)
            }
        }
    }
}

@Composable
fun LiveSwarmControlCenterView(
    viewModel: MainViewModel,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onDeploy: () -> Unit
) {
    val ultraAgents by viewModel.liveSwarmAgents.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeEndpoint = settings?.backendServerEndpoint ?: "http://127.0.0.1:8080"
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedAgentForDispatch by remember { mutableStateOf<SwarmAgentStatus?>(null) }

    val filteredAgents = when (selectedStatusFilter) {
        "ACTIVE" -> ultraAgents.filter { it.status == "ACTIVE" || it.status == "RUNNING" }
        "STANDBY" -> ultraAgents.filter { it.status == "STANDBY" || it.status == "IDLE" }
        else -> ultraAgents
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ULTRA Operational Directive Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Single ULTRA agent (Gemini 4 Argon) connected to $activeEndpoint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier.fillMaxWidth().height(110.dp).testTag("directive_prompt_input"),
                        placeholder = { Text("Enter ULTRA creative, canvas, or Printify POD Pop-Up directive...") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDeploy,
                        modifier = Modifier.fillMaxWidth().testTag("deploy_swarm_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXECUTE ULTRA CYCLE")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ULTRA Single-Agent Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (isLiveConnected) "Live Endpoint: $activeEndpoint" else "Local SQLite Engine ($activeEndpoint standby)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B)).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ULTRA Online (${ultraAgents.size})",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Swarm status filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "ACTIVE", "STANDBY").forEach { filter ->
                    FilterChip(
                        selected = selectedStatusFilter == filter,
                        onClick = { selectedStatusFilter = filter },
                        label = { Text(filter) },
                        modifier = Modifier.testTag("swarm_filter_$filter")
                    )
                }
            }
        }

        items(filteredAgents, key = { it.id }) { agent ->
            LiveAgentStatusCard(
                agent = agent,
                viewModel = viewModel,
                onDispatchDirective = { selectedAgentForDispatch = agent }
            )
        }
    }

    // Direct Agent Instruction Dialog
    selectedAgentForDispatch?.let { agent ->
        var customDirective by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedAgentForDispatch = null },
            title = { Text("Dispatch Directive to ${agent.name}") },
            text = {
                Column {
                    Text("Role: ${agent.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDirective,
                        onValueChange = { customDirective = it },
                        placeholder = { Text("Enter ad-hoc instructions for this agent...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customDirective.isNotBlank()) {
                            viewModel.dispatchAdHocTask(agent.id, "Direct Dispatch", customDirective)
                            selectedAgentForDispatch = null
                        }
                    },
                    enabled = customDirective.isNotBlank()
                ) {
                    Text("Dispatch")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedAgentForDispatch = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LiveAgentStatusCard(
    agent: SwarmAgentStatus,
    viewModel: MainViewModel,
    onDispatchDirective: () -> Unit
) {
    val operationalRole = remember(agent.role) { OperationalRole.fromRoleString(agent.role) }
    val currentStatus = remember(agent.status) { AgentStatus.fromString(agent.status) }
    val isActive = currentStatus.isActive
    val statusColor = if (isActive) ProfitGreen else Color(0xFFF59E0B)
    val icon = when {
        agent.name.contains("Orchestrator") || agent.name.contains("CEO") -> Icons.Default.AccountBox
        agent.name.contains("Scout") || agent.name.contains("Research") -> Icons.Default.Search
        agent.name.contains("Brand") -> Icons.Default.AutoAwesome
        agent.name.contains("Design") || agent.name.contains("Product") -> Icons.Default.Build
        agent.name.contains("Marketing") -> Icons.Default.Share
        agent.name.contains("Listing") || agent.name.contains("Content") -> Icons.Default.Edit
        agent.name.contains("Dropship") || agent.name.contains("Sourcing") -> Icons.Default.ShoppingCart
        agent.name.contains("Customer") || agent.name.contains("Support") -> Icons.Default.Call
        agent.name.contains("Finance") -> Icons.Default.AttachMoney
        agent.name.contains("Compliance") -> Icons.Default.Shield
        agent.name.contains("Reliability") -> Icons.Default.CloudSync
        else -> Icons.Default.Hub
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("agent_card_${agent.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(agent.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${operationalRole.title} • ${operationalRole.department.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentStatus.label.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Operational Role Department & Responsibilities
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = operationalRole.department.departmentName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Model: ${agent.model}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = operationalRole.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Directive: ${agent.currentTask}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Heartbeat: ${viewModel.formatRelativeTime(agent.lastPingTimestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isActive) "Status: LIVE ACTIVE" else "Status: STANDBY POOL",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Toggle Button (Active <-> Standby)
                TextButton(
                    onClick = {
                        val nextStatus = if (isActive) AgentStatus.STANDBY else AgentStatus.ACTIVE
                        viewModel.setAgentStatus(agent.id, nextStatus)
                    },
                    modifier = Modifier.testTag("toggle_status_${agent.id}")
                ) {
                    Icon(
                        if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isActive) Color(0xFFF59E0B) else ProfitGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isActive) "Switch to Standby" else "Switch to Active",
                        fontSize = 12.sp,
                        color = if (isActive) Color(0xFFF59E0B) else ProfitGreen
                    )
                }

                TextButton(onClick = onDispatchDirective) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dispatch Directive", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DecisionEngineView(
    viewModel: MainViewModel,
    decisions: List<DecisionEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (decisions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Zero Pending Decisions. Swarm is autonomous.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(decisions, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("engine_decision_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.agentSource, fontWeight = FontWeight.Bold, color = BrandEmerald)
                            Text(
                                text = viewModel.formatRelativeTime(item.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onReject(item.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("REJECT")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { onApprove(item.id) }) {
                                Text("APPROVE")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CentralizedLogsView(viewModel: MainViewModel, logs: List<LogEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            color = MaterialTheme.colorScheme.primary
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
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
