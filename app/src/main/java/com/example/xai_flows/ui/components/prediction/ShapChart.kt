package com.example.xai_flows.ui.components.prediction

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.models.ShapData
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextOverflow

// -------------------------
// ICON MAPPING FOR FEATURES
// -------------------------
private val featureIcons = mapOf(
    "temperature" to "🌡",
    "humidity" to "💧",
    "wind" to "🌬",
    "pressure" to "📉",
    "precipitation" to "☔",
    "visibility" to "👀",
    "uv" to "☀️"
)

// -------------------------
// MAIN CHART CONTAINER
// -------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapChart(
    data: List<ShapData>,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Positive", "Negative")
    var selectedTab by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var bottomSheetData by remember { mutableStateOf<ShapData?>(null) }
    var showShapInfoDialog by remember { mutableStateOf(false) }

    val filteredData = remember(selectedTab, data) {
        if (selectedTab == 0) data.filter { it.value >= 0 }
        else data.filter { it.value < 0 }
    }

    val displayedData = remember(expanded, filteredData) {
        if (expanded) filteredData else filteredData.take(5)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Animated shimmer gradient
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 4000, easing = LinearEasing),
            RepeatMode.Restart
        ), label = ""
    )

    Box {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0x55FFFFFF),
                            Color(0x22FFFFFF),
                            Color(0x55FFFFFF)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 500f, 1000f)
                    )
                )
                .padding(16.dp)
        ) {
            // Title + Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Weather Feature Impact (SHAP)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f) // ensures text doesn't overlap
                )

                IconButton(
                    onClick = { showShapInfoDialog = true },
                    modifier = Modifier.size(28.dp) // keep it visible and clickable
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info"
                    )
                }
            }

            // Tabs
            // Enhanced Tabs with gradient + glass effect
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index

                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 14.sp,           // decent size
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                softWrap = false,           // force single line
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 2.dp) // slight inner spacing
                            )
                        },
                        modifier = (
                                if (isSelected) {
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                } else {
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            color = Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                }
                                )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Chart with callback for drilldown
            ShapBarChart(
                data = displayedData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                onBarLongPress = { shapItem ->
                    bottomSheetData = shapItem
                }
            )

            Spacer(Modifier.height(12.dp))

            // Expand/Collapse Button
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show Less" else "View All")
            }
        }

        // Bottom Sheet
        bottomSheetData?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { bottomSheetData = null },
                sheetState = sheetState
            ) {
                ShapDetailSheet(item)
            }
        }

        // Info Dialog
        if (showShapInfoDialog) {
            ShapInfoDialog(onDismiss = { showShapInfoDialog = false })
        }
    }
}

// -------------------------
// BAR CHART WITH MEAN LINE
// -------------------------
@Composable
fun ShapBarChart(
    data: List<ShapData>,
    modifier: Modifier = Modifier,
    onBarLongPress: (ShapData) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = data.maxOfOrNull { abs(it.value) } ?: 1f
    val meanVal = data.map { abs(it.value) }.average().toFloat()

    Box(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(data) { index, item ->
                val normalizedHeight = (abs(item.value) / maxVal) * 200f
                val heightDp = with(LocalDensity.current) { normalizedHeight.toDp() }

                val animatedHeight by animateDpAsState(
                    targetValue = heightDp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ), label = ""
                )

                val infiniteTransition = rememberInfiniteTransition(label = "")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        tween(800, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ), label = ""
                )

                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(animatedHeight + 30.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    selectedIndex = if (selectedIndex == index) null else index
                                },
                                onLongPress = { onBarLongPress(item) }
                            )
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Bar
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(animatedHeight)
                    ) {
                        val gradient = Brush.verticalGradient(
                            colors = if (item.value >= 0)
                                listOf(Color(0xFF34D399), Color(0xFF059669))
                            else
                                listOf(Color(0xFFF87171), Color(0xFFB91C1C))
                        )

                        drawRoundRect(
                            brush = gradient,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )

                        if (selectedIndex == index) {
                            drawRoundRect(
                                color = if (item.value >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                topLeft = Offset(-4f, -4f),
                                size = androidx.compose.ui.geometry.Size(size.width + 8f, size.height + 8f),
                                cornerRadius = CornerRadius(14f, 14f),
                                alpha = glowAlpha,
                                style = Stroke(width = 3f)
                            )
                        }

                        // Mean SHAP dotted line
                        val meanY = size.height - (meanVal / maxVal) * size.height
                        val dashWidth = 10f
                        var currentX = 0f
                        while (currentX < size.width) {
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(currentX, meanY),
                                end = Offset(currentX + dashWidth, meanY),
                                strokeWidth = 2f
                            )
                            currentX += dashWidth * 2
                        }
                    }

                    // Icon + short label
                    val icon = featureIcons[item.feature.lowercase()] ?: ""
                    // Place label ABOVE the bar (end of bar height)
                    Text(
                        text = item.feature.take(5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-animatedHeight) - 16.dp) // 16.dp extra above bar
                    )
                }

                // Tooltip
                if (selectedIndex == index) {
                    ShapTooltipAboveBar(
                        item = item,
                        onDismiss = { selectedIndex = null },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-animatedHeight) - 20.dp) // 20dp gap above bar
                    )
                }
            }
        }


    }
}

// -------------------------
// TOOLTIP ABOVE BAR
// -------------------------
@Composable
fun ShapTooltipAboveBar(item: ShapData, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300), label = ""
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer { this.alpha = alpha }
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${item.feature}: ${"%.4f".format(item.value)} impact",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (item.value >= 0) "Positive Influence" else "Negative Influence",
                fontSize = 12.sp,
                color = if (item.value >= 0) Color(0xFF34D399) else Color(0xFFF87171)
            )
        }
    }
}

// -------------------------
// BOTTOM SHEET + TRENDLINE
// -------------------------
@Composable
fun ShapDetailSheet(item: ShapData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = item.feature,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Impact: ${"%.4f".format(item.value)}",
            fontSize = 16.sp,
            color = if (item.value >= 0) Color(0xFF059669) else Color(0xFFB91C1C)
        )

        Spacer(Modifier.height(16.dp))

        // Tiny Trendline (dummy data for now)
        TrendLineChart(values = listOf(-0.02f, 0.01f, 0.03f, item.value, 0.05f))
    }
}

@Composable
fun TrendLineChart(values: List<Float>) {
    val maxVal = values.maxOf { it }
    val minVal = values.minOf { it }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val stepX = size.width / (values.size - 1)
        val range = maxVal - minVal

        values.forEachIndexed { index, value ->
            if (index < values.lastIndex) {
                val x1 = index * stepX
                val y1 = size.height - ((value - minVal) / range) * size.height
                val x2 = (index + 1) * stepX
                val y2 = size.height - ((values[index + 1] - minVal) / range) * size.height

                drawLine(
                    color = Color(0xFF3B82F6),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 4f
                )
            }
        }
    }
}

// -------------------------
// INFO DIALOG
// -------------------------
@Composable
fun ShapInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What is SHAP?") },
        text = {
            Text(
                "SHAP (SHapley Additive exPlanations) explains ML predictions by showing how each feature contributed positively or negatively to the outcome."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
