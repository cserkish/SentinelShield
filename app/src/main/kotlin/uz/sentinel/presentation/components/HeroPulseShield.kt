package uz.sentinel.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.theme.CyberTeal
import uz.sentinel.presentation.theme.NeonAmber
import uz.sentinel.presentation.theme.NeonCrimson
import uz.sentinel.presentation.theme.NeonEmerald
import uz.sentinel.presentation.theme.ObsidianSurfaceElev

/**
 * Bosh ekrandagi markaziy jonli qalqon (Hero Pulse Shield).
 * Qurilmaning xavfsizlik salomatlik indeksini (0-100%) jonli animatsiyali
 * aylanuvchi nur halqasi va pulsatsiya to'lqini bilan ifodalaydi.
 */
@Composable
fun HeroPulseShield(
    modifier: Modifier = Modifier,
    isDeviceSafe: Boolean,
    healthScore: Int = 100, // 0..100
    riskCount: Int = 0,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Pulsatsiya animatsiyasi
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Nur aylanishi burchagi
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val mainColor = when {
        !isDeviceSafe || riskCount > 0 -> NeonCrimson
        healthScore < 85 -> NeonAmber
        else -> CyberTeal
    }

    val glowColor = mainColor.copy(alpha = 0.25f)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        borderColor = mainColor.copy(alpha = 0.35f),
        borderWidth = 1.5.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animatsiyali Jonli Qalqon Doirasi
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsatsiyalanuvchi Fon To'lqini
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2f - 16.dp.toPx()) * pulseScale
                    
                    // Tashqi mayin nurlanish
                    drawCircle(
                        color = glowColor,
                        radius = radius + 8.dp.toPx()
                    )

                    // Aylanuvchi Sweep Gradient halqasi
                    val strokeWidth = 5.dp.toPx()
                    val sweepBrush = Brush.sweepGradient(
                        0.0f to mainColor.copy(alpha = 0.1f),
                        0.5f to mainColor,
                        1.0f to mainColor.copy(alpha = 0.1f),
                        center = center
                    )

                    rotate(degrees = rotationAngle, pivot = center) {
                        drawCircle(
                            brush = sweepBrush,
                            radius = radius,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Markaziy Qalqon Doirasi
                Surface(
                    shape = CircleShape,
                    color = ObsidianSurfaceElev,
                    border = androidx.compose.foundation.BorderStroke(2.dp, mainColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isDeviceSafe && riskCount == 0) {
                            Image(
                                painter = painterResource(id = uz.sentinel.sentinelshield.R.drawable.sentinel_logo),
                                contentDescription = "Sentinel Shield Himoyasi",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = "Xavf",
                                tint = NeonCrimson,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Salomatlik Indeksi va Holat Matni
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$healthScore%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = mainColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = mainColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isDeviceSafe && riskCount == 0) "HIMOYALANGAN" else "TAHDID MAVJUD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isDeviceSafe && riskCount == 0)
                    "Barcha himoya modullari faol va xavfsiz"
                else
                    "$riskCount ta ilovada xavfli harakat aniqlandi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
