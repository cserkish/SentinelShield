package uz.sentinel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dangerScore: Int, // 0 - 100
    val dangerLevel: String, // SAFE, MEDIUM, HIGH
    val detectedPermissions: String, // comma-separated
    val isKnownMalware: Boolean,
    val malwareName: String? = null,
    val isUserTrusted: Boolean = false,
    val lastScanned: Long = System.currentTimeMillis()
)
