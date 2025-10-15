package com.example.dailyinsight.data.remote

import com.example.dailyinsight.data.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // 헬스체크
    @GET("/health")
    suspend fun health(): ApiResponse<HealthResponse>

    // 오늘의 추천 (범용)
    @GET("/api/recommendations/today")
    suspend fun getTodayRecommendations(): ApiResponse<List<RecommendationDto>>

    // 지난 추천(히스토리) — API.md 에 맞춰 엔드포인트/응답 감싸기
    @GET("/api/recommendations/history")
    suspend fun getHistoryRecommendations(): ApiResponse<Map<String, List<RecommendationDto>>>

    // 개인화 추천 (로그인 유저일 때)
    @GET("/api/recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("userId") userId: String
    ): ApiResponse<List<RecommendationDto>>

    // 주요 지수
    @GET("/api/indices/main")
    suspend fun getMainIndices(): ApiResponse<List<IndexDto>>
}