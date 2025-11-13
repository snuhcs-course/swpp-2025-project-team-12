package com.example.dailyinsight.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "briefing_card_cache")
@TypeConverters(BriefingListConverter::class)
data class BriefingCardCache(
    @PrimaryKey val filterKey: String, // 예: "ALL", "SECTOR_IT", "SECTOR_의류" 등
    val items: List<BriefingCardItem>, // 리스트 카드들
    val lastFetched: Long              // 마지막 갱신 시각
)

data class BriefingCardItem(
    val symbol: String,
    val name: String,
    val summary: String,
    val price: Double?,
    val change: Double?,   // 절대값
    val changePct: Double?,// %
    val sector: String?,
    val asOf: String?      // "2025-10-24" 등 (원본 그대로)
)