package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MomentsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// ──────────────── Data model ────────────────
data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val timestamp: Long
)

// ──────────────── Serialization helpers ─────────────────────
object StrokeSerializer {
    fun toJson(strokes: List<DrawStroke>): String {
        val arr = JSONArray()
        for (stroke in strokes) {
            val obj = JSONObject()
            obj.put("color", stroke.color.toArgb())
            obj.put("width", stroke.strokeWidth)
            obj.put("ts", stroke.timestamp)
            val pts = JSONArray()
            for (p in stroke.points) {
                val pt = JSONObject()
                pt.put("x", p.x)
                pt.put("y", p.y)
                pts.put(pt)
            }
            obj.put("points", pts)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun fromJson(json: String?): List<DrawStroke> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<DrawStroke>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val color = Color(obj.getInt("color"))
                val width = obj.getDouble("width").toFloat()
                val ts = obj.getLong("ts")
                val ptsArr = obj.getJSONArray("points")
                val points = mutableListOf<Offset>()
                for (j in 0 until ptsArr.length()) {
                    val pt = ptsArr.getJSONObject(j)
                    points.add(Offset(pt.getDouble("x").toFloat(), pt.getDouble("y").toFloat()))
                }
                result.add(DrawStroke(points, color, width, ts))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// ──────────────── Color palette ─────────────────────────────
private val paletteColors = listOf(
    Color(0xFF1C1917),
    Color(0xFFF43F5E),
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF64748B),
    Color.White
)

private val brushSizes = listOf(4f, 8f, 14f, 22f)

// ──────────────── Replay point helper ───────────────────────
private data class ReplayPoint(
    val color: Color,
    val width: Float,
    val point: Offset,
    val strokeIdx: Int
)

// ──────────────── Main screen ───────────────────────────────
@Composable
fun SketchpadScreen(
    viewModel: MomentsViewModel,
    memoryId: Int? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allMemories by viewModel.allMemories.collectAsStateWithLifecycle()

    // Load strokes from an existing memory if provided
    val initialStrokes = remember(memoryId, allMemories) {
        if (memoryId != null) {
            val mem = allMemories.firstOrNull { memory -> memory.id == memoryId }
            StrokeSerializer.fromJson(mem?.strokeData)
        } else {
            emptyList()
        }
    }

    var strokes by remember { mutableStateOf(initialStrokes) }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1C1917)) }
    var selectedSize by remember { mutableStateOf(4f) }
    var isEraser by remember { mutableStateOf(false) }
    var isReplaying by remember { mutableStateOf(false) }
    var replayProgress by remember { mutableStateOf(0) }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            snackMessage = null
        }
    }

    // Flatten all strokes → list of ReplayPoints for smooth replay
    val flatPoints: List<ReplayPoint> = remember(strokes) {
        val result = mutableListOf<ReplayPoint>()
        strokes.forEachIndexed { idx, stroke ->
            stroke.points.forEach { pt ->
                result.add(ReplayPoint(stroke.color, stroke.strokeWidth, pt, idx))
            }
        }
        result
    }

    // Rebuild visible strokes up to replayProgress during replay
    val replayStrokes: List<DrawStroke> = remember(replayProgress, strokes) {
        if (!isReplaying) return@remember strokes
        val groups = mutableMapOf<Int, MutableList<Offset>>()
        val meta = mutableMapOf<Int, Pair<Color, Float>>()
        val limit = minOf(replayProgress, flatPoints.size)
        for (i in 0 until limit) {
            val rp = flatPoints[i]
            groups.getOrPut(rp.strokeIdx) { mutableListOf() }.add(rp.point)
            meta[rp.strokeIdx] = Pair(rp.color, rp.width)
        }
        val result = mutableListOf<DrawStroke>()
        groups.entries.sortedBy { entry -> entry.key }.forEach { entry ->
            val colorAndWidth = meta[entry.key]!!
            result.add(DrawStroke(entry.value, colorAndWidth.first, colorAndWidth.second, 0L))
        }
        result
    }

    val displayedStrokes = if (isReplaying) replayStrokes else strokes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFAF9F6),
        topBar = {
            SketchpadTopBar(
                onBack = { viewModel.navigateTo("Home") },
                onClear = {
                    strokes = emptyList()
                    isReplaying = false
                },
                onReplay = {
                    if (strokes.isEmpty()) {
                        snackMessage = "Chưa có nét vẽ nào để phát lại!"
                        return@SketchpadTopBar
                    }
                    isReplaying = true
                    replayProgress = 0
                    scope.launch {
                        val total = flatPoints.size
                        while (replayProgress < total) {
                            val delayMs = if (total > 800) 4L else if (total > 300) 7L else 12L
                            replayProgress = minOf(replayProgress + maxOf(1, total / 600), total)
                            delay(delayMs)
                        }
                        delay(300)
                        isReplaying = false
                    }
                },
                onSave = {
                    val json = StrokeSerializer.toJson(strokes)
                    if (memoryId != null) {
                        val mem = allMemories.firstOrNull { memory -> memory.id == memoryId }
                        if (mem != null) {
                            viewModel.updateMemory(mem.copy(strokeData = json))
                            snackMessage = "Đã lưu bản phác thảo vào ký ức!"
                        }
                    } else {
                        viewModel.saveSketchMemory(context, json)
                        snackMessage = "Đã lưu bản phác thảo mới!"
                    }
                },
                isReplaying = isReplaying
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Canvas Area ───────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isEraser, selectedColor, selectedSize, isReplaying) {
                            if (isReplaying) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        val strokeColor = if (isEraser) Color.White else selectedColor
                                        strokes = strokes + DrawStroke(
                                            points = currentPoints,
                                            color = strokeColor,
                                            strokeWidth = if (isEraser) selectedSize * 3f else selectedSize,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        currentPoints = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    drawPaperGrid()

                    for (stroke in displayedStrokes) {
                        drawStrokePath(stroke)
                    }

                    if (currentPoints.size >= 2) {
                        drawStrokePath(
                            DrawStroke(
                                points = currentPoints,
                                color = if (isEraser) Color.White else selectedColor,
                                strokeWidth = if (isEraser) selectedSize * 3f else selectedSize,
                                timestamp = 0L
                            )
                        )
                    }
                }

                // Replay badge
                if (isReplaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color(0xFFF43F5E).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Replay",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Replaying",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Empty state hint
                if (strokes.isEmpty() && currentPoints.isEmpty() && !isReplaying) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Draw here",
                                tint = Color(0xFFD6D3D1),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Phác thảo ký ức của bạn...",
                                color = Color(0xFFD6D3D1),
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // ── Drawing Toolbar ───────────────────────────────
            DrawingToolbar(
                selectedColor = selectedColor,
                onColorSelected = { color -> selectedColor = color; isEraser = false },
                selectedSize = selectedSize,
                onSizeSelected = { size -> selectedSize = size },
                isEraser = isEraser,
                onEraserToggle = { isEraser = !isEraser },
                onUndo = {
                    if (strokes.isNotEmpty()) {
                        strokes = strokes.dropLast(1)
                    }
                },
                strokeCount = strokes.size
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ──────────────── Top Bar ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SketchpadTopBar(
    onBack: () -> Unit,
    onClear: () -> Unit,
    onReplay: () -> Unit,
    onSave: () -> Unit,
    isReplaying: Boolean
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Sketchpad",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1917)
                )
                Text(
                    "Phác thảo & phát lại ký ức",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = Color(0xFF78716C)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1C1917))
            }
        },
        actions = {
            IconButton(onClick = onClear, enabled = !isReplaying) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear all", tint = Color(0xFFEF4444))
            }
            IconButton(onClick = onReplay, enabled = !isReplaying) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Replay", tint = Color(0xFF8B5CF6))
            }
            IconButton(onClick = onSave, enabled = !isReplaying) {
                Icon(Icons.Default.Save, contentDescription = "Save sketch", tint = Color(0xFF10B981))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF9F6))
    )
}

// ──────────────── Drawing Toolbar ───────────────────────────
@Composable
private fun DrawingToolbar(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    selectedSize: Float,
    onSizeSelected: (Float) -> Unit,
    isEraser: Boolean,
    onEraserToggle: () -> Unit,
    onUndo: () -> Unit,
    strokeCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Color Palette Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Màu",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78716C),
                    modifier = Modifier.width(34.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paletteColors) { color ->
                        val isSelected = !isEraser && selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 28.dp)
                                .shadow(if (isSelected) 4.dp else 1.dp, CircleShape)
                                .background(color, CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF1C1917) else Color(0xFFE7E5E4),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(color) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Brush Size + Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nét",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78716C),
                    modifier = Modifier.width(34.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    brushSizes.forEach { size ->
                        val isSelected = selectedSize == size
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isSelected) Color(0xFF1C1917) else Color(0xFFF5F5F4),
                                    CircleShape
                                )
                                .border(1.dp, Color(0xFFE7E5E4), CircleShape)
                                .clickable { onSizeSelected(size) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((size / 2f + 2f).dp)
                                    .background(
                                        if (isSelected) Color.White else Color(0xFF1C1917),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Eraser
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isEraser) Color(0xFFFEF3C7) else Color(0xFFF5F5F4),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isEraser) Color(0xFFFCD34D) else Color(0xFFE7E5E4),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onEraserToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoFixOff,
                        contentDescription = "Eraser",
                        tint = if (isEraser) Color(0xFFD97706) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Undo
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF5F5F4), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE7E5E4), RoundedCornerShape(10.dp))
                        .clickable(enabled = strokeCount > 0) { onUndo() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (strokeCount > 0) Color(0xFF64748B) else Color(0xFFD6D3D1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (strokeCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "$strokeCount nét vẽ  •  nhấn ▶ để phát lại",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA8A29E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ──────────────── Canvas draw helpers ───────────────────────
private fun DrawScope.drawPaperGrid() {
    val gridColor = Color(0xFFF1F0EE)
    val step = 28f
    var x = 0f
    while (x <= size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

private fun DrawScope.drawStrokePath(stroke: DrawStroke) {
    if (stroke.points.size < 2) {
        if (stroke.points.isNotEmpty()) {
            drawCircle(
                color = stroke.color,
                radius = stroke.strokeWidth / 2f,
                center = stroke.points.first()
            )
        }
        return
    }
    val path = Path()
    path.moveTo(stroke.points[0].x, stroke.points[0].y)
    for (i in 1 until stroke.points.size) {
        val prev = stroke.points[i - 1]
        val curr = stroke.points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticBezierTo(prev.x, prev.y, midX, midY)
    }
    path.lineTo(stroke.points.last().x, stroke.points.last().y)

    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(
            width = stroke.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
