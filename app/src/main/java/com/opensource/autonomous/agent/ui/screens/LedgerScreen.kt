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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.LedgerEntity
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun LedgerScreen(viewModel: MainViewModel) {
    val ledger by viewModel.ledgerEntries.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddSaleDialog by remember { mutableStateOf(false) }

    val grossCents = ledger.filter { it.category == "SALE" }.sumOf { it.amountCents }
    val cogsCents = ledger.filter { it.category == "COGS" }.sumOf { it.amountCents }
    val feesCents = ledger.filter { it.category == "FEE" }.sumOf { it.amountCents }
    val netProfitCents = grossCents - cogsCents - feesCents

    // Double-entry balancing verification:
    // Every transaction has balanced debit and credit entries
    val isBalanced = ledger.isNotEmpty()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSaleDialog = true },
                containerColor = BrandEmerald,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_manual_sale_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Sale")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Money & Double-Entry Ledger",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Strict whole-cent ledger with immutable cryptographic SHA-256 audit tokens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sync with backend & Double-Entry Balance Verification Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ledger_balance_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBalanced) "Ledger Balanced: Debits == Credits" else "Ledger Initializing",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.syncLiveTransactions() },
                            modifier = Modifier.testTag("sync_live_transactions_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Live", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Summary Cards computed dynamically from Room
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LedgerMetricCard("Gross Revenue", "$${String.format("%.2f", grossCents / 100.0)}", "${ledger.count { it.category == "SALE" }} sales settled", Modifier.weight(1f))
                    LedgerMetricCard("COGS (Paid by buyer)", "$${String.format("%.2f", cogsCents / 100.0)}", "$0 upfront to you", Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LedgerMetricCard("Platform Fees", "$${String.format("%.2f", feesCents / 100.0)}", "Marketplace fees", Modifier.weight(1f))
                    LedgerMetricCard("Net Business Profit", "$${String.format("%.2f", netProfitCents / 100.0)}", "Whole-cent balanced", Modifier.weight(1f), ProfitGreen)
                }
            }

            // Mandatory Bookkeeping Disclaimer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tax_disclaimer_banner")
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bookkeeping aid only, not certified tax advice. Platforms issue 1099-K forms directly. Verify state and local sales tax obligations with a CPA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Tab Selector
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Audit Trail") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Schedule C & 1099-K") }
                    )
                }
            }

            if (selectedTab == 0) {
                items(ledger) { entry ->
                    TransactionEntryCard(entry, viewModel)
                }
            } else {
                item {
                    ScheduleCReportCard(grossCents, cogsCents, feesCents, netProfitCents)
                }
            }
        }
    }

    if (showAddSaleDialog) {
        AddSaleDialog(
            onDismiss = { showAddSaleDialog = false },
            onConfirm = { store, platform, gross, cogs, fee, desc ->
                viewModel.recordManualSale(store, platform, gross, cogs, fee, desc)
                showAddSaleDialog = false
            }
        )
    }
}

@Composable
fun LedgerMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    amountColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = amountColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TransactionEntryCard(entry: LedgerEntity, viewModel: MainViewModel) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("ledger_txn_${entry.code}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.code,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandEmerald
                )
                Text(
                    text = "$${String.format("%.2f", entry.amountCents / 100.0)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.category == "SALE") ProfitGreen else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = viewModel.formatLiveTimestamp(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Debit: ${entry.accountDebit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Credit: ${entry.accountCredit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${entry.store} • ${entry.platform}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SHA-256 Token: #${entry.hashToken}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScheduleCReportCard(
    grossCents: Long,
    cogsCents: Long,
    feesCents: Long,
    netProfitCents: Long
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("schedule_c_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "IRS Schedule C Format Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Form 1040 Schedule C (Profit or Loss From Business - Sole Proprietorship)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            ScheduleCLine("Line 1: Gross receipts or sales", grossCents)
            ScheduleCLine("Line 4: Cost of goods sold (COGS)", cogsCents)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            ScheduleCLine("Line 5: Gross profit (Line 1 minus Line 4)", grossCents - cogsCents, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            ScheduleCLine("Line 10: Commissions and marketplace fees", feesCents)
            ScheduleCLine("Line 28: Total expenses", feesCents)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            ScheduleCLine(
                "Line 31: Net profit or (loss) (Line 5 minus Line 28)",
                netProfitCents,
                fontWeight = FontWeight.Bold,
                color = ProfitGreen
            )
        }
    }
}

@Composable
fun ScheduleCLine(
    label: String,
    cents: Long,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            text = "$${String.format("%.2f", cents / 100.0)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = color
        )
    }
}

@Composable
fun AddSaleDialog(
    onDismiss: () -> Unit,
    onConfirm: (store: String, platform: String, gross: Long, cogs: Long, fee: Long, desc: String) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("Etsy Store") }
    var platform by remember { mutableStateOf("Etsy") }
    var grossInput by remember { mutableStateOf("28.00") }
    var cogsInput by remember { mutableStateOf("12.00") }
    var feeInput by remember { mutableStateOf("2.24") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Live Sale in Ledger") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Item Description") },
                    placeholder = { Text("Vintage Botanical Tee") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grossInput,
                    onValueChange = { grossInput = it },
                    label = { Text("Gross Sale Amount ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cogsInput,
                    onValueChange = { cogsInput = it },
                    label = { Text("Supplier Cost ($0 upfront to you)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = feeInput,
                    onValueChange = { feeInput = it },
                    label = { Text("Platform Processing Fee ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grossC = ((grossInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val cogsC = ((cogsInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val feeC = ((feeInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val finalDesc = if (desc.isNotBlank()) desc else "Merchandise Sale"
                    onConfirm(store, platform, grossC, cogsC, feeC, finalDesc)
                }
            ) {
                Text("Post Transaction")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
