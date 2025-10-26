package com.example.dailyinsight.data.network

import com.example.dailyinsight.data.dto.*
import retrofit2.Call
import retrofit2.http.*
/**
 * Unified API Service for all endpoints
 * Base URL: http://10.0.2.2:8000/ (no /api suffix)
 */
interface ApiService {

    // ============ Health Check ============
    @GET("health")
    suspend fun health(): ApiResponse<HealthResponse>

    // ============ Recommendations ============
    @GET("api/recommendations/today")
    suspend fun getTodayRecommendations(): ApiResponse<List<RecommendationDto>>

    @GET("api/recommendations/history")
    suspend fun getHistoryRecommendations(): ApiResponse<Map<String, List<RecommendationDto>>>

    @GET("api/recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("userId") userId: String
    ): ApiResponse<List<RecommendationDto>>

    // ============ Stock Details ============
    @GET("api/stocks/{ticker}")
    suspend fun getStockDetail(
        @Path("ticker") ticker: String
    ): ApiResponse<StockDetailDto>

    // ============ Market Index ============
    @GET("marketindex/stockindex/latest")
    suspend fun getStockIndex(): StockIndexLatestResponse

    @GET("marketindex/stockindex/history/{index_type}/")
    suspend fun getHistoricalData(
        @Path("index_type") indexType: String,
        @Query("days") days: Int
    ): StockIndexHistoryResponse

    // ============ Authentication ============
    @POST("user/login")
    fun logIn(
        @Body request: LogInRequest
    ): Call<LogInResponse>

    @POST("user/signup")
    fun signUp(
        @Body request: SignUpRequest
    ): Call<SignUpResponse>

    @POST("user/style/")
    fun setStyle(
        @Body request: SetStyleRequest
    ): Call<SetStyleResponse>
}