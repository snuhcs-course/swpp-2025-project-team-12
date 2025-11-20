package com.example.dailyinsight.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_detail_cache")
data class StockDetailCache(
    @PrimaryKey
    val ticker: String,
    val json: String,      // StockDetailDto 전체를 JSON 문자열로 저장
    val fetchedAt: Long    // 저장 시각
)