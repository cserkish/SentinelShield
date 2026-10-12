package uz.sentinel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virus_hashes")
data class VirusHashEntity(
    @PrimaryKey
    val sha256: String,
    val malwareName: String,
    val severity: Int, // 1: Low, 2: Medium, 3: Critical
    val addedAt: Long = System.currentTimeMillis()
)
