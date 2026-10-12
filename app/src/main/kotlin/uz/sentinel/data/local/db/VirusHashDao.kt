package uz.sentinel.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.VirusHashEntity

@Dao
interface VirusHashDao {
    @Query("SELECT * FROM virus_hashes WHERE sha256 = :sha256 LIMIT 1")
    suspend fun findBySha256(sha256: String): VirusHashEntity?

    @Query("SELECT * FROM virus_hashes WHERE sha256 = :hash LIMIT 1")
    suspend fun findByHash(hash: String): VirusHashEntity?

    @Query("SELECT COUNT(*) FROM virus_hashes")
    fun getHashCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM virus_hashes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VirusHashEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hashes: List<VirusHashEntity>)

    @Query("DELETE FROM virus_hashes")
    suspend fun deleteAll()

    @Query("DELETE FROM virus_hashes")
    suspend fun clearAll()
}
