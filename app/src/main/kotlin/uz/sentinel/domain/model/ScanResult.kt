package uz.sentinel.domain.model

data class ScanResult(
    val packageName: String,
    val appName: String,
    val dangerScore: Int,
    val dangerLevel: DangerLevel,
    val detectedPermissions: List<String>,
    val isKnownMalware: Boolean,
    val malwareName: String? = null,
    val isUserTrusted: Boolean = false,
    val isSystemApp: Boolean = false,
    val isUninstalledApkFile: Boolean = false,
    val apkFilePath: String? = null
)

enum class DangerLevel(val range: IntRange) {
    SAFE(0..30),
    MEDIUM(31..60),
    HIGH(61..100);

    companion object {
        fun fromScore(score: Int): DangerLevel = when {
            score <= SAFE.range.last   -> SAFE
            score <= MEDIUM.range.last -> MEDIUM
            else                       -> HIGH
        }
    }
}
