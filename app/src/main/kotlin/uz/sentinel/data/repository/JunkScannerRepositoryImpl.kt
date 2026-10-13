package uz.sentinel.data.repository

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.sentinel.domain.model.JunkCategory
import uz.sentinel.domain.model.JunkItem
import uz.sentinel.domain.repository.JunkScannerRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JunkScannerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : JunkScannerRepository {

    override suspend fun scanJunk(): List<JunkItem> = withContext(Dispatchers.IO) {
        val junkList = mutableListOf<JunkItem>()

        // 1. App Cache (Internal and External Cache)
        runCatching {
            scanDirectoryForJunk(context.cacheDir, JunkCategory.APP_CACHE, junkList)
            context.externalCacheDir?.let { scanDirectoryForJunk(it, JunkCategory.APP_CACHE, junkList) }
        }

        // 2. Temp and Log files in Downloads (.tmp, .log, .bak, .crdownload, .apk)
        runCatching {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                scanTempFiles(downloadsDir, junkList)
            }
        }

        // 3. Obsolete thumbnail cache (.thumbnails va thumbdata* fayllar)
        runCatching {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val thumbDirs = listOf(
                File(dcim, ".thumbnails"),
                File(pictures, ".thumbnails"),
                File(Environment.getExternalStorageDirectory(), ".thumbnails")
            )
            for (tDir in thumbDirs) {
                if (tDir.exists() && tDir.isDirectory) {
                    scanDirectoryForJunk(tDir, JunkCategory.THUMBNAILS, junkList)
                }
            }

            // DCIM ichidagi thumbdata* fayllarini ham aniqlash
            if (dcim.exists() && dcim.isDirectory) {
                dcim.listFiles()?.filter { it.isFile && it.name.lowercase().startsWith("thumbdata") }?.forEach { f ->
                    if (f.length() > 0) {
                        junkList.add(
                            JunkItem(
                                path = f.absolutePath,
                                name = f.name,
                                sizeBytes = f.length(),
                                category = JunkCategory.THUMBNAILS
                            )
                        )
                    }
                }
            }
        }

        // 4. Messencerlar vaqtinchalik keraksiz fayllari (WhatsApp ko'rilgan statuslari .Statuses va Sent nusxalari)
        runCatching {
            val root = Environment.getExternalStorageDirectory()
            val messengerJunkPaths = listOf(
                File(root, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(root, "WhatsApp/Media/.Statuses"),
                File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Sent"),
                File(root, "WhatsApp/Media/WhatsApp Video/Sent")
            )
            for (mDir in messengerJunkPaths) {
                if (mDir.exists() && mDir.isDirectory) {
                    scanDirectoryForJunk(mDir, JunkCategory.TEMP_FILES, junkList)
                }
            }
        }

        junkList
    }

    override suspend fun deleteJunk(items: List<JunkItem>): Long = withContext(Dispatchers.IO) {
        var totalFreedBytes = 0L
        for (item in items) {
            runCatching {
                val file = File(item.path)
                if (file.exists() && isSafeToDelete(file)) {
                    val size = if (file.isDirectory) file.walkTopDown().sumOf { it.length() } else file.length()
                    if (file.deleteRecursively()) {
                        totalFreedBytes += size
                    }
                }
            }
        }
        totalFreedBytes
    }

    private fun scanDirectoryForJunk(dir: File, category: JunkCategory, list: MutableList<JunkItem>) {
        runCatching {
            if (!dir.exists() || !dir.isDirectory) return
            dir.listFiles()?.forEach { file ->
                // Do not scan protected browser or user profile directories
                val norm = file.absolutePath.replace('\\', '/').lowercase()
                if (norm.contains("chrome") || norm.contains("browser") || norm.contains("auth")) return@forEach

                val size = if (file.isDirectory) {
                    file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                } else file.length()
                if (size > 0) {
                    list.add(
                        JunkItem(
                            path = file.absolutePath,
                            name = file.name,
                            sizeBytes = size,
                            category = category
                        )
                    )
                }
            }
        }
    }

    private fun scanTempFiles(dir: File, list: MutableList<JunkItem>) {
        runCatching {
            if (!dir.exists() || !dir.isDirectory) return
            dir.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile }
                .forEach { file ->
                    val name = file.name.lowercase()
                    when {
                        name.endsWith(".tmp") || name.endsWith(".log") || name.endsWith(".bak") || name.endsWith(".crdownload") -> {
                            list.add(
                                JunkItem(
                                    path = file.absolutePath,
                                    name = file.name,
                                    sizeBytes = file.length(),
                                    category = JunkCategory.TEMP_FILES
                                )
                            )
                        }
                        name.endsWith(".apk") -> {
                            list.add(
                                JunkItem(
                                    path = file.absolutePath,
                                    name = file.name,
                                    sizeBytes = file.length(),
                                    category = JunkCategory.DOWNLOADED_APKS
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun isSafeToDelete(file: File): Boolean {
        val normalizedPath = file.absolutePath.replace('\\', '/').lowercase()

        // 1. QAT'IY MAN QILINGAN: Google Chrome, Web brauzerlar, tizim hisoblari, sessiyalar va autentifikatsiya ma'lumotlari
        val protectedBrowsersAndCredentials = listOf(
            "chrome", "google.android.apps.chrome", "com.android.chrome",
            "firefox", "org.mozilla", "browser", "sbrowser", "opera",
            "cookies", "login", "auth", "session", "credential", "keystore", "accounts"
        )
        for (keyword in protectedBrowsersAndCredentials) {
            if (normalizedPath.contains(keyword)) {
                return false
            }
        }

        // 2. Miniatyura va eskizlar (.thumbnails va thumbdata) xavfsiz o'chiriladi
        if (normalizedPath.contains("/.thumbnails") || file.name.lowercase().startsWith("thumbdata")) {
            return true
        }

        // 3. Messencerlarning o'tkinchi status va yuborilgan dublikat nusxalari xavfsiz o'chiriladi
        if (normalizedPath.contains("/whatsapp/media/.statuses") ||
            normalizedPath.contains("/whatsapp video/sent") ||
            normalizedPath.contains("/whatsapp images/sent")) {
            return true
        }

        // 4. Vaqtinchalik va chala yuklangan fayllar (.crdownload, .tmp, .log, .bak)
        val ext = file.extension.lowercase()
        if (ext in listOf("tmp", "log", "bak", "crdownload")) {
            return true
        }

        // 5. Ilova kesh papkalari
        if (normalizedPath.contains("/cache/") || normalizedPath.endsWith("/cache")) {
            return true
        }

        // 6. FOYDALANUVCHINING SHAXSIY MUHIM PAPKALARINI ASLO O'CHIRMASLIK
        val protectedPersonalPaths = listOf("/dcim/", "/camera/", "/pictures/", "/movies/", "/music/", "/documents/")
        for (personal in protectedPersonalPaths) {
            if (normalizedPath.contains(personal)) {
                return false
            }
        }

        return true
    }
}
