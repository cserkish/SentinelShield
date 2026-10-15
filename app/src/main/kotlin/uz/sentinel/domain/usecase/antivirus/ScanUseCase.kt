package uz.sentinel.domain.usecase.antivirus

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.sentinel.domain.model.DangerLevel
import uz.sentinel.domain.model.ScanResult
import uz.sentinel.domain.repository.HashRepository
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject

class ScanUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val hashRepository: HashRepository,
    private val heuristicEngine: HeuristicEngine
) {

    suspend fun execute(packageName: String): ScanResult = withContext(Dispatchers.IO) {
        val packageInfo = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            return@withContext ScanResult(
                packageName = packageName,
                appName = packageName,
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                detectedPermissions = emptyList(),
                isKnownMalware = false,
                isSystemApp = false
            )
        }

        val appInfo = packageInfo.applicationInfo
        val appName = appInfo?.loadLabel(packageManager)?.toString() ?: packageName
        val apkPath = appInfo?.sourceDir
        val isSystem = heuristicEngine.isSystemOrOemApp(packageInfo)

        if (packageName == context.packageName) {
            return@withContext ScanResult(
                packageName = packageName,
                appName = appName,
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                detectedPermissions = emptyList(),
                isKnownMalware = false,
                isSystemApp = false
            )
        }

        if (isSystem) {
            return@withContext ScanResult(
                packageName = packageName,
                appName = appName,
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                detectedPermissions = emptyList(),
                isKnownMalware = false,
                isSystemApp = true
            )
        }

        val apkHash = if (apkPath != null) calculateSha256(apkPath) else ""
        val knownMalwareName = if (apkHash.isNotEmpty()) hashRepository.findMalware(apkHash) else null
        val heuristicResult = heuristicEngine.analyze(packageInfo, packageManager, isUninstalledArchive = false)

        val isKnownMalware = knownMalwareName != null
        val finalScore = if (isKnownMalware) 100 else heuristicResult.dangerScore
        val finalLevel = if (isKnownMalware) DangerLevel.HIGH else heuristicResult.dangerLevel

        ScanResult(
            packageName = packageName,
            appName = appName,
            dangerScore = finalScore,
            dangerLevel = finalLevel,
            detectedPermissions = heuristicResult.flaggedPermissions,
            isKnownMalware = isKnownMalware,
            malwareName = knownMalwareName,
            isSystemApp = false,
            isUninstalledApkFile = false,
            apkFilePath = apkPath
        )
    }

    /**
     * Deep-scans an uninstalled .apk file on external storage (e.g. /sdcard/Download/Taklifnoma.apk)
     * using PackageManager.getPackageArchiveInfo + SHA-256 + HeuristicEngine.
     */
    suspend fun executeApkFile(apkFile: File): ScanResult? = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || !apkFile.isFile) return@withContext null

        val archiveInfo = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        val appInfo = archiveInfo.applicationInfo
        appInfo?.sourceDir = apkFile.absolutePath
        appInfo?.publicSourceDir = apkFile.absolutePath

        val rawLabel = try {
            appInfo?.loadLabel(packageManager)?.toString()
        } catch (e: Exception) {
            null
        }
        val displayAppName = "[APK Fayl] ${rawLabel ?: apkFile.name}"
        val syntheticKey = "apkfile:${apkFile.absolutePath}"

        val apkHash = calculateSha256(apkFile.absolutePath)
        val knownMalwareName = if (apkHash.isNotEmpty()) hashRepository.findMalware(apkHash) else null
        val heuristicResult = heuristicEngine.analyze(archiveInfo, packageManager, isUninstalledArchive = true)

        val isKnownMalware = knownMalwareName != null
        val finalScore = if (isKnownMalware) 100 else heuristicResult.dangerScore
        val finalLevel = if (isKnownMalware) DangerLevel.HIGH else heuristicResult.dangerLevel

        ScanResult(
            packageName = syntheticKey,
            appName = displayAppName,
            dangerScore = finalScore,
            dangerLevel = finalLevel,
            detectedPermissions = heuristicResult.flaggedPermissions,
            isKnownMalware = isKnownMalware,
            malwareName = knownMalwareName,
            isSystemApp = false,
            isUninstalledApkFile = true,
            apkFilePath = apkFile.absolutePath
        )
    }

    /**
     * Finds all uninstalled .apk files across Downloads, Telegram, WhatsApp, and Documents folders.
     */
    suspend fun findUninstalledApkFiles(): List<File> = withContext(Dispatchers.IO) {
        val found = mutableListOf<File>()
        val seenPaths = mutableSetOf<String>()
        val root = Environment.getExternalStorageDirectory()

        val searchDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(root, "Telegram/Telegram Documents"),
            File(root, "Android/media/org.telegram.messenger/Telegram/Telegram Documents"),
            File(root, "Android/data/org.telegram.messenger/files/Telegram/Telegram Documents"),
            File(root, "WhatsApp/Media/WhatsApp Documents"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents")
        )

        for (dir in searchDirs) {
            runCatching {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown()
                        .maxDepth(4)
                        .filter { it.isFile && it.name.lowercase().endsWith(".apk") }
                        .forEach { file ->
                            if (seenPaths.add(file.canonicalPath)) {
                                found.add(file)
                            }
                        }
                }
            }
        }
        found
    }

    private fun calculateSha256(filePath: String): String {
        return try {
            val file = File(filePath)
            if (!file.exists()) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
