package uz.sentinel.domain.model

/**
 * Antivirus skanerlash natijasi — domain qatlami uchun sof data class.
 *
 * @param packageName        Ilova paket nomi  (e.g. "com.example.app")
 * @param appName            Foydalanuvchiga ko'rinadigan ilova nomi
 * @param dangerScore        0–100 oralig'idagi xavf bali
 * @param dangerLevel        [DangerLevel] enum: SAFE / MEDIUM / HIGH
 * @param detectedPermissions Xavfli deb topilgan ruxsatlar ro'yxati
 * @param isKnownMalware     SHA-256 hash viruslar bazasida topilganmi
 * @param malwareName        Topilgan zararli dastur nomi (null bo'lishi mumkin)
 * @param isUserTrusted      Foydalanuvchi bu ilovani "ishonchli" deb belgilaganmi
 * @param isSystemApp        OEM yoki Android tizim ilovasi ekanligi
 */
data class ScanResult(
    val packageName: String,
    val appName: String,
    val dangerScore: Int,
    val dangerLevel: DangerLevel,
    val detectedPermissions: List<String>,
    val isKnownMalware: Boolean,
    val malwareName: String? = null,
    val isUserTrusted: Boolean = false,
    val isSystemApp: Boolean = false
)

/**
 * Xavf darajasi.
 *
 * @param range Ball oralig'i (inclusive)
 */
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
