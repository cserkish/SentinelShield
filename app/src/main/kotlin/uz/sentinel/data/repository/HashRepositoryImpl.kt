package uz.sentinel.data.repository

import uz.sentinel.data.local.db.VirusHashDao
import uz.sentinel.data.local.entity.VirusHashEntity
import uz.sentinel.domain.repository.HashRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HashRepositoryImpl @Inject constructor(
    private val dao: VirusHashDao
) : HashRepository {

    override suspend fun findMalware(sha256: String): String? {
        return dao.findBySha256(sha256)?.malwareName
    }

    override suspend fun insertHash(sha256: String, malwareName: String) {
        dao.insert(
            VirusHashEntity(
                sha256 = sha256,
                malwareName = malwareName,
                severity = 3
            )
        )
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun clearAll() = dao.deleteAll()
}
