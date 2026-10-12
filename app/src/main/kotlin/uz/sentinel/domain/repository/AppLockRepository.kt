package uz.sentinel.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.LockedAppEntity
import uz.sentinel.presentation.applock.LockMethod

interface AppLockRepository {
    suspend fun isAppLocked(packageName: String): Boolean
    fun getLockedAppsFlow(): Flow<List<LockedAppEntity>>
    suspend fun lockApp(packageName: String, appName: String)
    suspend fun unlockApp(packageName: String)
    suspend fun getLockMethod(packageName: String): LockMethod
    suspend fun verifyPin(packageName: String, enteredPin: String): Boolean
    suspend fun verifyPattern(packageName: String, patternDots: List<Int>): Boolean
    suspend fun grantTemporaryAccess(packageName: String)
}
