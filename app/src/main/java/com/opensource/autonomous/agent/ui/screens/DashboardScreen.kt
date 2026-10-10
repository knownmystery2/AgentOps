package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agentops.ui.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToApprovals: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToStandby: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Daily Briefing & Console", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Welcome back admin. Monitor automated execution queues and system logs running via your Termux background framework instance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Active Infrastructure: Online", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onNavigateToApprovals() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Approvals Register Screen")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onNavigateToLedger() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Financial Ledger Sheets")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onNavigateToStandby() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Standby Power Allocations")
        }
    }
}
