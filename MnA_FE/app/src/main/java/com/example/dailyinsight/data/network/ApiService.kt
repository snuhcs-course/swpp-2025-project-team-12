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

    // ============ Stock Briefing & Stock Details ============

    @GET("api/company-list")
    suspend fun getBriefingList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("sort") sort: String? = null,
        @Query("industry") industry: String? = null, // 예: "전기.전자|은행"
        @Query("min") min: Int? = null,              // 시총 순위 시작 (0 = 1등)
        @Query("max") max: Int? = null               // 시총 순위 끝
    ): BriefingListResponse

    @GET("api/company-list")
    suspend fun getStockList(): ApiResponse<List<RecommendationDto>>

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

    //  내 관심 종목 가져오기
    @GET("user/info/portfolio")
    suspend fun getPortfolio(): Response<PortfolioResponse>

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
    suspend fun setCsrf(): Response<CsrfResponse>

    @POST("user/login")
    suspend fun logIn(
        @Body request: LogInRequest
    ): Response<UserApiResponse>

    @POST("user/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<UserApiResponse>

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
    suspend fun getName(): Response<UserNameResponse>

    // ============ User Info ============
    @POST("user/logout")
    suspend fun logOut(): Response<UserApiResponse>

    @DELETE("user/withdraw")
    suspend fun withdraw(): Response<UserApiResponse>

    @POST("user/info/name")
    suspend fun changeName(
        @Body request: ChangeNameRequest
    ): Response<UserApiResponse>

    @PUT("user/info/password")
    suspend fun changePassword(
        @Body password: ChangePasswordRequest
    ): Response<UserApiResponse>
}