package uz.sentinel.presentation.antivirus

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.sentinel.data.local.db.ScanResultDao
import uz.sentinel.data.local.entity.ScanResultEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.model.DangerLevel
import uz.sentinel.domain.model.ScanResult
import uz.sentinel.domain.usecase.antivirus.ScanUseCase
import javax.inject.Inject

data class AntivirusUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val currentScanningApp: String = "",
    val results: List<ScanResult> = emptyList(),
    val highRiskCount: Int = 0,
    val lastScanTime: Long = 0L
)

@HiltViewModel
class AntivirusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val scanUseCase: ScanUseCase,
    private val scanResultDao: ScanResultDao,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AntivirusUiState())
    val uiState: StateFlow<AntivirusUiState> = _uiState.asStateFlow()

    private val oemPrefixes = listOf(
        "com.samsung.", "com.sec.", "com.google.android.", "com.android.",
        "com.miui.", "com.xiaomi.", "com.huawei.", "com.honor.",
        "com.coloros.", "com.oppo.", "com.vivo.", "com.transsion.",
        "com.oneplus.", "com.meizu."
    )

    init {
        loadSavedResults()
    }

    private fun isSystemOrOem(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        if (oemPrefixes.any { pkg.startsWith(it) }) return true
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val flags = appInfo.flags
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (_: Exception) {
            false
        }
    }

    private fun loadSavedResults() {
        viewModelScope.launch {
            scanResultDao.getAllResultsFlow()
                .catch { emit(emptyList()) }
                .collect { entities ->
                    val results = entities.map { entity ->
                        val isSystem = isSystemOrOem(entity.packageName)
                        ScanResult(
                            packageName = entity.packageName,
                            appName = entity.appName,
                            dangerScore = if (isSystem) 0 else entity.dangerScore,
                            dangerLevel = if (isSystem) DangerLevel.SAFE else (DangerLevel.entries.find { it.name == entity.dangerLevel } ?: DangerLevel.SAFE),
                            detectedPermissions = if (isSystem) emptyList() else entity.detectedPermissions.split(",").filter { it.isNotBlank() },
                            isKnownMalware = if (isSystem) false else entity.isKnownMalware,
                            malwareName = if (isSystem) null else entity.malwareName,
                            isUserTrusted = entity.isUserTrusted,
                            isSystemApp = isSystem
                        )
                    }
                    val highRisks = results.count { it.dangerLevel == DangerLevel.HIGH && !it.isUserTrusted && !it.isSystemApp }
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        highRiskCount = highRisks
                    )
                }
        }
    }

    fun scanAllApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isScanning = true, scanProgress = 0f)

            val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                .filter { it.applicationInfo != null }

            val total = installedPackages.size
            val scanResults = mutableListOf<ScanResult>()

            installedPackages.forEachIndexed { index, pkg ->
                _uiState.value = _uiState.value.copy(
                    scanProgress = (index + 1).toFloat() / total,
                    currentScanningApp = pkg.applicationInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName
                )

                val result = scanUseCase.execute(pkg.packageName)
                scanResults.add(result)

                // Save to Room DB (tizim ilovalari 0 ball bilan saqlanadi)
                scanResultDao.insertResult(
                    ScanResultEntity(
                        packageName = result.packageName,
                        appName = result.appName,
                        dangerScore = if (result.isSystemApp) 0 else result.dangerScore,
                        dangerLevel = if (result.isSystemApp) DangerLevel.SAFE.name else result.dangerLevel.name,
                        detectedPermissions = if (result.isSystemApp) "" else result.detectedPermissions.joinToString(","),
                        isKnownMalware = if (result.isSystemApp) false else result.isKnownMalware,
                        malwareName = if (result.isSystemApp) null else result.malwareName,
                        isUserTrusted = result.isUserTrusted
                    )
                )
            }

            preferences.updateScanTimestamp()

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                scanProgress = 1f,
                currentScanningApp = "",
                lastScanTime = System.currentTimeMillis()
            )
        }
    }

    fun toggleTrustApp(packageName: String, currentTrusted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            scanResultDao.setTrustedStatus(packageName, !currentTrusted)
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored if cannot launch
        }
    }
}
