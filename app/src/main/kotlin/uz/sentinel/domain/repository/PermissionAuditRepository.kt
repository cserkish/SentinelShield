package uz.sentinel.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.sentinel.domain.model.AppPermissionInfo

interface PermissionAuditRepository {
    /** Returns ALL installed apps that hold at least one monitored dangerous permission. */
    fun observePermissionAudit(): Flow<List<AppPermissionInfo>>

    /** Returns apps that hold the given permission (e.g. android.permission.CAMERA). */
    suspend fun getAppsByPermission(permission: String): List<AppPermissionInfo>

    /** Force refresh / re-scan. */
    suspend fun refresh(): List<AppPermissionInfo>
}
