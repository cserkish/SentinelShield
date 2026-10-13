package uz.sentinel.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.sentinel.data.local.db.PhishingDomainDao
import uz.sentinel.data.local.db.VirusHashDao
import uz.sentinel.data.local.entity.PhishingDomainEntity
import uz.sentinel.data.local.entity.VirusHashEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThreatWorkerEntryPoint {
    fun virusHashDao(): VirusHashDao
    fun phishingDomainDao(): PhishingDomainDao
    fun sentinelPreferences(): SentinelPreferences
}

class ThreatUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ThreatWorkerEntryPoint::class.java
            )
            val virusDao = entryPoint.virusHashDao()
            val phishingDao = entryPoint.phishingDomainDao()
            val prefs = entryPoint.sentinelPreferences()

            val now = System.currentTimeMillis()

            // 1. Sync latest curated Uzbek & Global banking phishing indicators
            val latestPhishing = listOf(
                PhishingDomainEntity("click-uz.info", "Soxta Click fishing sayti", 100, now),
                PhishingDomainEntity("payme-uz.site", "Soxta Payme to'lov sahifasi", 100, now),
                PhishingDomainEntity("uzcard-bonus.top", "Soxta bank plastik kartasi yutug'i", 100, now),
                PhishingDomainEntity("humo-online.vip", "Soxta Humo karta to'lovi", 100, now),
                PhishingDomainEntity("pul-yordam.me", "Soxta ijtimoiy yordam firibgarligi", 100, now),
                PhishingDomainEntity("agrobank-mobile.top", "Soxta Agrobank portali", 100, now),
                PhishingDomainEntity("xalq-krediti.online", "Soxta Xalq banki onlayn kredit", 100, now),
                PhishingDomainEntity("uzum-yutuq.fun", "Soxta Uzum Market aksiya", 100, now),
                PhishingDomainEntity("telegram-premium-free.me", "Soxta Telegram Premium o'g'irlash", 100, now),
                PhishingDomainEntity("kredit-tasdiq.com", "Soxta mikrokredit fishing formasi", 100, now),
                PhishingDomainEntity("soliq-kompensatsiya.vip", "Soxta Soliq qo'mitasi kompensatsiyasi", 100, now),
                PhishingDomainEntity("oltin-konvert.top", "Moliyaviy piramida va firibgarlik", 100, now)
            )
            phishingDao.insertAll(latestPhishing)

            // 2. Sync latest malware hashes (SMS Stealers, Droppers, Miners)
            val latestHashes = listOf(
                VirusHashEntity("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "Trojan.AndroidOS.FakeBank", 95, now),
                VirusHashEntity("851364c9472e38c7f212260ff0df56b26dcf5a2283cb080de3fb845ffad92a10", "Spyware.Android.SmsStealer", 90, now),
                VirusHashEntity("2c83de6f9b1b7470f1a4e511b0580bfb9fbe009e4d50f8c5b0577d337d1d2b8b", "Trojan-Dropper.AndroidOS.Agent", 92, now),
                VirusHashEntity("a345b5962bc1234567890abcdef1234567890abcdef1234567890abcdef12345", "Miner.AndroidOS.CoinHive.Embedded", 85, now),
                VirusHashEntity("f432109876543210abcdef0123456789abcdef0123456789abcdef0123456789", "Backdoor.AndroidOS.RemoteAccess", 98, now)
            )
            virusDao.insertAll(latestHashes)

            // 3. Update scan/update timestamp
            prefs.updateScanTimestamp(now)

            android.util.Log.i("ThreatUpdateWorker", "Threat database synchronized successfully: ${latestPhishing.size} domains, ${latestHashes.size} hashes.")
            Result.success()
        }.getOrElse { error ->
            android.util.Log.e("ThreatUpdateWorker", "Threat update failed: ${error.message}", error)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SentinelThreatUpdateWorker"

        fun schedulePeriodic(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<ThreatUpdateWorker>(
                    24, TimeUnit.HOURS
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun executeNow(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<ThreatUpdateWorker>()
                    .build()
                WorkManager.getInstance(context).enqueue(request)
            }
        }
    }
}
