package com.opensource.autonomous.agent.ui.screens

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
import com.opensource.autonomous.agent.data.DecisionEntity
import com.opensource.autonomous.agent.data.OperationalRole
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun ApprovalsScreen(
    viewModel: MainViewModel,
    onNavigateToMockupStudio: () -> Unit
) {
    val decisions by viewModel.decisions.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var feedbackDialogDecision by remember { mutableStateOf<DecisionEntity?>(null) }

    val activeEndpoint = settings?.backendServerEndpoint ?: "http://127.0.0.1:8080"

    val filteredDecisions = when (selectedFilter) {
        "POD" -> decisions.filter { it.agentSource.contains("POD") || it.description.contains("Tee") || it.description.contains("Hoodie") }
        "DROPSHIP" -> decisions.filter { it.agentSource.contains("Dropship") || it.description.contains("Shortlist") }
        "SUPPORT" -> decisions.filter { it.agentSource.contains("Customer") || it.description.contains("Refund") }
        else -> decisions
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Live Approvals Queue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bound to live GET $activeEndpoint/api/decisions. Single-click approve/deny dispatches immediate REST action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${decisions.size})") },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "POD",
                        onClick = { selectedFilter = "POD" },
                        label = { Text("POD") },
                        modifier = Modifier.testTag("filter_pod_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "DROPSHIP",
                        onClick = { selectedFilter = "DROPSHIP" },
                        label = { Text("Dropship") },
                        modifier = Modifier.testTag("filter_dropship_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "SUPPORT",
                        onClick = { selectedFilter = "SUPPORT" },
                        label = { Text("Support") },
                        modifier = Modifier.testTag("filter_support_chip")
                    )
                }

                // Live status indicator row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isLiveConnected) ProfitGreen else Color(0xFFF59E0B),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLiveConnected) "Live Endpoint: $activeEndpoint" else "Local DB Standby ($activeEndpoint offline)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { viewModel.syncWithLiveBackend() },
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("refresh_live_approvals_btn")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Fetch", fontSize = 11.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (filteredDecisions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProfitGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Approval Queue Clean",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No pending actions awaiting human override. Autonomous agents continue monitoring live operations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(filteredDecisions, key = { it.id }) { decision ->
                    DecisionApprovalCard(
                        viewModel = viewModel,
                        decision = decision,
                        onApprove = { viewModel.approveDecision(decision.id) },
                        onDeny = { viewModel.rejectDecision(decision.id) },
                        onRejectWithFeedback = { feedbackDialogDecision = decision },
                        onInspectMockup = { onNavigateToMockupStudio() }
                    )
                }
            }
        }
    }

    // Feedback Dialog with Regenerate & Dispatch
    feedbackDialogDecision?.let { decision ->
        var feedbackInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { feedbackDialogDecision = null },
            title = {
                Text(
                    text = "Deny & Auto-Regenerate",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Instruct the agent swarm on specific adjustments (e.g. style change, color palette, pricing margins). A compliant version will be regenerated immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        placeholder = { Text("e.g. Remove distressed effect, switch font to clean sans-serif, warmer palette") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("feedback_reason_input"),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackInput.isNotBlank()) {
                            viewModel.rejectWithFeedback(decision, feedbackInput)
                            feedbackDialogDecision = null
                        }
                    },
                    enabled = feedbackInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("submit_feedback_regenerate_button")
                ) {
                    Text("Regenerate & Dispatch")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { feedbackDialogDecision = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DecisionApprovalCard(
    viewModel: MainViewModel,
    decision: DecisionEntity,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onRejectWithFeedback: () -> Unit,
    onInspectMockup: () -> Unit
) {
    val payload = viewModel.parseDecisionPayload(decision.payloadJson)
    val formattedTime = viewModel.formatLiveTimestamp(decision.timestamp)
    val relativeTime = viewModel.formatRelativeTime(decision.timestamp)
    val hmacSignature = viewModel.computeDecisionHmac(decision)
    val operationalRole = remember(decision.agentSource) { OperationalRole.fromRoleString(decision.agentSource) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_card_${decision.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${operationalRole.department.code} • ${decision.agentSource.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ProfitGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$0 Upfront Cost",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfitGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = decision.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Real parsed system clock timestamp
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$formattedTime ($relativeTime)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zero-Trust Cryptographic HMAC Verification Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ProfitGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero-Trust HMAC SHA-256 Enclave: $hmacSignature",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ProfitGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Economic Math Breakdown from Real JSON Payload
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Retail:", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", payload.priceCents / 100.0)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Supplier/POD Cost:", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", payload.costCents / 100.0)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Est. Platform Fees:", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", payload.feeCents / 100.0)}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Profit per Sale:", fontWeight = FontWeight.Bold, color = ProfitGreen, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "+$${String.format("%.2f", payload.profitCents / 100.0)} (${String.format("%.1f", payload.profitMarginPercent)}%)",
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compliance Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = BrandEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${payload.compliance} • Zero-Upfront Verified",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandEmerald
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: Dispatches immediate live POST requests
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier.weight(1f).testTag("deny_btn_${decision.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Deny")
                }

                OutlinedButton(
                    onClick = onRejectWithFeedback,
                    modifier = Modifier.weight(1f).testTag("reject_feedback_btn_${decision.id}")
                ) {
                    Text("Feedback")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.5f).testTag("approve_btn_${decision.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}
