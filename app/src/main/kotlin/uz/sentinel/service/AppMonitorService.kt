package uz.sentinel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import uz.sentinel.SentinelApp
import uz.sentinel.core.utils.IntruderSelfieManager
import uz.sentinel.domain.repository.AppLockRepository
import uz.sentinel.presentation.applock.LockOverlayView
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject
    lateinit var appLockRepository: AppLockRepository

    @Inject
    lateinit var intruderSelfieManager: IntruderSelfieManager

    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private var overlayView: View? = null
    private var currentLockedPackage: String? = null
    private var lastForegroundPackage: String = ""

    private val mainHandler = Handler(Looper.getMainLooper())
    private var packageScannerReceiver: PackageScannerReceiver? = null

    companion object {
        const val ACTION_START_FROM_BOOT = "uz.sentinel.service.START_FROM_BOOT"
        const val ACTION_START_FROM_WATCHDOG = "uz.sentinel.service.START_FROM_WATCHDOG"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 350L

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildSilentNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, buildSilentNotification())
            }
        } catch (t: Throwable) {
            // Sokin ushlash: ForegroundServiceStartNotAllowedException yoki MissingForegroundServiceTypeException
            android.util.Log.w("AppMonitorService", "startForeground failed gracefully: ${t.message}")
        }
        registerPackageScanner()
        startMonitoring()
    }

    private fun registerPackageScanner() {
        packageScannerReceiver = PackageScannerReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageScannerReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        removeOverlay()
        packageScannerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                val foregroundPackage = getForegroundPackage()
                if (foregroundPackage != null && foregroundPackage != lastForegroundPackage && foregroundPackage != packageName) {
                    lastForegroundPackage = foregroundPackage
                    handleForegroundAppChange(foregroundPackage)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        return try {
            val now = System.currentTimeMillis()
            val usageEvents = usageStatsManager.queryEvents(now - 3000L, now)
            val event = UsageEvents.Event()
            var foreground: String? = null
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foreground = event.packageName
                }
            }
            foreground
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun handleForegroundAppChange(targetPackage: String) {
        val isLocked = appLockRepository.isAppLocked(targetPackage)
        if (isLocked) {
            if (currentLockedPackage != targetPackage) {
                currentLockedPackage = targetPackage
                mainHandler.post { showOverlay(targetPackage) }
            }
        } else {
            if (currentLockedPackage != null) {
                currentLockedPackage = null
                mainHandler.post { removeOverlay() }
            }
        }
    }

    private fun showOverlay(targetPackage: String) {
        if (overlayView != null) return

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val lockView = LockOverlayView(
                context = this,
                appLockRepository = appLockRepository,
                intruderSelfieManager = intruderSelfieManager
            ).apply {
                setPackageName(targetPackage)
                setOnUnlockListener {
                    currentLockedPackage = null
                    mainHandler.post { removeOverlay() }
                }
            }

            overlayView = lockView
            windowManager.addView(lockView, params)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            overlayView = null
        }
    }

    private fun buildSilentNotification(): Notification {
        return NotificationCompat.Builder(this, SentinelApp.CHANNEL_MONITOR_SERVICE)
            .setContentTitle("SentinelShield Faol")
            .setContentText("Ilovalar himoyasi va xavfsizlik nazorati ishlamoqda")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}
