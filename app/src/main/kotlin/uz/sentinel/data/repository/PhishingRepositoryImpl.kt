package uz.sentinel.data.repository

import kotlinx.coroutines.flow.Flow
import uz.sentinel.data.local.db.PhishingDomainDao
import uz.sentinel.data.local.entity.PhishingDomainEntity
import uz.sentinel.domain.repository.PhishingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishingRepositoryImpl @Inject constructor(
    private val dao: PhishingDomainDao
) : PhishingRepository {

    private val targetBrands = listOf("uzcard", "click", "payme", "humo", "hamkorbank", "tbcbank", "kapitalbank")

    override suspend fun isPhishingDomain(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim()

        // 1. Direct match in local DB
        if (dao.findDomain(cleanDomain) != null) return true
        if (dao.matchSubdomain(cleanDomain) != null) return true

        // 2. Typosquatting / Fake Bank detection
        for (brand in targetBrands) {
            if (cleanDomain.contains(brand)) {
                // If it contains the brand name, but is NOT the official domain
                val isOfficial = cleanDomain.endsWith("$brand.uz") || cleanDomain == "$brand.uz"
                if (!isOfficial) {
                    // Suspicious fake bank link (e.g. click-uz.top, payme-24.site)
                    return true
                }
            }
        }

        return false
    }

    override fun getAllBlockedDomains(): Flow<List<PhishingDomainEntity>> = dao.getAllDomainsFlow()

    override suspend fun addPhishingDomain(domain: String, category: String) {
        dao.insertDomain(
            PhishingDomainEntity(
                domain = domain.lowercase().trim(),
                category = category
            )
        )
    }
}
