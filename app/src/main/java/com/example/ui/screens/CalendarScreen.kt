package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
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
import com.example.data.Memory
import com.example.ui.components.MemoryImageView
import com.example.ui.components.getTimeOfDayColor
import com.example.viewmodel.MomentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(viewModel: MomentsViewModel) {
    val allMemories by viewModel.allMemories.collectAsStateWithLifecycle()

    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Helper data
    val currentMonthYearString = remember(calendarState) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendarState.time)
    }

    val daysInMonth = remember(calendarState) {
        val tempCal = calendarState.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
        
        // Convert to start from Monday (0 to 6)
        // firstDayOfWeek: Sunday (1) -> 6, Monday (2) -> 0, Tuesday (3) -> 1 ... Saturday (7) -> 5
        val emptyLeadingSlots = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        
        val days = mutableListOf<Calendar?>()
        for (i in 0 until emptyLeadingSlots) {
            days.add(null)
        }
        for (i in 1..maxDays) {
            val dayCal = calendarState.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, i)
            days.add(dayCal)
        }
        days
    }

    // Filter memories of the selected date
    val selectedDateMemories = remember(allMemories, selectedDate) {
        allMemories.filter { mem ->
            val memCal = Calendar.getInstance().apply { timeInMillis = mem.timestamp }
            memCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    memCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        }.sortedBy { it.timestamp }
    }

    // List of date strings that have memories to render indicators
    val datesWithMemories = remember(allMemories) {
        allMemories.map { mem ->
            val memCal = Calendar.getInstance().apply { timeInMillis = mem.timestamp }
            "${memCal.get(Calendar.YEAR)}-${memCal.get(Calendar.MONTH)}-${memCal.get(Calendar.DAY_OF_MONTH)}"
        }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("calendar_screen_root")
    ) {
        // Screen Header
        Text(
            text = "Memory Calendar",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1917),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar controller Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Month / Year Selector header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCal = calendarState.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            calendarState = newCal
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Tháng trước")
                    }

                    Text(
                        text = currentMonthYearString.replaceFirstChar { it.uppercase() },
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            val newCal = calendarState.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            calendarState = newCal
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Tháng sau")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of week row
                val daysOfWeek = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF78716C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days grid
                val rows = daysInMonth.chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { rowDays ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowDays.forEach { dayCal ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayCal != null) {
                                        val isSelected = selectedDate.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                                selectedDate.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                                        
                                        val key = "${dayCal.get(Calendar.YEAR)}-${dayCal.get(Calendar.MONTH)}-${dayCal.get(Calendar.DAY_OF_MONTH)}"
                                        val hasMemories = datesWithMemories.contains(key)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) Color(0xFF1C1917) else Color.Transparent
                                                )
                                                .clickable {
                                                    selectedDate = dayCal
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp,
                                                color = if (isSelected) Color.White else Color(0xFF1C1917)
                                            )
                                            
                                            // Indicator dot
                                            if (hasMemories) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) Color.White else Color(0xFFF43F5E)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selected Date Label
        val selectedDateFormatted = remember(selectedDate) {
            SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDateFormatted,
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF57534E)
            )

            Text(
                text = "${selectedDateMemories.size} khoảnh khắc",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF78716C)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected date's memories list
        if (selectedDateMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "No events",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Không có khoảnh khắc nào được lưu lại trong ngày này.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Color(0xFF78716C),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(selectedDateMemories) { memory ->
                    CalendarMemoryCard(
                        memory = memory,
                        onClick = { viewModel.selectMemory(memory) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarMemoryCard(
    memory: Memory,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(1.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F4))
            ) {
                MemoryImageView(
                    photoPath = memory.photoPath,
                    timeOfDay = memory.timeOfDay,
                    filterApplied = memory.filterApplied,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memory.timeOfDay.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTimeOfDayColor(memory.timeOfDay)
                    )

                    Text(
                        text = memory.mood,
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78716C)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.caption,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1917),
                    maxLines = 2
                )

                if (memory.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = memory.notes,
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = Color(0xFF57534E),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
