package uz.sentinel.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.theme.*

data class StorageCategoryData(
    val name: String,
    val sizeBytes: Long,
    val color: Color
)

/**
 * Xotira axlat toifalari uchun zamonaviy Donut (Doiraviy diagramma) komponenti.
 * building-data-apps talablari bo'yicha interaktiv ma'lumotlar vizualizatsiyasi.
 */
@Composable
fun StorageDonutChart(
    modifier: Modifier = Modifier,
    categories: List<StorageCategoryData>,
    totalSizeBytes: Long,
    chartSize: Dp = 150.dp,
    strokeWidth: Dp = 16.dp
) {
    val totalMb = String.format("%.1f", totalSizeBytes / (1024.0 * 1024.0))
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(totalSizeBytes) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Markaziy Donut Diagrammasi
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                // Bo'sh fon aylanasi
                drawArc(
                    color = ObsidianSurfaceElev,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                if (totalSizeBytes > 0 && categories.isNotEmpty()) {
                    var currentStartAngle = -90f
                    for (category in categories) {
                        if (category.sizeBytes <= 0) continue
                        val sweep = (category.sizeBytes.toFloat() / totalSizeBytes.toFloat()) * 360f * animationProgress.value
                        
                        drawArc(
                            color = category.color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        currentStartAngle += sweep
                    }
                } else {
                    // Agar axlat topilmasa, toza holat nuri
                    drawArc(
                        color = NeonEmerald,
                        startAngle = -90f,
                        sweepAngle = 360f * animationProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx)
                    )
                }
            }

            // Markazdagi jami hajm yozuvi
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalMb,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "MB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Toifalar ro'yxati (Legend)
        Column(
            modifier = Modifier.padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val catMb = String.format("%.1f", category.sizeBytes / (1024.0 * 1024.0))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(category.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$catMb MB",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
