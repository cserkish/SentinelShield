package uz.sentinel.presentation.netguard

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.sentinel.data.local.entity.PhishingDomainEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.repository.PhishingRepository
import uz.sentinel.service.PhishingVpnService
import javax.inject.Inject

data class NetGuardUiState(
    val isVpnActive: Boolean = false,
    val blockedDomains: List<PhishingDomainEntity> = emptyList(),
    val blockedCount: Int = 0
)

@HiltViewModel
class NetGuardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phishingRepository: PhishingRepository,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetGuardUiState())
    val uiState: StateFlow<NetGuardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isNetGuardEnabled.collect { isEnabled ->
                _uiState.value = _uiState.value.copy(isVpnActive = isEnabled)
            }
        }
        viewModelScope.launch {
            phishingRepository.getAllBlockedDomains().collect { domains ->
                _uiState.value = _uiState.value.copy(
                    blockedDomains = domains,
                    blockedCount = domains.size
                )
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
                context.startService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
