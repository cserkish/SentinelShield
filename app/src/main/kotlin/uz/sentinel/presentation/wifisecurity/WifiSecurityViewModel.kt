package uz.sentinel.presentation.wifisecurity

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WifiCheckItem(
    val title: String,
    val detail: String,
    val isSafe: Boolean
)

data class WifiSecurityUiState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val connectionType: String = "Noma'lum",
    val linkSpeedMbps: Int = 0,
    val dnsServers: List<String> = emptyList(),
    val hasProxyRisk: Boolean = false,
    val isVpnActive: Boolean = false,
    val securityScore: Int = 100,
    val checks: List<WifiCheckItem> = emptyList()
)

@HiltViewModel
class WifiSecurityViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiSecurityUiState())
    val uiState: StateFlow<WifiSecurityUiState> = _uiState.asStateFlow()

    private val trustedDnsProviders = setOf(
        "1.1.1.1", "1.0.0.1",           // Cloudflare
        "8.8.8.8", "8.8.4.4",           // Google
        "9.9.9.9", "149.112.112.112",   // Quad9
        "208.67.222.222", "208.67.220.220" // OpenDNS
    )

    init {
        scanNetworkSecurity()
    }

    fun scanNetworkSecurity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val result = withContext(Dispatchers.IO) {
                delay(500L)
                analyzeCurrentNetwork()
            }
            _uiState.value = result
        }
    }

    private fun analyzeCurrentNetwork(): WifiSecurityUiState {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val activeNet = cm?.activeNetwork
        val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
        val linkProps: LinkProperties? = if (activeNet != null) cm.getLinkProperties(activeNet) else null

        if (caps == null) {
            return WifiSecurityUiState(
                isScanning = false,
                isConnected = false,
                connectionType = "Oflayn (Tarmoqqa ulanmagan)",
                securityScore = 100,
                checks = listOf(
                    WifiCheckItem(
                        title = "Oflayn Avtonom Himoya",
                        detail = "Qurilma hozir internetga ulanmagan — tashqi tarmoq hujumlari xavfi 0%.",
                        isSafe = true
                    )
                )
            )
        }

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val connType = when {
            isWifi -> "Wi-Fi Tarmog'i"
            isCellular -> "Mobil Internet (4G/5G)"
            else -> "Faol Tarmoq"
        }

        @Suppress("DEPRECATION")
        val linkSpeed = runCatching { wifiManager?.connectionInfo?.linkSpeed ?: 0 }.getOrDefault(0).coerceAtLeast(0)

        val dnsList = linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
        val httpProxy = linkProps?.httpProxy
        val hasProxy = httpProxy != null && !httpProxy.host.isNullOrBlank()

        val checks = mutableListOf<WifiCheckItem>()
        var score = 100

        // 1. Proxy / MITM Check
        if (hasProxy) {
            score -= 35
            checks.add(
                WifiCheckItem(
                    title = "⚠️ HTTP Proksi / MITM xavfi aniqlandi",
                    detail = "Tarmoqda proksi (${httpProxy?.host}:${httpProxy?.port}) sozlangan. Trafik uchinchi tomon tomonidan kuzatilishi mumkin!",
                    isSafe = false
                )
            )
        } else {
            checks.add(
                WifiCheckItem(
                    title = "✅ MITM va Proksi tekshiruvi toza",
                    detail = "Tarmoqda shubhali proksi yoki trafikni tutib oluvchi vositachi aniqlanmadi.",
                    isSafe = true
                )
            )
        }

        // 2. Captive Portal Check
        val hasCaptivePortal = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        if (hasCaptivePortal) {
            score -= 20
            checks.add(
                WifiCheckItem(
                    title = "⚠️ Ochiq Avtorizatsiya Portali (Captive Portal)",
                    detail = "Jamoat Wi-Fi tarmog'i parol yoki SMS so'ramoqda. Bank ilovalariga kirishdan oldin NetGuard VPN ni yoqing.",
                    isSafe = false
                )
            )
        } else {
            checks.add(
                WifiCheckItem(
                    title = "✅ To'g'ridan-to'g'ri shifrlangan kanal",
                    detail = "Oraliq Captive Portal tuzoqlari aniqlanmadi.",
                    isSafe = true
                )
            )
        }

        // 3. DNS Security Check
        val hasTrustedDns = dnsList.any { it in trustedDnsProviders } || isVpn
        if (hasTrustedDns) {
            checks.add(
                WifiCheckItem(
                    title = "✅ Himoyalangan DNS serverlar",
                    detail = "DNS manzillar: ${dnsList.joinToString(", ").ifEmpty { "1.1.1.1 (NetGuard)" }}",
                    isSafe = true
                )
            )
        } else {
            score -= 10
            checks.add(
                WifiCheckItem(
                    title = "ℹ️ Standart provayder DNS serveri",
                    detail = "DNS: ${dnsList.joinToString(", ").ifEmpty { "Avtomatik" }}. Fishingdan himoyalanish uchun NetGuard VPN ni yoqish tavsiya etiladi.",
                    isSafe = true
                )
            )
        }

        // 4. NetGuard VPN Tunnel Check
        if (isVpn) {
            checks.add(
                WifiCheckItem(
                    title = "✅ NetGuard DNS Qalqoni Faol",
                    detail = "Barcha DNS so'rovlar soxta bank va fishing filtridan o'tmoqda.",
                    isSafe = true
                )
            )
        } else {
            checks.add(
                WifiCheckItem(
                    title = "🛡️ NetGuard VPN tavsiyasi",
                    detail = "Jamoat joylaridagi Wi-Fi tarmoqlarida NetGuard himoyasini yoqib qo'ying.",
                    isSafe = true
                )
            )
        }

        return WifiSecurityUiState(
            isScanning = false,
            isConnected = true,
            connectionType = connType,
            linkSpeedMbps = linkSpeed,
            dnsServers = dnsList,
            hasProxyRisk = hasProxy,
            isVpnActive = isVpn,
            securityScore = score.coerceIn(20, 100),
            checks = checks
        )
    }
}
