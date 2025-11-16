package com.example.dailyinsight.data.network

import com.example.dailyinsight.data.dto.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import retrofit2.http.GET
import retrofit2.http.Path


/**
 * Unified API Service for all endpoints
 * Base URL: http://10.0.2.2:8000/ (no /api suffix)
 */
interface ApiService {

    // ============ Health Check ============
    @GET("health")
    suspend fun health(): ApiResponse<HealthResponse>

    // ============ Recommendations ============
    @GET("/api/recommendations/today/")
    suspend fun getTodayRecommendations(): ApiResponse<List<RecommendationDto>>

    @GET("api/recommendations/history/")
    suspend fun getStockRecommendations(): ApiResponse<Map<String, List<RecommendationDto>>>

    // ============ Stock Briefing & Stock Details ============

    @GET("marketindex/api/overview/{ticker}")
    suspend fun getStockBriefing(): LLMSummaryResponse

    // 텍스트 개요(요약/기본적/기술적/뉴스/날짜)
    @GET("api/overview/{ticker}")
    suspend fun getStockOverview(
        @Path("ticker") ticker: String
    ): StockOverviewDto

    // 수치(히스토리/표/프로필)
    @GET("api/reports/{ticker}")
    suspend fun getStockReport(
        @Path("ticker") ticker: String
    ): StockDetailDto

    // ======================================

    @GET("api/company-list")
    suspend fun getCompanyList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ) : Response<CompanyListResponse>

    // ============ Market Index ============
    @GET("marketindex/stockindex/latest")
    suspend fun getStockIndex(): StockIndexLatestResponse

    @GET("marketindex/stockindex/history/{index_type}/")
    suspend fun getHistoricalData(
        @Path("index_type") indexType: String,
        @Query("days") days: Int
    ): StockIndexHistoryResponse

    @GET("marketindex/overview")
    suspend fun getLLMSummaryLatest(): ResponseBody

    // ============ Authentication ============
    @GET("user/csrf")
    fun setCsrf(): Call<CsrfResponse>

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
  
    @POST("user/info/portfolio")
    suspend fun setPortfolio(
        @Body portfolio: PortfolioRequest
    ) : Response<PortfolioResponse>

    // ============ Auto Login ============
    @GET("user/info/name")
    fun getName(): Call<UserNameResponse>

    // ============ User Info ============
    @POST("user/logout")
    fun logOut(): Call<UserProfileResponse>

    @DELETE("user/withdraw")
    fun withdraw(): Call<UserProfileResponse>

    @POST("user/info/name")
    fun changeName(
        @Body request: ChangeNameRequest
    ): Call<UserProfileResponse>

    @PUT("user/info/password")
    fun changePassword(
        @Body password: ChangePasswordRequest
    ): Call<UserProfileResponse>
}