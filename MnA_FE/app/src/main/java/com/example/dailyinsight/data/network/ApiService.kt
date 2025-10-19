package com.example.dailyinsight.data.network

import com.example.dailyinsight.data.dto.StockIndexHistoryResponse
import okhttp3.Cookie
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class LogInRequest(
    val id: String,
    val password: String
)

data class SignUpRequest(
    val id: String,
    val password: String
)

data class LogInResponse(
    val message: String
)

data class SignUpResponse(
    val message: String
)

interface ApiService {
    @GET("marketindex/stockindex/latest")
    suspend fun getStockIndex(): ApiResponse

    @GET("marketindex/stockindex/history/{index_type}/")
    suspend fun getHistoricalData(
        @Path("index_type") indexType: String,
        @Query("days") days: Int
    ): StockIndexHistoryResponse

    @POST("user/login")
    fun logIn(
        @Body request: LogInRequest
    ) : Call<LogInResponse>

    @POST("user/signup")
    fun signUp(
        @Body request: SignUpRequest
    ) : Call<SignUpResponse>
}