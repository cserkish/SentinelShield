package uz.sentinel.domain.repository

/**
 * SHA-256 hash larni viruslar bazasida tekshirish uchun contract.
 */
interface HashRepository {

    /**
     * Berilgan [sha256] xeshi zararli dasturlar bazasida mavjudligini tekshiradi.
     * @return null — topilmadi, String — topilgan malware nomi
     */
    suspend fun findMalware(sha256: String): String?

    /**
     * Yangi sha256 → malwareName juftligini bazaga qo'shadi.
     * Rasmiy signature feed orqali chaqiriladi.
     */
    suspend fun insertHash(sha256: String, malwareName: String)

    /**
     * Bazadagi barcha xeshlar sonini qaytaradi (diagnostika uchun).
     */
    suspend fun count(): Int

    /**
     * Barcha yozuvlarni o'chiradi (bazani yangilashdan oldin).
     */
    suspend fun clearAll()
}
