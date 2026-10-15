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
import java.io.File
import javax.inject.Inject

data class AntivirusUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val currentScanningApp: String = "",
    val results: List<ScanResult> = emptyList(),
    val highRiskCount: Int = 0,
    val uninstalledApkCount: Int = 0,
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

    private fun isTargetStillPresent(packageNameOrKey: String): Boolean {
        if (packageNameOrKey.startsWith("apkfile:")) {
            val filePath = packageNameOrKey.removePrefix("apkfile:")
            return File(filePath).exists()
        }
        return try {
            packageManager.getPackageInfo(packageNameOrKey, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isTrueSystemApp(packageName: String): Boolean {
        if (packageName.startsWith("apkfile:")) return false
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
        viewModelScope.launch(Dispatchers.IO) {
            scanResultDao.getAllResultsFlow()
                .catch { emit(emptyList()) }
                .collect { entities ->
                    val validResults = mutableListOf<ScanResult>()
                    for (entity in entities) {
                        if (!isTargetStillPresent(entity.packageName)) {
                            scanResultDao.deleteResult(entity.packageName)
                            continue
                        }
                        val isApkFile = entity.packageName.startsWith("apkfile:")
                        val apkPath = if (isApkFile) entity.packageName.removePrefix("apkfile:") else null
                        val isSystem = !isApkFile && isTrueSystemApp(entity.packageName)

                        validResults.add(
                            ScanResult(
                                packageName = entity.packageName,
                                appName = entity.appName,
                                dangerScore = if (isSystem) 0 else entity.dangerScore,
                                dangerLevel = if (isSystem) DangerLevel.SAFE else (DangerLevel.entries.find { it.name == entity.dangerLevel } ?: DangerLevel.SAFE),
                                detectedPermissions = if (isSystem) emptyList() else entity.detectedPermissions.split(",").filter { it.isNotBlank() },
                                isKnownMalware = if (isSystem) false else entity.isKnownMalware,
                                malwareName = if (isSystem) null else entity.malwareName,
                                isUserTrusted = entity.isUserTrusted,
                                isSystemApp = isSystem,
                                isUninstalledApkFile = isApkFile,
                                apkFilePath = apkPath
                            )
                        )
                    }
                    val highRisks = validResults.count { it.dangerLevel == DangerLevel.HIGH && !it.isUserTrusted && !it.isSystemApp }
                    val apkFilesCount = validResults.count { it.isUninstalledApkFile }
                    _uiState.value = _uiState.value.copy(
                        results = validResults,
                        highRiskCount = highRisks,
                        uninstalledApkCount = apkFilesCount
                    )
                }
        }
    }

    fun scanAllApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isScanning = true, scanProgress = 0f)

            val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                .filter { it.applicationInfo != null }

            val uninstalledApkFiles = scanUseCase.findUninstalledApkFiles()

            val activeKeys = mutableSetOf<String>()
            installedPackages.forEach { activeKeys.add(it.packageName) }
            uninstalledApkFiles.forEach { activeKeys.add("apkfile:${it.absolutePath}") }

            // Purge stale DB records
            for (old in _uiState.value.results) {
                if (old.packageName !in activeKeys) {
                    scanResultDao.deleteResult(old.packageName)
                }
            }

            val total = (installedPackages.size + uninstalledApkFiles.size).coerceAtLeast(1)
            var currentStep = 0

            // 1. Scan installed apps
            for (pkg in installedPackages) {
                currentStep++
                _uiState.value = _uiState.value.copy(
                    scanProgress = currentStep.toFloat() / total,
                    currentScanningApp = pkg.applicationInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName
                )

                val existingEntity = scanResultDao.getResult(pkg.packageName)
                val result = scanUseCase.execute(pkg.packageName)
                val trusted = existingEntity?.isUserTrusted ?: result.isUserTrusted

                scanResultDao.insertResult(
                    ScanResultEntity(
                        packageName = result.packageName,
                        appName = result.appName,
                        dangerScore = if (result.isSystemApp) 0 else result.dangerScore,
                        dangerLevel = if (result.isSystemApp) DangerLevel.SAFE.name else result.dangerLevel.name,
                        detectedPermissions = if (result.isSystemApp) "" else result.detectedPermissions.joinToString(","),
                        isKnownMalware = if (result.isSystemApp) false else result.isKnownMalware,
                        malwareName = if (result.isSystemApp) null else result.malwareName,
                        isUserTrusted = trusted
                    )
                )
            }

            // 2. Deep-scan uninstalled .apk files on storage (Downloads, Telegram, WhatsApp, Documents)
            for (apkFile in uninstalledApkFiles) {
                currentStep++
                _uiState.value = _uiState.value.copy(
                    scanProgress = currentStep.toFloat() / total,
                    currentScanningApp = "APK fayl: ${apkFile.name}"
                )

                val result = scanUseCase.executeApkFile(apkFile) ?: continue
                val existingEntity = scanResultDao.getResult(result.packageName)
                val trusted = existingEntity?.isUserTrusted ?: false

                scanResultDao.insertResult(
                    ScanResultEntity(
                        packageName = result.packageName,
                        appName = result.appName,
                        dangerScore = result.dangerScore,
                        dangerLevel = result.dangerLevel.name,
                        detectedPermissions = result.detectedPermissions.joinToString(","),
                        isKnownMalware = result.isKnownMalware,
                        malwareName = result.malwareName,
                        isUserTrusted = trusted
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

    fun uninstallApp(packageNameOrKey: String) {
        if (packageNameOrKey.startsWith("apkfile:")) {
            viewModelScope.launch(Dispatchers.IO) {
                val path = packageNameOrKey.removePrefix("apkfile:")
                runCatching { File(path).delete() }
                scanResultDao.deleteResult(packageNameOrKey)
            }
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageNameOrKey")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored if cannot launch
        }
    }
}
