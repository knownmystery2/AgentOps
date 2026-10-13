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
import com.example.ui.theme.MyApplicationTheme
import com.example.agentops.ui.MainViewModel
import com.opensource.autonomous.agent.ui.screens.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clean architecture view model context binding
        val viewModel = MainViewModel(application)

        setContent {
            MyApplicationTheme {
                AgentOpsMainApp(viewModel)
            }
        }
    }
}

enum class ScreenTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONSOLE("Console", Icons.Default.Dashboard),
    OVERSEER("Overseer", Icons.Default.Shield),
    APPROVALS("Approvals", Icons.Default.PendingActions),
    POD("POD Studio", Icons.Default.Palette),
    LEDGER("Ledger", Icons.Default.AccountBalance),
    STANDBY("Standby", Icons.Default.CloudSync),
    SYSTEM("System", Icons.Default.Dns),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AgentOpsMainApp(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.CONSOLE) }

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
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
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
                ScreenTab.OVERSEER -> GodOverseerScreen(
                    viewModel = viewModel
                )
                ScreenTab.APPROVALS -> ApprovalsScreen(
                    viewModel = viewModel,
                    onNavigateToMockupStudio = { currentTab = ScreenTab.POD }
                )
                ScreenTab.POD -> PodStudioDesktopWorkspace(
                    viewModel = viewModel,
                    onNavigateBack = { currentTab = ScreenTab.CONSOLE }
                )
                ScreenTab.LEDGER -> LedgerScreen(
                    viewModel = viewModel
                )
                ScreenTab.STANDBY -> StandbyPoolScreen(
                    viewModel = viewModel
                )
                ScreenTab.SYSTEM -> SystemUiScreen(
                    viewModel = viewModel
                )
                ScreenTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

