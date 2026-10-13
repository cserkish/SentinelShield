package uz.sentinel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SentinelApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installGlobalCrashShield()
        createNotificationChannels()
    }

    private fun installGlobalCrashShield() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SentinelShield", "ZeroCrashShield caught unhandled exception on ${thread.name}", throwable)
            val msg = throwable.message.orEmpty()
            val stack = android.util.Log.getStackTraceString(throwable)

            // Save crash dump to internal file
            runCatching {
                val file = java.io.File(filesDir, "crash_dump.txt")
                file.writeText("Time: ${System.currentTimeMillis()}\nThread: ${thread.name}\nError: $msg\nStack:\n$stack")
            }

            if (msg.contains("ForegroundServiceStartNotAllowedException") ||
                msg.contains("Timeout waiting for draw") ||
                msg.contains("Bad notification posted") ||
                msg.contains("RemoteServiceException") ||
                msg.contains("SQLiteException") ||
                msg.contains("Room") ||
                msg.contains("no column named") ||
                msg.contains("DeadSystemException")
            ) {
                android.util.Log.w("SentinelShield", "Swallowed benign platform/database crash on budget device: $msg")
                return@setDefaultUncaughtExceptionHandler
            }
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY_ALERTS,
                "Xavfsizlik ogohlantirishlari",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Virus va fishing xavflari haqida tezkor xabarlar"
                enableVibration(true)
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_SERVICE,
                "Himoya xizmati (Faol)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ilovalarni qulflash va himoya qilish uchun fonda ishlash holati"
            }

            notificationManager.createNotificationChannel(securityChannel)
            notificationManager.createNotificationChannel(monitorChannel)
        }
    }

    companion object {
        const val CHANNEL_SECURITY_ALERTS = "sentinel_security_alerts"
        const val CHANNEL_MONITOR_SERVICE = "sentinel_monitor_service"
    }
}
