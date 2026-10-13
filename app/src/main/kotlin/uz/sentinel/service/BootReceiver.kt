package uz.sentinel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Qurilma yoqilganda (BOOT_COMPLETED) va ilova yangilanganda (MY_PACKAGE_REPLACED)
 * AppMonitorService va PhishingVpnService ni startForegroundService() orqali ishga tushiradi.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        // Only react to actual boot completion; avoid background FGS crash on MY_PACKAGE_REPLACED (Android 12+)
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                startAppMonitorService(context)
            }
        }
    }

    // ─── Service ishga tushirish ─────────────────────────────────────────────────

    private fun startAppMonitorService(context: Context) {
        try {
            val serviceIntent = Intent(context, AppMonitorService::class.java).apply {
                action = AppMonitorService.ACTION_START_FROM_BOOT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "AppMonitorService started safely")
        } catch (t: Throwable) {
            // Catches ForegroundServiceStartNotAllowedException on Android 12+ and OEM restrictions
            Log.w(TAG, "AppMonitorService boot start deferred or disallowed by OS: ${t.message}")
        }
    }
}
