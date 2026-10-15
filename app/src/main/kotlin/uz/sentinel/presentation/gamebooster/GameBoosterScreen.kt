package uz.sentinel.presentation.gamebooster

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoosterScreen(
    viewModel: GameBoosterViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val tel = uiState.telemetry

    val accentColor = if (uiState.isGameModeActive) NeonEmerald else CyberTeal
    val tempColor = when {
        tel.batteryTempCelsius >= 42f -> NeonCrimson
        tel.batteryTempCelsius >= 38f -> NeonAmber
        else -> NeonEmerald
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text("O'yin Rejimi & Tezlashtirish", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Og'ir yuklamadan xalos qilish (Game Turbo)",
                            fontSize = 11.sp,
                            color = CyberTeal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshTelemetryAndGames() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = CyberTeal)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. RAM, CPU/Harorat va Xotira Telemetriya Kartasi
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = ObsidianSurface,
                borderColor = accentColor.copy(alpha = 0.4f),
                borderWidth = 1.5.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (uiState.isGameModeActive) "O'YIN REJIMI FAOL 🚀" else "Standart Rejim",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = accentColor
                                )
                                Text(
                                    text = "Orqa fon jarayonlarini to'xtatib, FPS ni oshiradi",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isGameModeActive,
                            onCheckedChange = { viewModel.toggleGameMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = NeonEmerald
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 ta jonli ko'rsatkich: RAM, Harorat, Bo'sh joy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMiniBox(
                            modifier = Modifier.weight(1f),
                            title = "RAM Yuklama",
                            value = "${tel.ramUsagePercent}%",
                            sub = "${tel.freeRamMb} MB bo'sh",
                            color = if (tel.ramUsagePercent > 80) NeonAmber else CyberTeal
                        )
                        TelemetryMiniBox(
                            modifier = Modifier.weight(1f),
                            title = "Harorat",
                            value = "${String.format("%.1f", tel.batteryTempCelsius)}°C",
                            sub = if (tel.batteryTempCelsius >= 40f) "Qizigan" else "Normal",
                            color = tempColor
                        )
                        TelemetryMiniBox(
                            modifier = Modifier.weight(1f),
                            title = "Bo'sh Xotira",
                            value = "${String.format("%.1f", tel.freeStorageGb)} GB",
                            sub = "Jami ${String.format("%.0f", tel.totalStorageGb)} GB",
                            color = NeonCyan
                        )
                    }

                    if (uiState.boostMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.boostMessage!!,
                                color = NeonEmerald,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.boostSystemAndLaunchGame(null) },
                        enabled = !uiState.isBoosting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberTeal,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isBoosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ObsidianBlack,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RAM va CPU optimallashtirilmoqda...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1-Bosishda RAM Bo'shatish va Tezlashtirish", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "O'yinlar va Ilovalarni Tezlashtirib Ochish (${uiState.gamesList.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.gamesList, key = { it.packageName }) { game ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = ObsidianSurfaceElev,
                        borderColor = if (game.isDetectedGame) NeonEmerald.copy(alpha = 0.35f) else GlassBorderWhite,
                        borderWidth = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = game.appName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (game.isDetectedGame) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = NeonEmerald.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "🎮 O'YIN",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NeonEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = game.packageName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = { viewModel.boostSystemAndLaunchGame(game.packageName) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonEmerald.copy(alpha = 0.18f),
                                    contentColor = NeonEmerald
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Boost & Ochish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceElev,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
