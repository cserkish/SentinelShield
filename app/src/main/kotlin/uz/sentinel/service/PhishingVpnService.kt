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
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.repository.PhishingRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class PhishingVpnService : VpnService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    @Inject
    lateinit var preferences: SentinelPreferences

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: CompletableJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    @Volatile
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn(isUnexpected = false)
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

        // Recreate coroutine scope if a previous session was cancelled
        if (!serviceJob.isActive) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
        }

        try {
            val builder = Builder()
                .setSession("SentinelShield NetGuard")
                .setMtu(1500)
                .addAddress("10.1.10.1", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                // Only route DNS endpoints through TUN so regular traffic goes directly
                .addRoute("1.1.1.1", 32)
                .addRoute("1.0.0.1", 32)

            val pfd = builder.establish()
            if (pfd == null) {
                Log.w(TAG, "VPN interface could not be established")
                stopVpn(isUnexpected = true)
                return
            }

            vpnInterface = pfd
            isRunning = true

            serviceScope.launch {
                preferences.setNetGuardEnabled(true)
                val fd = pfd.fileDescriptor
                if (fd == null || !fd.valid()) {
                    stopVpn(isUnexpected = true)
                    return@launch
                }

                try {
                    val vpnInput = FileInputStream(fd)
                    val vpnOutput = FileOutputStream(fd)
                    val buffer = ByteArray(32767)

                    while (isActive && isRunning) {
                        val length = try {
                            vpnInput.read(buffer)
                        } catch (e: IOException) {
                            break
                        }
                        if (length > 0) {
                            val packetCopy = buffer.copyOfRange(0, length)
                            launch {
                                handleIncomingIpPacket(packetCopy, vpnOutput)
                            }
                        } else if (length < 0) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in VPN loop", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopVpn(isUnexpected = true)
        }
    }

    /**
     * Parses IPv4 UDP DNS packets, checks queried domains against PhishingRepository,
     * returns NXDOMAIN for phishing/mining domains, or forwards safe queries via a protected UDP socket.
     */
    private suspend fun handleIncomingIpPacket(packet: ByteArray, vpnOutput: FileOutputStream) {
        try {
            if (packet.size < 28) return
            val version = (packet[0].toInt() ushr 4) and 0x0F
            if (version != 4) return // IPv4 only
            val ihl = (packet[0].toInt() and 0x0F) * 4
            if (ihl < 20 || packet.size < ihl + 8) return

            val protocol = packet[9].toInt() and 0xFF
            if (protocol != 17) return // UDP = 17

            val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
            if (destPort != 53) return

            val udpPayloadOffset = ihl + 8
            val dnsPayloadLen = packet.size - udpPayloadOffset
            if (dnsPayloadLen < 12) return

            val dnsPayload = packet.copyOfRange(udpPayloadOffset, packet.size)
            val queriedDomain = extractDnsQueryDomain(dnsPayload)

            val dnsResponsePayload: ByteArray = if (!queriedDomain.isNullOrBlank() &&
                phishingRepository.isPhishingDomain(queriedDomain)
            ) {
                Log.w(TAG, "Blocked phishing/mining DNS query: $queriedDomain")
                notifyPhishingBlocked(queriedDomain)
                buildBlockedDnsResponse(dnsPayload)
            } else {
                forwardDnsQuery(dnsPayload, packet.copyOfRange(16, 20), destPort) ?: return
            }

            val responseIpPacket = buildUdpIpResponsePacket(
                originalPacket = packet,
                ihl = ihl,
                dnsResponse = dnsResponsePayload
            )
            synchronized(vpnOutput) {
                vpnOutput.write(responseIpPacket)
                vpnOutput.flush()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed handling DNS packet: ${e.message}")
        }
    }

    private fun extractDnsQueryDomain(dns: ByteArray): String? {
        return try {
            if (dns.size <= 12) return null
            var pos = 12
            val labels = mutableListOf<String>()
            while (pos < dns.size) {
                val len = dns[pos].toInt() and 0xFF
                if (len == 0) break
                if ((len and 0xC0) == 0xC0) break // pointer
                pos++
                if (pos + len > dns.size) return null
                labels.add(String(dns, pos, len, Charsets.UTF_8))
                pos += len
            }
            if (labels.isEmpty()) null else labels.joinToString(".").lowercase()
        } catch (e: Exception) {
            null
        }
    }

    private fun buildBlockedDnsResponse(dnsQuery: ByteArray): ByteArray {
        val resp = dnsQuery.copyOf()
        // Set QR = 1 (response), RA = 1, RCODE = 3 (NXDOMAIN)
        resp[2] = ((dnsQuery[2].toInt() and 0x7F) or 0x81).toByte()
        resp[3] = 0x83.toByte()
        return resp
    }

    private fun forwardDnsQuery(dnsQuery: ByteArray, destIpBytes: ByteArray, destPort: Int): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            if (!protect(socket)) {
                Log.w(TAG, "Failed to protect upstream DNS socket")
                return null
            }
            socket.soTimeout = 4000
            val upstreamAddr = InetAddress.getByAddress(destIpBytes)
            val reqPacket = DatagramPacket(dnsQuery, dnsQuery.size, upstreamAddr, destPort)
            socket.send(reqPacket)

            val respBuf = ByteArray(4096)
            val respPacket = DatagramPacket(respBuf, respBuf.size)
            socket.receive(respPacket)
            respBuf.copyOfRange(0, respPacket.length)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { socket?.close() }
        }
    }

    private fun buildUdpIpResponsePacket(
        originalPacket: ByteArray,
        ihl: Int,
        dnsResponse: ByteArray
    ): ByteArray {
        val udpLen = 8 + dnsResponse.size
        val totalLen = 20 + udpLen
        val out = ByteBuffer.allocate(totalLen)

        // IPv4 Header (20 bytes)
        out.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        out.put(0x00.toByte()) // DSCP/ECN
        out.putShort(totalLen.toShort())
        out.putShort(originalPacket.let { ((it[4].toInt() and 0xFF) shl 8 or (it[5].toInt() and 0xFF)).toShort() })
        out.putShort(0x4000.toShort()) // Don't Fragment
        out.put(64.toByte()) // TTL
        out.put(17.toByte()) // Protocol = UDP (17)
        out.putShort(0.toShort()) // Header checksum placeholder (offset 10)

        // Swap Source IP (original Dest IP 16..19) and Dest IP (original Src IP 12..15)
        out.put(originalPacket, 16, 4)
        out.put(originalPacket, 12, 4)

        // UDP Header (8 bytes): Swap Src Port and Dest Port
        val origSrcPort = ((originalPacket[ihl].toInt() and 0xFF) shl 8) or (originalPacket[ihl + 1].toInt() and 0xFF)
        val origDstPort = ((originalPacket[ihl + 2].toInt() and 0xFF) shl 8) or (originalPacket[ihl + 3].toInt() and 0xFF)
        out.putShort(origDstPort.toShort())
        out.putShort(origSrcPort.toShort())
        out.putShort(udpLen.toShort())
        out.putShort(0.toShort()) // UDP checksum optional in IPv4 (0 = none)

        // DNS Payload
        out.put(dnsResponse)

        val bytes = out.array()
        // Calculate IPv4 header checksum over first 20 bytes
        var sum = 0
        for (i in 0 until 20 step 2) {
            val word = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            sum += word
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        bytes[10] = ((checksum ushr 8) and 0xFF).toByte()
        bytes[11] = (checksum and 0xFF).toByte()

        return bytes
    }

    private fun stopVpn(isUnexpected: Boolean = false) {
        isRunning = false
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { preferences.setNetGuardEnabled(false) }
        }
        try {
            serviceJob.cancel()
        } catch (e: Exception) {}
        try {
            vpnInterface?.close()
        } catch (e: Exception) {}
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)

        if (isUnexpected) {
            notifyVpnDisconnected()
        }
        stopSelf()
    }

    private var lastAlertTimeMs = 0L

    private fun notifyPhishingBlocked(domain: String) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTimeMs < 8_000L) return
        lastAlertTimeMs = now
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val notif = NotificationCompat.Builder(this, SentinelApp.CHANNEL_SECURITY_ALERTS)
                .setContentTitle("🛡️ NetGuard Xavfli Saytni Blokladi!")
                .setContentText("Bloklangan fishing/mayner manzili: $domain")
                .setSmallIcon(android.R.drawable.ic_secure)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm?.notify(("vpn_block_$domain").hashCode(), notif)
        }
    }

    private fun notifyVpnDisconnected() {
        runCatching {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val warningNotification = NotificationCompat.Builder(this, SentinelApp.CHANNEL_SECURITY_ALERTS)
                .setContentTitle("NetGuard Himoyasi To'xtatildi")
                .setContentText("Tarmoq fishing va mayner filtri o'chiq. Qayta yoqish uchun ilovaga kiring.")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager?.notify(NOTIFICATION_DISCONNECTED_ID, warningNotification)
        }
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
        stopVpn(isUnexpected = false)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PhishingVpnService"
        const val ACTION_START_FROM_BOOT = "uz.sentinel.service.VPN_START_FROM_BOOT"
        const val ACTION_STOP = "uz.sentinel.service.ACTION_STOP_VPN"
        const val NOTIFICATION_ID = 2002
        const val NOTIFICATION_DISCONNECTED_ID = 2003
    }
}
