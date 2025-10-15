package com.example.dailyinsight.data.api

import com.example.dailyinsight.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MnaApiService {
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("recommendations/general")
    suspend fun getRecommendations(
        @Query("risk") risk: String,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<Recommendation>>
}