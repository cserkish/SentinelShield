package uz.sentinel.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.sentinel.data.repository.*
import uz.sentinel.domain.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHashRepository(impl: HashRepositoryImpl): HashRepository

    @Binds
    @Singleton
    abstract fun bindPermissionAuditRepository(impl: PermissionAuditRepositoryImpl): PermissionAuditRepository

    @Binds
    @Singleton
    abstract fun bindJunkScannerRepository(impl: JunkScannerRepositoryImpl): JunkScannerRepository

    @Binds
    @Singleton
    abstract fun bindPhishingRepository(impl: PhishingRepositoryImpl): PhishingRepository

    @Binds
    @Singleton
    abstract fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository
}
