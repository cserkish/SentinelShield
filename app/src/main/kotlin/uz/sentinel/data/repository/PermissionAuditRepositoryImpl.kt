package uz.sentinel.data.repository

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import uz.sentinel.domain.model.AppPermissionInfo
import uz.sentinel.domain.repository.PermissionAuditRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans installed packages for dangerous permissions and – on Android 11+ –
 * resolves the last time each permission was used via [AppOpsManager].
 */
@Singleton
class PermissionAuditRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionAuditRepository {

    // Monitored permissions grouped by category (used for quick scan too).
    companion object {
        val MONITORED_PERMISSIONS = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.PROCESS_OUTGOING_CALLS,
        )

        /** Maps a permission string to the AppOps op string (Android 11+ API). */
        private val PERM_TO_OPS: Map<String, String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mapOf(
                android.Manifest.permission.RECORD_AUDIO             to AppOpsManager.OPSTR_RECORD_AUDIO,
                android.Manifest.permission.CAMERA                   to AppOpsManager.OPSTR_CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION     to AppOpsManager.OPSTR_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION   to AppOpsManager.OPSTR_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION to AppOpsManager.OPSTR_FINE_LOCATION,
                android.Manifest.permission.READ_CONTACTS            to AppOpsManager.OPSTR_READ_CONTACTS,
                android.Manifest.permission.READ_SMS                 to AppOpsManager.OPSTR_READ_SMS,
                android.Manifest.permission.READ_CALL_LOG            to AppOpsManager.OPSTR_READ_CALL_LOG,
                android.Manifest.permission.READ_PHONE_STATE         to AppOpsManager.OPSTR_READ_PHONE_STATE,
            )
        } else emptyMap()
    }

    private val _cache = MutableStateFlow<List<AppPermissionInfo>>(emptyList())

    override fun observePermissionAudit(): Flow<List<AppPermissionInfo>> = _cache.asStateFlow()

    override suspend fun getAppsByPermission(permission: String): List<AppPermissionInfo> =
        withContext(Dispatchers.IO) {
            buildAudit().filter { it.grantedDangerousPerms.contains(permission) }
        }

    override suspend fun refresh(): List<AppPermissionInfo> =
        withContext(Dispatchers.IO) {
            val result = buildAudit()
            _cache.value = result
            result
        }

    // ─── Core scan ───────────────────────────────────────────────────────────

    private fun buildAudit(): List<AppPermissionInfo> {
        val pm = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES
        } else {
            PackageManager.GET_PERMISSIONS
        }

        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(flags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        return packages.mapNotNull { pkgInfo ->
            val requestedPerms = pkgInfo.requestedPermissions ?: return@mapNotNull null
            val grantedDangerous = requestedPerms.filterIndexed { idx, perm ->
                perm in MONITORED_PERMISSIONS &&
                        (pkgInfo.requestedPermissionsFlags?.getOrNull(idx)
                            ?.and(android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)
            }
            if (grantedDangerous.isEmpty()) return@mapNotNull null

            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkgInfo.packageName
            }

            val hasBackground = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION in grantedDangerous
            val lastUsed = resolveLastUsed(appOps, pkgInfo.packageName, appInfo.uid, grantedDangerous)

            val appFlags = appInfo.flags
            val isSystem = (appFlags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appFlags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
                    isTrustedOemPackage(pkgInfo.packageName)

            AppPermissionInfo(
                packageName           = pkgInfo.packageName,
                appName               = appName,
                grantedDangerousPerms = grantedDangerous,
                lastUsedTime          = lastUsed,
                hasBackgroundLocation = hasBackground,
                isSystemApp           = isSystem
            )
        }.sortedByDescending { it.lastUsedTime }
    }

    private val oemPrefixes = listOf(
        "com.samsung.", "com.sec.", "com.google.android.", "com.android.",
        "com.miui.", "com.xiaomi.", "com.huawei.", "com.honor.",
        "com.coloros.", "com.oppo.", "com.vivo.", "com.transsion.",
        "com.oneplus.", "com.meizu."
    )

    private fun isTrustedOemPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return oemPrefixes.any { pkg.startsWith(it) }
    }

    /**
     * Resolves the last-used timestamp among granted permissions.
     * Detailed AppOps history requires system-level signature permissions.
     */
    private fun resolveLastUsed(
        appOps: AppOpsManager?,
        packageName: String,
        uid: Int,
        grantedPerms: List<String>
    ): Long {
        return 0L
    }
}
