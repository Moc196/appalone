package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Memory
import com.example.ui.components.MemoryImageView
import com.example.viewmodel.MomentsViewModel

@Composable
fun MapScreen(viewModel: MomentsViewModel) {
    val allMemories by viewModel.allMemories.collectAsStateWithLifecycle()
    
    // Only display memories that are NOT locked and have GPS coordinates
    val mappedMemories = remember(allMemories) {
        allMemories.filter { !it.isLocked && it.latitude != null && it.longitude != null }
    }

    // Map navigation states
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_screen_root")
    ) {
        // Aesthetic Stylized Canvas-based Map Layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F5F0)) // Warm soft paper color map base
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 3.0f)
                        offset += pan
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f + offset.x, height / 2f + offset.y)
            val scale = 200f * zoom

            // Draw a beautiful blue river running across the screen
            val riverPath = Path().apply {
                moveTo(center.x - scale * 3f, center.y - scale * 2f)
                cubicTo(
                    center.x - scale * 1f, center.y - scale * 1f,
                    center.x + scale * 0.5f, center.y - scale * 0.5f,
                    center.x + scale * 2f, center.y + scale * 2f
                )
            }
            drawPath(
                path = riverPath,
                color = Color(0xFFBACFE6),
                style = Stroke(width = 40f * zoom)
            )

            // Draw a central park/lake area (simulating Hoan Kiem Lake)
            drawOval(
                color = Color(0xFFAFD6B6), // Light green park land
                topLeft = Offset(center.x - scale * 0.6f, center.y - scale * 0.6f),
                size = Size(scale * 1.2f, scale * 1.2f)
            )

            drawOval(
                color = Color(0xFFD4E6F1), // Soft lake water
                topLeft = Offset(center.x - scale * 0.4f, center.y - scale * 0.4f),
                size = Size(scale * 0.8f, scale * 0.8f)
            )

            // Draw street network grids (aesthetic abstract roads)
            val roadColor = Color(0xFFEBE8E0)
            val roadWidth = 8f * zoom
            
            // Horizontal streets
            for (i in -4..4) {
                drawLine(
                    color = roadColor,
                    start = Offset(center.x - scale * 5f, center.y + scale * i),
                    end = Offset(center.x + scale * 5f, center.y + scale * i),
                    strokeWidth = roadWidth
                )
            }
            // Vertical streets
            for (i in -4..4) {
                drawLine(
                    color = roadColor,
                    start = Offset(center.x + scale * i, center.y - scale * 5f),
                    end = Offset(center.x + scale * i, center.y + scale * 5f),
                    strokeWidth = roadWidth
                )
            }
        }

        // Render Photo Pins dynamically placed on our simulated coordinate space
        // Hanoi Center: lat 21.0285, lng 105.8542
        mappedMemories.forEach { memory ->
            val lat = memory.latitude ?: 21.0285
            val lng = memory.longitude ?: 105.8542

            // Project coordinate relative to hanoi center
            val dx = (lng - 105.8542) * 8000f
            val dy = -(lat - 21.0285) * 8000f // Flip y for screen coords

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Calculate position with zoom & panning offset
                val boxWidth = 60.dp
                val boxHeight = 65.dp

                // Local values to draw
                val scaleFactor = zoom
                
                // Track item layout dynamically via Box and custom offsets
                Box(
                    modifier = Modifier
                        .offset(
                            x = (dx * scaleFactor).dp + offset.x.dp + (180.dp), // add screen padding center
                            y = (dy * scaleFactor).dp + offset.y.dp + (260.dp)
                        )
                        .size(boxWidth, boxHeight)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE7E5E4)), RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectMemory(memory) }
                        .padding(3.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tiny Polaroid Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF5F5F4))
                        ) {
                            MemoryImageView(
                                photoPath = memory.photoPath,
                                timeOfDay = memory.timeOfDay,
                                filterApplied = memory.filterApplied,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Tiny pin dot pointer below image
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pin Location",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Overlay Map HUD controls with Back Button
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("Home") },
                modifier = Modifier
                    .shadow(2.dp, CircleShape)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Home")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Bản đồ Ký ức",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1917)
                )
                Text(
                    text = "Xem những dấu chân kỷ niệm của nhóm bạn",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = Color(0xFF78716C)
                )
            }
        }

        // Recenter / Map tools
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    zoom = 1.0f
                    offset = Offset(0f, 0f)
                },
                containerColor = Color.White,
                contentColor = Color(0xFF1C1917),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter Map")
            }
        }
    }
}
