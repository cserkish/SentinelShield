package uz.sentinel.service

import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * JobScheduler orqali har 15 daqiqada AppMonitorService ning faolligini tekshiradi.
 * Service o'lgan bo'lsa startForegroundService() bilan qayta turtadi.
 *
 * Manifest'da android:permission="android.permission.BIND_JOB_SERVICE" qo'shilishi shart.
 */
class ServiceWatchdog : JobService() {

    companion object {
        private const val TAG             = "ServiceWatchdog"
        const val  JOB_ID                = 9001
        private const val INTERVAL_MS    = 15L * 60L * 1_000L   // 15 daqiqa

        /**
         * ServiceWatchdog ni JobScheduler ga ro'yxatdan o'tkazadi.
         * Ilova ishga tushganda (Application#onCreate) bir marta chaqiriladi.
         */
        fun schedule(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)
                    as? JobScheduler ?: return

            // Agar ish allaqachon navbatda bo'lsa qayta qo'shmaymiz
            if (scheduler.getPendingJob(JOB_ID) != null) {
                Log.d(TAG, "Watchdog already scheduled, skipping.")
                return
            }

            val jobInfo = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, ServiceWatchdog::class.java)
            )
                .setPeriodic(INTERVAL_MS)
                .setPersisted(true)                   // RECEIVE_BOOT_COMPLETED kerak
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .build()

            val result = scheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Watchdog scheduled successfully (interval=${INTERVAL_MS}ms)")
            } else {
                Log.e(TAG, "Watchdog schedule failed (result=$result)")
            }
        }

        /** Agar kerak bo'lsa Watchdog ni bekor qiladi. */
        fun cancel(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)
                    as? JobScheduler ?: return
            scheduler.cancel(JOB_ID)
            Log.d(TAG, "Watchdog cancelled")
        }
    }

    // ─── JobService callbacks ────────────────────────────────────────────────────

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "onStartJob: checking AppMonitorService...")
        checkAndRestartService()
        jobFinished(params, false)   // false → qayta schedule kerak emas (setPeriodic ishlatilgan)
        return false                 // ish sinxron bajarilyapti
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // true → job interrupted bo'lsa qayta schedule qil
        return true
    }

    // ─── Service tekshirish ──────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun checkAndRestartService() {
        if (!isAppMonitorServiceRunning()) {
            Log.w(TAG, "AppMonitorService is dead — restarting...")
            restartAppMonitorService()
        } else {
            Log.d(TAG, "AppMonitorService is alive.")
        }
    }

    /**
     * ActivityManager#getRunningServices() API 26+ da cheklangan, lekin
     * o'z servislarimiz uchun hali ham ishlaydi (maxNum=Int.MAX_VALUE).
     */
    @Suppress("DEPRECATION")
    private fun isAppMonitorServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false

        val className = AppMonitorService::class.java.name
        return try {
            manager.getRunningServices(Int.MAX_VALUE)
                ?.any { it.service.className == className }
                ?: false
        } catch (e: Exception) {
            Log.e(TAG, "isRunning check failed", e)
            false
        }
    }

    private fun restartAppMonitorService() {
        try {
            val intent = Intent(applicationContext, AppMonitorService::class.java).apply {
                action = AppMonitorService.ACTION_START_FROM_WATCHDOG
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            Log.d(TAG, "AppMonitorService restart command sent.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart AppMonitorService", e)
        }
    }
}
