package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Memory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onDelete: (Memory) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "A preserved moment",
                            fontFamily = FontFamily.Serif,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_detail_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Detail"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_memory_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Moment",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFFAF9F6) // Matches soft scrapbook white
                    )
                )
            },
            containerColor = Color(0xFFFAF9F6) // Warm paper-white canvas background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Polaroid with rotational slight tilt and tape
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .rotate(-2f)
                            .width(320.dp)
                    ) {
                        // The Polaroid core Card styled with Frosted Glass theme
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.65f)),
                            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.8f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Full aspect image box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                                ) {
                                    MemoryImageView(
                                        photoPath = memory.photoPath,
                                        timeOfDay = memory.timeOfDay,
                                        filterApplied = memory.filterApplied,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Soft photo filters styling
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val filterTint = when (memory.filterApplied.lowercase(Locale.ROOT)) {
                                            "vintage chrome" -> Color(210, 180, 140, 20)
                                            "lomo glow" -> Color(0, 150, 255, 12)
                                            else -> Color.Transparent
                                        }
                                        if (filterTint != Color.Transparent) {
                                            drawRect(color = filterTint)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Caption & parameters inside Polaroid margins
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    val fullDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(memory.timestamp))
                                    Text(
                                        text = fullDate,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF78716C), // stone-500
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = memory.caption,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1C1917), // stone-900
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = memory.timeOfDay.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = getTimeOfDayColor(memory.timeOfDay)
                                        )

                                        Text(
                                            text = "feeling: ${memory.mood}",
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF57534E) // stone-600
                                        )
                                    }
                                }
                            }
                        }

                        // Tape overlay physically holding the polaroid to the screen!
                        TapeOverlay(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-4).dp)
                                .rotate(4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Preserved metadata notes - Frosted Glass panel
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.75f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Atmosphere Log".uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location Logged",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = memory.location,
                                    fontSize = 14.sp,
                                    color = Color(0xFF334155),
                                    fontFamily = FontFamily.Serif
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Weather Preserved",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = memory.weather,
                                    fontSize = 14.sp,
                                    color = Color(0xFF334155),
                                    fontFamily = FontFamily.Serif
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "This is a private, emotional memory held exclusively in your local sandbox. Nobody else can view, share, or track this moment.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Confirm delete dialog sheet/modal
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text(
                            text = "Release this moment?",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to fade this memory away? This operation is permanent.",
                            fontFamily = FontFamily.Serif
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDelete(memory)
                                showDeleteConfirm = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("confirm_delete_button")
                        ) {
                            Text("Release")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Retain")
                        }
                    }
                )
            }
        }
    }
}
