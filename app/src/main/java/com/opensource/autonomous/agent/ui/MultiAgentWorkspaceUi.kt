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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensource.autonomous.agent.data.DecisionEntity
import com.opensource.autonomous.agent.data.LogEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWorkspaceDashboard(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Control Center", "Decision Engine", "Centralized Logs")

    val decisions by viewModel.decisions.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var rawInputPrompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AUTONOMOUS AGENT CORE v2.0",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
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
                0 -> ControlCenterView(
                    prompt = rawInputPrompt,
                    onPromptChange = { rawInputPrompt = it },
                    onDeploy = {
                        viewModel.initializeBusiness(rawInputPrompt)
                        rawInputPrompt = ""
                    }
                )
                1 -> DecisionEngineView(decisions, viewModel::approveDecision, viewModel::rejectDecision)
                2 -> CentralizedLogsView(logs)
            }
        }
    }
}

@Composable
fun ControlCenterView(prompt: String, onPromptChange: (String) -> Unit, onDeploy: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Operational Directive Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("directive_prompt_input"),
                    placeholder = { Text("Enter business idea, pivot directive, or automation request script...") },
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
                    Text("DEPLOY MULTI-AGENT SWARM")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Active Infrastructure Modules", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val agents = listOf(
                "CEO" to Icons.Default.AccountBox,
                "Research" to Icons.Default.Search,
                "Product" to Icons.Default.Build,
                "Marketing" to Icons.Default.Share,
                "Content" to Icons.Default.Edit,
                "Support" to Icons.Default.Call,
                "Finance" to Icons.Default.Menu
            )
            items(agents) { (name, icon) ->
                AgentStatusRow(name = name, icon = icon)
            }
        }
    }
}

@Composable
fun AgentStatusRow(name: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color(0xFF4CAF50), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("STANDBY", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun DecisionEngineView(decisions: List<DecisionEntity>, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    if (decisions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No actions awaiting human override authorization.", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(decisions) { decision ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("decision_card_${decision.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CRITICAL SECURE OVERRIDE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Module: ${decision.agentSource}", fontWeight = FontWeight.SemiBold)
                        Text("Action: ${decision.description}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { onReject(decision.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("deny_pipeline_button_${decision.id}")
                            ) {
                                Text("DENY PIPELINE")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            ElevatedButton(
                                onClick = { onApprove(decision.id) },
                                modifier = Modifier.testTag("execute_action_button_${decision.id}")
                            ) {
                                Text("EXECUTE ACTION")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CentralizedLogsView(logs: List<LogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
            .testTag("terminal_logs_column")
    ) {
        items(logs) { log ->
            Text(
                text = "[${log.timestamp}] [Agent-${log.agentId}] >> ${log.message}",
                color = Color(0xFF00FF66),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
