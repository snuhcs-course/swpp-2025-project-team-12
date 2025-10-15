package com.example.dailyinsight.data

import com.example.dailyinsight.data.remote.RetrofitClient

class Repository {
    private val api = RetrofitClient.api

    suspend fun today() = runCatching { api.getTodayRecommendations() }
    suspend fun history() = runCatching { api.getHistoryRecommendations() }
    suspend fun indices() = runCatching { api.getMainIndices() }
}