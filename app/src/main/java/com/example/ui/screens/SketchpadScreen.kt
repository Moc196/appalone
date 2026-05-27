package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MomentsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

// ──────────────── Data model ────────────────
data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val timestamp: Long          // millis when the stroke was begun
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
    Color(0xFF1C1917),  // stone-900  (default ink)
    Color(0xFFF43F5E),  // rose
    Color(0xFF0EA5E9),  // sky blue
    Color(0xFF10B981),  // emerald
    Color(0xFFF59E0B),  // amber
    Color(0xFF8B5CF6),  // violet
    Color(0xFFEC4899),  // pink
    Color(0xFF64748B),  // slate
    Color.White
)

private val brushSizes = listOf(4f, 8f, 14f, 22f)

// ──────────────── Main screen ───────────────────────────────
@Composable
fun SketchpadScreen(
    viewModel: MomentsViewModel,
    memoryId: Int? = null   // If non-null, load existing strokeData from that memory
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allMemories = androidx.lifecycle.compose.collectAsStateWithLifecycle(
        viewModel.allMemories
    ).value

    // Load strokes from an existing memory if provided
    val initialStrokes = remember(memoryId) {
        if (memoryId != null) {
            val mem = allMemories.firstOrNull { it.id == memoryId }
            StrokeSerializer.fromJson(mem?.strokeData)
        } else emptyList()
    }

    // Mutable drawing state
    var strokes by remember { mutableStateOf(initialStrokes) }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1C1917)) }
    var selectedSize by remember { mutableStateOf(4f) }
    var isEraser by remember { mutableStateOf(false) }

    // Replay state
    var isReplaying by remember { mutableStateOf(false) }
    var replayProgress by remember { mutableStateOf(0) }   // index into flat point list
    var snackMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when message is set
    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    // Flatten all strokes into a single list of (color, width, point) triples for replay
    data class ReplayPoint(val color: Color, val width: Float, val point: Offset, val strokeIdx: Int)

    val flatPoints: List<ReplayPoint> = remember(strokes) {
        val result = mutableListOf<ReplayPoint>()
        strokes.forEachIndexed { idx, stroke ->
            stroke.points.forEach { pt ->
                result.add(ReplayPoint(stroke.color, stroke.strokeWidth, pt, idx))
            }
        }
        result
    }

    // Replay strokes rendered up to replayProgress
    val replayStrokes: List<DrawStroke> = remember(replayProgress, strokes) {
        if (!isReplaying) return@remember strokes
        val grouped = mutableListOf<DrawStroke>()
        val groups = mutableMapOf<Int, MutableList<Offset>>()
        val meta = mutableMapOf<Int, Pair<Color, Float>>()
        for (i in 0 until minOf(replayProgress, flatPoints.size)) {
            val rp = flatPoints[i]
            groups.getOrPut(rp.strokeIdx) { mutableListOf() }.add(rp.point)
            meta[rp.strokeIdx] = Pair(rp.color, rp.width)
        }
        groups.entries.sortedBy { it.key }.forEach { (idx, pts) ->
            val (color, width) = meta[idx]!!
            grouped.add(DrawStroke(pts, color, width, 0L))
        }
        grouped
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
                            // Adaptive speed: faster for many points
                            val delay = if (total > 800) 4L else if (total > 300) 7L else 12L
                            replayProgress += maxOf(1, total / 600)
                            delay(delay)
                        }
                        replayProgress = total
                        delay(300)
                        isReplaying = false
                    }
                },
                onSave = {
                    val json = StrokeSerializer.toJson(strokes)
                    if (memoryId != null) {
                        val mem = allMemories.firstOrNull { it.id == memoryId }
                        if (mem != null) {
                            viewModel.updateMemory(mem.copy(strokeData = json))
                            snackMessage = "Đã lưu bản phác thảo vào ký ức!"
                        }
                    } else {
                        // Save as standalone sketch memory
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

            // ── Canvas Area ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                Canvas(
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
                    // Draw paper grid background
                    drawPaperGrid()

                    // Draw committed strokes (or replay strokes)
                    for (stroke in displayedStrokes) {
                        drawStrokePath(stroke)
                    }

                    // Draw in-progress stroke
                    if (currentPoints.size >= 2) {
                        val stroke = DrawStroke(
                            points = currentPoints,
                            color = if (isEraser) Color.White else selectedColor,
                            strokeWidth = if (isEraser) selectedSize * 3f else selectedSize,
                            timestamp = 0L
                        )
                        drawStrokePath(stroke)
                    }
                }

                // Replay badge overlay
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
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // ── Toolbar ───────────────────────────────────────────────
            DrawingToolbar(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it; isEraser = false },
                selectedSize = selectedSize,
                onSizeSelected = { selectedSize = it },
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1C1917))
            }
        },
        actions = {
            // Undo-all / Clear
            IconButton(onClick = onClear, enabled = !isReplaying) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear all", tint = Color(0xFFEF4444))
            }
            // Replay
            IconButton(onClick = onReplay, enabled = !isReplaying) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Replay", tint = Color(0xFF8B5CF6))
            }
            // Save
            IconButton(onClick = onSave, enabled = !isReplaying) {
                Icon(Icons.Default.Save, contentDescription = "Save sketch", tint = Color(0xFF10B981))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFFAF9F6)
        )
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

            // ── Color Palette ─────────────────────────────────
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

            // ── Brush Size ────────────────────────────────────
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

                // Eraser button
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

                // Undo button
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

            // ── Stroke counter ────────────────────────────────
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

// ──────────────── Canvas helpers ────────────────────────────
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
        // Single dot
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
        // Smooth catmull-rom-like midpoint curve
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
