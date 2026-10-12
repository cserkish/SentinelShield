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

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",      // HTC / bazı Xiaomi qurilmalar
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {  // HTC
                startAppMonitorService(context)
                startPhishingVpnService(context)
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
            Log.d(TAG, "AppMonitorService started")
        } catch (e: Exception) {
            Log.e(TAG, "AppMonitorService start failed", e)
        }
    }

    private fun startPhishingVpnService(context: Context) {
        try {
            val serviceIntent = Intent(context, PhishingVpnService::class.java).apply {
                action = PhishingVpnService.ACTION_START_FROM_BOOT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "PhishingVpnService started")
        } catch (e: Exception) {
            Log.e(TAG, "PhishingVpnService start failed", e)
        }
    }
}
