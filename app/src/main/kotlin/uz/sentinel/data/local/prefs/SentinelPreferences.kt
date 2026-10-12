package uz.sentinel.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sentinel_prefs")

@Singleton
class SentinelPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_BRAND_STABILITY_CONFIGURED = booleanPreferencesKey("brand_stability_configured")
        val KEY_APPLOCK_ENABLED = booleanPreferencesKey("applock_enabled")
        val KEY_APPLOCK_PIN_HASH = stringPreferencesKey("applock_pin_hash")
        val KEY_APPLOCK_PATTERN = stringPreferencesKey("applock_pattern")
        val KEY_NETGUARD_ENABLED = booleanPreferencesKey("netguard_enabled")
        val KEY_LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val KEY_LAST_CLEAN_BYTES = longPreferencesKey("last_clean_bytes")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isBrandStabilityConfigured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BRAND_STABILITY_CONFIGURED] ?: false
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APPLOCK_ENABLED] ?: false
    }

    val appLockPinHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_APPLOCK_PIN_HASH]
    }

    val isNetGuardEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NETGUARD_ENABLED] ?: false
    }

    val lastScanTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SCAN_TIMESTAMP] ?: 0L
    }

    val lastCleanBytes: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_CLEAN_BYTES] ?: 0L
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

    suspend fun savePinHash(hash: String) {
        context.dataStore.edit { it[KEY_APPLOCK_PIN_HASH] = hash }
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
}
