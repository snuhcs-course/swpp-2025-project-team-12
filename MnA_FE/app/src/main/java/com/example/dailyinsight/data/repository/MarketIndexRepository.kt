package com.example.dailyinsight.data.repository

import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.marketindex.StockIndexData

class MarketIndexRepository {
    private val apiService = RetrofitInstance.api

    // The function now returns the nested map directly
    suspend fun getMarketData(): Map<String, StockIndexData> {
        return apiService.getStockIndex().data
    }
}