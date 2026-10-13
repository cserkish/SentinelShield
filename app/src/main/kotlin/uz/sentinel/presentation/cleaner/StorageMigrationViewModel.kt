package uz.sentinel.presentation.cleaner

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class HeavyFileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val extension: String
)

data class StorageMigrationUiState(
    val isLoading: Boolean = false,
    val heavyFiles: List<HeavyFileItem> = emptyList(),
    val totalHeavySizeBytes: Long = 0L,
    val externalStorageDetected: Boolean = false
)

@HiltViewModel
class StorageMigrationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StorageMigrationUiState())
    val uiState: StateFlow<StorageMigrationUiState> = _uiState.asStateFlow()

    init {
        scanHeavyFiles()
    }

    fun scanHeavyFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val files = withContext(Dispatchers.IO) {
                val resultList = mutableListOf<HeavyFileItem>()
                val targetDirs = listOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                )

                for (dir in targetDirs) {
                    if (dir != null && dir.exists() && dir.isDirectory) {
                        runCatching {
                            dir.walkTopDown()
                                .maxDepth(4)
                                .filter { it.isFile && it.length() >= 50L * 1024 * 1024 } // > 50 MB
                                .forEach { file ->
                                    resultList.add(
                                        HeavyFileItem(
                                            name = file.name,
                                            path = file.absolutePath,
                                            sizeBytes = file.length(),
                                            extension = file.extension.uppercase()
                                        )
                                    )
                                }
                        }
                    }
                }
                resultList.sortedByDescending { it.sizeBytes }
            }

            val totalBytes = files.sumOf { it.sizeBytes }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                heavyFiles = files,
                totalHeavySizeBytes = totalBytes
            )
        }
    }
}
