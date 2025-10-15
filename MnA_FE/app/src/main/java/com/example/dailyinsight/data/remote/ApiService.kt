package com.example.dailyinsight.data.remote

import com.example.dailyinsight.data.dto.IndexDto
import com.example.dailyinsight.data.dto.RecommendationDto
import retrofit2.http.GET

interface ApiService {
    @GET("api/recommendations/today")
    suspend fun getTodayRecommendations(): List<RecommendationDto>

    @GET("api/recommendations/history")
    suspend fun getHistoryRecommendations(): Map<String, List<RecommendationDto>>
    // 예: {"오늘":[...], "어제":[...], "2025-10-02":[...]}

    @GET("api/indices/main")
    suspend fun getMainIndices(): List<IndexDto> // KOSPI, KOSDAQ
}