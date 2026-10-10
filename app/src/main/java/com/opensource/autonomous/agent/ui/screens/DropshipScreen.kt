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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.DropshipEntity
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun DropshipScreen(
    viewModel: MainViewModel,
    onNavigateToApprovals: () -> Unit
) {
    val items by viewModel.dropshipItems.collectAsState()
    var zeroCostFilterActive by remember { mutableStateOf(true) }

    val displayItems = if (zeroCostFilterActive) items.filter { it.zeroUpfrontCost } else items

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Dropshipping Sourcing Shortlist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verified suppliers with guaranteed $0 upfront payment. You only pay suppliers after the customer pays you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = zeroCostFilterActive,
                        onClick = { zeroCostFilterActive = !zeroCostFilterActive },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Zero Upfront Cost Filter (Enforced)") },
                        modifier = Modifier.testTag("zero_cost_filter_chip")
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(displayItems) { item ->
                DropshipItemCard(
                    item = item,
                    onToggleShortlist = { viewModel.toggleShortlist(item.id, !item.inShortlist) },
                    onQueueForApproval = { onNavigateToApprovals() }
                )
            }
        }
    }
}

@Composable
fun DropshipItemCard(
    item: DropshipEntity,
    onToggleShortlist: () -> Unit,
    onQueueForApproval: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dropship_card_${item.id}")
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
                        text = item.niche.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${item.supplierRating} Rating", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Supplier: ${item.supplierName} • Tracked Shipping: ${item.shippingDays}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transparent Economics Grid
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
                        Text("Suggested Retail Price:", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", item.retailPriceCents / 100.0)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Supplier Cost (Paid by customer checkout):", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", item.costCents / 100.0)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Platform & Processing Fees:", style = MaterialTheme.typography.bodySmall)
                        Text("$${String.format("%.2f", item.feeCents / 100.0)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Your Net Profit per Sale:", fontWeight = FontWeight.Bold, color = ProfitGreen, style = MaterialTheme.typography.bodySmall)
                        Text("+$${String.format("%.2f", item.profitCents / 100.0)} (${(item.profitCents * 100 / item.retailPriceCents)}%)", fontWeight = FontWeight.Bold, color = ProfitGreen, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Upfront Out-of-Pocket Risk:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("$0.00 Guaranteed", fontWeight = FontWeight.Bold, color = BrandEmerald, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleShortlist,
                    modifier = Modifier.testTag("toggle_shortlist_btn_${item.id}")
                ) {
                    Text(if (item.inShortlist) "Remove" else "Shortlist")
                }

                Button(
                    onClick = onQueueForApproval,
                    modifier = Modifier.testTag("send_to_approvals_btn_${item.id}")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ready for Review")
                }
            }
        }
    }
}
