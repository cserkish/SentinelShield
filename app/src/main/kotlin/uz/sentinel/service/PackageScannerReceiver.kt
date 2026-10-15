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
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.db.ScanResultDao
import uz.sentinel.data.local.entity.ScanResultEntity
import uz.sentinel.domain.model.DangerLevel
import uz.sentinel.domain.usecase.antivirus.ScanUseCase
import javax.inject.Inject

@AndroidEntryPoint
class PackageScannerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scanUseCase: ScanUseCase

    @Inject
    lateinit var scanResultDao: ScanResultDao

    @Inject
    lateinit var lockedAppDao: LockedAppDao

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val data = intent.data ?: return
        val packageName = data.schemeSpecificPart ?: return

        if (packageName == context.packageName) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                        val existing = scanResultDao.getResult(packageName)
                        val result = scanUseCase.execute(packageName)
                        scanResultDao.insertResult(
                            ScanResultEntity(
                                packageName = result.packageName,
                                appName = result.appName,
                                dangerScore = if (result.isSystemApp) 0 else result.dangerScore,
                                dangerLevel = if (result.isSystemApp) DangerLevel.SAFE.name else result.dangerLevel.name,
                                detectedPermissions = if (result.isSystemApp) "" else result.detectedPermissions.joinToString(","),
                                isKnownMalware = if (result.isSystemApp) false else result.isKnownMalware,
                                malwareName = if (result.isSystemApp) null else result.malwareName,
                                isUserTrusted = existing?.isUserTrusted ?: false
                            )
                        )
                        if (result.dangerLevel == DangerLevel.HIGH || result.isKnownMalware) {
                            showMalwareAlertNotification(context, result.appName, result.dangerScore)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            scanResultDao.deleteResult(packageName)
                            lockedAppDao.deleteByPackageName(packageName)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                pendingResult.finish()
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
