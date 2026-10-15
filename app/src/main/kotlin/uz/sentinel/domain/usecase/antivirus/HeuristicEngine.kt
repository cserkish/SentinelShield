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

    private val officialPopularApps = setOf(
        "org.telegram.messenger",
        "org.thunderdog.challegram",
        "com.instagram.android",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.google.android.gm",
        "com.google.android.apps.maps",
        "com.google.android.apps.photos",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.snapchat.android",
        "com.twitter.android",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.vkontakte.android",
        "ru.ok.android",
        "com.viber.voip",
        "com.imo.android.imoim",
        "com.pinterest",
        "com.linkedin.android",
        "com.android.chrome"
    )

    fun isTrustedOemPrefix(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return trustedOemPrefixes.any { pkg.startsWith(it) }
    }

    fun isOfficialPopularApp(packageName: String): Boolean {
        return officialPopularApps.contains(packageName.lowercase())
    }

    /**
     * Strict system/OEM check: ONLY returns true if the OS kernel/PackageManager confirms
     * FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP, or if an OEM package was installed by the system/store.
     * Prevents fake malware APKs from spoofing "com.android.*" or "com.samsung.*" package names!
     */
    fun isSystemOrOemApp(packageInfo: PackageInfo): Boolean {
        val appInfo = packageInfo.applicationInfo ?: return false
        val flags = appInfo.flags
        val isTrueSystemFlag = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return isTrueSystemFlag
    }

    fun analyze(
        packageInfo: PackageInfo,
        packageManager: PackageManager,
        isUninstalledArchive: Boolean = false
    ): HeuristicResult {
        val pkg = packageInfo.packageName.lowercase()
        val isTrueSystem = !isUninstalledArchive && isSystemOrOemApp(packageInfo)

        // 1. Haqiqiy tizim ilovalari uchun 0 ball va XAVFSIZ
        if (isTrueSystem) {
            return HeuristicResult(
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                flaggedPermissions = emptyList(),
                isOfficialStore = true,
                isSystemApp = true
            )
        }

        val isOfficialStore = !isUninstalledArchive && isInstalledFromOfficialStore(packageInfo.packageName, packageManager)

        // 2. Rasmiy do'kondan o'rnatilgan ommabop ilovalar uchun 0 ball
        if (!isUninstalledArchive && isOfficialStore && (isOfficialPopularApp(pkg) || isTrustedOemPrefix(pkg))) {
            return HeuristicResult(
                dangerScore = 0,
                dangerLevel = DangerLevel.SAFE,
                flaggedPermissions = emptyList(),
                isOfficialStore = true,
                isSystemApp = false
            )
        }

        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        var score = 0
        val flaggedPermissions = mutableListOf<String>()

        val appName = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString()?.lowercase() ?: ""
        val hasOverlay = permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
        val hasInternet = permissions.contains("android.permission.INTERNET")
        val hasSms = permissions.contains("android.permission.SEND_SMS") ||
                permissions.contains("android.permission.RECEIVE_SMS") ||
                permissions.contains("android.permission.READ_SMS")

        // 3. Anti-Spoofing: Tizim ilovasi bo'lmasa-yu, lekin com.android.*, com.samsung.*, com.google.android.* nomini olgan bo'lsa
        val isSpoofedSystemPackage = !isTrueSystem && !isOfficialStore && (
            pkg.startsWith("com.android.system") ||
            pkg.contains("google.play.services.update") ||
            appName.contains("google play services update") ||
            (appName == "flashlight" && (hasOverlay || hasSms)) ||
            (appName.contains("super cleaner") && hasOverlay) ||
            (isTrustedOemPrefix(pkg) && hasSms)
        )

        if (isSpoofedSystemPackage) {
            score = 100
            flaggedPermissions.add("🚨 Soxta tizim ilovasi / Paket nomi qalbakilashtirilgan (Spoofing)")
        }

        // 4. O'rnatilmagan .apk fayllar uchun maxsus chuqur tahlil (masalan: Taklifnoma.apk, Fotom.apk, Soliq.apk)
        if (isUninstalledArchive) {
            score += 20
            flaggedPermissions.add("📦 Tashqi yuklab olingan o'rnatilmagan APK fayl")
            val suspiciousSocialNames = listOf("taklifnoma", "fotom", "rasm", "video", "soliq", "jarima", "mukofot", "bonus", "kredit", "click", "payme", "uzcard", "humo")
            if (suspiciousSocialNames.any { appName.contains(it) || pkg.contains(it) }) {
                score += 45
                flaggedPermissions.add("🚨 Ijtimoiy muhandislik / Troyan APK nomi aniqlandi")
            }
        }

        if (hasSms) {
            score += 25
            flaggedPermissions.add("SMS yuborish va OTP kodlarni o'qish")
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

        // 🚨 Haqiqiy Bank Troyani imzosi
        if (hasSms && (hasOverlay || isUninstalledArchive) && hasInternet) {
            score = maxOf(score + 25, 90)
            flaggedPermissions.add("⚠️ Bank ilovalarini to'sib OTP o'g'irlash xavfi (Banker Trojan)")
        }

        if (!isOfficialStore && score > 0 && !isUninstalledArchive) {
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
