package uz.sentinel.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.db.ScanResultDao
import uz.sentinel.data.local.prefs.SentinelPreferences
import javax.inject.Inject

data class DashboardUiState(
    val highRiskAppCount: Int = 0,
    val lockedAppCount: Int = 0,
    val lastCleanedMb: Double = 0.0,
    val isNetGuardActive: Boolean = false,
    val isDeviceSafe: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    scanResultDao: ScanResultDao,
    lockedAppDao: LockedAppDao,
    preferences: SentinelPreferences
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        scanResultDao.getHighRiskCountFlow(),
        lockedAppDao.getLockedAppsFlow(),
        preferences.lastCleanBytes,
        preferences.isNetGuardEnabled
    ) { highRisks, lockedApps, cleanBytes, netGuardEnabled ->
        DashboardUiState(
            highRiskAppCount = highRisks,
            lockedAppCount = lockedApps.size,
            lastCleanedMb = cleanBytes / (1024.0 * 1024.0),
            isNetGuardActive = netGuardEnabled,
            isDeviceSafe = highRisks == 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
