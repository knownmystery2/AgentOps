package com.opensource.autonomous.agent.ui.pod

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun PodApiTelemetryView(
    formattedPayload: String,
    product: PrintifyProductBlank,
    color: PrintifyColorSwatch,
    zone: PrintifyPrintZone,
    sku: String,
    onPublish: () -> Unit,
    isPublishing: Boolean,
    viewModel: MainViewModel? = null,
    isDarkTheme: Boolean = true
) {
    val context = LocalContext.current
    val connectionStatus by (viewModel?.printifyConnectionStatus?.collectAsState() ?: remember { mutableStateOf("Ready for fulfillment: kriticallimitz.printify.me") })
    val isTesting by (viewModel?.isTestingPrintify?.collectAsState() ?: remember { mutableStateOf(false) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("pod_api_telemetry_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Connection Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color(0xFF10B981), modifier = Modifier.padding(6.dp).size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Printify REST API Gateway",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "kriticallimitz.printify.me • Shop ID: 26650832",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel?.testPrintifyConnection() },
                        enabled = !isTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("test_printify_api_btn")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "Test", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Live API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = connectionStatus ?: "Connected: kriticallimitz.printify.me (Shop ID: 26650832)",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 2. Double-Entry Accounting Simulation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Double-Entry Ledger Settlement",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Debit: 1020 Printify Settlement", fontSize = 11.sp, color = Color(0xFF10B981))
                    Text("+$${String.format("%.2f", product.retailPriceCents / 100.0)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Credit: 4020 Merchandise Revenue", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    Text("+$${String.format("%.2f", product.retailPriceCents / 100.0)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Debit: 5010 Cost of Goods Sold", fontSize = 11.sp, color = Color(0xFFF59E0B))
                    Text("-$${String.format("%.2f", product.cogsCents / 100.0)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Credit: 2010 AP Monster Digital", fontSize = 11.sp, color = Color(0xFFEF4444))
                    Text("+$${String.format("%.2f", product.cogsCents / 100.0)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }
        }

        // 3. Raw Printify JSON Payload Viewer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) Color(0xFF0B1120) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1))
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
                    Text(
                        text = "POST /v1/shops/26650832/products.json",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Printify JSON", formattedPayload))
                            Toast.makeText(context, "Copied JSON Payload to Clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .background(if (isDarkTheme) Color(0xFF050811) else Color.White, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = formattedPayload,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF166534)
                    )
                }
            }
        }

        // 4. Staging Action Button
        Button(
            onClick = onPublish,
            enabled = !isPublishing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("publish_telemetry_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isPublishing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Staging in Register...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.CloudDone, contentDescription = "Publish", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish & Stage in PENDING STAGING ($sku)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
