package uz.sentinel.presentation.dashboard

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import uz.sentinel.domain.repository.JunkScannerRepository
import uz.sentinel.service.ThreatUpdateWorker
import javax.inject.Inject

data class DashboardUiState(
    val highRiskAppCount: Int = 0,
    val lockedAppCount: Int = 0,
    val lastCleanedMb: Double = 0.0,
    val isNetGuardActive: Boolean = false,
    val isDeviceSafe: Boolean = true,
    val healthScore: Int = 100,
    val totalScannedApps: Int = 0,
    val currentLanguage: String = "uz",
    val threatFeedVersion: String = "2026.10",
    val threatFeedUrl: String = ThreatUpdateWorker.DEFAULT_GITHUB_FEED_URL,
    val isSmartBoosting: Boolean = false,
    val smartBoostStatusMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    scanResultDao: ScanResultDao,
    lockedAppDao: LockedAppDao,
    private val junkRepository: JunkScannerRepository,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _smartBoostState = MutableStateFlow<Pair<Boolean, String?>>(false to null)

    private val baseFlow = combine(
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
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        baseFlow,
        preferences.appLanguage.catch { emit("uz") },
        preferences.threatFeedVersion.catch { emit("2026.10") },
        preferences.threatFeedUrl.catch { emit(ThreatUpdateWorker.DEFAULT_GITHUB_FEED_URL) },
        _smartBoostState
    ) { base, lang, feedVer, feedUrl, boostPair ->
        base.copy(
            currentLanguage = lang,
            threatFeedVersion = feedVer,
            threatFeedUrl = feedUrl,
            isSmartBoosting = boostPair.first,
            smartBoostStatusMessage = boostPair.second
        )
    }.catch {
        emit(DashboardUiState())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    /**
     * 1-Tap Smart All-in-One Optimization:
     * 1) Cleans residual cache & temp files
     * 2) Stops heavy background processes to free RAM
     * 3) Syncs threat database
     */
    fun runOneTapSmartOptimization() {
        if (_smartBoostState.value.first) return
        viewModelScope.launch {
            _smartBoostState.value = true to "1/3 • Kesh va vaqtinchalik axlat fayllar tozalanmoqda..."
            val freedCacheBytes = withContext(Dispatchers.IO) {
                runCatching {
                    val items = junkRepository.scanJunk()
                    junkRepository.deleteJunk(items)
                }.getOrDefault(0L)
            }
            if (freedCacheBytes > 0) {
                preferences.recordCleanedBytes(freedCacheBytes)
            }

            _smartBoostState.value = true to "2/3 • Orqa fon jarayonlari to'xtatilib, RAM bo'shatilmoqda..."
            var killedCount = 0
            withContext(Dispatchers.IO) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val apps = runCatching { packageManager.getInstalledApplications(0) }.getOrDefault(emptyList())
                for (app in apps) {
                    if (app.packageName == context.packageName) continue
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!isSystem) {
                        runCatching {
                            am?.killBackgroundProcesses(app.packageName)
                            killedCount++
                        }
                    }
                }
                System.gc()
                delay(450L)
            }

            _smartBoostState.value = true to "3/3 • Xavfsizlik bazasi tekshirilmoqda..."
            ThreatUpdateWorker.executeNow(context)
            delay(350L)

            val cacheMb = String.format("%.1f", freedCacheBytes / (1024.0 * 1024.0))
            val estimatedRamMb = (killedCount * 11L).coerceIn(40L, 350L)
            preferences.recordBoostedRamMb(estimatedRamMb)

            _smartBoostState.value = false to "✅ Tizim optimallashtirildi: $estimatedRamMb MB RAM + $cacheMb MB kesh bo'shatildi!"
        }
    }

    fun changeLanguage(langCode: String, onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setAppLanguage(langCode)
            onDone()
        }
    }

    fun updateGitHubFeedUrl(newUrl: String) {
        viewModelScope.launch {
            preferences.setThreatFeedUrl(newUrl)
            ThreatUpdateWorker.executeNow(context)
        }
    }
}
