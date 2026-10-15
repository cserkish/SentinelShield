package uz.sentinel.service

import android.app.Notification
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import uz.sentinel.SentinelApp
import uz.sentinel.core.utils.IntruderSelfieManager
import uz.sentinel.domain.repository.AppLockRepository
import uz.sentinel.presentation.applock.LockMethod
import uz.sentinel.presentation.applock.LockOverlayView
import uz.sentinel.presentation.applock.LockPromptActivity
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject
    lateinit var appLockRepository: AppLockRepository

    @Inject
    lateinit var intruderSelfieManager: IntruderSelfieManager

    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var overlayView: View? = null
    private var currentLockedPackage: String? = null
    private var lastForegroundPackage: String = ""

    private val mainHandler = Handler(Looper.getMainLooper())
    private var screenStateReceiver: android.content.BroadcastReceiver? = null
    @Volatile
    private var isScreenInteractive: Boolean = true

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

        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        isScreenInteractive = powerManager?.isInteractive ?: true

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
            android.util.Log.w("AppMonitorService", "startForeground failed gracefully: ${t.message}")
        }
        registerScreenStateReceiver()
        startMonitoring()
    }

    private fun registerScreenStateReceiver() {
        screenStateReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenInteractive = false
                        lastForegroundPackage = ""
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenInteractive = true
                        serviceScope.launch {
                            val fg = getForegroundPackage(lookbackMs = 15_000L)
                            if (fg != null && fg != packageName) {
                                lastForegroundPackage = fg
                                handleForegroundAppChange(fg)
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        removeOverlay()
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                if (isScreenInteractive) {
                    val foregroundPackage = getForegroundPackage()
                    if (foregroundPackage != null && foregroundPackage != lastForegroundPackage && foregroundPackage != packageName) {
                        lastForegroundPackage = foregroundPackage
                        handleForegroundAppChange(foregroundPackage)
                    }
                    delay(POLL_INTERVAL_MS)
                } else {
                    delay(2500L)
                }
            }
        }
    }

    private fun getForegroundPackage(lookbackMs: Long = 3500L): String? {
        return try {
            val now = System.currentTimeMillis()
            val usageEvents = usageStatsManager.queryEvents(now - lookbackMs, now)
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
                val method = appLockRepository.getLockMethod(targetPackage)
                mainHandler.post {
                    presentLockChallenge(targetPackage, method)
                }
            }
        } else {
            if (currentLockedPackage != null) {
                currentLockedPackage = null
                mainHandler.post { removeOverlay() }
            }
        }
    }

    /**
     * Launches the transparent LockPromptActivity (FragmentActivity) so BiometricPrompt,
     * PIN, and Pattern work with full hardware acceleration, and falls back to LockOverlayView
     * if OEM background activity restrictions block startActivity.
     */
    private fun presentLockChallenge(targetPackage: String, method: LockMethod) {
        val canDrawOverlays = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

        // When SYSTEM_ALERT_WINDOW is granted on Android 10+, starting a transparent Activity from
        // a service is allowed by the OS and enables native BiometricPrompt (Fingerprint/Face).
        val launchedActivity = try {
            val intent = LockPromptActivity.createIntent(this, targetPackage)
            startActivity(intent)
            currentLockedPackage = null
            true
        } catch (e: Exception) {
            false
        }

        if (!launchedActivity && canDrawOverlays) {
            showOverlay(targetPackage)
        }
    }

    private fun showOverlay(targetPackage: String) {
        if (overlayView != null) {
            (overlayView as? LockOverlayView)?.setPackageName(targetPackage)
            return
        }

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
                setOnDismissToHomeListener {
                    currentLockedPackage = null
                    lastForegroundPackage = ""
                    mainHandler.post {
                        removeOverlay()
                        launchHomeScreen()
                    }
                }
            }

            overlayView = lockView
            windowManager.addView(lockView, params)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun launchHomeScreen() {
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
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
