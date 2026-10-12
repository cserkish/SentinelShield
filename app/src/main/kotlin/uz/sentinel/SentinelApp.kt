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
        createNotificationChannels()
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
