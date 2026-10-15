package com.opensource.autonomous.agent.ui.screens

import androidx.compose.runtime.Composable
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun PrintifyCloneScreen(
    viewModel: MainViewModel? = null,
    onNavigateBack: () -> Unit = {}
) {
    PodStudioDesktopWorkspace(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}
