package com.example.dailyinsight.data.repository

import com.example.dailyinsight.data.dto.StockIndexHistoryItem // New import
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.data.dto.StockIndexData

class MarketIndexRepository {
    private val apiService = RetrofitInstance.api

    suspend fun getMarketData(): Map<String, StockIndexData> {
        return apiService.getStockIndex().data
    }

    // Update the return type of this function
    suspend fun getHistoricalData(indexType: String, days: Int): List<StockIndexHistoryItem> {
        return apiService.getHistoricalData(indexType, days).data
    }
}