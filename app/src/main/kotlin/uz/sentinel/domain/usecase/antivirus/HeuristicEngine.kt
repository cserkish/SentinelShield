package uz.sentinel.domain.usecase.antivirus

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import uz.sentinel.domain.model.DangerLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicEngine @Inject constructor() {

    fun analyze(packageInfo: PackageInfo, packageManager: PackageManager): HeuristicResult {
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        var score = 0
        val flaggedPermissions = mutableListOf<String>()

        // Analyze dangerous permission combinations
        val hasSms = permissions.contains("android.permission.SEND_SMS") || permissions.contains("android.permission.RECEIVE_SMS") || permissions.contains("android.permission.READ_SMS")
        val hasOverlay = permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
        val hasInternet = permissions.contains("android.permission.INTERNET")

        if (hasSms) {
            score += 25
            flaggedPermissions.add("SMS yuborish/o'qish")
        }
        if (permissions.contains("android.permission.RECORD_AUDIO")) {
            score += 20
            flaggedPermissions.add("Mikrofonni yashirin tinglash")
        }
        if (permissions.contains("android.permission.READ_CONTACTS") || permissions.contains("android.permission.WRITE_CONTACTS")) {
            score += 15
            flaggedPermissions.add("Kontaktlar ro'yxatiga kirish")
        }
        if (hasOverlay) {
            score += 15
            flaggedPermissions.add("Ekranni to'sib qo'yish (Overlay)")
        }
        if (permissions.contains("android.permission.CAMERA")) {
            score += 10
            flaggedPermissions.add("Kamera ruxsati")
        }
        if (permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            score += 20
            flaggedPermissions.add("Boshqa ilovalarni yashirin o'rnatish (Dropper)")
        }
        if (permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            score += 35
            flaggedPermissions.add("Maxsus imkoniyatlarni boshqarish (Ekran va parollarni o'qish)")
        }
        if (permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")) {
            score += 5
            flaggedPermissions.add("Telefon yonganda ishga tushish")
        }

        // 🚨 High-Confidence Banking Trojan Signature Combination (SMS + Overlay/Accessibility + Internet)
        if (hasSms && hasOverlay && hasInternet) {
            score = maxOf(score + 25, 85)
            flaggedPermissions.add("⚠️ Bank ilovalarini to'sib OTP o'g'irlash xavfi (Banker)")
        }

        // Check if installed from official Google Play Store
        val isOfficialStore = isInstalledFromGooglePlay(packageInfo.packageName, packageManager)
        if (!isOfficialStore && score > 0) {
            // Apply multiplier for sideloaded apps requesting suspicious permissions
            score = (score * 1.35).toInt()
        }

        val finalScore = score.coerceIn(0, 100)
        val level = when {
            finalScore <= 30 -> DangerLevel.SAFE
            finalScore <= 60 -> DangerLevel.MEDIUM
            else -> DangerLevel.HIGH
        }

        return HeuristicResult(
            dangerScore = finalScore,
            dangerLevel = level,
            flaggedPermissions = flaggedPermissions,
            isOfficialStore = isOfficialStore
        )
    }

    private fun isInstalledFromGooglePlay(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            installer == "com.android.vending"
        } catch (e: Exception) {
            false
        }
    }
}

data class HeuristicResult(
    val dangerScore: Int,
    val dangerLevel: DangerLevel,
    val flaggedPermissions: List<String>,
    val isOfficialStore: Boolean
)
