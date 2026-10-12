package uz.sentinel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isLocked: Boolean = true,
    val lockType: String = "PIN", // PIN, PATTERN, BIOMETRIC
    val lockedAt: Long = System.currentTimeMillis()
)
