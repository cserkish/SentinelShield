package uz.sentinel.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import uz.sentinel.data.local.entity.*

@Database(
    entities = [
        VirusHashEntity::class,
        PhishingDomainEntity::class,
        LockedAppEntity::class,
        IntruderPhotoEntity::class,
        ScanResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun virusHashDao(): VirusHashDao
    abstract fun phishingDomainDao(): PhishingDomainDao
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun intruderPhotoDao(): IntruderPhotoDao
    abstract fun scanResultDao(): ScanResultDao
}
