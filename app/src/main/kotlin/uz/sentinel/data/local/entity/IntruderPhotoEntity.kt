package uz.sentinel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_photos")
data class IntruderPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetAppPackage: String,
    val targetAppName: String,
    val encryptedPhotoPath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptCount: Int = 3
)
