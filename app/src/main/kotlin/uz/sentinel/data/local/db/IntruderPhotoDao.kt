package uz.sentinel.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.IntruderPhotoEntity

@Dao
interface IntruderPhotoDao {
    @Query("SELECT * FROM intruder_photos ORDER BY timestamp DESC")
    fun getAllPhotosFlow(): Flow<List<IntruderPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: IntruderPhotoEntity): Long

    @Query("DELETE FROM intruder_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("DELETE FROM intruder_photos")
    suspend fun clearAll()
}
