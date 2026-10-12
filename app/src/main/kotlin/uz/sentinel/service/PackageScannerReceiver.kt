package uz.sentinel.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.sentinel.SentinelApp
import uz.sentinel.domain.model.DangerLevel
import uz.sentinel.domain.usecase.antivirus.ScanUseCase
import javax.inject.Inject

@AndroidEntryPoint
class PackageScannerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scanUseCase: ScanUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            val data = intent.data ?: return
            val packageName = data.schemeSpecificPart ?: return

            if (packageName == context.packageName) return // Ignore self

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = scanUseCase.execute(packageName)
                    if (result.dangerLevel == DangerLevel.HIGH || result.isKnownMalware) {
                        showMalwareAlertNotification(context, result.appName, result.dangerScore)
                    }
                } catch (e: Exception) {
                    // Sokin ushlash
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showMalwareAlertNotification(context: Context, appName: String, score: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, SentinelApp.CHANNEL_SECURITY_ALERTS)
            .setContentTitle("Xavfli Ilova Aniqlandi!")
            .setContentText("Yangi o'rnatilgan '$appName' ilovasi yuqori xavfga ega ($score/100). Uni o'chirish tavsiya etiladi.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(appName.hashCode(), notification)
    }
}
