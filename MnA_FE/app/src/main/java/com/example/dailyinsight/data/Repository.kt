package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.IndexDto
import com.example.dailyinsight.data.dto.RecommendationDto

interface Repository {
    suspend fun getTodayRecommendations(): List<RecommendationDto>
    suspend fun getHistoryRecommendations(): Map<String, List<RecommendationDto>>
    suspend fun getMainIndices(): List<IndexDto>
}