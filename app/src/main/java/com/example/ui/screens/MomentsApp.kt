package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Memory
import com.example.ui.components.MemoryCard
import com.example.ui.components.MemoryDetailDialog
import com.example.ui.components.MemoryImageView
import com.example.ui.components.getTimeOfDayColor
import com.example.ui.components.UpdateDialog
import com.example.utils.UpdateChecker
import com.example.utils.UpdateInfo
import com.example.BuildConfig
import com.example.viewmodel.MomentsViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MomentsApp(
    viewModel: MomentsViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedMemory by viewModel.selectedMemoryDetail.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    if (!isUserLoggedIn) {
        LoginScreen(viewModel = viewModel)
        return
    }

    // Check for updates on startup
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        val remoteUpdate = UpdateChecker.checkUpdate()
        if (remoteUpdate != null && remoteUpdate.versionCode > BuildConfig.VERSION_CODE) {
            updateInfoState = remoteUpdate
        }
    }

    if (updateInfoState != null) {
        UpdateDialog(
            updateInfo = updateInfoState!!,
            onDismiss = { updateInfoState = null }
        )
    }

    // Observe active memory detail overlay click
    if (selectedMemory != null) {
        MemoryDetailDialog(
            memory = selectedMemory!!,
            onDismiss = { viewModel.selectMemory(null) },
            onDelete = { memory -> viewModel.deleteMemory(memory) },
            onUpdate = { memory -> viewModel.updateMemory(memory) }
        )
    }

    // Adapt to responsive screen orientations
    val configuration = LocalConfiguration.current
    val useNavRail = configuration.screenWidthDp > 600

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        containerColor = Color(0xFFFAF9F6) // Warm paper-white canvas background matching Frosted Glass mock
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavRail) {
                NavigationRailContainer(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        "Home" -> HomeScreen(viewModel = viewModel)
                        "Camera" -> CameraScreen(viewModel = viewModel)
                        "Timeline" -> TimelineScreen(viewModel = viewModel)
                        "Calendar" -> CalendarScreen(viewModel = viewModel)
                        "Profile" -> ProfileScreen(viewModel = viewModel)
                        "Map" -> MapScreen(viewModel = viewModel)
                        "Challenges" -> ChallengesScreen(viewModel = viewModel)
                        "TimeCapsules" -> TimeCapsulesScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(6.dp, RoundedCornerShape(36.dp))
            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(36.dp))
            .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f)), RoundedCornerShape(36.dp))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.height(64.dp)
        ) {
            NavigationBarItem(
                selected = currentScreen == "Home",
                onClick = { onNavigate("Home") },
                icon = { Icon(if (currentScreen == "Home") Icons.Filled.Home else Icons.Outlined.Home, "Home Dashboard") },
                label = { Text("Today", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f) // soft glow active indicator
                ),
                modifier = Modifier.testTag("nav_today")
            )

            NavigationBarItem(
                selected = currentScreen == "Camera",
                onClick = { onNavigate("Camera") },
                icon = { Icon(if (currentScreen == "Camera") Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera, "Vintage Camera") },
                label = { Text("Capture", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.testTag("nav_camera")
            )

            NavigationBarItem(
                selected = currentScreen == "Timeline",
                onClick = { onNavigate("Timeline") },
                icon = { Icon(if (currentScreen == "Timeline") Icons.AutoMirrored.Filled.List else Icons.Outlined.List, "Memory Journal") },
                label = { Text("Timeline", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.testTag("nav_timeline")
            )

            NavigationBarItem(
                selected = currentScreen == "Calendar",
                onClick = { onNavigate("Calendar") },
                icon = { Icon(if (currentScreen == "Calendar") Icons.Filled.DateRange else Icons.Outlined.DateRange, "Memory Calendar") },
                label = { Text("Lịch", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.testTag("nav_calendar")
            )

            NavigationBarItem(
                selected = currentScreen == "Profile",
                onClick = { onNavigate("Profile") },
                icon = { Icon(if (currentScreen == "Profile") Icons.Filled.Face else Icons.Outlined.Face, "Aura Weather Map") },
                label = { Text("Aura", fontFamily = FontFamily.Serif, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.testTag("nav_aura")
            )
        }
    }
}

@Composable
fun NavigationRailContainer(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationRail(
        containerColor = Color.White.copy(alpha = 0.65f), // glossy glass panel
        modifier = Modifier
            .fillMaxHeight()
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f)) // soft right-side glowing divider
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            NavigationRailItem(
                selected = currentScreen == "Home",
                onClick = { onNavigate("Home") },
                icon = { Icon(if (currentScreen == "Home") Icons.Filled.Home else Icons.Outlined.Home, "Today") },
                label = { Text("Today", fontFamily = FontFamily.Serif) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            NavigationRailItem(
                selected = currentScreen == "Camera",
                onClick = { onNavigate("Camera") },
                icon = { Icon(if (currentScreen == "Camera") Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera, "Capture") },
                label = { Text("Capture", fontFamily = FontFamily.Serif) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            NavigationRailItem(
                selected = currentScreen == "Timeline",
                onClick = { onNavigate("Timeline") },
                icon = { Icon(if (currentScreen == "Timeline") Icons.AutoMirrored.Filled.List else Icons.Outlined.List, "Timeline") },
                label = { Text("Timeline", fontFamily = FontFamily.Serif) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            NavigationRailItem(
                selected = currentScreen == "Calendar",
                onClick = { onNavigate("Calendar") },
                icon = { Icon(if (currentScreen == "Calendar") Icons.Filled.DateRange else Icons.Outlined.DateRange, "Calendar") },
                label = { Text("Calendar", fontFamily = FontFamily.Serif) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            NavigationRailItem(
                selected = currentScreen == "Profile",
                onClick = { onNavigate("Profile") },
                icon = { Icon(if (currentScreen == "Profile") Icons.Filled.Face else Icons.Outlined.Face, "Aura Map") },
                label = { Text("Aura", fontFamily = FontFamily.Serif) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    selectedTextColor = Color(0xFF1E293B),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: MomentsViewModel) {
    val todayMap by viewModel.todayMemories.collectAsStateWithLifecycle()
    val auraReflection by viewModel.currentAuraReflection.collectAsStateWithLifecycle()
    val isAuraLoading by viewModel.isAuraLoading.collectAsStateWithLifecycle()

    // Gentle microcopy rotating quotes comforting banner
    val peacefulSayings = remember {
        listOf(
            "Hold this fleeting morning closely before the noon rush.",
            "A single quiet snapshot keeps today's soul immortal.",
            "Are you breathing? Relax your shoulders and capture this dusk.",
            "Some of the most beautiful poetry happens in simple silence.",
            "The evening light shifts. Let's save a piece of tonight."
        )
    }
    val randomizedSaying = remember { peacefulSayings.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("home_screen_root")
    ) {
        // Poetic Top Date Header matching Frosted Glass mock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val dayName = remember { SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()) }
                // Format matching "October 24th" custom style
                val dateString = remember {
                    val base = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())
                    val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    val suffix = when {
                        day in 11..13 -> "th"
                        day % 10 == 1 -> "st"
                        day % 10 == 2 -> "nd"
                        day % 10 == 3 -> "rd"
                        else -> "th"
                    }
                    "$base$suffix"
                }
                Text(
                    text = dayName.uppercase(Locale.getDefault()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA8A29E), // text-stone-400
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateString,
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF292524), // text-stone-800
                    fontWeight = FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.6f), CircleShape)
                    .border(1.dp, Color(0xFFF5F5F4), CircleShape) // border-stone-100
                    .shadow(1.dp, CircleShape)
                    .clickable { /* No-op poetic button */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Today",
                    tint = Color(0xFF44403C),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Shortcut Chips for Gamification features
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Challenges Button
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo("Challenges") }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Thử thách",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Thử thách",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917)
                    )
                }
            }

            // Map Button
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo("Map") }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Bản đồ",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bản đồ",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917)
                    )
                }
            }

            // Time Capsules Button
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo("TimeCapsules") }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Hộp quà",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hộp quà",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrapbook Grid (2x2) of the 4 daily capture blocks with polaroid-card feel
        Text(
            text = "Today's moments".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Cards display layout
        val slots = listOf("Morning", "Noon", "Evening", "Night")
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Group slots into rows of 2 for beautiful layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DailySlotCard(
                    slotName = "Morning",
                    memory = todayMap["Morning"],
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.selectMemory(it) },
                    onCapture = {
                        viewModel.targetCaptureSlot.value = "Morning"
                        viewModel.navigateTo("Camera")
                    }
                )
                DailySlotCard(
                    slotName = "Noon",
                    memory = todayMap["Noon"],
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.selectMemory(it) },
                    onCapture = {
                        viewModel.targetCaptureSlot.value = "Noon"
                        viewModel.navigateTo("Camera")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DailySlotCard(
                    slotName = "Evening",
                    memory = todayMap["Evening"],
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.selectMemory(it) },
                    onCapture = {
                        viewModel.targetCaptureSlot.value = "Evening"
                        viewModel.navigateTo("Camera")
                    }
                )
                DailySlotCard(
                    slotName = "Night",
                    memory = todayMap["Night"],
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.selectMemory(it) },
                    onCapture = {
                        viewModel.targetCaptureSlot.value = "Night"
                        viewModel.navigateTo("Camera")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Comforting Quote microcard styled with Frosted Glass theme
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                            ),
                            shape = CircleShape
                        )
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Day Star",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = randomizedSaying,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF44403C), // stone-700
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Gemini Private Emotional Mirror (Aura Reflection block)
        Text(
            text = "Gemini emotional mirror".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF78716C), // stone-500
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.65f)), // Frosted White panel
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reflective Aura Mirror",
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917) // stone-900
                    )
                    
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Aura magic",
                        tint = Color(0xFFF43F5E), // matching warm pink dusk
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "A private AI mirroring companion. Gathers your captured captions and thoughts from this week to evict a calm, poetic mirror of your emotional aura.",
                    fontSize = 12.sp,
                    color = Color(0xFF78716C), // stone-500
                    lineHeight = 17.sp,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (auraReflection.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = auraReflection,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFF1C1917), // stone-900
                            lineHeight = 20.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { viewModel.generateAuraInterpretation() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1917)), // dark stone-900 button
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("aura_consult_button"),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isAuraLoading
                ) {
                    if (isAuraLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Evoke Aura Reflection",
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun DailySlotCard(
    slotName: String,
    memory: Memory?,
    modifier: Modifier = Modifier,
    onSelect: (Memory) -> Unit,
    onCapture: () -> Unit
) {
    if (memory != null) {
        // Render Polaroid card representation if captured!
        MemoryCard(
            memory = memory,
            modifier = modifier,
            onClick = { onSelect(memory) }
        )
    } else {
        // Empty slot invitation card styled with Frosted Glass period gradient colors
        val bgBrush = when (slotName) {
            "Morning" -> androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFFFFEDD5).copy(alpha = 0.65f), Color(0xFFFFFBEB).copy(alpha = 0.65f))
            )
            "Noon" -> androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFFE0F2FE).copy(alpha = 0.65f), Color(0xFFEFF6FF).copy(alpha = 0.65f))
            )
            "Evening" -> androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFFFFE4E6).copy(alpha = 0.65f), Color(0xFFFFF7ED).copy(alpha = 0.65f))
            )
            else -> androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFFF5F5F4).copy(alpha = 0.7f), Color(0xFFE7E5E4).copy(alpha = 0.7f))
            )
        }

        val themeBorder = when (slotName) {
            "Morning" -> Color(0xFFFDE68A).copy(alpha = 0.8f)
            "Noon" -> Color(0xFF93C5FD).copy(alpha = 0.8f)
            "Evening" -> Color(0xFFFCA5A5).copy(alpha = 0.8f)
            else -> Color(0xFFD6D3D1).copy(alpha = 0.8f) // stone-300
        }

        val iconGradColors = when (slotName) {
            "Morning" -> listOf(Color(0xFFF97316), Color(0xFFFCD34D)) // Warm orange/amber
            "Noon" -> listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8)) // Sky blue
            "Evening" -> listOf(Color(0xFFF43F5E), Color(0xFFFDA4AF)) // Sunset sunrise rose
            else -> listOf(Color(0xFF475569), Color(0xFF94A3B8)) // Slate gray
        }

        val descriptor = when (slotName) {
            "Morning" -> "Sunrise light..."
            "Noon" -> "Midday shadows..."
            "Evening" -> "Catch the light..."
            else -> "Waiting for stars..."
        }

        Box(
            modifier = modifier
                .aspectRatio(0.85f)
                .shadow(2.dp, RoundedCornerShape(32.dp))
                .background(bgBrush, RoundedCornerShape(32.dp))
                .border(BorderStroke(1.2.dp, themeBorder), RoundedCornerShape(32.dp))
                .clickable { onCapture() }
                .testTag("empty_slot_$slotName"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(14.dp)
            ) {
                // Mock Photo circular play button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = iconGradColors),
                            shape = CircleShape
                        )
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (slotName == "Night") Icons.Default.DarkMode else Icons.Default.AddAPhoto,
                        contentDescription = "Capture $slotName",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = slotName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF57534E), // stone-600
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = descriptor,
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF78716C), // stone-500
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CameraScreen(viewModel: MomentsViewModel) {
    val context = LocalContext.current
    val slot by viewModel.targetCaptureSlot.collectAsStateWithLifecycle()
    val filter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val caption by viewModel.activeCaption.collectAsStateWithLifecycle()
    val mood by viewModel.activeMood.collectAsStateWithLifecycle()
    val location by viewModel.activeLocation.collectAsStateWithLifecycle()
    val presetName by viewModel.selectedPresetName.collectAsStateWithLifecycle()
    val unlockedFilters by viewModel.unlockedFilters.collectAsStateWithLifecycle()
    val isTimeCapsule by viewModel.isTimeCapsule.collectAsStateWithLifecycle()
    val unlockDelayHours by viewModel.unlockDelayHours.collectAsStateWithLifecycle()

    val availablePresets = viewModel.presetAesthetics[slot] ?: emptyList()

    // Activity camera picker to allow importing real device photos too!
    val realPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { takePictureBitmap ->
        if (takePictureBitmap != null) {
            viewModel.saveNostalgicMemory(context, takePictureBitmap)
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            realPhotoLauncher.launch()
        } else {
            android.widget.Toast.makeText(context, "Quyền truy cập máy ảnh bị từ chối", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("camera_screen_root")
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.navigateTo("Home") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preserve $slot",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large simulated analog film screen frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(8.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Analog generated visual composition view
            MemoryImageView(
                photoPath = "", // Simulate
                timeOfDay = slot,
                filterApplied = filter,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic viewports overlays (soft grain noise filter simulation)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.05f)
                    .background(Color.White)
            )

            // Physical Film camera details badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${filter.uppercase()}  •  STAMP ON",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFFB923C)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SELECT REAL CAMERA VS SIMULATED COMPOSE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        realPhotoLauncher.launch()
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("device_camera_button")
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Device Camera", fontSize = 12.sp, fontFamily = FontFamily.Serif)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CHOOSE PRESET LANDSCAPE THEMES
        Text(
            text = "Atmosphere Template (Simulation Mode)".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            availablePresets.forEach { preset ->
                val isSelected = preset.title == presetName
                Box(
                    modifier = Modifier
                        .border(
                            1.5.dp,
                            if (isSelected) getTimeOfDayColor(slot) else Color(0xFFE2E8F0),
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (isSelected) getTimeOfDayColor(slot).copy(alpha = 0.08f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.selectedPresetName.value = preset.title
                            viewModel.activeCaption.value = preset.caption
                            viewModel.activeLocation.value = preset.location
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = preset.title,
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = if (isSelected) getTimeOfDayColor(slot) else Color(0xFF64748B),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CHOOSE VINTAGE FILTERS
        Text(
            text = "Film Filter Effects".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        val filters = unlockedFilters.toList()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filterOpt ->
                val isSelected = filterOpt == filter
                Box(
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.activeFilter.value = filterOpt }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filterOpt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // FORM FIELDS
        Text(
            text = "Personal Caption & Feelings".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { viewModel.activeCaption.value = it },
            placeholder = { Text("Write down what your soul feels right now...", fontFamily = FontFamily.Serif, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("caption_field"),
            shape = RoundedCornerShape(8.dp),
            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF818CF8),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mood Selector
            var showMoodDropdown by remember { mutableStateOf(false) }
            val moods = listOf("Peaceful", "Nostalgic", "Quiet", "Cozy", "Reflective", "Grateful")
            
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = mood,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Emotion", fontSize = 11.sp) },
                    trailingIcon = { IconButton(onClick = { showMoodDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth().testTag("mood_field"),
                    shape = RoundedCornerShape(8.dp)
                )
                DropdownMenu(
                    expanded = showMoodDropdown,
                    onDismissRequest = { showMoodDropdown = false }
                ) {
                    moods.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item, fontFamily = FontFamily.Serif) },
                            onClick = {
                                viewModel.activeMood.value = item
                                showMoodDropdown = false
                            }
                        )
                    }
                }
            }

            // Location Box
            OutlinedTextField(
                value = location,
                onValueChange = { viewModel.activeLocation.value = it },
                label = { Text("Location", fontSize = 11.sp) },
                placeholder = { Text("Window Seat", fontSize = 11.sp) },
                modifier = Modifier.weight(1f).testTag("location_field"),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // TIME CAPSULE LOCK OPTION
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Khóa Hộp quà Thời gian",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Ảnh sẽ bị khóa và hiển thị dưới dạng quà tặng kèm đếm ngược.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                
                Switch(
                    checked = isTimeCapsule,
                    onCheckedChange = { viewModel.isTimeCapsule.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFFD1FAE5)
                    )
                )
            }
        }

        if (isTimeCapsule) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Thời gian khóa hộp quà:",
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = Color(0xFF44403C)
                )

                var expandedDelay by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expandedDelay = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1C1917)),
                        border = BorderStroke(1.dp, Color(0xFFE7E5E4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "$unlockDelayHours giờ", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    
                    DropdownMenu(
                        expanded = expandedDelay,
                        onDismissRequest = { expandedDelay = false }
                    ) {
                        listOf(1, 2, 4, 8, 12, 24).forEach { hours ->
                            DropdownMenuItem(
                                text = { Text("$hours giờ", fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    viewModel.unlockDelayHours.value = hours
                                    expandedDelay = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SHUTTER CONFIRM BUTTON
        Button(
            onClick = { viewModel.saveNostalgicMemory(context) },
            colors = ButtonDefaults.buttonColors(containerColor = getTimeOfDayColor(slot)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("shutter_save_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Camera, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SNAP SHUTTER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
fun TimelineScreen(viewModel: MomentsViewModel) {
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()
    var selectedMoodFilter by remember { mutableStateOf("All") }

    val filteredMemories = remember(memories, selectedMoodFilter) {
        if (selectedMoodFilter == "All") memories
        else memories.filter { it.mood.lowercase() == selectedMoodFilter.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
            .testTag("timeline_screen_root")
    ) {
        Text(
            text = "PRESERVED ARCHIVE",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Your memory thread",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mood Filtering tabs
        val moodFilters = listOf("All", "Quiet", "Nostalgic", "Peaceful", "Reflective")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            moodFilters.forEach { moodTab ->
                val isSelected = moodTab == selectedMoodFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) Color(0xFF1C1917) else Color.White.copy(alpha = 0.5f))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.6f)
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { selectedMoodFilter = moodTab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = moodTab,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        color = if (isSelected) Color.White else Color(0xFF44403C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AllInbox,
                        contentDescription = "Empty memory archive",
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your archive is still quiet.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Take a moment photo to see it blossom.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        } else {
            // Lazy vertical column of cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("timeline_list")
            ) {
                items(filteredMemories) { mem ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MemoryCard(
                            memory = mem,
                            modifier = Modifier
                                .width(310.dp)
                                .graphicsLayer {
                                    // Slight organic random offset/tilt based on memory id for aesthetic look
                                    rotationZ = (mem.id % 5 - 2).toFloat()
                                },
                            onClick = { viewModel.selectMemory(mem) }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: MomentsViewModel) {
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()

    // Calculate metrics
    val totalCount = memories.size
    val slots = listOf("Morning", "Noon", "Evening", "Night")
    val countBySlot = remember(memories) {
        slots.associateWith { slot -> memories.count { it.timeOfDay == slot } }
    }
    val countByMood = remember(memories) {
        memories.groupBy { it.mood }.mapValues { it.value.size }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("aura_screen_root")
    ) {
        Text(
            text = "MINDFUL METRICS",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Your inner climate",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Glowing Orbs Weather Map representation
        Text(
            text = "Emotional atmospheric weather".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // dark starry night canvas
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive customizable drawing mapping emotional aura
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .blur(25.dp) // creates glassmorphism blur atmospheric orb
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Soft floating color overlapping orbs based on slot logging frequencies!
                        val morningPct = if (totalCount > 0) (countBySlot["Morning"] ?: 0).toFloat() / totalCount else 0.25f
                        val noonPct = if (totalCount > 0) (countBySlot["Noon"] ?: 0).toFloat() / totalCount else 0.25f
                        val eveningPct = if (totalCount > 0) (countBySlot["Evening"] ?: 0).toFloat() / totalCount else 0.25f
                        val nightPct = if (totalCount > 0) (countBySlot["Night"] ?: 0).toFloat() / totalCount else 0.25f

                        // Draw Morning Gold orb
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = (40f + morningPct * 80f),
                            center = Offset(size.width * 0.4f, size.height * 0.4f)
                        )
                        // Draw Noon Blue orb
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = (40f + noonPct * 80f),
                            center = Offset(size.width * 0.6f, size.height * 0.35f)
                        )
                        // Draw Evening Peach orb
                        drawCircle(
                            color = Color(0xFFF97316),
                            radius = (40f + eveningPct * 80f),
                            center = Offset(size.width * 0.35f, size.height * 0.6f)
                        )
                        // Draw Night Indigo orb
                        drawCircle(
                            color = Color(0xFF818CF8),
                            radius = (40f + nightPct * 80f),
                            center = Offset(size.width * 0.65f, size.height * 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AURA ALIGNMENT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Representing your captured solar fractions.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Metric Rows
        Text(
            text = "Presets saved counts".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.75f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                slots.forEach { slot ->
                    val cnt = countBySlot[slot] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getTimeOfDayColor(slot), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = slot,
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                color = Color(0xFF1C1917) // stone-900
                            )
                        }
                        Text(
                            text = "$cnt preserved",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF57534E), // stone-600
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (slot != "Night") {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MOOD FRACTIONS
        Text(
            text = "Mindful Emotions spectrum".uppercase(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.75f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (countByMood.isEmpty()) {
                    Text(
                        text = "No emotions logged yet.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = Color(0xFFA8A29E), // stone-400
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    countByMood.forEach { (mood, cnt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "feeling '$mood'",
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                color = Color(0xFF1C1917) // stone-900
                            )
                            Text(
                                text = "$cnt moments",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFF57534E) // stone-600
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Đăng xuất",
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
