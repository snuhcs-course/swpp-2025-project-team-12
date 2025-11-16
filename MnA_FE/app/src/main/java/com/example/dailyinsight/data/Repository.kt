package com.example.dailyinsight.data
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto

interface Repository {
    suspend fun getTodayRecommendations(): List<RecommendationDto>
    suspend fun getStockRecommendations(): Map<String, List<RecommendationDto>>
    suspend fun getStockReport(ticker: String): StockDetailDto
    suspend fun getStockOverview(ticker: String): StockOverviewDto
}