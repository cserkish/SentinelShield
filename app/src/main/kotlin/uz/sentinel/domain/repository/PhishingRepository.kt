package uz.sentinel.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.entity.PhishingDomainEntity

interface PhishingRepository {
    suspend fun isPhishingDomain(domain: String): Boolean
    fun getAllBlockedDomains(): Flow<List<PhishingDomainEntity>>
    suspend fun addPhishingDomain(domain: String, category: String)
}
