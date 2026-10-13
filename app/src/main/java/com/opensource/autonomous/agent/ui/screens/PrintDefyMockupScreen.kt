package com.opensource.autonomous.agent.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.os.Environment
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opensource.autonomous.agent.ui.MainViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintDefyMockupScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(Color(0xFFE8E2D2)) }
    var selectedArea by remember { mutableStateOf("Chest") }
    var designScale by remember { mutableStateOf(1.0f) }
    val context = LocalContext.current
    
    val colors = listOf(
        Pair("Charcoal", Color(0xFF262E3D)),
        Pair("Natural", Color(0xFFE8E2D2)),
        Pair("Forest", Color(0xFF1E3A2B)),
        Pair("Terracotta", Color(0xFFB86B53)),
        Pair("White", Color(0xFFF8FAFC))
    )

    // Automatically scale design when switching print area
    LaunchedEffect(selectedArea) {
        designScale = if (selectedArea == "Pocket") 0.3f else 1.0f
    }
    
    fun exportToPng(color: Color) {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint().apply { this.color = color.hashCode() }
        canvas.drawRect(0f, 0f, 1000f, 1000f, paint)
        
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "mockup.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Print Defy: Mockup Tool") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { TextButton(onClick = { exportToPng(selectedColor) }) { Text("Export Mockup") } }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    IconButton(onClick = {}) { Icon(Icons.Default.Palette, "Variants") }
                    FloatingActionButton(onClick = {}) { Icon(Icons.Default.Add, "Add") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Layers, "Layers") }
                }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            NavigationRail {
                IconButton(onClick = {}) { Icon(Icons.Default.List, "Catalog") }
                IconButton(onClick = {}) { Icon(Icons.Default.Image, "Graphics") }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    drawRect(color = selectedColor)
                    drawRect(Color.Gray, blendMode = BlendMode.Multiply)
                    drawRect(Color.Blue.copy(alpha = 0.5f), size = size.times(designScale))
                }
            }

            Column(modifier = Modifier.width(200.dp).fillMaxHeight().padding(16.dp)) {
                Text("Print Area", style = MaterialTheme.typography.titleSmall)
                Row {
                    FilterChip(selected = selectedArea == "Chest", onClick = { selectedArea = "Chest" }, label = { Text("Chest") })
                    FilterChip(selected = selectedArea == "Pocket", onClick = { selectedArea = "Pocket" }, label = { Text("Pocket") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Colors", style = MaterialTheme.typography.titleSmall)
                colors.forEach { (name, col) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedColor = col }.padding(8.dp)) {
                        Box(modifier = Modifier.size(24.dp).background(col, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        }
    }
}
