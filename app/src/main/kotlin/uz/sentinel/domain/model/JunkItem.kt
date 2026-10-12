package uz.sentinel.domain.model

enum class JunkCategory(val title: String) {
    APP_CACHE("Ilovalar keshi"),
    TEMP_FILES("Vaqtinchalik fayllar (.tmp, .log, .bak)"),
    THUMBNAILS("Miniatyuralar (.thumbnails)"),
    DOWNLOADED_APKS("O'rnatilgan APK qoldiqlari")
}

data class JunkItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val category: JunkCategory,
    val isSelected: Boolean = true
)
