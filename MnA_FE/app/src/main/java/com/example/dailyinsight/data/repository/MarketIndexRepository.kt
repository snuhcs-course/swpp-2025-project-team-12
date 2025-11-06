package com.example.dailyinsight.data.repository

import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.dto.LLMSummaryData
import com.google.gson.Gson

class MarketIndexRepository {
    private val apiService = RetrofitInstance.api

    suspend fun getMarketData(): Map<String, StockIndexData> {
        return apiService.getStockIndex().data
    }

    // Update the return type of this function
    suspend fun getHistoricalData(indexType: String, days: Int): List<StockIndexHistoryItem> {
        return apiService.getHistoricalData(indexType, days).data
    }

    suspend fun getLLMSummary(): LLMSummaryData {
        val response = apiService.getLLMSummary()
        // Parse the JSON string in llmOutput field
        return Gson().fromJson(response.llmOutput, LLMSummaryData::class.java)
    }
}