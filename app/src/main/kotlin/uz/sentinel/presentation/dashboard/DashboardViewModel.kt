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
    val isDeviceSafe: Boolean = true,
    val healthScore: Int = 100,
    val totalScannedApps: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    scanResultDao: ScanResultDao,
    lockedAppDao: LockedAppDao,
    preferences: SentinelPreferences
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        scanResultDao.getHighRiskCountFlow().catch { emit(0) },
        scanResultDao.getAllResultsFlow().catch { emit(emptyList()) },
        lockedAppDao.getLockedAppsFlow().catch { emit(emptyList()) },
        preferences.lastCleanBytes.catch { emit(0L) },
        preferences.isNetGuardEnabled.catch { emit(false) }
    ) { highRisks, scannedApps, lockedApps, cleanBytes, netGuardEnabled ->
        val score = when {
            highRisks > 0 -> (100 - (highRisks * 25)).coerceIn(20, 60)
            !netGuardEnabled -> 92
            else -> 100
        }
        DashboardUiState(
            highRiskAppCount = highRisks,
            lockedAppCount = lockedApps.size,
            lastCleanedMb = cleanBytes / (1024.0 * 1024.0),
            isNetGuardActive = netGuardEnabled,
            isDeviceSafe = highRisks == 0,
            healthScore = score,
            totalScannedApps = scannedApps.size
        )
    }.catch {
        emit(DashboardUiState())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}

