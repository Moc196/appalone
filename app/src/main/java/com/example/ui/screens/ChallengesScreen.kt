package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.viewmodel.MomentsViewModel

@Composable
fun ChallengesScreen(viewModel: MomentsViewModel) {
    val activeChallenge by viewModel.currentChallenge.collectAsStateWithLifecycle()
    val unlockedFilters by viewModel.unlockedFilters.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("challenges_screen_root")
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
                    text = "Thử thách Chụp ảnh",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1917)
                )
                Text(
                    text = "Chụp ảnh theo chủ đề để mở khóa các filter",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = Color(0xFF78716C)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Challenge Card (BeReal style but personalized)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeChallenge != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFEAB308), Color(0xFFFDE047))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Active Challenge",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Thử thách hôm nay dành cho bạn:",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Color(0xFF78716C)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"$activeChallenge\"",
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val rewardFilter = viewModel.challengeRewards[activeChallenge] ?: ""
                    Text(
                        text = "Phần thưởng mở khóa: Filter \"$rewardFilter\"",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Button(
                        onClick = {
                            // Prepopulate caption with challenge and navigate to Camera
                            viewModel.activeCaption.value = "[Thử thách] $activeChallenge"
                            viewModel.navigateTo("Camera")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1917)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Chụp ngay")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chụp ảnh thử thách",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "No challenge",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bạn chưa kích hoạt thử thách nào hôm nay.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Color(0xFF78716C),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.selectRandomChallenge() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1917)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = "Nhận thử thách ngẫu nhiên",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Rewards Section / Unlocked Filters list
        Text(
            text = "Bộ sưu tập Filter Kỷ niệm",
            fontFamily = FontFamily.Serif,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1917),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val allFilterRewards = viewModel.challengeRewards.values.toSet()
        val standardFilters = setOf("Standard Soft", "Vintage Chrome", "Lomo Glow", "Noir")
        val filterList = (standardFilters + allFilterRewards).toList()

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filterList.forEach { filterName ->
                val isUnlocked = unlockedFilters.contains(filterName)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color.White else Color(0xFFF5F5F4)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = filterName,
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) Color(0xFF1C1917) else Color(0xFF78716C)
                            )
                            Text(
                                text = if (isUnlocked) "Sẵn sàng sử dụng" else "Khóa (Hoàn thành thử thách để mở)",
                                fontFamily = FontFamily.Serif,
                                fontSize = 12.sp,
                                color = if (isUnlocked) Color(0xFF10B981) else Color(0xFF78716C)
                            )
                        }

                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (isUnlocked) "Mở khóa" else "Khóa",
                            tint = if (isUnlocked) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
