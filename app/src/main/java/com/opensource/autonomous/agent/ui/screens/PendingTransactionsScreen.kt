package com.opensource.autonomous.agent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.PendingTransactionEntity
import com.opensource.autonomous.agent.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class TransactionFilter(val label: String) {
    PENDING("Pending"),
    HIGH_VALUE("High Value (>$50)"),
    COGS("COGS & Production"),
    REFUNDS("Refunds"),
    PAYOUTS("Payouts"),
    HISTORY("History (All)")
}

/**
 * Compose screen displaying pending financial transactions stored in the local Room database,
 * requiring explicit user approval with individual Accept and Reject actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val statusMessage by viewModel.transactionApprovalStatusMessage.collectAsState()
    val isProcessing by viewModel.isProcessingTransaction.collectAsState()

    var activeFilter by remember { mutableStateOf(TransactionFilter.PENDING) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var rejectionDialogTx by remember { mutableStateOf<PendingTransactionEntity?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    val approvedCount = remember(allTransactions) { allTransactions.count { it.status == "APPROVED" } }
    val rejectedCount = remember(allTransactions) { allTransactions.count { it.status == "REJECTED" } }
    val pendingCount = remember(pendingTransactions) { pendingTransactions.size }
    val pendingTotalCents = remember(pendingTransactions) { pendingTransactions.sumOf { it.amountCents } }

    val filteredList = remember(activeFilter, pendingTransactions, allTransactions, searchQuery) {
        val baseList = when (activeFilter) {
            TransactionFilter.PENDING -> pendingTransactions
            TransactionFilter.HIGH_VALUE -> pendingTransactions.filter { it.amountCents >= 5000L }
            TransactionFilter.COGS -> pendingTransactions.filter { it.category == "COGS" }
            TransactionFilter.REFUNDS -> pendingTransactions.filter { it.category == "REFUND" }
            TransactionFilter.PAYOUTS -> pendingTransactions.filter { it.category == "PAYOUT" }
            TransactionFilter.HISTORY -> allTransactions
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.transactionCode.lowercase().contains(q) ||
                it.title.lowercase().contains(q) ||
                it.vendorOrPayee.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("pending_transactions_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pending Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room DB Authorization Queue • Accept or Reject",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("tx_nav_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("open_create_tx_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = "New Transaction",
                            tint = BrandEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_pending_tx_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Pending Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Notification Banner
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    color = BrandEmerald.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrandEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearTransactionStatusMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(14.dp),
                                tint = BrandEmerald
                            )
                        }
                    }
                }
            }

            // Summary Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Awaiting Approval",
                    value = "$pendingCount txns",
                    subValue = "$${String.format(Locale.US, "%.2f", pendingTotalCents / 100.0)} total",
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    testTag = "pending_metric_card"
                )
                MetricCard(
                    title = "Reconciled",
                    value = "$approvedCount approved",
                    subValue = "In Room Ledger",
                    accentColor = ProfitGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "approved_metric_card"
                )
                MetricCard(
                    title = "Denied",
                    value = "$rejectedCount rejected",
                    subValue = "Compliance logs",
                    accentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    testTag = "rejected_metric_card"
                )
            }

            // Search Bar & Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("tx_search_input"),
                placeholder = { Text("Search by code, vendor, description...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips Scroll Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionFilter.values().forEach { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            // Quick Batch Action Header (when items exist in pending)
            if (activeFilter == TransactionFilter.PENDING && pendingTransactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredList.size} pending authorization",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = {
                            val lowRiskIds = pendingTransactions
                                .filter { it.riskScore <= 20 }
                                .map { it.id }
                            if (lowRiskIds.isNotEmpty()) {
                                viewModel.batchApproveTransactions(lowRiskIds)
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.testTag("approve_low_risk_all_btn")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve All Low-Risk (≤20)", fontSize = 11.sp, color = ProfitGreen)
                    }
                }
            }

            // List of Transactions
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Pending Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No entries match '$searchQuery'." else "All transactions in the Room database have been reviewed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                ) {
                    items(filteredList, key = { it.id }) { tx ->
                        PendingTransactionCard(
                            transaction = tx,
                            isProcessing = isProcessing,
                            onAccept = { viewModel.approveTransaction(tx.id) },
                            onRejectPrompt = {
                                rejectionDialogTx = tx
                                rejectionReason = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Rejection Confirmation Dialog with Reason
    rejectionDialogTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { rejectionDialogTx = null },
            title = { Text("Reject Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to reject transaction ${tx.transactionCode} ($${String.format(Locale.US, "%.2f", tx.amountCents / 100.0)})?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection reason (optional)") },
                        placeholder = { Text("e.g., Unbudgeted expenditure / mismatch") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rejection_reason_input"),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTransaction(tx.id, rejectionReason.ifBlank { null })
                        rejectionDialogTx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_rejection_btn")
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectionDialogTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Pending Transaction Dialog
    if (showCreateDialog) {
        CreateTransactionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, amount, category, payee, debit, credit ->
                viewModel.createPendingTransaction(
                    title = title,
                    description = desc,
                    amountCents = amount,
                    category = category,
                    vendorOrPayee = payee,
                    accountDebit = debit,
                    accountCredit = credit
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PendingTransactionCard(
    transaction: PendingTransactionEntity,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onRejectPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US) }
    val formattedDate = remember(transaction.createdAt) { sdf.format(Date(transaction.createdAt)) }
    val formattedAmount = remember(transaction.amountCents) {
        String.format(Locale.US, "$%.2f", transaction.amountCents / 100.0)
    }

    val categoryColor = when (transaction.category) {
        "COGS" -> Color(0xFF38BDF8)
        "REFUND" -> Color(0xFFF87171)
        "PAYOUT" -> Color(0xFFA78BFA)
        "VENDOR_EXPENSE" -> Color(0xFFFB923C)
        else -> ProfitGreen
    }

    val riskColor = when {
        transaction.riskScore <= 20 -> ProfitGreen
        transaction.riskScore <= 45 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.error
    }

    val isPending = transaction.status == "PENDING"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_card_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            1.dp,
            if (isPending) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Code, Category, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = transaction.category,
                                color = categoryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = transaction.transactionCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (transaction.category == "REFUND") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Narrative Description
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Accounting & Vendor Metadata Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${transaction.platform} • ${transaction.vendorOrPayee}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dr: ${transaction.accountDebit}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Cr: ${transaction.accountCredit}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Risk Assessment Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(riskColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Risk Score: ${transaction.riskScore}/100 • ${transaction.riskReason}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status or Action Buttons
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reject Button
                    OutlinedButton(
                        onClick = onRejectPrompt,
                        enabled = !isProcessing,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reject_tx_button_${transaction.id}")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject Transaction",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }

                    // Accept Button
                    Button(
                        onClick = onAccept,
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfitGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp)
                            .testTag("accept_tx_button_${transaction.id}")
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Accept Transaction",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accept & Post", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Reviewed Status Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (transaction.status == "APPROVED") ProfitGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (transaction.status == "APPROVED") Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (transaction.status == "APPROVED") ProfitGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (transaction.status == "APPROVED") "Approved & Reconciled into Double-Entry Ledger" else "Rejected • Authorization Denied by Founder",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (transaction.status == "APPROVED") ProfitGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTransactionDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, amountCents: Long, category: String, payee: String, debit: String, credit: String) -> Unit
) {
    var title by remember { mutableStateOf("Printify POD Apparel Fulfillment Batch") }
    var description by remember { mutableStateOf("Production invoice for direct customer orders") }
    var amountText by remember { mutableStateOf("48.50") }
    var category by remember { mutableStateOf("COGS") }
    var payee by remember { mutableStateOf("SwiftPOD Fulfillment Center") }
    var debitAccount by remember { mutableStateOf("5010 Cost of Goods Sold") }
    var creditAccount by remember { mutableStateOf("1010 Operating Cash Clearing") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Pending Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Transaction Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_tx_title_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($ USD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_tx_amount_input")
                )
                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text("Payee / Vendor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amountText.toDoubleOrNull() ?: 0.0
                    val amountCents = (amountDouble * 100).toLong()
                    if (title.isNotBlank() && amountCents > 0L) {
                        onCreate(title, description, amountCents, category, payee, debitAccount, creditAccount)
                    }
                },
                modifier = Modifier.testTag("submit_create_tx_btn")
            ) {
                Text("Add to Queue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
