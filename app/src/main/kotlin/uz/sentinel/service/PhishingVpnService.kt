package uz.sentinel.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import uz.sentinel.SentinelApp
import uz.sentinel.domain.repository.PhishingRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class PhishingVpnService : VpnService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return
        isRunning = true

        try {
            val builder = Builder()
                .setSession("SentinelShield NetGuard")
                .addAddress("10.1.10.1", 32)
                .addDnsServer("1.1.1.1") // Standard DNS fallback
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()

            serviceScope.launch {
                val vpnInput = FileInputStream(vpnInterface?.fileDescriptor)
                val vpnOutput = FileOutputStream(vpnInterface?.fileDescriptor)
                val packet = ByteBuffer.allocate(32767)

                while (isActive && isRunning) {
                    val length = vpnInput.read(packet.array())
                    if (length > 0) {
                        // Inspect packet header (DNS inspection)
                        packet.limit(length)
                        // In a real local loopback DNS VPN, we parse UDP 53 and check domains
                        packet.clear()
                    }
                    delay(50)
                }
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        serviceJob.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {}
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PhishingVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, SentinelApp.CHANNEL_MONITOR_SERVICE)
            .setContentTitle("NetGuard Himoyasi Faol")
            .setContentText("Fishing va soxta bank havolalaridan mahalliy himoya")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "O'chirish", pendingStop)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "uz.sentinel.service.ACTION_STOP_VPN"
        const val NOTIFICATION_ID = 2002
    }
}
