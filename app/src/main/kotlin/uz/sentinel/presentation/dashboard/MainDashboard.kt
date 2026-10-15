package uz.sentinel.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import uz.sentinel.core.utils.LocaleHelper
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.aiagent.AiActionTarget
import uz.sentinel.presentation.aiagent.AiAssistantScreen
import uz.sentinel.presentation.aiagent.AiAssistantViewModel
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.components.HeroPulseShield
import uz.sentinel.presentation.reporthelp.ReportHelpScreen
import uz.sentinel.presentation.settings.SettingsScreen
import uz.sentinel.presentation.theme.*
import uz.sentinel.sentinelshield.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: DashboardViewModel,
    preferences: SentinelPreferences,
    onNavigateToAntivirus: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToNetGuard: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToStability: () -> Unit,
    onNavigateToAppUninstall: () -> Unit = {},
    onNavigateToGameBooster: () -> Unit = {},
    onNavigateToWifiSecurity: () -> Unit = {},
    onLanguageChanged: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOfflineOnly by preferences.isOfflineOnlyMode.collectAsState(initial = false)
    val isGameModeActive by preferences.isGameModeActive.collectAsState(initial = false)

    var selectedBottomTab by rememberSaveable { mutableIntStateOf(0) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                                painter = painterResource(id = R.drawable.sentinel_logo),
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
                                stringResource(R.string.app_name),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isOfflineOnly) "🔵 Oflayn Avtonom Himoya" else "🟢 Onlayn & Oflayn Himoya",
                                fontSize = 11.sp,
                                color = CyberTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    // Quick Language Switcher Button
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Surface(
                            shape = CircleShape,
                            color = ObsidianSurfaceElev,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderWhite),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = "Tilni tanlash",
                                    tint = CyberTeal,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                    // Quick Game Mode / Boost Button
                    IconButton(onClick = onNavigateToGameBooster) {
                        Surface(
                            shape = CircleShape,
                            color = ObsidianSurfaceElev,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderTeal),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SportsEsports,
                                    contentDescription = "O'yin rejimi",
                                    tint = if (isGameModeActive) NeonEmerald else CyberTeal,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text("Asosiy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("AI Agent", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("Hisobot & Yordam", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 3,
                    onClick = { selectedBottomTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedBottomTab) {
                0 -> HomeProtectionTab(
                    uiState = uiState,
                    isGameModeActive = isGameModeActive,
                    onNavigateToAntivirus = onNavigateToAntivirus,
                    onNavigateToGameBooster = onNavigateToGameBooster,
                    onNavigateToAiTab = { selectedBottomTab = 1 },
                    onNavigateToCleaner = onNavigateToCleaner,
                    onNavigateToAppLock = onNavigateToAppLock,
                    onNavigateToNetGuard = onNavigateToNetGuard,
                    onNavigateToPermissions = onNavigateToPermissions,
                    onNavigateToAppUninstall = onNavigateToAppUninstall,
                    onNavigateToWifiSecurity = onNavigateToWifiSecurity,
                    onRunSmartBoost = { viewModel.runOneTapSmartOptimization() }
                )
                1 -> {
                    val aiViewModel: AiAssistantViewModel = hiltViewModel()
                    AiAssistantScreen(
                        viewModel = aiViewModel,
                        onExecuteAction = { target ->
                            when (target) {
                                AiActionTarget.SCAN_ANTIVIRUS -> onNavigateToAntivirus()
                                AiActionTarget.BOOST_GAME_MODE -> onNavigateToGameBooster()
                                AiActionTarget.CLEAN_STORAGE -> onNavigateToCleaner()
                                AiActionTarget.OPEN_APPLOCK -> onNavigateToAppLock()
                                AiActionTarget.OPEN_NETGUARD -> onNavigateToNetGuard()
                                AiActionTarget.OPEN_WIFI_AUDIT -> onNavigateToWifiSecurity()
                                AiActionTarget.OPEN_PERMISSIONS -> onNavigateToPermissions()
                            }
                        }
                    )
                }
                2 -> ReportHelpScreen(
                    dashboardState = uiState,
                    onNavigateToAntivirus = onNavigateToAntivirus,
                    onNavigateToGameBooster = onNavigateToGameBooster
                )
                3 -> SettingsScreen(
                    dashboardState = uiState,
                    preferences = preferences,
                    onNavigateToStability = onNavigateToStability,
                    onNavigateToAppLock = onNavigateToAppLock,
                    onNavigateToNetGuard = onNavigateToNetGuard,
                    onLanguageChanged = onLanguageChanged
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Tilni tanlang / Select Language", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocaleHelper.supportedLanguages.forEach { (code, name) ->
                        val isSelected = uiState.currentLanguage == code
                        OutlinedButton(
                            onClick = {
                                showLanguageDialog = false
                                viewModel.changeLanguage(code) {
                                    onLanguageChanged()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) CyberTeal.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = if (isSelected) "✓ $name (${code.uppercase()})" else "$name (${code.uppercase()})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberTeal else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun HomeProtectionTab(
    uiState: DashboardUiState,
    isGameModeActive: Boolean,
    onNavigateToAntivirus: () -> Unit,
    onNavigateToGameBooster: () -> Unit,
    onNavigateToAiTab: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToNetGuard: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAppUninstall: () -> Unit,
    onNavigateToWifiSecurity: () -> Unit = {},
    onRunSmartBoost: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Markaziy Jonli Qalqon
        HeroPulseShield(
            isDeviceSafe = uiState.isDeviceSafe,
            healthScore = uiState.healthScore,
            riskCount = uiState.highRiskAppCount,
            onClick = onNavigateToAntivirus
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1-Tap Smart All-in-One Optimization Button
        Button(
            onClick = onRunSmartBoost,
            enabled = !uiState.isSmartBoosting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberTeal,
                contentColor = ObsidianBlack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState.isSmartBoosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ObsidianBlack,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.smartBoostStatusMessage ?: "Optimallashtirilmoqda...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1-Bosishda Kompleks Optimallashtirish (RAM + Kesh)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (!uiState.isSmartBoosting && uiState.smartBoostStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeonEmerald.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.smartBoostStatusMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonEmerald,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. O'yin Rejimi & Og'ir Nagruzkadan Xalos Qiluvchi (Featured Card)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = ObsidianSurface,
            borderColor = NeonEmerald.copy(alpha = 0.45f),
            borderWidth = 1.5.dp,
            onClick = onNavigateToGameBooster
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonEmerald.copy(alpha = 0.15f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "O'yin Rejimi & Tezlashtirish",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonEmerald.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (isGameModeActive) "FAOL 🚀" else "GAME TURBO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Telefonni og'ir nagruzkadan xalos qilish va o'yinlarni qotmasdan ochish",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = NeonEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. AI Agent Smart Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurfaceElev,
            borderColor = GlassBorderTeal,
            borderWidth = 1.dp,
            onClick = onNavigateToAiTab
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🤖 Sentinel AI Maslahatchi (Oflayn & Onlayn)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CyberTeal
                        )
                        Text(
                            text = "Telefonni skanerlab tavsiya oling yoki tushunmagan narsangizni so'rang",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyberTeal)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. 2x2 Asosiy Himoya Modullari
        Row(modifier = Modifier.fillMaxWidth()) {
            ModernModuleCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.antivirus_title),
                subtitle = if (uiState.highRiskAppCount > 0)
                    "${uiState.highRiskAppCount} ta xavf!"
                else
                    "${uiState.totalScannedApps} ilova/APK toza",
                icon = Icons.Default.BugReport,
                accentColor = if (uiState.highRiskAppCount > 0) NeonCrimson else CyberTeal,
                onClick = onNavigateToAntivirus
            )
            Spacer(modifier = Modifier.width(12.dp))
            ModernModuleCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.cleaner_title),
                subtitle = "${String.format("%.1f", uiState.lastCleanedMb)} MB tozalandi",
                icon = Icons.Default.CleaningServices,
                accentColor = NeonEmerald,
                onClick = onNavigateToCleaner
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            ModernModuleCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.applock_title),
                subtitle = "${uiState.lockedAppCount} ta ilova qulflangan",
                icon = Icons.Default.Lock,
                accentColor = NeonViolet,
                onClick = onNavigateToAppLock
            )
            Spacer(modifier = Modifier.width(12.dp))
            ModernModuleCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.netguard_title),
                subtitle = if (uiState.isNetGuardActive) "DNS Himoya Faol" else "O'chirilgan",
                icon = Icons.Default.VpnLock,
                accentColor = NeonCyan,
                onClick = onNavigateToNetGuard
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Wi-Fi Auditi, Ruxsatlar va Ilovalarni O'chirish
        Row(modifier = Modifier.fillMaxWidth()) {
            ModernModuleCard(
                modifier = Modifier.weight(1f),
                title = "Wi-Fi Auditi",
                subtitle = "Tarmoq & MITM tekshiruvi",
                icon = Icons.Default.WifiFind,
                accentColor = NeonCyan,
                onClick = onNavigateToWifiSecurity
            )
            Spacer(modifier = Modifier.width(12.dp))
            ModernModuleCard(
                modifier = Modifier.weight(1f),
                title = "Ruxsatlar",
                subtitle = "Kamera & mikrofon",
                icon = Icons.Default.Security,
                accentColor = CyberTeal,
                onClick = onNavigateToPermissions
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = NeonAmber.copy(alpha = 0.25f),
            borderWidth = 1.dp,
            onClick = onNavigateToAppUninstall
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = NeonAmber)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ilovalarni O'chirish (Debloat)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Keraksiz ilovalar va zavod bloatware dasturlarini tozalash",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = ObsidianSurface,
        borderColor = accentColor.copy(alpha = 0.25f),
        borderWidth = 1.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                fontSize = 11.sp
            )
        }
    }
}
