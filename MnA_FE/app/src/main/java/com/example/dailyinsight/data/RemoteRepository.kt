package com.example.dailyinsight.data
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import com.example.dailyinsight.data.network.ApiService

class RemoteRepository(
    private val api: ApiService
) : Repository {

    override suspend fun getTodayRecommendations(): List<RecommendationDto> =
        api.getTodayRecommendations().data ?: emptyList()

    override suspend fun getStockRecommendations(): Map<String, List<RecommendationDto>> =
        api.getStockRecommendations().data ?: emptyMap()

    override suspend fun getStockReport(ticker: String): StockDetailDto {
        return api.getStockReport(ticker)
    }

    override suspend fun getStockOverview(ticker: String): StockOverviewDto {
        return api.getStockOverview(ticker)
    }
}