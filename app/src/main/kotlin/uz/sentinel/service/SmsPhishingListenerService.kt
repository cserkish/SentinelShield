package uz.sentinel.service

import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import uz.sentinel.SentinelApp
import uz.sentinel.domain.repository.PhishingRepository
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class SmsPhishingListenerService : NotificationListenerService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 1. Detects dangerous executable/package attachments in Telegram, WhatsApp, SMS (.apk, .xapk, .apks, .dex, .scr, .msi)
    private val apkAttachmentPattern = Pattern.compile(
        "\\b([\\w\\-\\.\\s]{1,64}\\.(?:apk|xapk|apks|dex|scr|msi))\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 2. Detects URLs and bare domains
    private val urlPattern = Pattern.compile(
        "(?:https?://)?(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z]{2,10}\\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)",
        Pattern.CASE_INSENSITIVE
    )

    // 3. Known URL shorteners used to disguise phishing links
    private val urlShortenerDomains = setOf(
        "bit.ly", "clck.ru", "cutt.ly", "tinyurl.com", "shorturl.at",
        "is.gd", "rb.gy", "t.co", "goo.su", "u.to", "tiny.cc", "ow.ly"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val fullMessage = "$title $text $bigText $subText"

        if (fullMessage.isBlank()) return

        // A. ALWAYS warn if any .apk / .xapk / .dex file is detected in the notification!
        val apkMatcher = apkAttachmentPattern.matcher(fullMessage)
        if (apkMatcher.find()) {
            val fileName = apkMatcher.group(1)?.trim() ?: "noma'lum.apk"
            showSecurityAlert(
                alertId = ("apk_$fileName").hashCode(),
                title = "🚨 Xavfli APK Fayl Ogohlantirishi!",
                body = "Xabarda '$fileName' o'rnatish fayli aniqlandi! Messenjer va SMS orqali kelgan APK fayllarni aslo o'rnatmang — bu bank kartangizni o'g'irlovchi troyan bo'lishi mumkin!"
            )
        }

        // B. Extract all URLs/domains and ALWAYS warn on URL shorteners & Phishing domains
        val candidates = mutableSetOf<String>()
        val matcher = urlPattern.matcher(fullMessage.lowercase())
        while (matcher.find()) {
            matcher.group()?.let { candidates.add(it) }
        }

        if (candidates.isNotEmpty()) {
            scope.launch {
                for (rawUrl in candidates) {
                    val domain = extractDomain(rawUrl)
                    if (domain.isBlank() || domain.endsWith(".apk") || domain.endsWith(".xapk")) continue

                    // Always warn on URL shorteners
                    if (isUrlShortener(domain)) {
                        showSecurityAlert(
                            alertId = ("short_$domain").hashCode(),
                            title = "⚠️ Yashirilgan Qisqa Havola Aniqlandi!",
                            body = "Xabaringizda qisqartirilgan havola ($domain) mavjud. Firibgarlar fishing saytlarni yashirish uchun qisqa linklardan foydalanadi — ehtiyot bo'ling!"
                        )
                        continue
                    }

                    // Always warn on Phishing / Mining / Fake Bank domains
                    if (phishingRepository.isPhishingDomain(domain)) {
                        showSecurityAlert(
                            alertId = ("phish_$domain").hashCode(),
                            title = "🛑 Xavfli Fishing Havolasi!",
                            body = "Xabaringizda soxta/fishing havola aniqlandi: $domain. Unga kirmang va karta ma'lumotlaringizni kiritmang!"
                        )
                    }
                }
            }
        }
    }

    private fun isUrlShortener(domain: String): Boolean {
        val clean = domain.lowercase().trim()
        return urlShortenerDomains.any { clean == it || clean.endsWith(".$it") }
    }

    private fun extractDomain(url: String): String {
        return try {
            val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val uri = java.net.URI(normalized)
            (uri.host ?: url).removePrefix("www.")
        } catch (e: Exception) {
            url.removePrefix("http://").removePrefix("https://").removePrefix("www.").substringBefore("/")
        }
    }

    private fun showSecurityAlert(alertId: Int, title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, SentinelApp.CHANNEL_SECURITY_ALERTS)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alertId, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
