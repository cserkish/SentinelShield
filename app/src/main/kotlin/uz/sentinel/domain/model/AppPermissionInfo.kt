package uz.sentinel.domain.model

data class AppPermissionInfo(
    val packageName: String,
    val appName: String,
    val grantedDangerousPerms: List<String>,
    val lastUsedTime: Long,           // epoch ms; 0 if never / unknown
    val hasBackgroundLocation: Boolean,
    val isSystemApp: Boolean = false
)
