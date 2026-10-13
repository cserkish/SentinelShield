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

    // Himoya qilinadigan brendlar (soxtalashtirish, typosquatting va pirat saytlarni ushlash uchun)
    private val targetBrands = listOf(
        "payme", "paycom", "click", "uzcard", "humo", "hamkorbank",
        "tbcbank", "kapitalbank", "anorbank", "ipotekabank", "agrobank",
        "sqb", "xalqbanki", "asakabank", "alofabank", "infinbank", "trustbank"
    )

    // Rasmiy, qonuniy domenlar va ularning barcha subdomenlari (ULTRA WHITELIST)
    private val officialWhitelistedDomains = setOf(
        // Payme / Paycom ekosistemasi
        "payme.uz", "paycom.uz", "payme.cloud", "paycom.network", "payme.info",

        // Click ekosistemasi
        "click.uz", "click.cloud",

        // Uzcard & Humo
        "uzcard.uz", "plum.uz", "humo.uz", "humocard.uz", "smartbank.uz",

        // Markaziy Bank va Davlat xizmatlari
        "cbu.uz", "my.gov.uz", "gov.uz", "egov.uz", "id.gov.uz", "oneid.uz",
        "soliq.uz", "lex.uz", "mf.uz", "stat.uz",

        // O'zbekistonning barcha rasmiy banklari
        "nbu.uz", "tbcbank.uz", "kapitalbank.uz", "hamkorbank.uz", "anorbank.uz",
        "ipotekabank.uz", "agrobank.uz", "sqb.uz", "xalqbanki.uz", "asaka.uz",
        "asakabank.uz", "alofabank.uz", "infinbank.uz", "trustbank.uz", "mkbank.uz",
        "turonbank.uz", "davrbank.uz", "tengebank.uz", "ziraatbank.uz", "octobank.uz",
        "garantbank.uz", "hayotbank.uz", "apexbank.uz", "poytaxtbank.uz", "yangibank.uz",
        "madadinvestbank.uz", "asiaalliancebank.uz", "ravnaqbank.uz", "aab.uz", "saderatbank.uz",

        // Ishonchli global infratuzilma va ommabop platformalar (Samsung, Telegram, Meta, Google)
        "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com", "firebaseio.com",
        "apple.com", "icloud.com", "cloudflare.com", "one.one.one.one",
        "telegram.org", "t.me", "yandex.uz", "yandex.ru", "ya.ru",
        "samsung.com", "secb2b.com", "samsungqbe.com",
        "instagram.com", "whatsapp.com", "facebook.com", "fbcdn.net",
        "youtube.com", "googlevideo.com", "tiktok.com", "byteoversea.com",
        "twitter.com", "x.com", "github.com", "microsoft.com",
        "clickup.com", "clickhouse.com", "doubleclick.net", "clickfunnels.com",
        "clickbank.com", "humblebundle.com", "humor.com"
    )

    // Yashirin kripto-maynerlar va mining pullar (Monero, XMRig, CoinHive tarmoqlari)
    private val knownMiningDomains = setOf(
        "supportxmr.com", "minexmr.com", "nanopool.org", "ethermine.org",
        "f2pool.com", "antpool.com", "coinhive.com", "crypto-loot.com",
        "xmrig.com", "monerocean.stream", "xmrpool.eu", "hashvault.pro"
    )

    private fun isOfficialTrustedDomain(domain: String): Boolean {
        return officialWhitelistedDomains.any { official ->
            domain == official || domain.endsWith(".$official")
        }
    }

    private fun isKnownMiningDomain(domain: String): Boolean {
        return knownMiningDomains.any { miner ->
            domain == miner || domain.endsWith(".$miner")
        }
    }

    override suspend fun isPhishingDomain(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .substringBefore("?")
            .substringBefore("#")
            .removeSuffix(".")

        if (cleanDomain.isBlank() || !cleanDomain.contains(".")) return false

        // 1. QAT'IY OQ RO'YXAT (Rasmiy Fintex, banklar, platformalar va subdomenlari doimo XAVFSIZ)
        if (isOfficialTrustedDomain(cleanDomain)) {
            return false // Original rasmiy servis! Hech qanday soxta ogohlantirish berilmaydi.
        }

        // 2. Kripto-maynerlar va mining pullar
        if (isKnownMiningDomain(cleanDomain)) {
            return true
        }

        // 3. Lokal ma'lumotlar bazasidagi qora ro'yxat (Blocklist DB)
        if (dao.findDomain(cleanDomain) != null) return true
        if (dao.matchSubdomain(cleanDomain) != null) return true

        // 3. PIRAT / SOXTA (TYPOSQUATTING & FAKE BANK / PIRATE PAYME) ANIQLASH
        val labels = cleanDomain.split(".").filter { it.isNotBlank() }
        if (labels.size < 2) return false
        val sld = if (labels.size >= 2) labels[labels.size - 2] else labels[0]
        val normalizedSld = sld.replace("1", "l").replace("0", "o").replace("rn", "m").replace("vv", "w")

        for (brand in targetBrands) {
            // Rasmiy oq ro'yxatdan o'tmagan bo'lsa-yu, lekin brend nomidan foydalansa (masalan: payme-uz.com, payme-bonus.ru, auth-click.top)
            for (label in labels) {
                if (label == brand || label.startsWith("$brand-") || label.endsWith("-$brand") || label.contains("-$brand-")) {
                    return true // Pirat / Soxta brend aniqlandi!
                }
            }

            // Levenshtein va homoglyph tahlili (masalan: cl1ck, payrne, uzkard)
            if (sld.length in (brand.length - 1)..(brand.length + 1)) {
                if (normalizedSld == brand && sld != brand) {
                    return true
                }

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
