package com.example.dailyinsight.data.network

import com.example.dailyinsight.ui.marketindex.StockIndexData
import retrofit2.http.GET

interface ApiService {
    @GET("marketindex/stockindex/latest")
    suspend fun getStockIndex(): ApiResponse
}