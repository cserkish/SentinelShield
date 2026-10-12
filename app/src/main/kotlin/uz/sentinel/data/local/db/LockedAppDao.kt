package uz.sentinel.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.LockedAppEntity

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    fun getLockedAppsFlow(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): LockedAppEntity?

    @Query("SELECT packageName FROM locked_apps WHERE isLocked = 1")
    suspend fun getLockedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun lockApp(app: LockedAppEntity)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun setLockStatus(packageName: String, isLocked: Boolean)

    @Delete
    suspend fun removeApp(app: LockedAppEntity)
}
