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

    private val targetBrands = listOf(
        "uzcard", "click", "payme", "humo", "hamkorbank",
        "tbcbank", "kapitalbank", "anorbank", "ipotekabank", "agrobank"
    )

    private val officialBankDomains = setOf(
        "click.uz", "payme.uz", "uzcard.uz", "humo.uz", "humocard.uz",
        "hamkorbank.uz", "tbcbank.uz", "kapitalbank.uz", "anorbank.uz",
        "ipotekabank.uz", "agrobank.uz", "nbu.uz", "sqb.uz", "xalqbanki.uz",
        "asaka.uz", "asakabank.uz", "alofabank.uz", "infinbank.uz", "trustbank.uz"
    )

    private val globalWhitelistedDomains = setOf(
        "clickup.com", "clickhouse.com", "doubleclick.net", "clickfunnels.com",
        "clickbank.com", "humblebundle.com", "humor.com"
    )

    override suspend fun isPhishingDomain(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")

        if (cleanDomain.isBlank() || !cleanDomain.contains(".")) return false

        // 1. Direct match in local DB
        if (dao.findDomain(cleanDomain) != null) return true
        if (dao.matchSubdomain(cleanDomain) != null) return true

        // 2. Whitelist checks
        if (officialBankDomains.any { cleanDomain == it || cleanDomain.endsWith(".$it") }) {
            return false
        }
        if (globalWhitelistedDomains.any { cleanDomain == it || cleanDomain.endsWith(".$it") }) {
            return false
        }

        // Extract domain labels (e.g., "auth.click-pay.xyz" -> ["auth", "click-pay", "xyz"])
        val labels = cleanDomain.split(".").filter { it.isNotBlank() }
        if (labels.size < 2) return false
        val sld = if (labels.size >= 2) labels[labels.size - 2] else labels[0]
        val normalizedSld = sld.replace("1", "l").replace("0", "o").replace("rn", "m").replace("vv", "w")

        // 3. Typosquatting & Fake Bank Detection
        for (brand in targetBrands) {
            // Check if any label is exactly equal to the brand or contains hyphenated brand (e.g. click-login, payme-uz)
            for (label in labels) {
                if (label == brand || label.startsWith("$brand-") || label.endsWith("-$brand") || label.contains("-$brand-")) {
                    return true
                }
            }

            // Levenshtein distance check on SLD (catches cl1ck, payrne, uzkard, humo0)
            if (sld.length in (brand.length - 1)..(brand.length + 1)) {
                // If homoglyph normalized SLD equals brand (e.g. cl1ck -> click, payrne -> payme)
                if (normalizedSld == brand && sld != brand) {
                    return true
                }

                // If edit distance is 1 (e.g. paym.xyz, uzkard.com, clickk.site)
                val dist = levenshteinDistance(normalizedSld, brand)
                if (dist == 1) {
                    return true
                }
            }
        }

        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
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
