package com.example.dailyinsight.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "stock_overview_cache")
@TypeConverters(OverviewConverter::class)
data class OverviewCache(
    @PrimaryKey val symbol: String,
    val overviewJson: OverviewPayload, // 표 전체 섹션을 한 번에 저장 (JSON 모델)
    val lastFetched: Long
)

data class OverviewPayload(
    val scale: Map<String, Map<String, String>>?,   // "규모" 섹션
    val value: Map<String, Map<String, String>>?,   // "가치"
    val profit: Map<String, Map<String, String>>?,  // "수익성"
    val dividend: Map<String, Map<String, String>>? // "배당금"
)