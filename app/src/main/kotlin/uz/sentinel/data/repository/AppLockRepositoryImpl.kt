package uz.sentinel.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.entity.LockedAppEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.repository.AppLockRepository
import uz.sentinel.presentation.applock.LockMethod
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    private val dao: LockedAppDao,
    private val preferences: SentinelPreferences
) : AppLockRepository {

    // Store temporarily unlocked packages with timestamp
    private val temporarilyUnlocked = ConcurrentHashMap<String, Long>()

    override suspend fun isAppLocked(packageName: String): Boolean {
        val unlockedTime = temporarilyUnlocked[packageName]
        if (unlockedTime != null && System.currentTimeMillis() - unlockedTime < 5 * 60 * 1000L) {
            return false
        }

        val app = dao.getApp(packageName)
        return app?.isLocked == true
    }

    override fun getLockedAppsFlow(): Flow<List<LockedAppEntity>> = dao.getLockedAppsFlow()

    override suspend fun lockApp(packageName: String, appName: String) {
        temporarilyUnlocked.remove(packageName)
        dao.lockApp(LockedAppEntity(packageName = packageName, appName = appName, isLocked = true))
    }

    override suspend fun unlockApp(packageName: String) {
        temporarilyUnlocked.remove(packageName)
        dao.setLockStatus(packageName, false)
    }

    override suspend fun getLockMethod(packageName: String): LockMethod {
        val app = dao.getApp(packageName)
        return when (app?.lockType) {
            "PATTERN" -> LockMethod.PATTERN
            "BIOMETRIC" -> LockMethod.BIOMETRIC
            else -> LockMethod.PIN
        }
    }

    override suspend fun verifyPin(packageName: String, enteredPin: String): Boolean {
        val savedValue = preferences.appLockPinHash.first()
        val enteredHash = SentinelPreferences.hashPin(enteredPin)

        if (savedValue.isNullOrEmpty()) {
            val defaultHash = SentinelPreferences.hashPin("000000")
            return MessageDigest.isEqual(
                enteredHash.toByteArray(Charsets.UTF_8),
                defaultHash.toByteArray(Charsets.UTF_8)
            )
        }

        // Backward compatibility: if an older build saved a raw 6-digit PIN in DataStore,
        // verify it safely and migrate it to a salted SHA-256 hash on success.
        if (!SentinelPreferences.isSha256Hash(savedValue)) {
            val matchesPlain = MessageDigest.isEqual(
                enteredPin.trim().toByteArray(Charsets.UTF_8),
                savedValue.trim().toByteArray(Charsets.UTF_8)
            )
            if (matchesPlain) {
                preferences.savePinHash(enteredHash)
            }
            return matchesPlain
        }

        return MessageDigest.isEqual(
            enteredHash.toByteArray(Charsets.UTF_8),
            savedValue.lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    override suspend fun verifyPattern(packageName: String, patternDots: List<Int>): Boolean {
        if (patternDots.size < 4) return false
        val savedPatternHash = preferences.appLockPatternHash.first()
        if (savedPatternHash.isNullOrEmpty()) {
            // If no pattern was previously saved, store the first valid pattern and allow unlock
            preferences.savePattern(patternDots)
            return true
        }
        val enteredPatternHash = SentinelPreferences.hashPattern(patternDots)
        return MessageDigest.isEqual(
            enteredPatternHash.toByteArray(Charsets.UTF_8),
            savedPatternHash.toByteArray(Charsets.UTF_8)
        )
    }

    override suspend fun grantTemporaryAccess(packageName: String) {
        temporarilyUnlocked[packageName] = System.currentTimeMillis()
    }
}
