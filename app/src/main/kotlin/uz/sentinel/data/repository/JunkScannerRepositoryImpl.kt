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

        // 1. App Cache (Internal and External)
        scanDirectoryForJunk(context.cacheDir, JunkCategory.APP_CACHE, junkList)
        context.externalCacheDir?.let { scanDirectoryForJunk(it, JunkCategory.APP_CACHE, junkList) }

        // 2. Temp and Log files in accessible external storage
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && downloadsDir.exists()) {
            scanTempFiles(downloadsDir, junkList)
        }

        junkList
    }

    override suspend fun deleteJunk(items: List<JunkItem>): Long = withContext(Dispatchers.IO) {
        var totalFreedBytes = 0L
        for (item in items) {
            val file = File(item.path)
            if (file.exists() && isSafeToDelete(file)) {
                val size = if (file.isDirectory) file.walkTopDown().sumOf { it.length() } else file.length()
                if (file.deleteRecursively()) {
                    totalFreedBytes += size
                }
            }
        }
        totalFreedBytes
    }

    private fun scanDirectoryForJunk(dir: File, category: JunkCategory, list: MutableList<JunkItem>) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            val size = if (file.isDirectory) file.walkTopDown().sumOf { it.length() } else file.length()
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

    private fun scanTempFiles(dir: File, list: MutableList<JunkItem>) {
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

    private fun isSafeToDelete(file: File): Boolean {
        val path = file.absolutePath.lowercase()
        // NEVER touch protected personal directories
        val forbiddenPaths = listOf("dcim", "pictures", "movies", "music", "documents", "telegram", "whatsapp")
        for (forbidden in forbiddenPaths) {
            if (path.contains("/$forbidden/")) {
                return false
            }
        }
        return true
    }
}
