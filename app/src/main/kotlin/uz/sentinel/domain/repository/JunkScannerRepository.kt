package uz.sentinel.domain.repository

import uz.sentinel.domain.model.JunkItem

interface JunkScannerRepository {
    suspend fun scanJunk(): List<JunkItem>
    suspend fun deleteJunk(items: List<JunkItem>): Long // Returns freed bytes
}
