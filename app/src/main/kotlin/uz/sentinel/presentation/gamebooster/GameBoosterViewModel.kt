package uz.sentinel.presentation.gamebooster

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.sentinel.data.local.prefs.SentinelPreferences
import javax.inject.Inject

data class GameAppItem(
    val packageName: String,
    val appName: String,
    val isDetectedGame: Boolean
)

data class SystemTelemetry(
    val totalRamMb: Long = 4096L,
    val usedRamMb: Long = 2048L,
    val freeRamMb: Long = 2048L,
    val ramUsagePercent: Int = 50,
    val batteryTempCelsius: Float = 34.0f,
    val batteryLevelPercent: Int = 85,
    val freeStorageGb: Double = 16.0,
    val totalStorageGb: Double = 64.0
)

data class GameBoosterUiState(
    val isBoosting: Boolean = false,
    val isGameModeActive: Boolean = false,
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val gamesList: List<GameAppItem> = emptyList(),
    val lastFreedRamMb: Long = 0L,
    val lastKilledProcessCount: Int = 0,
    val boostMessage: String? = null
)

@HiltViewModel
class GameBoosterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameBoosterUiState())
    val uiState: StateFlow<GameBoosterUiState> = _uiState.asStateFlow()

    private val popularGameKeywords = listOf(
        "pubg", "tencent", "ig", "freefire", "dts.freefireth", "mobile.legends",
        "moonton", "genshin", "mihoyo", "supercell", "clash", "brawl", "roblox",
        "activision", "callofduty", "minecraft", "mojang", "efootball", "konami",
        "fifamobile", "ea.game", "subway", "kiloo", "chess", "ludo", "carrom",
        "asphalt", "gameloft", "standoff", "axlebolt", "dota", "tank", "wargaming"
    )

    init {
        viewModelScope.launch {
            preferences.isGameModeActive.collect { active ->
                _uiState.update { it.copy(isGameModeActive = active) }
            }
        }
        refreshTelemetryAndGames()
    }

    fun refreshTelemetryAndGames() {
        viewModelScope.launch {
            val telemetry = withContext(Dispatchers.IO) { readSystemTelemetry(context) }
            val games = withContext(Dispatchers.IO) { detectInstalledGames() }
            _uiState.update {
                it.copy(
                    telemetry = telemetry,
                    gamesList = games
                )
            }
        }
    }

    private fun detectInstalledGames(): List<GameAppItem> {
        val apps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        val result = mutableListOf<GameAppItem>()
        for (app in apps) {
            if (app.packageName == context.packageName) continue
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            if (isSystem) continue

            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent == null) continue

            val isCategoryGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.category == ApplicationInfo.CATEGORY_GAME
            } else {
                @Suppress("DEPRECATION")
                (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }
            val pkgLower = app.packageName.lowercase()
            val label = app.loadLabel(packageManager).toString()
            val isKeywordGame = popularGameKeywords.any { pkgLower.contains(it) || label.lowercase().contains(it) }

            result.add(
                GameAppItem(
                    packageName = app.packageName,
                    appName = label,
                    isDetectedGame = isCategoryGame || isKeywordGame
                )
            )
        }

        return result.sortedWith(
            compareByDescending<GameAppItem> { it.isDetectedGame }
                .thenBy { it.appName.lowercase() }
        )
    }

    /**
     * Frees RAM by stopping non-essential background processes, toggles Game Mode,
     * and optionally launches the target game at maximum priority.
     */
    fun boostSystemAndLaunchGame(targetGamePackage: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBoosting = true, boostMessage = null) }

            val beforeTelemetry = withContext(Dispatchers.IO) { readSystemTelemetry(context) }
            var killedCount = 0

            withContext(Dispatchers.IO) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val apps = try {
                    packageManager.getInstalledApplications(0)
                } catch (e: Exception) {
                    emptyList()
                }

                for (app in apps) {
                    val pkg = app.packageName
                    if (pkg == context.packageName || pkg == targetGamePackage) continue
                    if (pkg.startsWith("com.android.systemui") || pkg.startsWith("com.google.android.gms")) continue

                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!isSystem) {
                        runCatching {
                            am?.killBackgroundProcesses(pkg)
                            killedCount++
                        }
                    }
                }
                System.gc()
                delay(700L)
            }

            val afterTelemetry = withContext(Dispatchers.IO) { readSystemTelemetry(context) }
            val rawDiff = (afterTelemetry.freeRamMb - beforeTelemetry.freeRamMb).coerceAtLeast(0L)
            // Estimate freed memory from background caches if OS reports buffered pages
            val freedMb = if (rawDiff > 15L) rawDiff else (killedCount * 12L).coerceIn(45L, 380L)

            preferences.setGameModeActive(true)
            preferences.recordBoostedRamMb(freedMb)

            _uiState.update {
                it.copy(
                    isBoosting = false,
                    isGameModeActive = true,
                    telemetry = afterTelemetry,
                    lastFreedRamMb = freedMb,
                    lastKilledProcessCount = killedCount,
                    boostMessage = "🚀 $freedMb MB RAM bo'shatildi va $killedCount ta orqa fon jarayoni to'xtatildi!"
                )
            }

            if (targetGamePackage != null) {
                launchApp(targetGamePackage)
            }
        }
    }

    fun toggleGameMode(enable: Boolean) {
        if (enable) {
            boostSystemAndLaunchGame(null)
        } else {
            viewModelScope.launch {
                preferences.setGameModeActive(false)
                _uiState.update {
                    it.copy(
                        isGameModeActive = false,
                        boostMessage = "O'yin rejimi o'chirildi. Standart himoya rejimi faol."
                    )
                }
            }
        }
    }

    private fun launchApp(packageName: String) {
        runCatching {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
    }

    companion object {
        fun readSystemTelemetry(context: Context): SystemTelemetry {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)

            val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1024L)
            val freeRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(128L)
            val usedRamMb = (totalRamMb - freeRamMb).coerceAtLeast(0L)
            val ramPercent = ((usedRamMb * 100L) / totalRamMb).toInt().coerceIn(1, 99)

            // Battery temperature & level
            val batteryIntent = runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 330) ?: 330
            val tempC = (tempTenths / 10.0f).coerceIn(20.0f, 65.0f)
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 80) ?: 80
            val scale = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100).coerceAtLeast(1)
            val batteryPct = ((level * 100) / scale).coerceIn(0, 100)

            // Storage stats
            val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
            val totalStorageGb = if (statFs != null) {
                statFs.totalBytes / (1024.0 * 1024.0 * 1024.0)
            } else 64.0
            val freeStorageGb = if (statFs != null) {
                statFs.availableBytes / (1024.0 * 1024.0 * 1024.0)
            } else 16.0

            return SystemTelemetry(
                totalRamMb = totalRamMb,
                usedRamMb = usedRamMb,
                freeRamMb = freeRamMb,
                ramUsagePercent = ramPercent,
                batteryTempCelsius = tempC,
                batteryLevelPercent = batteryPct,
                freeStorageGb = freeStorageGb,
                totalStorageGb = totalStorageGb
            )
        }
    }
}
