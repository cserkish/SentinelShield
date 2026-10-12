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

    // Regex to match URLs in notification texts
    private val urlPattern = Pattern.compile(
        "https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)"
    )

    private val suspiciousKeywords = listOf(
        "karta bloklandi", "plastik karta", "yordam puli", "parol", "kod", "tasdiqlang",
        "bir martalik kod", "pul o'tkazildi", "yutuq", "kartangiz"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val fullMessage = "$title $text".lowercase()

        // Check if message contains URLs and suspicious financial keywords
        val matcher = urlPattern.matcher(fullMessage)
        if (matcher.find()) {
            val extractedUrl = matcher.group()
            val containsKeywords = suspiciousKeywords.any { fullMessage.contains(it) }

            if (containsKeywords) {
                scope.launch {
                    val domain = extractDomain(extractedUrl)
                    val isPhishing = phishingRepository.isPhishingDomain(domain)

                    if (isPhishing || !isLegitimateDomain(domain)) {
                        showPhishingAlert(domain)
                    }
                }
            }
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun isLegitimateDomain(domain: String): Boolean {
        val safeDomains = listOf("click.uz", "payme.uz", "uzcard.uz", "humocard.uz", "gov.uz")
        return safeDomains.any { domain.endsWith(it) }
    }

    private fun showPhishingAlert(domain: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, SentinelApp.CHANNEL_SECURITY_ALERTS)
            .setContentTitle("Xavfli Fishing Havolasi!")
            .setContentText("Xabaringizda shubhali havola aniqlandi: $domain. Unga kirmang!")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
