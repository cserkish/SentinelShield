package uz.sentinel.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.sentinel.core.utils.LocaleHelper
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.dashboard.DashboardUiState
import uz.sentinel.presentation.theme.*
import uz.sentinel.sentinelshield.R
import uz.sentinel.service.ThreatUpdateWorker

@Composable
fun SettingsScreen(
    dashboardState: DashboardUiState,
    preferences: SentinelPreferences,
    onNavigateToStability: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToNetGuard: () -> Unit,
    onLanguageChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOfflineOnly by preferences.isOfflineOnlyMode.collectAsState(initial = false)
    val defaultLockMethod by preferences.appLockDefaultMethod.collectAsState(initial = "PIN")

    var showFeedUrlDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Tilni almashtirish (Language Selector Card)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = GlassBorderTeal,
            borderWidth = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = CyberTeal)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ilova Tili / Language / Язык",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "O'zingizga qulay tilni tanlang",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LocaleHelper.supportedLanguages.forEach { (code, label) ->
                        val selected = dashboardState.currentLanguage == code
                        FilterChip(
                            selected = selected,
                            onClick = {
                                scope.launch {
                                    preferences.setAppLanguage(code)
                                    onLanguageChanged()
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 2. Internetli va Internetsiz (Hybrid / Offline Mode) Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = GlassBorderWhite,
            borderWidth = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isOfflineOnly) Icons.Default.CloudOff else Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = if (isOfflineOnly) NeonAmber else NeonEmerald
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "To'liq Oflayn (Internetsiz) Rejim",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isOfflineOnly)
                                    "Faol: Ilova internet ishlatmasdan 100% ichki baza bilan ishlaydi"
                                else
                                    "Gibrid rejim: Internetsiz ham ishlaydi, internet bo'lsa bazani yangilaydi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isOfflineOnly,
                        onCheckedChange = { enabled ->
                            scope.launch { preferences.setOfflineOnlyMode(enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = NeonAmber
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFeedUrlDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub JSON URL", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            ThreatUpdateWorker.executeNow(context)
                            Toast.makeText(context, "Baza yangilanmoqda (v${dashboardState.threatFeedVersion})...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberTeal,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bazani Yangilash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Xavfsizlik va Qulflash Sozlamalari
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = GlassBorderWhite,
            borderWidth = 1.dp,
            onClick = onNavigateToAppLock
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = NeonViolet)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("AppLock va PIN / Barmoq izi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Joriy usul: $defaultLockMethod • ${dashboardState.lockedAppCount} ta ilova qulflangan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // 4. NetGuard VPN Sozlamalari
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = GlassBorderWhite,
            borderWidth = 1.dp,
            onClick = onNavigateToNetGuard
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.VpnLock, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("NetGuard Anti-Fishing DNS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (dashboardState.isNetGuardActive) "Holati: Faol" else "Holati: O'chiq",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // 5. Batareya va Fonda Uzluksiz Ishlash (OEM Stability Guide)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = GlassBorderWhite,
            borderWidth = 1.dp,
            onClick = onNavigateToStability
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = NeonEmerald)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Batareya va Avto-ishga tushish (OEM)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Xiaomi, Samsung, Redmi va Huawei uchun fon sozlamalari",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // 6. Ilova haqida
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurfaceElev,
            borderColor = GlassBorderWhite,
            borderWidth = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("SentinelShield Kiber-Himoya Tizimi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Versiya: 1.6.0 (Build 9) • Oflayn & Onlayn Gibrid AI Arxitektura • Nol Reklama",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showFeedUrlDialog) {
        var urlInput by remember { mutableStateOf(dashboardState.threatFeedUrl) }
        AlertDialog(
            onDismissRequest = { showFeedUrlDialog = false },
            title = { Text("GitHub Threat JSON Manbasi", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("GitHub Raw JSON URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferences.setThreatFeedUrl(urlInput)
                            ThreatUpdateWorker.executeNow(context)
                        }
                        showFeedUrlDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedUrlDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
