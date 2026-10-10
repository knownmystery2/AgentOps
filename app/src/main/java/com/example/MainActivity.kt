package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.MyApplicationTheme
import com.opensource.autonomous.agent.data.AppDatabase
import com.opensource.autonomous.agent.ui.MainViewModel
import com.opensource.autonomous.agent.network.LocalWebhookServer
import com.opensource.autonomous.agent.ui.AdvancedWorkspaceDashboard
import com.opensource.autonomous.agent.ui.screens.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var webhookServer: LocalWebhookServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext, lifecycleScope)
        val viewModel = MainViewModel(db)

        // 1. Safe, platform-compliant background loop scheduling that works with your worker
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.opensource.autonomous.agent.worker.AgentOrchestrationWorker>(
            15, TimeUnit.MINUTES
        ).build()
        
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "AgentAutomationLoop",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // 2. Start embedded open-source webhook server for incoming tasks/inquiries
        webhookServer = LocalWebhookServer(port = 8080, db = db).also { it.start() }

        setContent {
            MyApplicationTheme {
                AgentOpsMainApp(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webhookServer?.stop()
    }
}

enum class ScreenTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONSOLE("Console", Icons.Default.Dashboard),
    SWARM("Swarm", Icons.Default.Hub),
    APPROVALS("Approvals", Icons.Default.PendingActions),
    POD("POD Studio", Icons.Default.Palette),
    DROPSHIP("Dropship", Icons.Default.Inventory2),
    LEDGER("Ledger", Icons.Default.AccountBalance),
    STANDBY("Standby", Icons.Default.CloudSync),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AgentOpsMainApp(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.CONSOLE) }
    val decisions by viewModel.decisions.collectAsState()
    val pendingDecisionsCount = decisions.size

    // Handle back button on secondary tabs
    if (currentTab != ScreenTab.CONSOLE) {
        BackHandler {
            currentTab = ScreenTab.CONSOLE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                ScreenTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == ScreenTab.APPROVALS && pendingDecisionsCount > 0) {
                                        Badge { Text("$pendingDecisionsCount") }
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandEmerald,
                            indicatorColor = BrandEmerald.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.CONSOLE -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToApprovals = { currentTab = ScreenTab.APPROVALS },
                    onNavigateToLedger = { currentTab = ScreenTab.LEDGER },
                    onNavigateToStandby = { currentTab = ScreenTab.STANDBY }
                )
                ScreenTab.SWARM -> AdvancedWorkspaceDashboard(
                    viewModel = viewModel
                )
                ScreenTab.APPROVALS -> ApprovalsScreen(
                    viewModel = viewModel,
                    onNavigateToMockupStudio = { currentTab = ScreenTab.POD }
                )
                ScreenTab.POD -> PodMockupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentTab = ScreenTab.CONSOLE }
                )
                ScreenTab.DROPSHIP -> DropshipScreen(
                    viewModel = viewModel,
                    onNavigateToApprovals = { currentTab = ScreenTab.APPROVALS }
                )
                ScreenTab.LEDGER -> LedgerScreen(
                    viewModel = viewModel
                )
                ScreenTab.STANDBY -> StandbyPoolScreen(
                    viewModel = viewModel
                )
                ScreenTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
