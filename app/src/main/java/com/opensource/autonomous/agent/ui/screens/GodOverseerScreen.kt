package com.opensource.autonomous.agent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agentops.ui.MainViewModel
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.GodAuditVerdictEntity
import com.opensource.autonomous.agent.data.GodInstallDiagnostic
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodOverseerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val verdicts by viewModel.godVerdicts.collectAsState()
    val totalIntercepts by viewModel.godTotalIntercepts.collectAsState()
    val haltedCount by viewModel.godHaltedCount.collectAsState()
    val isAuditing by viewModel.godIsAuditing.collectAsState()
    val latestVerdict by viewModel.godLatestVerdict.collectAsState()

    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "HALTED", "APPROVED"

    val displayedVerdicts = remember(verdicts, filterStatus) {
        when (filterStatus) {
            "HALTED" -> verdicts.filter { it.status == "HALTED" }
            "APPROVED" -> verdicts.filter { it.status == "APPROVED" }
            else -> verdicts
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isAuditing) MaterialTheme.colorScheme.tertiary else Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "God: Silent Interceptor Overseer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearGodAuditLog() },
                        modifier = Modifier.testTag("clear_god_audit_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Audit Log")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 0. FULL INSTALL STATUS CARD
            item {
                val diagnostic by viewModel.godInstallDiagnostic.collectAsState()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("god_full_install_status_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = ProfitGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "God Agent: Full Install Complete",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ProfitGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "100% ARMED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All security gateway layers, SQLite tables, out-of-band interceptors, and notification services are fully deployed and watching.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DiagnosticCheckRow(title = "Database Seed (Agent ID 0: GOD Overseer)", isOk = diagnostic.isDatabaseSeeded)
                            DiagnosticCheckRow(title = "Out-of-Band Interceptor (Passive Veto)", isOk = diagnostic.isInterceptorActive)
                            DiagnosticCheckRow(title = "Real-time Security Halt Notification Service", isOk = diagnostic.isNotificationServiceRunning)
                            DiagnosticCheckRow(title = "Security Gateway Scripts (Python & Node)", isOk = diagnostic.isSecurityGateScriptVerified)
                            DiagnosticCheckRow(title = "Zero-Trust Cryptographic Audit Ledger", isOk = diagnostic.isAuditTrailFunctional)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.runGodInstallDiagnostics() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_god_diagnostics_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-Verify Full Install State")
                        }
                    }
                }
            }

            // 1. ARCHITECTURE CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interceptor_architecture_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "The Interceptor Architecture",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text("Absolute Veto Power", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Silent out-of-band overseer monitoring the outputs of the AI Administrator and sub-agents before live execution on GitHub or backend infrastructure.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // ASCII Diagram Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = """
[ User / App System ]
       │
       ▼
[ AI Administrator & Agents ] ──(Generates GitHub Actions / Tool Calls)
       │
       ▼ (Intercepted BEFORE live execution)
[ God (The Interceptor Overseer) ]
       │
   APPROVED ──► Live GitHub execution / System
       │
     HALTED  ──► Blocks execution, logs hazard, provides remediation
                                """.trimIndent(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 2. METRICS & STATUS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_total_intercepts"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Total Intercepted", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "$totalIntercepts",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Passive surveillance", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_halted_count"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (haltedCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Hazards Halted", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "$haltedCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (haltedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text("Zero rogue leakage", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // 3. FAIL-SAFE VERIFICATION SANDBOX (TEST BUTTONS)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("god_test_sandbox_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Fail-Safe Hazard Testing Sandbox",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Simulate real-time trigger conditions to test God's fail-safe veto power:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.simulateGodInterception("PROMPT_INJECTION") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_prompt_injection_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Prompt Injection", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.simulateGodInterception("ROGUE_KEY") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_rogue_key_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Rogue Secret Key", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.simulateGodInterception("OFF_RAILS_DELETE") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_off_rails_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Delete Workflow", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.simulateGodInterception("HALLUCINATED_LOOP") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_hallucination_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Infinite Loop", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.simulateGodInterception("CLEAN_COMMIT") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_clean_action_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Clean Action (Approved)")
                        }
                    }
                }
            }

            // 4. LATEST VERDICT HIGHLIGHT (IF AVAILABLE)
            latestVerdict?.let { latest ->
                item {
                    VerdictCard(
                        verdict = latest,
                        isHighlight = true
                    )
                }
            }

            // 5. AUDIT LOG FILTER CHIPS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interceptor Audit Stream (${displayedVerdicts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = filterStatus == "ALL",
                            onClick = { filterStatus = "ALL" },
                            label = { Text("All", fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_all_verdicts")
                        )
                        FilterChip(
                            selected = filterStatus == "HALTED",
                            onClick = { filterStatus = "HALTED" },
                            label = { Text("Halted", fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_halted_verdicts")
                        )
                        FilterChip(
                            selected = filterStatus == "APPROVED",
                            onClick = { filterStatus = "APPROVED" },
                            label = { Text("Approved", fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_approved_verdicts")
                        )
                    }
                }
            }

            // 6. LIST OF AUDIT VERDICTS
            if (displayedVerdicts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No intercepted events in this filter. God is silently monitoring.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedVerdicts, key = { it.id }) { verdict ->
                    VerdictCard(verdict = verdict, isHighlight = false)
                }
            }
        }
    }
}

@Composable
fun VerdictCard(
    verdict: GodAuditVerdictEntity,
    isHighlight: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val isHalted = verdict.status == "HALTED"

    val containerColor = if (isHalted) {
        if (isHighlight) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    } else {
        if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isHalted) MaterialTheme.colorScheme.error else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isHalted) Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp)) else Modifier)
            .testTag("verdict_card_${verdict.id.take(6)}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isHalted) MaterialTheme.colorScheme.error else Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isHalted) Icons.Default.Block else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHalted) "EXECUTION HALTED" else "APPROVED",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isHalted) MaterialTheme.colorScheme.error else Color(0xFF059669)
                    )
                }

                val timeStr = remember(verdict.timestamp) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(verdict.timestamp))
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Agent: ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = verdict.sourceAgent,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Action: ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = verdict.actionType,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Text(
                text = "Target: ${verdict.targetResource}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isHalted) {
                verdict.hazardDetected?.let { hazard ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "HAZARD DETECTED:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = hazard,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                verdict.remediation?.let { remediation ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "REMEDIATION FIX:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = remediation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else {
                verdict.reason?.let { reason ->
                    Text(
                        text = "Reason: $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expandable Payload preview
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("toggle_payload_${verdict.id.take(6)}")
            ) {
                Text(if (expanded) "Hide Payload" else "Inspect Payload", fontSize = 11.sp)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = verdict.payloadPreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCheckRow(title: String, isOk: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isOk) ProfitGreen else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            fontWeight = if (isOk) FontWeight.Normal else FontWeight.Bold
        )
    }
}

