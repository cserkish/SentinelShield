package uz.sentinel.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.sentinel.data.local.db.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SentinelDatabase {
        return Room.databaseBuilder(
            context,
            SentinelDatabase::class.java,
            "sentinel_shield.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideVirusHashDao(db: SentinelDatabase): VirusHashDao = db.virusHashDao()

    @Provides
    fun providePhishingDomainDao(db: SentinelDatabase): PhishingDomainDao = db.phishingDomainDao()

    @Provides
    fun provideLockedAppDao(db: SentinelDatabase): LockedAppDao = db.lockedAppDao()

    @Provides
    fun provideIntruderPhotoDao(db: SentinelDatabase): IntruderPhotoDao = db.intruderPhotoDao()

    @Provides
    fun provideScanResultDao(db: SentinelDatabase): ScanResultDao = db.scanResultDao()
}
