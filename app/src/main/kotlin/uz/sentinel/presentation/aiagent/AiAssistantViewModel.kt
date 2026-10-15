package uz.sentinel.presentation.aiagent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.db.ScanResultDao
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.gamebooster.GameBoosterViewModel
import javax.inject.Inject

enum class AiActionTarget(val label: String) {
    SCAN_ANTIVIRUS("🛡️ Antivirus tekshiruvi"),
    BOOST_GAME_MODE("🚀 O'yin rejimi & RAM tozalash"),
    CLEAN_STORAGE("🧹 Xotirani tozalash"),
    OPEN_APPLOCK("🔒 Ilovalarni qulflash"),
    OPEN_NETGUARD("🌐 NetGuard VPN va Link Tekshirgich"),
    OPEN_WIFI_AUDIT("📶 Wi-Fi Xavfsizlik Auditi"),
    OPEN_PERMISSIONS("🕵️ Ruxsatlar auditi")
}

data class AiChatMessage(
    val id: Long = System.currentTimeMillis(),
    val isFromUser: Boolean,
    val text: String,
    val recommendedAction: AiActionTarget? = null
)

data class AiAssistantUiState(
    val isAnalyzing: Boolean = false,
    val isOnlineAvailable: Boolean = true,
    val isOfflineOnlyMode: Boolean = false,
    val messages: List<AiChatMessage> = emptyList()
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanResultDao: ScanResultDao,
    private val lockedAppDao: LockedAppDao,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isOfflineOnlyMode.collect { offlineOnly ->
                _uiState.update {
                    it.copy(
                        isOfflineOnlyMode = offlineOnly,
                        isOnlineAvailable = !offlineOnly && checkNetworkOnline()
                    )
                }
            }
        }
        // Initial welcome & smart device scan greeting
        runSmartDeviceDiagnosis(isInitialGreeting = true)
    }

    private fun checkNetworkOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Scans the entire phone state (Threats, RAM, Temperature, Storage, AppLock, NetGuard)
     * and generates a personalized AI recommendation report (works 100% offline & online).
     */
    fun runSmartDeviceDiagnosis(isInitialGreeting: Boolean = false) {
        viewModelScope.launch {
            if (!isInitialGreeting) {
                appendMessage(
                    AiChatMessage(
                        id = System.nanoTime(),
                        isFromUser = true,
                        text = "🔍 Telefonimni skanerlab, nima qilish kerakligini tavsiya ber"
                    )
                )
            }
            _uiState.update { it.copy(isAnalyzing = true) }

            val report = withContext(Dispatchers.IO) {
                delay(450L)
                val telemetry = GameBoosterViewModel.readSystemTelemetry(context)
                val highRisks = runCatching { scanResultDao.getHighRiskCountFlow().first() }.getOrDefault(0)
                val scannedCount = runCatching { scanResultDao.getAllResultsFlow().first().size }.getOrDefault(0)
                val lockedCount = runCatching { lockedAppDao.getLockedAppsFlow().first().size }.getOrDefault(0)
                val netGuardOn = runCatching { preferences.isNetGuardEnabled.first() }.getOrDefault(false)
                val gameModeOn = runCatching { preferences.isGameModeActive.first() }.getOrDefault(false)
                val online = checkNetworkOnline()

                val tips = mutableListOf<String>()
                var primaryAction: AiActionTarget? = null

                if (highRisks > 0) {
                    tips.add("🚨 **Kritik xavf:** Qurilmangizda **$highRisks ta xavfli ilova yoki APK fayl** aniqlandi! Darhol Antivirus bo'limiga kirib ularni o'chirib tashlang.")
                    primaryAction = AiActionTarget.SCAN_ANTIVIRUS
                } else if (scannedCount == 0) {
                    tips.add("🛡️ **Antivirus:** Hali to'liq chuqur tekshiruv o'tkazilmagan. O'rnatilgan ilovalar va `Download`/`Telegram` papkasidagi `.apk` fayllarni tekshirishni tavsiya qilaman.")
                    primaryAction = AiActionTarget.SCAN_ANTIVIRUS
                } else {
                    tips.add("✅ **Antivirus:** $scannedCount ta ilova va APK fayl tekshirilgan — zararli virus topilmadi.")
                }

                if (telemetry.ramUsagePercent >= 75 || telemetry.batteryTempCelsius >= 39f) {
                    tips.add("🚀 **Tizim yuklamasi va O'yin rejimi:** RAM **${telemetry.ramUsagePercent}%** band (${telemetry.freeRamMb} MB bo'sh), harorat **${String.format("%.1f", telemetry.batteryTempCelsius)}°C**. Telefon qotmasligi va o'yinlarda FPS tushmasligi uchun **O'yin rejimi & Tezlashtirish** modulini ishga tushiring.")
                    if (primaryAction == null) primaryAction = AiActionTarget.BOOST_GAME_MODE
                } else {
                    tips.add("⚡ **Tezkorlik:** RAM yuklamasi **${telemetry.ramUsagePercent}%** (${telemetry.freeRamMb} MB bo'sh), harorat **${String.format("%.1f", telemetry.batteryTempCelsius)}°C** (barqaror)${if (gameModeOn) ", O'yin rejimi faol" else ""}.")
                }

                if (!netGuardOn) {
                    tips.add("🌐 **Fishingdan himoya:** **NetGuard VPN** hozir o'chiq. Soxta Click/Payme/Uzcard fishing saytlari va yashirin kripto-maynerlarni bloklash uchun uni yoqib qo'ying.")
                    if (primaryAction == null) primaryAction = AiActionTarget.OPEN_NETGUARD
                } else {
                    tips.add("🌐 **NetGuard VPN:** Faol (soxta bank va fishing saytlar bloklanmoqda).")
                }

                if (lockedCount == 0) {
                    tips.add("🔒 **Maxfiylik (AppLock):** Hozircha hech bir ilova qulflanmagan. Shaxsiy messenjerlar va **Sozlamalar (Anti-Tamper)** ilovasini PIN yoki Barmoq izi bilan qulflab qo'yishni maslahat beraman.")
                    if (primaryAction == null) primaryAction = AiActionTarget.OPEN_APPLOCK
                } else {
                    tips.add("🔒 **AppLock:** $lockedCount ta ilova PIN/Biometrik qulf bilan himoyalangan.")
                }

                if (telemetry.freeStorageGb < 5.0) {
                    tips.add("🧹 **Xotira:** Bo'sh joy kam qolgan (**${String.format("%.1f", telemetry.freeStorageGb)} GB**). Kesh va vaqtinchalik axlat fayllarni tozalang.")
                    if (primaryAction == null) primaryAction = AiActionTarget.CLEAN_STORAGE
                }

                val modeBadge = if (online) "🟢 Onlayn + Oflayn Gibrid AI" else "🔵 Avtonom Oflayn AI"
                val intro = if (isInitialGreeting) {
                    "Assalomu alaykum! Men **Sentinel AI Agent**man ($modeBadge). Internet bo'lsa ham, bo'lmasa ham telefoningizni tahlil qilib, sizga yordam beraman.\n\n📊 **Qurilmangizning hozirgi jonli tahlili:**"
                } else {
                    "📊 **Telefonning jonli skanerlash natijasi va tavsiyalarim:**"
                }

                val fullText = buildString {
                    append(intro)
                    append("\n\n")
                    tips.forEach { tip ->
                        append("• ").append(tip).append("\n\n")
                    }
                    append("💡 *Ilovadagi istalgan funksiya haqida savol bering — hammasini oddiy tilda tushuntirib beraman!*")
                }

                Pair(fullText, primaryAction ?: AiActionTarget.BOOST_GAME_MODE)
            }

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    isOnlineAvailable = checkNetworkOnline()
                )
            }
            appendMessage(
                AiChatMessage(
                    id = System.nanoTime(),
                    isFromUser = false,
                    text = report.first,
                    recommendedAction = report.second
                )
            )
        }
    }

    fun askQuestion(userQuestion: String) {
        val cleanQ = userQuestion.trim()
        if (cleanQ.isEmpty()) return

        appendMessage(
            AiChatMessage(
                id = System.nanoTime(),
                isFromUser = true,
                text = cleanQ
            )
        )

        val qLower = cleanQ.lowercase()
        if (qLower.contains("skaner") || qLower.contains("tekshir") || qLower.contains("tavsiya") || qLower.contains("diagnost")) {
            runSmartDeviceDiagnosis(isInitialGreeting = false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            delay(300L)
            val (reply, action) = generateKnowledgeReply(qLower)
            _uiState.update { it.copy(isAnalyzing = false) }
            appendMessage(
                AiChatMessage(
                    id = System.nanoTime(),
                    isFromUser = false,
                    text = reply,
                    recommendedAction = action
                )
            )
        }
    }

    private fun generateKnowledgeReply(q: String): Pair<String, AiActionTarget?> {
        return when {
            q.contains("o'yin") || q.contains("oyin") || q.contains("game") || q.contains("qot") || q.contains("ram") || q.contains("tez") || q.contains("fps") || q.contains("nagruzka") -> {
                Pair(
                    "🎮 **O'yin Rejimi va Tezlashtirish (Game Booster) qanday ishlaydi?**\n\n" +
                    "1. **Orqa fonni tozalash:** O'yinga kirishdan oldin keraksiz orqa fon ilovalarini to'xtatib, **RAM (operativ xotira)** ni bo'shatadi.\n" +
                    "2. **Og'ir nagruzkani kamaytirish:** O'yin vaqtida og'ir skanerlash jarayonlarini pauza qiladi, natijada protsessor (CPU) bor kuchini o'yinga yo'naltiradi va FPS oshadi.\n" +
                    "3. **Boost & Ochish:** O'yin ro'yxatidan o'zingiz xohlagan o'yin yonidagi **'Boost & Ochish'** tugmasini bossangiz, telefon avtomatik tezlashtirilib, o'yin ishga tushadi!",
                    AiActionTarget.BOOST_GAME_MODE
                )
            }
            q.contains("internet") || q.contains("oflayn") || q.contains("offline") || q.contains("onlayn") -> {
                Pair(
                    "🌐 **Internetli va Internetsiz (Gibrid) Ishlash Tizimi:**\n\n" +
                    "• **Internetsiz (Oflayn):** Antivirusning SHA-256 va evristik skayneri, o'rnatilmagan `.apk` tekshirgichi, AppLock, O'yin rejimi, Xotira tozalash va men (AI Agent) **100% internetsiz** ishlaymiz!\n" +
                    "• **Internet bo'lganda (Onlayn):** Ilova GitHub'dagi ochiq `sentinel-threats.json` bazasidan yangi chiqqan fishing saytlar va virus xeshlarini avtomatik yuklab oladi.\n" +
                    "• **Sozlamalar** bo'limidan **'To'liq Oflayn Rejim'** ni ham yoqib qo'yishingiz mumkin.",
                    null
                )
            }
            q.contains("qulf") || q.contains("applock") || q.contains("pin") || q.contains("barmoq") || q.contains("selfie") || q.contains("rasm") -> {
                Pair(
                    "🔒 **Ilovalarni Qulflash (AppLock) va Anti-Tamper:**\n\n" +
                    "• **3 xil qulf usuli:** 6 xonali **PIN kod**, **Grafik kalit (Pattern)** yoki **Barmoq izi (Biometrik)** tanlashingiz mumkin.\n" +
                    "• **🛡️ Anti-Tamper:** Ro'yxat boshidagi **Sozlamalar (`com.android.settings`)** ilovasini qulflab qo'ysangiz, hech kim telefoningiz sozlamalariga kirib `SentinelShield` ni o'chira olmaydi.\n" +
                    "• **Intruder Selfie:** Kimdir qulflangan ilovangizga 3 marta xato PIN kiritsa, old kamera sezdirmasdan uning rasmini oladi va shifrlab saqlaydi!",
                    AiActionTarget.OPEN_APPLOCK
                )
            }
            q.contains("fishing") || q.contains("vpn") || q.contains("netguard") || q.contains("sms") || q.contains("telegram") || q.contains("apk") || q.contains("karta") -> {
                Pair(
                    "🛡️ **Telegram APK, Qisqa Linklar va Fishing Himoyasi:**\n\n" +
                    "1. **Telegram/SMS `.apk` tutqichi:** Agar sizga Telegram yoki SMS orqali `Taklifnoma.apk`, `Fotom.apk` kabi zararli fayl yoki `bit.ly`, `clck.ru` kabi qisqa link kelsa, ilova darhol xavf haqida ogohlantiradi.\n" +
                    "2. **NetGuard DNS VPN:** Internet tezligini pasaytirmasdan, faqat soxta Click/Payme/Uzcard fishing saytlari va kripto-maynerlarni (`NXDOMAIN`) bloklaydi.\n" +
                    "3. **Chuqur Antivirus:** `Download` va `Telegram` papkasidagi hali o'rnatilmagan `.apk` fayllarni ham ichidan tekshirib beradi!",
                    AiActionTarget.SCAN_ANTIVIRUS
                )
            }
            q.contains("toza") || q.contains("kesh") || q.contains("xotira") || q.contains("joy") -> {
                Pair(
                    "🧹 **Xotira Tozalash (Cleaner):**\n\n" +
                    "• Ilovalar keshini, `.thumbnails` eskizlarini, WhatsApp/Telegram vaqtinchalik qoldiqlarini va chala yuklangan `.tmp`/`.crdownload` fayllarni topib tozalaydi.\n" +
                    "• **Shaxsiy rasm, video va brauzer parollariga aslo tegmaydi** — 100% xavfsiz!",
                    AiActionTarget.CLEAN_STORAGE
                )
            }
            q.contains("wifi") || q.contains("wi-fi") || q.contains("tarmoq") || q.contains("proksi") || q.contains("mitm") -> {
                Pair(
                    "📶 **Wi-Fi va Tarmoq Xavfsizligi Auditi:**\n\n" +
                    "• Jamoat joylaridagi (kafe, aeroport, mehmonxona) Wi-Fi tarmoqlarida xakerlar **MITM (vositachi)** yoki soxta **DNS/Proksi** orqali trafikingizni kuzatishi mumkin.\n" +
                    "• **Wi-Fi Auditi** bo'limi ulangan tarmoqning tezligini, DNS serverlarini, HTTP proksi xavfini va Captive Portal tuzoqlarini bir soniyada tekshirib beradi!",
                    AiActionTarget.OPEN_WIFI_AUDIT
                )
            }
            q.contains("ruxsat") || q.contains("kamera") || q.contains("mikrofon") || q.contains("audit") -> {
                Pair(
                    "🕵️ **Maxfiylik va Ruxsatlar Auditi:**\n\n" +
                    "• Qaysi ilovalar sizning **Kamerangiz**, **Mikrofoningiz**, **Joylashuvingiz (GPS)** va **Kontakt/SMS** laringizga ruxsat olganini bitta ro'yxatda ko'rsatadi.\n" +
                    "• Shubhali ilova yonidagi ⚙️ tugmasini bosib, uning ruxsatini darhol bekor qilishingiz mumkin.",
                    AiActionTarget.OPEN_PERMISSIONS
                )
            }
            else -> {
                Pair(
                    "🤖 **Sentinel AI Yordamchi:**\n\n" +
                    "Men quyidagi vazifalarda sizga yordam bera olaman:\n" +
                    "• **'Telefonimni tekshir'** deb yozsangiz — viruslar, RAM yuklamasi, harorat va xotirani skanerlab aniq tavsiya beraman.\n" +
                    "• **'O'yin rejimi'**, **'AppLock'**, **'NetGuard VPN'**, **'Telegram APK viruslar'** yoki **'Oflayn ishlash'** haqida so'rasangiz — batafsil tushuntirib beraman.\n\n" +
                    "Quyidagi tezkor tugmalardan birini bosing:",
                    AiActionTarget.SCAN_ANTIVIRUS
                )
            }
        }
    }

    private fun appendMessage(msg: AiChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages + msg)
        }
    }
}
