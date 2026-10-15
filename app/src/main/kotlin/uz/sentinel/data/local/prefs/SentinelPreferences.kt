package uz.sentinel.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import uz.sentinel.service.ThreatUpdateWorker
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sentinel_prefs")

@Singleton
class SentinelPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PIN_SALT = "SentinelShield#AppLock#Salt_2026"
        private val SHA256_HEX_REGEX = Regex("^[0-9a-fA-F]{64}$")

        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_BRAND_STABILITY_CONFIGURED = booleanPreferencesKey("brand_stability_configured")
        val KEY_APPLOCK_ENABLED = booleanPreferencesKey("applock_enabled")
        val KEY_APPLOCK_PIN_HASH = stringPreferencesKey("applock_pin_hash")
        val KEY_APPLOCK_PATTERN = stringPreferencesKey("applock_pattern")
        val KEY_APPLOCK_DEFAULT_METHOD = stringPreferencesKey("applock_default_method")
        val KEY_NETGUARD_ENABLED = booleanPreferencesKey("netguard_enabled")
        val KEY_LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val KEY_LAST_CLEAN_BYTES = longPreferencesKey("last_clean_bytes")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_THREAT_FEED_URL = stringPreferencesKey("threat_feed_url")
        val KEY_THREAT_FEED_VERSION = stringPreferencesKey("threat_feed_version")
        val KEY_OFFLINE_ONLY_MODE = booleanPreferencesKey("offline_only_mode")
        val KEY_GAME_MODE_ACTIVE = booleanPreferencesKey("game_mode_active")
        val KEY_LAST_BOOSTED_RAM_MB = longPreferencesKey("last_boosted_ram_mb")

        fun hashPin(rawPin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest((PIN_SALT + rawPin.trim()).toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun hashPattern(patternDots: List<Int>): String {
            val serialized = patternDots.joinToString("-")
            return hashPin("PATTERN:$serialized")
        }

        fun isSha256Hash(value: String): Boolean = SHA256_HEX_REGEX.matches(value)
    }

    private val safePrefsFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            android.util.Log.e("SentinelPreferences", "DataStore error, emitting empty preferences", exception)
            emit(emptyPreferences())
        }

    val isOnboardingCompleted: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isBrandStabilityConfigured: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_BRAND_STABILITY_CONFIGURED] ?: false
    }

    val isAppLockEnabled: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_APPLOCK_ENABLED] ?: false
    }

    val appLockPinHash: Flow<String?> = safePrefsFlow.map { prefs ->
        prefs[KEY_APPLOCK_PIN_HASH]
    }

    val appLockPatternHash: Flow<String?> = safePrefsFlow.map { prefs ->
        prefs[KEY_APPLOCK_PATTERN]
    }

    val appLockDefaultMethod: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_APPLOCK_DEFAULT_METHOD] ?: "PIN"
    }

    val isNetGuardEnabled: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_NETGUARD_ENABLED] ?: false
    }

    val lastScanTimestamp: Flow<Long> = safePrefsFlow.map { prefs ->
        prefs[KEY_LAST_SCAN_TIMESTAMP] ?: 0L
    }

    val lastCleanBytes: Flow<Long> = safePrefsFlow.map { prefs ->
        prefs[KEY_LAST_CLEAN_BYTES] ?: 0L
    }

    val appLanguage: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "uz"
    }

    val threatFeedUrl: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_THREAT_FEED_URL] ?: ThreatUpdateWorker.DEFAULT_GITHUB_FEED_URL
    }

    val threatFeedVersion: Flow<String> = safePrefsFlow.map { prefs ->
        prefs[KEY_THREAT_FEED_VERSION] ?: "2026.10"
    }

    val isOfflineOnlyMode: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_OFFLINE_ONLY_MODE] ?: false
    }

    val isGameModeActive: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_GAME_MODE_ACTIVE] ?: false
    }

    val lastBoostedRamMb: Flow<Long> = safePrefsFlow.map { prefs ->
        prefs[KEY_LAST_BOOSTED_RAM_MB] ?: 0L
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setBrandStabilityConfigured(configured: Boolean) {
        context.dataStore.edit { it[KEY_BRAND_STABILITY_CONFIGURED] = configured }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_APPLOCK_ENABLED] = enabled }
    }

    suspend fun savePinHash(pinOrHash: String) {
        val normalized = if (isSha256Hash(pinOrHash)) pinOrHash.lowercase() else hashPin(pinOrHash)
        context.dataStore.edit { it[KEY_APPLOCK_PIN_HASH] = normalized }
    }

    suspend fun savePattern(patternDots: List<Int>) {
        context.dataStore.edit { it[KEY_APPLOCK_PATTERN] = hashPattern(patternDots) }
    }

    suspend fun setAppLockDefaultMethod(method: String) {
        context.dataStore.edit { it[KEY_APPLOCK_DEFAULT_METHOD] = method }
    }

    suspend fun setNetGuardEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NETGUARD_ENABLED] = enabled }
    }

    suspend fun updateScanTimestamp(time: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[KEY_LAST_SCAN_TIMESTAMP] = time }
    }

    suspend fun recordCleanedBytes(bytes: Long) {
        context.dataStore.edit { it[KEY_LAST_CLEAN_BYTES] = bytes }
    }

    suspend fun setAppLanguage(langCode: String) {
        context.dataStore.edit { it[KEY_APP_LANGUAGE] = langCode }
        context.getSharedPreferences("sentinel_locale_sync", Context.MODE_PRIVATE)
            .edit()
            .putString("lang", langCode)
            .apply()
    }

    suspend fun setThreatFeedUrl(url: String) {
        context.dataStore.edit { it[KEY_THREAT_FEED_URL] = url.trim() }
    }

    suspend fun setThreatFeedVersion(version: String) {
        context.dataStore.edit { it[KEY_THREAT_FEED_VERSION] = version }
    }

    suspend fun setOfflineOnlyMode(offlineOnly: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE_ONLY_MODE] = offlineOnly }
    }

    suspend fun setGameModeActive(active: Boolean) {
        context.dataStore.edit { it[KEY_GAME_MODE_ACTIVE] = active }
    }

    suspend fun recordBoostedRamMb(freedMb: Long) {
        context.dataStore.edit { it[KEY_LAST_BOOSTED_RAM_MB] = freedMb }
    }
}
