package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.geometry.Offset
import com.example.data.Memory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryCard(
    memory: Memory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Polaroid cardboard style container
    val cardColor = Color(0xFFFAF9F6) // Warm creamy paper white

    Card(
        modifier = modifier
            .shadow(if (memory.timeOfDay == "Night") 6.dp else 3.dp, RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .testTag("memory_card_${memory.id}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.65f)),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Memory image box with analog aspect ratio and filters
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                MemoryImageView(
                    photoPath = memory.photoPath,
                    timeOfDay = memory.timeOfDay,
                    filterApplied = memory.filterApplied,
                    modifier = Modifier.fillMaxSize()
                )

                // Small vintage physical grain/stains overlay or light leak
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Vintage filter color multiplication overlays
                    val tint = when (memory.filterApplied.lowercase(Locale.ROOT)) {
                        "vintage chrome" -> Color(210, 180, 140, 25) // warm sepia
                        "lomo glow" -> Color(0, 150, 255, 18) // cold cyan leak
                        else -> Color.Transparent
                    }
                    if (tint != Color.Transparent) {
                        drawRect(color = tint)
                    }

                    // Bottom light leak spot
                    if (memory.filterApplied.lowercase(Locale.ROOT) == "lomo glow" || memory.timeOfDay == "Evening") {
                        drawCircle(
                            color = Color(0xFFFF8A8A).copy(alpha = 0.15f),
                            radius = size.width * 0.4f,
                            center = Offset(size.width, size.height * 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text section mimicking handwriting/typewriter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memory.timeOfDay.uppercase(Locale.ROOT),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTimeOfDayColor(memory.timeOfDay)
                    )

                    // Small mood badge
                    Text(
                        text = "• ${memory.mood}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        fontFamily = FontFamily.Serif
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Caption
                Text(
                    text = memory.caption,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1E293B),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date stamp in analog orange stamp style
                val dateString = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(memory.timestamp))
                Text(
                    text = dateString,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFFFB923C), // pixel stamp orange
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun MemoryImageView(
    photoPath: String,
    timeOfDay: String,
    filterApplied: String,
    modifier: Modifier = Modifier
) {
    val fileExists = if (photoPath.startsWith("/")) File(photoPath).exists() else false
    
    // Apply grayscale for Noir, standard warmth, or clean look
    val colorFilter = when (filterApplied.lowercase(Locale.ROOT)) {
        "noir" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        else -> null
    }

    if (fileExists) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = "Visual Moment",
            colorFilter = colorFilter,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Fallback: draw gorgeous, abstract artistic generated backgrounds!
        Canvas(modifier = modifier) {
            val drawHeight = size.height
            val drawWidth = size.width

            // Dynamic background gradients based on slot
            val startColor = when (timeOfDay.lowercase(Locale.ROOT)) {
                "morning" -> Color(0xFFFFFBEB)
                "noon" -> Color(0xFFF0F9FF)
                "evening" -> Color(0xFFFFF1F2)
                else -> Color(0xFF0F172A)
            }
            val endColor = when (timeOfDay.lowercase(Locale.ROOT)) {
                "morning" -> Color(0xFFFDE68A)
                "noon" -> Color(0xFF93C5FD)
                "evening" -> Color(0xFFFCA5A5)
                else -> Color(0xFF312E81)
            }

            drawRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset(0f, 0f),
                    end = Offset(drawWidth, drawHeight)
                )
            )

            // Draw a symbolic lens sphere representing the emotional sun or moon shifting
            val glowColor = when (timeOfDay.lowercase(Locale.ROOT)) {
                "morning" -> Color(0xFFFBBF24).copy(alpha = 0.6f)
                "noon" -> Color(0xFF38BDF8).copy(alpha = 0.5f)
                "evening" -> Color(0xFFF97316).copy(alpha = 0.6f)
                else -> Color(0xFF818CF8).copy(alpha = 0.4f)
            }
            drawCircle(
                color = glowColor,
                radius = drawWidth * 0.3f,
                center = Offset(drawWidth * 0.5f, drawHeight * 0.45f)
            )

            // Draw artistic minimalist mountain lines representing nostalgic landscape
            val pathPaint = androidx.compose.ui.graphics.Paint().apply {
                color = when (timeOfDay.lowercase(Locale.ROOT)) {
                    "night" -> Color(0xFFFFFFFF).copy(alpha = 0.15f)
                    else -> Color(0xFF000000).copy(alpha = 0.08f)
                }
                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                strokeWidth = 3f
                isAntiAlias = true
            }
            
            // Draw neat overlapping waves on the cardboard
            val wavePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, drawHeight * 0.8f)
                quadraticTo(drawWidth * 0.3f, drawHeight * 0.7f, drawWidth * 0.6f, drawHeight * 0.85f)
                quadraticTo(drawWidth * 0.8f, drawHeight * 0.9f, drawWidth, drawHeight * 0.75f)
            }
            drawPath(path = wavePath, color = pathPaint.color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
    }
}

// Tape adhesive overlay on top of polaroids
@Composable
fun TapeOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(90.dp)
            .height(24.dp)
            .graphicsLayer(alpha = 0.8f)
            .background(
                color = Color.White.copy(alpha = 0.4f), // clean frosted white tape
                shape = RoundedCornerShape(4.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
    )
}

fun getTimeOfDayColor(timeOfDay: String): Color {
    return when (timeOfDay.lowercase(Locale.ROOT)) {
        "morning" -> Color(0xFFD97706) // Warm gold
        "noon" -> Color(0xFF0284C7) // Sky blue
        "evening" -> Color(0xFFEA580C) // Sunset peach
        else -> Color(0xFF818CF8) // Night indigo
    }
}
