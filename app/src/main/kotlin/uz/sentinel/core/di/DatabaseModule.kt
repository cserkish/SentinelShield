package uz.sentinel.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()
                    // Seed initial known phishing domains
                    db.execSQL("INSERT OR REPLACE INTO phishing_domains (domain, reason, addedAt) VALUES ('click-uz.info', 'Soxta Click fishing sayti', $now)")
                    db.execSQL("INSERT OR REPLACE INTO phishing_domains (domain, reason, addedAt) VALUES ('payme-uz.site', 'Soxta Payme to''lov sahifasi', $now)")
                    db.execSQL("INSERT OR REPLACE INTO phishing_domains (domain, reason, addedAt) VALUES ('uzcard-bonus.top', 'Soxta bank plastik kartasi yutug''i', $now)")
                    db.execSQL("INSERT OR REPLACE INTO phishing_domains (domain, reason, addedAt) VALUES ('humo-online.vip', 'Soxta Humo karta to''lovi', $now)")
                    db.execSQL("INSERT OR REPLACE INTO phishing_domains (domain, reason, addedAt) VALUES ('pul-yordam.me', 'Soxta ijtimoiy yordam firibgarligi', $now)")

                    // Seed sample malware signatures
                    db.execSQL("INSERT OR REPLACE INTO virus_hashes (sha256, malwareName, severity, addedAt) VALUES ('e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'Trojan.AndroidOS.FakeBank', 95, $now)")
                    db.execSQL("INSERT OR REPLACE INTO virus_hashes (sha256, malwareName, severity, addedAt) VALUES ('851364c9472e38c7f212260ff0df56b26dcf5a2283cb080de3fb845ffad92a10', 'Spyware.Android.SmsStealer', 90, $now)")
                }
            })
            .fallbackToDestructiveMigration()
            .build()
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
