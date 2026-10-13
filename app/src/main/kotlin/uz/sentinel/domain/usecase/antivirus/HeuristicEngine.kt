package uz.sentinel.domain.usecase.antivirus

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import uz.sentinel.domain.model.DangerLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicEngine @Inject constructor() {

    private val trustedOemPrefixes = listOf(
        "com.samsung.",
        "com.sec.",
        "com.google.android.",
        "com.android.",
        "com.miui.",
        "com.xiaomi.",
        "com.huawei.",
        "com.honor.",
        "com.coloros.",
        "com.oppo.",
        "com.vivo.",
        "com.transsion.",
        "com.oneplus.",
        "com.meizu."
    )

    private val officialStorePackages = setOf(
        "com.android.vending",                  // Google Play Store
        "com.sec.android.app.samsungapps",      // Samsung Galaxy Store
        "com.xiaomi.mipicks",                   // Xiaomi GetApps
        "com.huawei.appmarket",                 // Huawei AppGallery
        "com.oppo.market",                      // Oppo App Market
        "com.vivo.appstore",                    // Vivo App Store
        "ru.vk.store"                           // RuStore
    )

    fun isTrustedOemPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return trustedOemPrefixes.any { pkg.startsWith(it) }
    }

    fun isSystemOrOemApp(packageInfo: PackageInfo): Boolean {
        val appInfo = packageInfo.applicationInfo
        val flags = appInfo?.flags ?: 0
        val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (isSystem) return true

        return isTrustedOemPackage(packageInfo.packageName)
    }

    fun analyze(packageInfo: PackageInfo, packageManager: PackageManager): HeuristicResult {
        // Tizim va rasmiy OEM ilovalari uchun avtomatik 0 ball va XAVFSIZ (False Positive oldini olish)
        if (isSystemOrOemApp(packageInfo)) {
            return HeuristicResult(
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                flaggedPermissions = emptyList(),
                isOfficialStore = true,
                isSystemApp = true
            )
        }

        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        var score = 0
        val flaggedPermissions = mutableListOf<String>()

        // Foydalanuvchi o'rnatgan ilovalar uchun xavfli ruxsatlar kombinatsiyasini tahlil qilish
        val hasSms = permissions.contains("android.permission.SEND_SMS") ||
                permissions.contains("android.permission.RECEIVE_SMS") ||
                permissions.contains("android.permission.READ_SMS")
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

        // 🚨 Haqiqiy Bank Troyani imzosi (Faqat norasmiy uchinchi tomon ilovalarida)
        if (hasSms && hasOverlay && hasInternet) {
            score = maxOf(score + 25, 85)
            flaggedPermissions.add("⚠️ Bank ilovalarini to'sib OTP o'g'irlash xavfi (Banker)")
        }

        // Rasmiy do'konlar (Google Play, Galaxy Store, GetApps va h.k.) tekshiruvi
        val isOfficialStore = isInstalledFromOfficialStore(packageInfo.packageName, packageManager)
        if (!isOfficialStore && score > 0) {
            // Tashqi manbalardan (APK) yuklangan shubhali ilovalar uchun koeffitsient
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
            isOfficialStore = isOfficialStore,
            isSystemApp = false
        )
    }

    private fun isInstalledFromOfficialStore(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            installer in officialStorePackages
        } catch (e: Exception) {
            false
        }
    }
}

data class HeuristicResult(
    val dangerScore: Int,
    val dangerLevel: DangerLevel,
    val flaggedPermissions: List<String>,
    val isOfficialStore: Boolean,
    val isSystemApp: Boolean = false
)
