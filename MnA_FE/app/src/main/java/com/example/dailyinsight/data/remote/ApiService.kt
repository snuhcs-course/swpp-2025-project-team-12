package com.example.dailyinsight.data.remote

import com.example.dailyinsight.data.dto.*
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
interface ApiService {

    // 헬스체크 (루트 경로) → 절대 경로로 우회
    @GET("/health")
    suspend fun health(): ApiResponse<HealthResponse>

    // 오늘의 추천 (상대 경로; /api 제거)
    @GET("recommendations/today")
    suspend fun getTodayRecommendations(): ApiResponse<List<RecommendationDto>>

    // 지난 추천(히스토리)
    @GET("recommendations/history")
    suspend fun getHistoryRecommendations(): ApiResponse<Map<String, List<RecommendationDto>>>

    // 개인화 추천
    @GET("recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("userId") userId: String
    ): ApiResponse<List<RecommendationDto>>

    // 종목 상세 (상대 경로; /api 제거)
    @GET("stocks/{ticker}")
    suspend fun getStockDetail(
        @Path("ticker") ticker: String
    ): ApiResponse<StockDetailDto>
}

