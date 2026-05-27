package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Memory
import com.example.viewmodel.MomentsViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimeCapsulesScreen(viewModel: MomentsViewModel) {
    val allMemories by viewModel.allMemories.collectAsStateWithLifecycle()

    // Separate locked and unlocked time capsules
    val lockedCapsules = remember(allMemories) {
        allMemories.filter { it.isLocked }
    }

    val unlockedCapsules = remember(allMemories) {
        allMemories.filter { !it.isLocked && it.unlockTime > 0L }
    }

    // Refresh timers periodically
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(key1 = lockedCapsules) {
        while (lockedCapsules.isNotEmpty()) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("time_capsules_screen_root")
    ) {
        // Back Header Button Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("Home") },
                modifier = Modifier
                    .shadow(1.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(1.dp, Color(0xFFE7E5E4)), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Home")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hộp Quà Thời Gian",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1917)
                )
                Text(
                    text = "Gửi thông điệp bí mật và khóa lại theo thời gian",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = Color(0xFF78716C)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab selection for Locked vs Unlocked history
        var selectedTabIndex by remember { mutableStateOf(0) }
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF1C1917),
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Đang khóa (${lockedCapsules.size})", fontFamily = FontFamily.Serif) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Đã mở khóa (${unlockedCapsules.size})", fontFamily = FontFamily.Serif) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTabIndex == 0) {
            if (lockedCapsules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "No capsules",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không có hộp quà nào đang bị khóa.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = Color(0xFF78716C)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(lockedCapsules) { capsule ->
                        LockedGiftBoxCard(
                            capsule = capsule,
                            currentTime = currentTime,
                            onUnwrap = { viewModel.unwrapTimeCapsule(capsule) }
                        )
                    }
                }
            }
        } else {
            if (unlockedCapsules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "No opened capsules",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chưa có hộp quà nào được mở khóa trước đây.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = Color(0xFF78716C)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(unlockedCapsules) { capsule ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectMemory(capsule) }
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF5F5F4))
                                ) {
                                    com.example.ui.components.MemoryImageView(
                                        photoPath = capsule.photoPath,
                                        timeOfDay = capsule.timeOfDay,
                                        filterApplied = capsule.filterApplied,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = capsule.caption,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Đã mở: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(capsule.timestamp)),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF78716C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockedGiftBoxCard(
    capsule: Memory,
    currentTime: Long,
    onUnwrap: () -> Unit
) {
    val timeLeft = capsule.unlockTime - currentTime
    val canUnlock = timeLeft <= 0

    // Tilt animation for active unlocked gift box
    val infiniteTransition = rememberInfiniteTransition(label = "shaking")
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "angle"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canUnlock) Color(0xFFFEF2F2) else Color(0xFFF5F5F4)
        ),
        border = BorderStroke(1.2.dp, if (canUnlock) Color(0xFFFCA5A5) else Color(0xFFE7E5E4)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clickable(enabled = canUnlock, onClick = onUnwrap)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gift Box Icon with gradient brush and dynamic shaking if ready
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .rotate(if (canUnlock) tiltAngle else 0f)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (canUnlock) {
                                listOf(Color(0xFFEF4444), Color(0xFFF87171)) // Red glowing active
                            } else {
                                listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)) // Slate passive
                            }
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = "Hộp quà",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (canUnlock) "Sẵn sàng mở quà!" else "Đang khóa",
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (canUnlock) Color(0xFFDC2626) else Color(0xFF44403C)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (canUnlock) {
                Text(
                    text = "Bấm để mở bao bì",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            } else {
                // Formatting countdown duration
                val totalSeconds = (timeLeft / 1000).coerceAtLeast(0)
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                val timeString = String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)

                Text(
                    text = timeString,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78716C)
                )
            }
        }
    }
}
