package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.remote.ApiService

class DefaultRepository(
    private val api: ApiService
) : Repository {

    override suspend fun getTodayRecommendations(): List<RecommendationDto> =
        api.getTodayRecommendations().data ?: emptyList()

    override suspend fun getHistoryRecommendations(): Map<String, List<RecommendationDto>> =
        api.getHistoryRecommendations().data ?: emptyMap()

    override suspend fun getStockDetail(ticker: String): StockDetailDto {
        return api.getStockDetail(ticker).data
            ?: throw NoSuchElementException("Stock detail not found for $ticker")
    }
}