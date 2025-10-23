package com.example.dailyinsight.data
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.network.ApiService

class RemoteRepository(
    private val api: ApiService
) : Repository {

    override suspend fun getTodayRecommendations(): List<RecommendationDto> =
        api.getTodayRecommendations().data ?: emptyList()

    override suspend fun getHistoryRecommendations(): Map<String, List<RecommendationDto>> =
        api.getHistoryRecommendations().data ?: emptyMap()

    override suspend fun getStockDetail(ticker: String): StockDetailDto {
        // API 호출을 통해 StockDetailDto를 가져오고,
        // 데이터가 null이면 예외를 던지도록 처리합니다.
        return api.getStockDetail(ticker).data
            ?: throw NoSuchElementException("Stock detail not found for $ticker")
    }
}