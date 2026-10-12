package uz.sentinel.presentation.applock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.sentinel.core.utils.IntruderSelfieManager
import uz.sentinel.data.local.db.IntruderPhotoDao
import uz.sentinel.data.local.entity.IntruderPhotoEntity
import java.io.File
import javax.inject.Inject

data class IntruderPhotoItem(
    val entity: IntruderPhotoEntity,
    val bitmap: Bitmap?
)

data class IntruderPhotosUiState(
    val isLoading: Boolean = true,
    val photos: List<IntruderPhotoItem> = emptyList()
)

@HiltViewModel
class IntruderPhotosViewModel @Inject constructor(
    private val dao: IntruderPhotoDao,
    private val intruderSelfieManager: IntruderSelfieManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntruderPhotosUiState())
    val uiState: StateFlow<IntruderPhotosUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            dao.getAllPhotosFlow().collect { entities ->
                val items = withContext(Dispatchers.IO) {
                    entities.map { entity ->
                        val bitmap = runCatching {
                            val file = File(entity.encryptedPhotoPath)
                            if (file.exists()) {
                                val decryptedBytes = intruderSelfieManager.decryptFile(file)
                                BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                            } else null
                        }.getOrNull()
                        IntruderPhotoItem(entity, bitmap)
                    }
                }
                _uiState.value = IntruderPhotosUiState(isLoading = false, photos = items)
            }
        }
    }

    fun deletePhoto(item: IntruderPhotoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                File(item.entity.encryptedPhotoPath).delete()
            }
            dao.deletePhoto(item.entity.id)
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value.photos
            current.forEach { item ->
                runCatching { File(item.entity.encryptedPhotoPath).delete() }
            }
            dao.clearAll()
        }
    }
}
