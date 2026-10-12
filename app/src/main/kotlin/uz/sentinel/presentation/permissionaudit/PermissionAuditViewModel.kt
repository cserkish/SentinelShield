package uz.sentinel.presentation.permissionaudit

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.sentinel.domain.model.AppPermissionInfo
import uz.sentinel.domain.repository.PermissionAuditRepository
import javax.inject.Inject

sealed interface PermissionAuditUiState {
    object Loading : PermissionAuditUiState
    data class Success(
        val all: List<AppPermissionInfo>,
        val microphoneApps: List<AppPermissionInfo>,
        val cameraApps: List<AppPermissionInfo>,
        val locationApps: List<AppPermissionInfo>,
        val contactsApps: List<AppPermissionInfo>
    ) : PermissionAuditUiState
    data class Error(val message: String) : PermissionAuditUiState
}

@HiltViewModel
class PermissionAuditViewModel @Inject constructor(
    private val repository: PermissionAuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionAuditUiState>(PermissionAuditUiState.Loading)
    val uiState: StateFlow<PermissionAuditUiState> = _uiState.asStateFlow()

    /** Holds the Intent to open app settings (consumed by the UI via LaunchedEffect). */
    private val _openSettings = MutableStateFlow<Intent?>(null)
    val openSettings: StateFlow<Intent?> = _openSettings.asStateFlow()

    init {
        // Observe cached data changes in real-time
        viewModelScope.launch {
            repository.observePermissionAudit().collect { apps ->
                _uiState.value = mapToUiState(apps)
            }
        }
        loadApps()
    }

    /** Triggers a full re-scan of installed apps. */
    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = PermissionAuditUiState.Loading
            runCatching { repository.refresh() }
                .onFailure { _uiState.value = PermissionAuditUiState.Error(it.message ?: "Unknown error") }
        }
    }

    /** Emits an Intent that the Compose UI can collect and launch. */
    fun openAppSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        _openSettings.value = intent
    }

    /** Called after the UI has consumed the settings intent. */
    fun onSettingsIntentConsumed() {
        _openSettings.value = null
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun mapToUiState(apps: List<AppPermissionInfo>): PermissionAuditUiState.Success {
        fun List<AppPermissionInfo>.byPerm(vararg perms: String) =
            filter { app -> app.grantedDangerousPerms.any { it in perms } }

        return PermissionAuditUiState.Success(
            all = apps,
            microphoneApps = apps.byPerm(android.Manifest.permission.RECORD_AUDIO),
            cameraApps     = apps.byPerm(android.Manifest.permission.CAMERA),
            locationApps   = apps.byPerm(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            contactsApps   = apps.byPerm(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_SMS
            )
        )
    }
}
