package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto

interface Repository {
    suspend fun getTodayRecommendations(): List<RecommendationDto>
    suspend fun getStockRecommendations(): Map<String, List<RecommendationDto>>
    suspend fun getStockDetail(ticker: String): StockDetailDto
}