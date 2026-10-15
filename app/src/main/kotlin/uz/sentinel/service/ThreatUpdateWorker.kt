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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uz.sentinel.data.local.db.PhishingDomainDao
import uz.sentinel.data.local.db.VirusHashDao
import uz.sentinel.data.local.entity.PhishingDomainEntity
import uz.sentinel.data.local.entity.VirusHashEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

@Serializable
data class RemotePhishingDto(
    val domain: String,
    val category: String,
    val riskScore: Int = 100
)

@Serializable
data class RemoteMalwareHashDto(
    val sha256: String,
    val malwareName: String,
    val severity: Int = 95
)

@Serializable
data class RemoteThreatFeedDto(
    val version: String = "2026.10",
    val updatedAt: Long = 0L,
    val phishingDomains: List<RemotePhishingDto> = emptyList(),
    val malwareHashes: List<RemoteMalwareHashDto> = emptyList()
)

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

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
            val isOfflineOnly = prefs.isOfflineOnlyMode.first()
            val feedUrl = prefs.threatFeedUrl.first().ifBlank { DEFAULT_GITHUB_FEED_URL }

            // 1. Hybrid Online/Offline mode: if online allowed, try GitHub JSON first, then fallback to bundled offline JSON
            val rawJson = if (isOfflineOnly) {
                readBundledAssetJson()
            } else {
                fetchRemoteJson(feedUrl) ?: readBundledAssetJson()
            }

            if (!rawJson.isNullOrBlank()) {
                val feed = jsonParser.decodeFromString<RemoteThreatFeedDto>(rawJson)

                val domains = feed.phishingDomains.mapNotNull { dto ->
                    val clean = dto.domain.lowercase().trim()
                    if (clean.isNotEmpty()) {
                        PhishingDomainEntity(
                            domain = clean,
                            category = dto.category,
                            riskScore = dto.riskScore.coerceIn(1, 100),
                            addedAt = now
                        )
                    } else null
                }

                val hashes = feed.malwareHashes.mapNotNull { dto ->
                    val cleanHash = dto.sha256.lowercase().trim()
                    if (cleanHash.length == 64) {
                        VirusHashEntity(
                            sha256 = cleanHash,
                            malwareName = dto.malwareName,
                            severity = dto.severity,
                            addedAt = now
                        )
                    } else null
                }

                if (domains.isNotEmpty()) phishingDao.insertAll(domains)
                if (hashes.isNotEmpty()) virusDao.insertAll(hashes)

                prefs.updateScanTimestamp(now)
                prefs.setThreatFeedVersion(feed.version)
                android.util.Log.i(
                    "ThreatUpdateWorker",
                    "Synced threat feed v${feed.version}: ${domains.size} domains, ${hashes.size} hashes"
                )
            }

            Result.success()
        }.getOrElse { error ->
            android.util.Log.e("ThreatUpdateWorker", "Threat update failed: ${error.message}", error)
            Result.retry()
        }
    }

    private fun fetchRemoteJson(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "SentinelShield-Android/1.4")
            }
            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                    it.readText()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("ThreatUpdateWorker", "Remote GitHub fetch failed (${e.message}), using local fallback")
            null
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private fun readBundledAssetJson(): String? {
        return try {
            applicationContext.assets.open("sentinel-threats.json").bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val WORK_NAME = "SentinelThreatUpdateWorker"
        // Default open GitHub raw JSON URL from this repository
        const val DEFAULT_GITHUB_FEED_URL =
            "https://raw.githubusercontent.com/cserkish/SentinelShield/main/sentinel-threats.json"

        fun schedulePeriodic(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<ThreatUpdateWorker>(
                    12, TimeUnit.HOURS
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
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
