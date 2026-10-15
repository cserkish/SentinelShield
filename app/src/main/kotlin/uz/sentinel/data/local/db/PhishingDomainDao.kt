package uz.sentinel.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.PhishingDomainEntity

@Dao
interface PhishingDomainDao {
    @Query("SELECT * FROM phishing_domains WHERE domain = :domain LIMIT 1")
    suspend fun findDomain(domain: String): PhishingDomainEntity?

    @Query("SELECT * FROM phishing_domains WHERE :domain LIKE '%.' || domain LIMIT 1")
    suspend fun matchSubdomain(domain: String): PhishingDomainEntity?

    @Query("SELECT * FROM phishing_domains ORDER BY addedAt DESC LIMIT 100")
    fun getAllDomainsFlow(): Flow<List<PhishingDomainEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(entity: PhishingDomainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PhishingDomainEntity>)

    @Query("DELETE FROM phishing_domains WHERE domain = :domain")
    suspend fun deleteDomain(domain: String)
}
