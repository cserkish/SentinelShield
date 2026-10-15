package uz.sentinel.presentation.reporthelp

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.dashboard.DashboardUiState
import uz.sentinel.presentation.gamebooster.GameBoosterViewModel
import uz.sentinel.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportHelpScreen(
    dashboardState: DashboardUiState,
    onNavigateToAntivirus: () -> Unit = {},
    onNavigateToGameBooster: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📊 Xavfsizlik Hisoboti", "❓ Yordam & Qo'llanma")
    val telemetry = remember { GameBoosterViewModel.readSystemTelemetry(context) }
    val dateStr = remember {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = CyberTeal,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberTeal
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) CyberTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // TAB 1: SECURITY & SYSTEM REPORT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = ObsidianSurface,
                    borderColor = GlassBorderTeal,
                    borderWidth = 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Qurilma Xavfsizlik Pasporti",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sana: $dateStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (dashboardState.isDeviceSafe) NeonEmerald.copy(alpha = 0.15f) else NeonCrimson.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${dashboardState.healthScore}/100 BALL",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (dashboardState.isDeviceSafe) NeonEmerald else NeonCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ReportMetricRow("Tekshirilgan ilovalar va APK'lar", "${dashboardState.totalScannedApps} ta", CyberTeal)
                        ReportMetricRow("Aniqlangan yuqori xavflar", "${dashboardState.highRiskAppCount} ta", if (dashboardState.highRiskAppCount > 0) NeonCrimson else NeonEmerald)
                        ReportMetricRow("Qulflangan shaxsiy ilovalar (AppLock)", "${dashboardState.lockedAppCount} ta", NeonViolet)
                        ReportMetricRow("NetGuard DNS VPN holati", if (dashboardState.isNetGuardActive) "Faol (Himoyalangan)" else "O'chiq", NeonCyan)
                        ReportMetricRow("Tozalangan axlat va kesh", "${String.format("%.1f", dashboardState.lastCleanedMb)} MB", NeonEmerald)
                        ReportMetricRow("RAM (Operativ xotira) yuklamasi", "${telemetry.ramUsagePercent}% (${telemetry.freeRamMb} MB bo'sh)", CyberTeal)
                        ReportMetricRow("Batareya harorati va quvvati", "${String.format("%.1f", telemetry.batteryTempCelsius)}°C / ${telemetry.batteryLevelPercent}%", NeonAmber)
                        ReportMetricRow("Bo'sh ichki xotira", "${String.format("%.1f", telemetry.freeStorageGb)} GB / ${String.format("%.0f", telemetry.totalStorageGb)} GB", NeonCyan)
                        ReportMetricRow("Viruslar bazasi versiyasi", "v${dashboardState.threatFeedVersion} (Gibrid Oflayn+Onlayn)", NeonEmerald)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val summaryText = buildString {
                                    append("🛡️ SentinelShield Xavfsizlik Hisoboti ($dateStr)\n")
                                    append("• Salomatlik indeksi: ${dashboardState.healthScore}/100\n")
                                    append("• Tekshirilgan ilovalar: ${dashboardState.totalScannedApps} ta (Xavflar: ${dashboardState.highRiskAppCount})\n")
                                    append("• RAM: ${telemetry.ramUsagePercent}% (${telemetry.freeRamMb} MB bo'sh)\n")
                                    append("• Harorat: ${String.format("%.1f", telemetry.batteryTempCelsius)}°C\n")
                                    append("• Bo'sh xotira: ${String.format("%.1f", telemetry.freeStorageGb)} GB\n")
                                    append("• NetGuard VPN: ${if (dashboardState.isNetGuardActive) "Faol" else "O'chiq"}\n")
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "SentinelShield Hisoboti")
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Hisobotni ulashish"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal,
                                contentColor = ObsidianBlack
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hisobotni ulashish / Eksport qilish", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // TAB 2: HELP & USER GUIDE (FAQ)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HelpGuideCard(
                    icon = Icons.Default.WifiOff,
                    title = "1. Internetsiz (Oflayn) va Internetli ishlash",
                    body = "Ilova 100% internetsiz ham ishlay oladi! Ichki xotirada tayyor virus xeshlari, evristik tahlilchi va oflayn AI Agent mavjud. Internet bo'lganda esa GitHub bazadan yangi tahdidlarni avtomatik yuklab oladi."
                )
                HelpGuideCard(
                    icon = Icons.Default.SportsEsports,
                    title = "2. O'yin Rejimi (Game Booster) qanday ishlaydi?",
                    body = "O'yinga kirishdan oldin 'Boost & Ochish' tugmasini bossangiz, ilova orqa fondagi og'ir jarayonlarni to'xtatib RAM ni bo'shatadi, telefon qizishini kamaytiradi va o'yin qotmasdan ishlashini ta'minlaydi."
                )
                HelpGuideCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "3. AI Agent'dan qanday foydalanaman?",
                    body = "Pastki menyudan 'AI Agent' bo'limiga o'ting va 'Tahlil' tugmasini bosing. Sun'iy intellekt telefoningizni tekshirib, nima qilish kerakligini aytadi va tushunmagan savollaringizga javob beradi."
                )
                HelpGuideCard(
                    icon = Icons.Default.BugReport,
                    title = "4. Telegram APK va Fishingdan himoya",
                    body = "Antivirus nafaqat o'rnatilgan ilovalarni, balki Download va Telegram papkasidagi hali o'rnatilmagan .apk fayllarni ham tekshiradi. Xabarlarda .apk fayl yoki qisqa link (bit.ly, clck.ru) kelsa darhol ogohlantiradi."
                )
                HelpGuideCard(
                    icon = Icons.Default.Lock,
                    title = "5. AppLock va Anti-Tamper (O'chirishdan himoya)",
                    body = "AppLock bo'limida shaxsiy ilovalaringiz bilan birga 'Sozlamalar' (Settings) ilovasini ham PIN yoki Barmoq izi bilan qulflab qo'ysangiz, begonalar ilovani to'xtata olmaydi."
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReportMetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun HelpGuideCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = GlassBorderWhite,
        borderWidth = 1.dp
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
