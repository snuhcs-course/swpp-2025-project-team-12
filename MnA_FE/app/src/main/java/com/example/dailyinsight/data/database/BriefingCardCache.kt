package com.example.dailyinsight.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dailyinsight.data.dto.RecommendationDto
@Entity(tableName = "briefing_cards")
data class BriefingCardCache(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val price: Long,
    val change: Long,
    val changeRate: Double,
    val headline: String?,   // overview의 summary
    val label: String?,      // overview의 label (상승/하락/중립)
    val confidence: Double?, // overview의 confidence
    val fetchedAt: Long,      // 저장된 시간 (정렬용)
    val isFavorite: Boolean = false
){ //  UI 객체로 변환하는 함수
    fun toDto(): RecommendationDto {
        return RecommendationDto(
            ticker = ticker,
            name = name,
            price = price,
            change = change,
            changeRate = changeRate,
            headline = headline,
            isFavorite = isFavorite
        )
    }
}