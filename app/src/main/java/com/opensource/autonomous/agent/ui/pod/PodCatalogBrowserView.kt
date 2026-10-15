package com.opensource.autonomous.agent.ui.pod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.sp

@Composable
fun PodCatalogBrowserView(
    catalog: List<PrintifyProductBlank>,
    selectedProduct: PrintifyProductBlank,
    onSelectProduct: (PrintifyProductBlank) -> Unit,
    onSwitchToDesign: () -> Unit,
    isDarkTheme: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Apparel", "Drinkware", "Accessories", "Home Decor")

    val filteredProducts = remember(searchQuery, selectedCategory, catalog) {
        catalog.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.brandModel.contains(searchQuery, ignoreCase = true) ||
                item.blueprintId.contains(searchQuery, ignoreCase = true) ||
                item.baseSku.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pod_catalog_browser_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("catalog_search_input"),
            placeholder = { Text("Search Printify Blueprints (e.g., Bella 3001, Comfort Colors, Mug)...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF06B6D4)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                focusedContainerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                unfocusedContainerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
            )
        )

        // 2. Category Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("catalog_cat_$cat")
                )
            }
        }

        // 3. Selected Product Quick Blueprint Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
            border = BorderStroke(1.5.dp, Color(0xFF2563EB))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = selectedProduct.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFF2563EB).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "BP #${selectedProduct.blueprintId}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${selectedProduct.brandModel} • ${selectedProduct.weight}",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Button(
                        onClick = onSwitchToDesign,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("load_into_canvas_btn")
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Design", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Provider & Specs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Provider: ${selectedProduct.providerName} (${selectedProduct.providerLocation})",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Wholesale: $${String.format("%.2f", selectedProduct.cogsCents / 100.0)} • Retail: $${String.format("%.2f", selectedProduct.retailPriceCents / 100.0)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                }

                // Available Sizes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sizes:", fontSize = 10.sp, color = Color.Gray)
                    selectedProduct.availableSizes.forEach { sz ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = sz,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        // 4. Products List
        Text(
            text = "Printify Global Catalog (${filteredProducts.size} Products)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts) { item ->
                val isCurrent = item.id == selectedProduct.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProduct(item) }
                        .testTag("catalog_item_${item.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) {
                            if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                        } else {
                            if (isDarkTheme) Color(0xFF1E293B) else Color.White
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrent) Color(0xFF2563EB) else (if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                )
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "+${item.profitMarginPercent}% Margin",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${item.brandModel} • ${item.category}",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "Base COGS: $${String.format("%.2f", item.cogsCents / 100.0)} | Suggested: $${String.format("%.2f", item.retailPriceCents / 100.0)}",
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        RadioButton(
                            selected = isCurrent,
                            onClick = { onSelectProduct(item) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                        )
                    }
                }
            }
        }
    }
}
