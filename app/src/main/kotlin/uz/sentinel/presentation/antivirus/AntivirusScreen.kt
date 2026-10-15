package uz.sentinel.presentation.antivirus

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.domain.model.DangerLevel
import uz.sentinel.domain.model.ScanResult
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*
import uz.sentinel.sentinelshield.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntivirusScreen(
    viewModel: AntivirusViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val statusColor = when {
        uiState.highRiskCount > 0 -> NeonCrimson
        else -> CyberTeal
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text(stringResource(R.string.antivirus_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.scanAllApps() },
                        enabled = !uiState.isScanning
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Qayta skanerlash", tint = CyberTeal)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = ObsidianSurface,
                borderColor = statusColor.copy(alpha = 0.35f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isScanning) {
                        ScanningRadar(color = CyberTeal)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Chuqur tahlil (Ilovalar + APK fayllar)...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = CyberTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.currentScanningApp.ifEmpty { "Ilovalar va xotiradagi APK fayllar tekshirilmoqda..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { uiState.scanProgress },
                            color = CyberTeal,
                            trackColor = ObsidianSurfaceElev,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor.copy(alpha = 0.4f)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.highRiskCount > 0) Icons.Default.Warning else Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (uiState.highRiskCount > 0)
                                "${uiState.highRiskCount} ta xavfli ilova / APK fayl topildi!"
                            else
                                "Qurilma va xotira to'liq himoyalangan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "O'rnatilgan ilovalar va Download/Telegram ichidagi (${uiState.uninstalledApkCount} ta) o'rnatilmagan APK fayllar tekshirildi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.scanAllApps() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal,
                                contentColor = ObsidianBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chuqur skanerlash (Ilovalar + APK)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tahlil natijalari (${uiState.results.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianSurfaceElev
                ) {
                    Text(
                        text = "DEEP APK + SHA-256",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.results, key = { it.packageName }) { result ->
                    ModernScanResultItem(
                        result = result,
                        onTrustToggle = { viewModel.toggleTrustApp(result.packageName, result.isUserTrusted) },
                        onUninstall = { viewModel.uninstallApp(result.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanningRadar(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    Box(
        modifier = Modifier.size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = radius * 0.6f,
                style = Stroke(width = 1.dp.toPx())
            )

            val sweepBrush = Brush.sweepGradient(
                0.0f to Color.Transparent,
                0.7f to color.copy(alpha = 0.1f),
                1.0f to color,
                center = center
            )

            rotate(degrees = angle, pivot = center) {
                drawCircle(
                    brush = sweepBrush,
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun ModernScanResultItem(
    result: ScanResult,
    onTrustToggle: () -> Unit,
    onUninstall: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = if (result.isSystemApp)
            GlassBorderWhite
        else when (result.dangerLevel) {
            DangerLevel.SAFE -> GlassBorderTeal
            DangerLevel.MEDIUM -> NeonAmber.copy(alpha = 0.3f)
            DangerLevel.HIGH -> NeonCrimson.copy(alpha = 0.4f)
        },
        borderWidth = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (result.isUninstalledApkFile) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonAmber.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "📦 O'rnatilmagan APK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = result.apkFilePath ?: result.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (result.isSystemApp) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonEmerald.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Tizim ilovasi / Xavfsiz",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    val badgeColor = when (result.dangerLevel) {
                        DangerLevel.SAFE -> NeonEmerald
                        DangerLevel.MEDIUM -> NeonAmber
                        DangerLevel.HIGH -> NeonCrimson
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${result.dangerScore}/100",
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!result.isSystemApp && result.detectedPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aniqlangan xavflar: ${result.detectedPermissions.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.dangerLevel == DangerLevel.HIGH) NeonCrimson else NeonAmber
                )
            }

            if (!result.isSystemApp && (result.dangerLevel == DangerLevel.HIGH || result.isUninstalledApkFile) && !result.isUserTrusted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onTrustToggle,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ishonaman")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUninstall,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson)
                    ) {
                        Text(if (result.isUninstalledApkFile) "APK faylni o'chirish" else "O'chirish")
                    }
                }
            }
        }
    }
}
