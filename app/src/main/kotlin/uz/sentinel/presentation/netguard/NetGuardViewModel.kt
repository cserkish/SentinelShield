package uz.sentinel.presentation.netguard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.sentinel.data.local.entity.PhishingDomainEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.repository.PhishingRepository
import uz.sentinel.service.PhishingVpnService
import javax.inject.Inject

data class UrlCheckVerdict(
    val url: String,
    val isDangerous: Boolean,
    val message: String
)

data class NetGuardUiState(
    val isVpnActive: Boolean = false,
    val blockedDomains: List<PhishingDomainEntity> = emptyList(),
    val blockedCount: Int = 0,
    val urlCheckVerdict: UrlCheckVerdict? = null
)

@HiltViewModel
class NetGuardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phishingRepository: PhishingRepository,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetGuardUiState())
    val uiState: StateFlow<NetGuardUiState> = _uiState.asStateFlow()

    private val shorteners = setOf(
        "bit.ly", "clck.ru", "cutt.ly", "tinyurl.com", "shorturl.at", "is.gd", "rb.gy", "t.co"
    )

    init {
        viewModelScope.launch {
            preferences.isNetGuardEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isVpnActive = isEnabled) }
            }
        }
        viewModelScope.launch {
            phishingRepository.getAllBlockedDomains().collect { domains ->
                _uiState.update {
                    it.copy(
                        blockedDomains = domains,
                        blockedCount = domains.size
                    )
                }
            }
        }
    }

    fun toggleVpn(enable: Boolean) {
        viewModelScope.launch {
            preferences.setNetGuardEnabled(enable)
            val intent = Intent(context, PhishingVpnService::class.java).apply {
                if (!enable) action = PhishingVpnService.ACTION_STOP
            }
            if (enable) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                runCatching { context.startService(intent) }
                    .onFailure { context.stopService(Intent(context, PhishingVpnService::class.java)) }
            }
        }
    }

    /**
     * Allows the user to paste or type any URL/link to check whether it is phishing,
     * a disguised URL shortener, an .apk dropper link, or safe.
     */
    fun checkUrlSafety(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val lower = trimmed.lowercase()
            val domain = lower
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
                .substringBefore("/")
                .substringBefore("?")
                .substringBefore(":")

            val verdict = when {
                lower.endsWith(".apk") || lower.contains(".apk?") -> {
                    UrlCheckVerdict(
                        url = trimmed,
                        isDangerous = true,
                        message = "🚨 XAVFLI: Bu havola to'g'ridan-to'g'ri .APK dastur yuklab oladi! Bank troyani bo'lishi mumkin — ochmang!"
                    )
                }
                shorteners.any { domain == it || domain.endsWith(".$it") } -> {
                    UrlCheckVerdict(
                        url = trimmed,
                        isDangerous = true,
                        message = "⚠️ SHUBHALI QISQA HAVOLA ($domain): Firibgarlar haqiqiy fishing manzilni yashirish uchun qisqa linklardan foydalanadi!"
                    )
                }
                phishingRepository.isPhishingDomain(domain) -> {
                    UrlCheckVerdict(
                        url = trimmed,
                        isDangerous = true,
                        message = "🛑 FISHING / SOXTA SAYT ANIQLANDI ($domain)! Karta raqami yoki SMS kodni aslo kiritmang!"
                    )
                }
                else -> {
                    UrlCheckVerdict(
                        url = trimmed,
                        isDangerous = false,
                        message = "✅ XAVFSIZ: '$domain' fishing va maynerlar qora ro'yxatida topilmadi."
                    )
                }
            }
            _uiState.update { it.copy(urlCheckVerdict = verdict) }
        }
    }
}
