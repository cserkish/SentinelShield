package uz.sentinel.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.ScanResultEntity

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY dangerScore DESC")
    fun getAllResultsFlow(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE packageName = :packageName LIMIT 1")
    suspend fun getResult(packageName: String): ScanResultEntity?

    @Query("""
        SELECT COUNT(*) FROM scan_results 
        WHERE dangerScore >= 61 
          AND isUserTrusted = 0 
          AND packageName NOT LIKE 'com.samsung.%' 
          AND packageName NOT LIKE 'com.sec.%' 
          AND packageName NOT LIKE 'com.google.android.%' 
          AND packageName NOT LIKE 'com.android.%' 
          AND packageName NOT LIKE 'com.miui.%' 
          AND packageName NOT LIKE 'com.xiaomi.%'
          AND packageName NOT LIKE 'com.huawei.%'
    """)
    fun getHighRiskCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ScanResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<ScanResultEntity>)

    @Query("UPDATE scan_results SET isUserTrusted = :isTrusted WHERE packageName = :packageName")
    suspend fun setTrustedStatus(packageName: String, isTrusted: Boolean)

    @Query("DELETE FROM scan_results WHERE packageName = :packageName")
    suspend fun deleteResult(packageName: String)

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()
}
