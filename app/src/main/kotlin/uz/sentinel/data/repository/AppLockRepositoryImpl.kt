package uz.sentinel.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.entity.LockedAppEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.domain.repository.AppLockRepository
import uz.sentinel.presentation.applock.LockMethod
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
        // If temporarily unlocked within the last 5 minutes, return false
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

    private fun hashPin(pin: String): String {
        val salt = "SentinelShield#AppLock#Salt_2026"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override suspend fun verifyPin(packageName: String, enteredPin: String): Boolean {
        val savedHash = preferences.appLockPinHash.first()
        val defaultHash = hashPin("000000")
        val enteredHash = hashPin(enteredPin)

        return if (savedHash.isNullOrEmpty()) {
            java.security.MessageDigest.isEqual(
                enteredHash.toByteArray(Charsets.UTF_8),
                defaultHash.toByteArray(Charsets.UTF_8)
            )
        } else {
            java.security.MessageDigest.isEqual(
                enteredHash.toByteArray(Charsets.UTF_8),
                savedHash.toByteArray(Charsets.UTF_8)
            )
        }
    }

    override suspend fun verifyPattern(packageName: String, patternDots: List<Int>): Boolean {
        // Pattern verification (minimum 4 dots)
        return patternDots.size >= 4
    }

    override suspend fun grantTemporaryAccess(packageName: String) {
        temporarilyUnlocked[packageName] = System.currentTimeMillis()
    }
}
