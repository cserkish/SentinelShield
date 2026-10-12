package uz.sentinel.core.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Telefon brendini aniqlaydi va brand-specific Settings Intent'larini qaytaradi.
 * Qo'llab-quvvatlanadigan brendlar: Xiaomi, Samsung, Huawei, Honor, Oppo, Realme, Vivo,
 * OnePlus, Meizu va standart Android.
 */
object BrandDetector {

    enum class Brand {
        XIAOMI, SAMSUNG, HUAWEI, HONOR, OPPO, REALME, VIVO, ONEPLUS, MEIZU, STANDARD
    }

    // ─── Brand Detection ────────────────────────────────────────────────────────

    /**
     * Build.MANUFACTURER va Build.BRAND maydonlarini tahlil qilib brendni qaytaradi.
     */
    fun detect(): Brand {
        val manufacturer = Build.MANUFACTURER.lowercase().trim()
        val brand = Build.BRAND.lowercase().trim()

        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                    brand.contains("redmi") || brand.contains("poco")        -> Brand.XIAOMI

            manufacturer.contains("samsung") || brand.contains("samsung")    -> Brand.SAMSUNG

            manufacturer.contains("huawei") && !brand.contains("honor")      -> Brand.HUAWEI

            brand.contains("honor") || manufacturer.contains("honor")        -> Brand.HONOR

            manufacturer.contains("oppo") && !brand.contains("realme")       -> Brand.OPPO

            brand.contains("realme") || manufacturer.contains("realme")      -> Brand.REALME

            manufacturer.contains("vivo") || brand.contains("vivo")          -> Brand.VIVO

            manufacturer.contains("oneplus") || brand.contains("oneplus")    -> Brand.ONEPLUS

            manufacturer.contains("meizu") || brand.contains("meizu")        -> Brand.MEIZU

            else                                                              -> Brand.STANDARD
        }
    }

    // ─── Battery / Power Optimization Intent'lari ───────────────────────────────

    /**
     * Har bir brend uchun "Batareya optimallashtirish" / "Power management" sahifasiga
     * o'tadigan Intent'ni qaytaradi. Agar brend xususiy sahifani qo'llamasa null qaytadi.
     */
    fun getOptimizationIntent(context: Context): Intent? = when (detect()) {

        Brand.XIAOMI -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
            }
        } ?: safeIntent {
            Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        }

        Brand.SAMSUNG -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
        }

        Brand.HUAWEI -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }
        }

        Brand.HONOR -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }
        }

        Brand.OPPO -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.FakeActivity"
                )
            }
        }

        Brand.REALME -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                )
            }
        }

        Brand.VIVO -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.vivo.abe",
                    "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            }
        }

        Brand.ONEPLUS -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
        }

        Brand.MEIZU -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.SmartPermissionActivity"
                )
            }
        }

        Brand.STANDARD -> null
    }

    // ─── AutoStart Intent'lari ───────────────────────────────────────────────────

    /**
     * Har bir brend uchun "AutoStart" / "Startup manager" sahifasiga
     * o'tadigan Intent'ni qaytaradi.
     */
    fun getAutoStartIntent(context: Context): Intent? = when (detect()) {

        Brand.XIAOMI -> safeIntent {
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                putExtra("package_name", context.packageName)
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
        }

        Brand.SAMSUNG -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm.ui.ram.AppRunningControlActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.ram.AppRunningControlActivity"
                )
            }
        }

        Brand.HUAWEI -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
            }
        }

        Brand.HONOR -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        }

        Brand.OPPO -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            }
        }

        Brand.REALME -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        }

        Brand.VIVO -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
        } ?: safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
            }
        }

        Brand.ONEPLUS -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
        }

        Brand.MEIZU -> safeIntent {
            Intent().apply {
                component = ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.SmartPermissionActivity"
                )
            }
        }

        Brand.STANDARD -> null
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    /**
     * Intent yaratishga urinadi; ActivityNotFoundException yoki boshqa xatolik bo'lsa null qaytaradi.
     */
    private inline fun safeIntent(block: () -> Intent): Intent? {
        return try {
            block()
        } catch (e: Exception) {
            null
        }
    }
}
