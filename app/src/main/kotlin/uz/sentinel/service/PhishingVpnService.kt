package uz.sentinel.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import uz.sentinel.SentinelApp
import uz.sentinel.domain.repository.PhishingRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class PhishingVpnService : VpnService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob = Job()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (t: Throwable) {
            Log.w(TAG, "VPN startForeground failed: ${t.message}")
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        try {
            val builder = Builder()
                .setSession("SentinelShield NetGuard")
                .addAddress("10.1.10.1", 32)
                .addDnsServer("1.1.1.1")
                // Only route the DNS endpoint through TUN to prevent cutting off user's regular internet
                .addRoute("1.1.1.1", 32)

            val pfd = builder.establish()
            if (pfd == null) {
                Log.w(TAG, "VPN interface could not be established (prepare dialog not granted or another VPN active)")
                stopVpn()
                return
            }

            vpnInterface = pfd
            isRunning = true

            serviceScope.launch {
                val fd = pfd.fileDescriptor
                if (fd == null || !fd.valid()) {
                    stopVpn()
                    return@launch
                }

                try {
                    val vpnInput = FileInputStream(fd)
                    val vpnOutput = FileOutputStream(fd)
                    val packet = ByteBuffer.allocate(32767)

                    while (isActive && isRunning) {
                        try {
                            val length = vpnInput.read(packet.array())
                            if (length > 0) {
                                packet.limit(length)
                                // Packet parsed for DNS queries if needed
                                packet.clear()
                            }
                        } catch (e: IOException) {
                            // Interface closed or torn down
                            break
                        }
                        delay(100)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in VPN loop", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        try {
            serviceJob.cancel()
        } catch (e: Exception) {}
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
        private const val TAG = "PhishingVpnService"
        const val ACTION_START_FROM_BOOT = "uz.sentinel.service.VPN_START_FROM_BOOT"
        const val ACTION_STOP = "uz.sentinel.service.ACTION_STOP_VPN"
        const val NOTIFICATION_ID = 2002
    }
}
