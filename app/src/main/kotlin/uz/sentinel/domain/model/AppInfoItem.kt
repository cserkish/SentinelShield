package uz.sentinel.domain.model

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val sizeBytes: Long,
    val isSystemApp: Boolean,
    val isBloatware: Boolean,
    val versionName: String
)
