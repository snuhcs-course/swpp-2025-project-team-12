package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.StockItem

interface StockRepository {
    suspend fun fetchStocks(): List<StockItem>
    suspend fun submitSelectedStocks(selected: Set<String>): Boolean
}
