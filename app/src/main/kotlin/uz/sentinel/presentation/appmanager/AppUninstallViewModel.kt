package uz.sentinel.presentation.appmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.sentinel.domain.model.AppInfoItem
import java.io.File
import javax.inject.Inject

data class AppUninstallUiState(
    val isLoading: Boolean = true,
    val userApps: List<AppInfoItem> = emptyList(),
    val bloatwareApps: List<AppInfoItem> = emptyList(),
    val totalUserAppsSizeMb: Double = 0.0
)

@HiltViewModel
class AppUninstallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUninstallUiState())
    val uiState: StateFlow<AppUninstallUiState> = _uiState.asStateFlow()

    private val knownBloatwarePrefixes = listOf(
        "com.facebook.system",
        "com.facebook.appmanager",
        "com.facebook.services",
        "com.miui.analytics",
        "com.miui.msa.global",
        "com.transsion.analyzer",
        "com.transsion.magicshow",
        "com.aura.",
        "com.ironsource."
    )

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val (userList, bloatList) = withContext(Dispatchers.IO) {
                val packages = try {
                    packageManager.getInstalledPackages(0)
                } catch (e: Exception) {
                    emptyList()
                }

                val uList = mutableListOf<AppInfoItem>()
                val bList = mutableListOf<AppInfoItem>()

                for (pkg in packages) {
                    if (pkg.packageName == context.packageName) continue

                    val appInfo = pkg.applicationInfo ?: continue
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                    val appName = appInfo.loadLabel(packageManager).toString()
                    val sizeBytes = try {
                        val file = File(appInfo.sourceDir)
                        if (file.exists()) file.length() else 0L
                    } catch (e: Exception) {
                        0L
                    }

                    val isBloat = knownBloatwarePrefixes.any { pkg.packageName.lowercase().contains(it) }

                    val item = AppInfoItem(
                        packageName = pkg.packageName,
                        appName = appName,
                        sizeBytes = sizeBytes,
                        isSystemApp = isSystem,
                        isBloatware = isBloat,
                        versionName = pkg.versionName ?: "1.0"
                    )

                    if (!isSystem) {
                        uList.add(item)
                    } else if (isBloat) {
                        bList.add(item)
                    }
                }

                Pair(
                    uList.sortedByDescending { it.sizeBytes },
                    bList.sortedByDescending { it.sizeBytes }
                )
            }

            val totalMb = userList.sumOf { it.sizeBytes } / (1024.0 * 1024.0)

            _uiState.value = AppUninstallUiState(
                isLoading = false,
                userApps = userList,
                bloatwareApps = bloatList,
                totalUserAppsSizeMb = totalMb
            )
        }
    }
}
