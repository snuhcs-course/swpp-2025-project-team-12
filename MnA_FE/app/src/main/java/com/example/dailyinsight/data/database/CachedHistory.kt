package com.example.dailyinsight.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dailyinsight.data.dto.StockIndexHistoryItem

@Entity(tableName = "stock_history_cache")
@TypeConverters(StockHistoryConverter::class) // 1번 파일의 변환기 등록
data class CachedHistory(
    @PrimaryKey
    val indexType: String, // "KOSPI" 또는 "KOSDAQ"

    val data: List<StockIndexHistoryItem>, // 1년치 차트 데이터 리스트
    val yearHigh: Double,                  // 52주 최고가
    val yearLow: Double,                   // 52주 최저가
    val lastFetched: Long                  // 마지막으로 API에서 가져온 시간
)