package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opensource.autonomous.agent.ui.components.PrintifyCanvas

@Composable
fun PrintifyCloneScreen() {
    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Left Sidebar (Navigation)
        NavigationRail {
            // Add navigation items here
        }
        
        // Center Canvas
        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
            PrintifyCanvas()
        }
        
        // Right Sidebar (Controls, Layers, AI)
        Column(modifier = Modifier.width(300.dp).fillMaxHeight().padding(16.dp)) {
            Text("Controls", style = MaterialTheme.typography.titleMedium)
            // Add controls and layer manager here
        }
    }
}
