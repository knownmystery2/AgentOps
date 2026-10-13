package com.opensource.autonomous.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PrintifyCanvas() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        // Garment Template
        Box(
            modifier = Modifier
                .size(300.dp, 400.dp)
                .background(Color.White)
        ) {
            // Print Area
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(150.dp, 200.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary)
            )
        }
    }
}
