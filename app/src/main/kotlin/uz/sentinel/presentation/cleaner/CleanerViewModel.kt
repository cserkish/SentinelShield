package uz.sentinel.presentation.cleaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.model.JunkItem
import uz.sentinel.domain.repository.JunkScannerRepository
import java.io.File
import javax.inject.Inject

data class CleanerUiState(
    val isScanning: Boolean = false,
    val isCleaning: Boolean = false,
    val items: List<JunkItem> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val lastCleanedBytes: Long = 0L,
    val cleanSuccessMessage: String? = null
)

@HiltViewModel
class CleanerViewModel @Inject constructor(
    private val junkRepository: JunkScannerRepository,
    private val preferences: SentinelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    init {
        startScan()
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, cleanSuccessMessage = null)
            val junk = junkRepository.scanJunk()
            val total = junk.filter { it.isSelected }.sumOf { it.sizeBytes }
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                items = junk,
                totalSizeBytes = total
            )
        }
    }

    fun toggleItemSelection(path: String) {
        val updated = _uiState.value.items.map {
            if (it.path == path) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.value = _uiState.value.copy(
            items = updated,
            totalSizeBytes = updated.filter { it.isSelected }.sumOf { it.sizeBytes }
        )
    }

    fun cleanSelected() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true)
            val currentItems = _uiState.value.items
            val selected = currentItems.filter { it.isSelected }
            val freedBytes = junkRepository.deleteJunk(selected)
            preferences.recordCleanedBytes(freedBytes)

            // Keep unselected items or any items that still exist on disk
            val remaining = currentItems.filter { !it.isSelected || File(it.path).exists() }
            val remainingSelectedSize = remaining.filter { it.isSelected }.sumOf { it.sizeBytes }

            val freedMb = String.format("%.1f", freedBytes / (1024.0 * 1024.0))
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                items = remaining,
                totalSizeBytes = remainingSelectedSize,
                lastCleanedBytes = freedBytes,
                cleanSuccessMessage = "$freedMb MB kesh va vaqtinchalik fayllar xavfsiz tozalandi!"
            )
        }
    }
}
