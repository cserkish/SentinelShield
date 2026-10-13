package uz.sentinel.domain.usecase.antivirus

import android.content.Context
import android.content.pm.PackageManager
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

        // O'zimizning ilovamiz (SentinelShield) har doim xavfsiz
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

        // Tizim va rasmiy ommabop ilovalar (Telegram, Instagram, YouTube va b.) doimo XAVFSIZ (0 ball)
        val isOfficialPopular = heuristicEngine.isOfficialPopularApp(packageName)
        if (isSystem || isOfficialPopular) {
            return@withContext ScanResult(
                packageName = packageName,
                appName = appName,
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                detectedPermissions = emptyList(),
                isKnownMalware = false,
                isSystemApp = isSystem
            )
        }

        // 1. Foydalanuvchi o'rnatgan APK lar uchun SHA-256 heshini hisoblash
        val apkHash = if (apkPath != null) {
            calculateSha256(apkPath)
        } else {
            ""
        }

        // 2. Ofline viruslar xesh bazasidan qidirish
        val knownMalwareName = if (apkHash.isNotEmpty()) {
            hashRepository.findMalware(apkHash)
        } else null

        // 3. Evristik ruxsatlar tahlili
        val heuristicResult = heuristicEngine.analyze(packageInfo, packageManager)

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
            isSystemApp = false
        )
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
