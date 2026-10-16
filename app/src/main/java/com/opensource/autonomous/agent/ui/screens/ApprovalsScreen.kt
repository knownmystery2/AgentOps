package com.opensource.autonomous.agent.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.DecisionEntity
import com.opensource.autonomous.agent.data.GodAuditVerdictEntity
import com.opensource.autonomous.agent.data.OperationalRole
import com.opensource.autonomous.agent.ui.ApprovalRegisterStats
import com.opensource.autonomous.agent.ui.ApprovalRegisterViewModel
import com.opensource.autonomous.agent.ui.MainViewModel
import com.opensource.autonomous.agent.ui.PendingBusinessActionProposal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalsScreen(
    viewModel: MainViewModel,
    approvalRegisterViewModel: ApprovalRegisterViewModel = viewModel.approvalRegisterViewModel,
    onNavigateToMockupStudio: () -> Unit
) {
    val context = LocalContext.current
    val registerStats by approvalRegisterViewModel.registerStats.collectAsState()
    val filteredDecisions by approvalRegisterViewModel.filteredDecisions.collectAsState()
    val selectedFilter by approvalRegisterViewModel.selectedFilter.collectAsState()
    val searchQuery by approvalRegisterViewModel.searchQuery.collectAsState()
    val isProcessingAction by approvalRegisterViewModel.isProcessingAction.collectAsState()
    val statusMessage by approvalRegisterViewModel.statusMessage.collectAsState()

    val decisions by viewModel.decisions.collectAsState()
    val isLiveConnected by viewModel.isLiveConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val godHaltedVerdicts by viewModel.godHaltedVerdicts.collectAsState()
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()

    var showCreateActionDialog by remember { mutableStateOf(false) }
    var feedbackDialogDecision by remember { mutableStateOf<DecisionEntity?>(null) }
    var rejectReasonDecision by remember { mutableStateOf<DecisionEntity?>(null) }
    var selectedActionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val activeEndpoint = settings?.backendServerEndpoint ?: "http://127.0.0.1:8080"

    // Toast feedback on status changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            approvalRegisterViewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Main Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Approval Register",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandEmerald.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, BrandEmerald.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "$0 COGS Rule",
                                    color = BrandEmerald,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Central confirmation register for agent-proposed transactions, deploys & actions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showCreateActionDialog = true },
                            modifier = Modifier.testTag("create_action_proposal_btn")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "New Action", tint = BrandEmerald)
                        }

                        IconButton(
                            onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedActionIds = emptySet()
                            },
                            modifier = Modifier.testTag("toggle_selection_mode_btn")
                        ) {
                            Icon(
                                if (isSelectionMode) Icons.Default.ChecklistRtl else Icons.Default.Checklist,
                                contentDescription = "Batch Select",
                                tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // KPI Stat Ribbon
                ApprovalRegisterStatsRibbon(
                    stats = registerStats,
                    onPendingClick = { approvalRegisterViewModel.setFilter("PENDING") },
                    onHighRiskClick = { approvalRegisterViewModel.setFilter("HIGH_RISK") },
                    onApprovedClick = { approvalRegisterViewModel.setFilter("APPROVED") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { approvalRegisterViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by SKU, Agent, Action ID or Keyword...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { approvalRegisterViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("approval_register_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { approvalRegisterViewModel.setFilter("ALL") },
                        label = { Text("All (${decisions.size})") },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { approvalRegisterViewModel.setFilter("PENDING") },
                        label = { Text("Pending (${registerStats.pendingCount})") },
                        modifier = Modifier.testTag("filter_pending_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "TRANSACTIONS",
                        onClick = { approvalRegisterViewModel.setFilter("TRANSACTIONS") },
                        label = { Text("Transactions (${pendingTransactions.size})") },
                        modifier = Modifier.testTag("filter_transactions_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "GOD_FLAGGED",
                        onClick = { approvalRegisterViewModel.setFilter("GOD_FLAGGED") },
                        label = { Text("Flagged by God (${godHaltedVerdicts.size})") },
                        modifier = Modifier.testTag("filter_god_chip"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "HIGH_RISK",
                        onClick = { approvalRegisterViewModel.setFilter("HIGH_RISK") },
                        label = { Text("High Risk (${registerStats.highRiskPendingCount})") },
                        modifier = Modifier.testTag("filter_high_risk_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "POD",
                        onClick = { approvalRegisterViewModel.setFilter("POD") },
                        label = { Text("Print on Demand") },
                        modifier = Modifier.testTag("filter_pod_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "DROPSHIP",
                        onClick = { approvalRegisterViewModel.setFilter("DROPSHIP") },
                        label = { Text("Dropship") },
                        modifier = Modifier.testTag("filter_dropship_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "APPROVED",
                        onClick = { approvalRegisterViewModel.setFilter("APPROVED") },
                        label = { Text("Approved (${registerStats.approvedCount})") },
                        modifier = Modifier.testTag("filter_approved_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == "REJECTED",
                        onClick = { approvalRegisterViewModel.setFilter("REJECTED") },
                        label = { Text("Rejected (${registerStats.rejectedCount})") },
                        modifier = Modifier.testTag("filter_rejected_chip")
                    )
                }

                // Batch Selection Bar (Visible when in Selection Mode)
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedActionIds.size} Selected",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        val pendingIds = filteredDecisions.filter { it.status == "PENDING" }.map { it.id }.toSet()
                                        selectedActionIds = if (selectedActionIds.size == pendingIds.size) emptySet() else pendingIds
                                    }
                                ) {
                                    Text("Select All Pending", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (selectedActionIds.isNotEmpty()) {
                                            approvalRegisterViewModel.batchConfirm(selectedActionIds.toList())
                                            selectedActionIds = emptySet()
                                            isSelectionMode = false
                                        }
                                    },
                                    enabled = selectedActionIds.isNotEmpty() && !isProcessingAction,
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("batch_confirm_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Batch Confirm", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (selectedActionIds.isNotEmpty()) {
                                            approvalRegisterViewModel.batchReject(selectedActionIds.toList())
                                            selectedActionIds = emptySet()
                                            isSelectionMode = false
                                        }
                                    },
                                    enabled = selectedActionIds.isNotEmpty() && !isProcessingAction,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("batch_reject_btn")
                                ) {
                                    Text("Batch Reject", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Live Connection Status Strip
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
                            text = if (isLiveConnected) "Live Autonomous Gateway: $activeEndpoint" else "Local SQLite Enclave ($activeEndpoint standby)",
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateActionDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Propose Action") },
                text = { Text("Propose Action") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("propose_action_fab")
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                selectedFilter == "TRANSACTIONS" -> {
                    PendingTransactionsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                selectedFilter == "GOD_FLAGGED" -> {
                    if (godHaltedVerdicts.isEmpty()) {
                        EmptyRegisterState(
                            icon = Icons.Default.Shield,
                            title = "No Halted Tasks in Security Register",
                            subtitle = "The God Agent has not intercepted any prompt injections, rogue logic, or hazardous operations."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(godHaltedVerdicts, key = { it.id }) { verdict ->
                                GodApprovalRegisterCard(
                                    verdict = verdict,
                                    onApproveOverride = { viewModel.approveGodHaltedTask(verdict.id) },
                                    onApplyRemediation = { viewModel.approveGodHaltedTask(verdict.id) },
                                    onReject = { viewModel.rejectGodHaltedTask(verdict.id) }
                                )
                            }
                        }
                    }
                }

                filteredDecisions.isEmpty() -> {
                    EmptyRegisterState(
                        icon = Icons.Default.CheckCircle,
                        title = "Approval Register Clear",
                        subtitle = if (searchQuery.isNotEmpty()) "No pending actions matched '${searchQuery}'." else "All agent proposals and transactions have been confirmed. Swarm operations active."
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(filteredDecisions, key = { it.id }) { decision ->
                            val isSelected = selectedActionIds.contains(decision.id)
                            ApprovalRegisterProposalCard(
                                viewModel = viewModel,
                                decision = decision,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedActionIds = if (isSelected) {
                                        selectedActionIds - decision.id
                                    } else {
                                        selectedActionIds + decision.id
                                    }
                                },
                                onApprove = { approvalRegisterViewModel.confirmAction(decision.id) },
                                onDeny = { rejectReasonDecision = decision },
                                onRejectWithFeedback = { feedbackDialogDecision = decision },
                                onInspectMockup = onNavigateToMockupStudio
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Create New Business Action Proposal
    if (showCreateActionDialog) {
        CreatePendingActionDialog(
            onDismiss = { showCreateActionDialog = false },
            onCreate = { proposal ->
                approvalRegisterViewModel.createPendingAction(proposal)
                showCreateActionDialog = false
            }
        )
    }

    // Modal 2: Rejection Reason Dialog
    rejectReasonDecision?.let { decision ->
        var reasonText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectReasonDecision = null },
            title = { Text("Reject Business Action", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Specify rejection rationale to record in the compliance audit trail:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        placeholder = { Text("e.g. Unfavorable margin, Trademark risk, Outside brand theme") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("reject_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        approvalRegisterViewModel.rejectAction(decision.id, reasonText.ifBlank { null })
                        rejectReasonDecision = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reject_btn")
                ) {
                    Text("Reject Action")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rejectReasonDecision = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 3: Feedback Dialog with Auto-Regenerate
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

/**
 * KPI Stat Ribbon for the Approval Register
 */
@Composable
fun ApprovalRegisterStatsRibbon(
    stats: ApprovalRegisterStats,
    onPendingClick: () -> Unit,
    onHighRiskClick: () -> Unit,
    onApprovedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pending Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .clickable { onPendingClick() }
                .width(130.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.pendingCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            }
        }

        // Zero-Cost Verified Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ProfitGreen.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.3f)),
            modifier = Modifier.width(130.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zero Cost", style = MaterialTheme.typography.labelSmall, color = ProfitGreen)
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$0 COGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProfitGreen
                )
            }
        }

        // High Risk Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (stats.highRiskPendingCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (stats.highRiskPendingCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .clickable { onHighRiskClick() }
                .width(130.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("High Risk", style = MaterialTheme.typography.labelSmall, color = if (stats.highRiskPendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = if (stats.highRiskPendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.highRiskPendingCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (stats.highRiskPendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Approved Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandEmerald.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, BrandEmerald.copy(alpha = 0.3f)),
            modifier = Modifier
                .clickable { onApprovedClick() }
                .width(130.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Approved", style = MaterialTheme.typography.labelSmall, color = BrandEmerald)
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.approvedCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandEmerald
                )
            }
        }
    }
}

/**
 * Enterprise Proposal Card for the Approval Register
 */
@Composable
fun ApprovalRegisterProposalCard(
    viewModel: MainViewModel,
    decision: DecisionEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onRejectWithFeedback: () -> Unit,
    onInspectMockup: () -> Unit
) {
    val context = LocalContext.current
    val payload = viewModel.parseDecisionPayload(decision.payloadJson)
    val formattedTime = viewModel.formatLiveTimestamp(decision.timestamp)
    val relativeTime = viewModel.formatRelativeTime(decision.timestamp)
    val hmacSignature = viewModel.computeDecisionHmac(decision)
    val operationalRole = remember(decision.agentSource) { OperationalRole.fromRoleString(decision.agentSource) }
    var isExpanded by remember { mutableStateOf(false) }

    val isPending = decision.status == "PENDING"
    val isApproved = decision.status == "APPROVED"
    val isRejected = decision.status == "REJECTED"

    // Risk Level detection from payload
    val riskLevel = when {
        decision.payloadJson.contains("\"riskLevel\":\"CRITICAL\"") -> "CRITICAL"
        decision.payloadJson.contains("\"riskLevel\":\"HIGH\"") -> "HIGH"
        decision.payloadJson.contains("\"riskLevel\":\"MEDIUM\"") -> "MEDIUM"
        else -> "LOW"
    }

    val riskColor = when (riskLevel) {
        "CRITICAL" -> Color(0xFFDC2626)
        "HIGH" -> Color(0xFFEA580C)
        "MEDIUM" -> Color(0xFFD97706)
        else -> ProfitGreen
    }

    val isPodAction = decision.agentSource.contains("Creative", ignoreCase = true) ||
            decision.description.contains("POD", ignoreCase = true) ||
            decision.description.contains("Mockup", ignoreCase = true) ||
            decision.description.contains("Tee", ignoreCase = true) ||
            decision.description.contains("Hoodie", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_card_${decision.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Selection Checkbox + Source Agent + Risk Badge + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode && isPending) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

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

                    Spacer(modifier = Modifier.width(6.dp))

                    // Risk Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = riskColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$riskLevel RISK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isApproved -> ProfitGreen.copy(alpha = 0.15f)
                        isRejected -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = decision.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isApproved -> ProfitGreen
                            isRejected -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action ID and Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = decision.id,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$formattedTime ($relativeTime)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = decision.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Zero-Cost Verified Banner & Economic Ledger Breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zero-Cost Rule ($0 Founder COGS)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ProfitGreen)
                        }
                        Text(
                            text = "100% Net Margin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ProfitGreen
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Founder Upfront Cost:", style = MaterialTheme.typography.bodySmall)
                        Text("$0.00", fontWeight = FontWeight.Bold, color = ProfitGreen, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Retail / Est. Revenue:", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", payload.priceCents / 100.0)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Projected Net Profit:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("+$${String.format("%.2f", payload.profitCents / 100.0)}", fontWeight = FontWeight.Bold, color = ProfitGreen, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zero-Trust Cryptographic HMAC Verification
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero-Trust HMAC: $hmacSignature",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ProfitGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expandable Telemetry JSON Inspector
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Payload Telemetry" else "Inspect Payload Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Raw JSON Schema", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Payload", decision.payloadJson))
                                        Toast.makeText(context, "Payload copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = decision.payloadJson,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (isPending) {
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
                        Text("Deny", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRejectWithFeedback,
                        modifier = Modifier.weight(1f).testTag("reject_feedback_btn_${decision.id}")
                    ) {
                        Text("Feedback", fontSize = 12.sp)
                    }

                    if (isPodAction) {
                        IconButton(
                            onClick = onInspectMockup,
                            modifier = Modifier.testTag("inspect_mockup_btn_${decision.id}")
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Inspect Mockup", tint = BrandEmerald)
                        }
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1.4f).testTag("approve_btn_${decision.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm", fontSize = 12.sp)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isApproved) ProfitGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isApproved) "✓ Confirmed & Dispatched into Live Ledger Execution" else "✗ Rejected by Compliance Overseer",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isApproved) ProfitGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal to Propose / Register a New Business Action into SQLite Room DB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePendingActionDialog(
    onDismiss: () -> Unit,
    onCreate: (PendingBusinessActionProposal) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var agentSource by remember { mutableStateOf("Creative Design Agent") }
    var actionType by remember { mutableStateOf("POD_PUBLISH") }
    var targetResource by remember { mutableStateOf("storefront/catalog") }
    var estimatedRevenue by remember { mutableStateOf("29.99") }
    var riskLevel by remember { mutableStateOf("LOW") }

    val agentOptions = listOf(
        "Creative Design Agent",
        "Storefront Deployment Agent",
        "Dropship Fulfillment Agent",
        "Market Intelligence Agent",
        "Accounting Ledger Agent",
        "Customer Support Sentinel"
    )

    val actionOptions = listOf(
        "POD_PUBLISH",
        "STOREFRONT_DEPLOY",
        "SUPPLIER_SYNC",
        "MARKETING_DISPATCH",
        "LEDGER_TRANSFER",
        "GOD_REMEDIATION"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Register New Business Action", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Text(
                    text = "Propose an action to the Approval Register. Enforces the $0 upfront COGS rule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Proposal Title") },
                    placeholder = { Text("e.g. Publish Oversized Streetwear Tee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_action_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Action Description") },
                    placeholder = { Text("Details of design, SKU, or deployment") },
                    modifier = Modifier.fillMaxWidth().height(80.dp).testTag("new_action_desc_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetResource,
                        onValueChange = { targetResource = it },
                        label = { Text("Target Resource") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estimatedRevenue,
                        onValueChange = { estimatedRevenue = it },
                        label = { Text("Target Retail ($)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Source Agent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    agentOptions.forEach { agent ->
                        FilterChip(
                            selected = agentSource == agent,
                            onClick = { agentSource = agent },
                            label = { Text(agent, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Action Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actionOptions.forEach { action ->
                        FilterChip(
                            selected = actionType == action,
                            onClick = { actionType = action },
                            label = { Text(action, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val revCents = (estimatedRevenue.toDoubleOrNull() ?: 29.99 * 100).toLong()
                        onCreate(
                            PendingBusinessActionProposal(
                                title = title,
                                agentSource = agentSource,
                                actionType = actionType,
                                description = description.ifBlank { "Autonomous business action registered via Approval Register" },
                                targetResource = targetResource,
                                estimatedCostCents = 0L, // Always $0 COGS
                                estimatedRevenueCents = revCents,
                                riskLevel = riskLevel,
                                zeroCostVerified = true
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("submit_create_action_proposal_btn")
            ) {
                Text("Submit to Register")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Empty Register Placeholder
 */
@Composable
fun EmptyRegisterState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = ProfitGreen,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GodApprovalRegisterCard(
    verdict: GodAuditVerdictEntity,
    onApproveOverride: () -> Unit,
    onApplyRemediation: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("god_register_card_${verdict.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "FLAGGED BY GOD OVERSEER",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(verdict.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target: ${verdict.targetResource}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Source Agent: ${verdict.sourceAgent} • Action: ${verdict.actionType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "🚨 Hazard: ${verdict.hazardDetected ?: "Unknown anomaly"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (!verdict.remediation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Remediation:\n${verdict.remediation}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApproveOverride,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1.1f).testTag("approve_override_btn_${verdict.id}")
                ) {
                    Text("Approve Override", fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = onApplyRemediation,
                    modifier = Modifier.weight(1f).testTag("apply_remediation_btn_${verdict.id}")
                ) {
                    Text("Apply Fix", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(0.9f).testTag("reject_flagged_btn_${verdict.id}")
                ) {
                    Text("Reject", fontSize = 11.sp)
                }
            }
        }
    }
}
