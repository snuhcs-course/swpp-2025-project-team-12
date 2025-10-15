package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.IndexDto
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.remote.ApiService

class DefaultRepository(
    private val api: ApiService
) : Repository {

    override suspend fun getTodayRecommendations(): List<RecommendationDto> =
        api.getTodayRecommendations().data ?: emptyList()

    override suspend fun getHistoryRecommendations(): Map<String, List<RecommendationDto>> =
        api.getHistoryRecommendations().data ?: emptyMap()

    override suspend fun getMainIndices(): List<IndexDto> =
        api.getMainIndices().data ?: emptyList()
}