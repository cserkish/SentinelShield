package uz.sentinel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phishing_domains")
data class PhishingDomainEntity(
    @PrimaryKey
    val domain: String,
    val category: String, // e.g. "FakeBank", "CredentialHarvester", "LotteryScam"
    val riskScore: Int = 100,
    val addedAt: Long = System.currentTimeMillis()
)
