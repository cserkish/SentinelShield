package uz.sentinel.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.components.HeroPulseShield
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: DashboardViewModel,
    onNavigateToAntivirus: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToNetGuard: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToStability: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurfaceElev,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderTeal),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Image(
                                painter = painterResource(id = uz.sentinel.sentinelshield.R.drawable.sentinel_logo),
                                contentDescription = "SentinelShield Logo",
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "SentinelShield",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Kiber Himoya Tizimi",
                                fontSize = 11.sp,
                                color = CyberTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStability) {
                        Surface(
                            shape = CircleShape,
                            color = ObsidianSurfaceElev,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderWhite),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.BatterySaver,
                                    contentDescription = "Batareya sozlamasi",
                                    tint = CyberTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Markaziy Jonli Qalqon (Hero Pulse Shield & Health Telemetry)
            HeroPulseShield(
                isDeviceSafe = uiState.isDeviceSafe,
                healthScore = uiState.healthScore,
                riskCount = uiState.highRiskAppCount,
                onClick = onNavigateToAntivirus
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Telemetriya Qisqa Satri (building-data-apps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMetricBadge(
                    modifier = Modifier.weight(1f),
                    label = "Tekshirildi",
                    value = "${uiState.totalScannedApps}",
                    accentColor = CyberTeal
                )
                QuickMetricBadge(
                    modifier = Modifier.weight(1f),
                    label = "Tozalandi",
                    value = "${String.format("%.1f", uiState.lastCleanedMb)} MB",
                    accentColor = NeonEmerald
                )
                QuickMetricBadge(
                    modifier = Modifier.weight(1f),
                    label = "NetGuard",
                    value = if (uiState.isNetGuardActive) "Faol" else "O'chiq",
                    accentColor = if (uiState.isNetGuardActive) NeonCyan else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Asosiy Himoya Modullari",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberTeal.copy(alpha = 0.12f)
                ) {
                    Text(
                        "4 TA MODUL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberTeal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 2x2 Shishasimon Modullar To'plami (Frosted Glass Cards)
            Row(modifier = Modifier.fillMaxWidth()) {
                ModernModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Antivirus",
                    subtitle = if (uiState.highRiskAppCount > 0)
                        "${uiState.highRiskAppCount} ta xavf!"
                    else
                        "${uiState.totalScannedApps} ilova xavfsiz",
                    icon = Icons.Default.BugReport,
                    accentColor = if (uiState.highRiskAppCount > 0) NeonCrimson else CyberTeal,
                    onClick = onNavigateToAntivirus
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Tozalash",
                    subtitle = "Kesh va qoldiqlar",
                    icon = Icons.Default.CleaningServices,
                    accentColor = NeonEmerald,
                    onClick = onNavigateToCleaner
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ModernModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "AppLock",
                    subtitle = "${uiState.lockedAppCount} ta ilova qulflangan",
                    icon = Icons.Default.Lock,
                    accentColor = NeonViolet,
                    onClick = onNavigateToAppLock
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "NetGuard",
                    subtitle = if (uiState.isNetGuardActive) "DNS Himoya Faol" else "O'chirilgan",
                    icon = Icons.Default.VpnLock,
                    accentColor = NeonCyan,
                    onClick = onNavigateToNetGuard
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Maxfiylik va Ruxsatlar Auditi (Full-width Glass Card)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPermissions() },
                shape = RoundedCornerShape(20.dp),
                backgroundColor = ObsidianSurface,
                borderColor = GlassBorderWhite,
                borderWidth = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberTealDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderTeal),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Maxfiylik va Ruxsatlar Auditi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Kamera, mikrofon va kontaktlar monitoringi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun QuickMetricBadge(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = GlassBorderWhite,
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ModernModuleCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        backgroundColor = ObsidianSurface,
        borderColor = accentColor.copy(alpha = 0.25f),
        borderWidth = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
