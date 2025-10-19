package com.example.dailyinsight.data.network

import okhttp3.Cookie
import com.example.dailyinsight.ui.marketindex.StockIndexData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST


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

    @POST("user/login")
    fun logIn(
        @Body request: LogInRequest
    ) : Call<LogInResponse>

    @POST("user/signup")
    fun signUp(
        @Body request: SignUpRequest
    ) : Call<SignUpResponse>
}