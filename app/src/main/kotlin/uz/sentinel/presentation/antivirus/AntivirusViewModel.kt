package uz.sentinel.presentation.antivirus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        loadSavedResults()
    }

    private fun loadSavedResults() {
        viewModelScope.launch {
            scanResultDao.getAllResultsFlow().collect { entities ->
                val results = entities.map { entity ->
                    ScanResult(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        dangerScore = entity.dangerScore,
                        dangerLevel = DangerLevel.valueOf(entity.dangerLevel),
                        detectedPermissions = entity.detectedPermissions.split(",").filter { it.isNotBlank() },
                        isKnownMalware = entity.isKnownMalware,
                        malwareName = entity.malwareName,
                        isUserTrusted = entity.isUserTrusted
                    )
                }
                val highRisks = results.count { it.dangerLevel == DangerLevel.HIGH && !it.isUserTrusted }
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

                // Save to Room DB
                scanResultDao.insertResult(
                    ScanResultEntity(
                        packageName = result.packageName,
                        appName = result.appName,
                        dangerScore = result.dangerScore,
                        dangerLevel = result.dangerLevel.name,
                        detectedPermissions = result.detectedPermissions.joinToString(","),
                        isKnownMalware = result.isKnownMalware,
                        malwareName = result.malwareName,
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
        } catch (e: Exception) {
            // Ignored if cannot launch
        }
    }
}
