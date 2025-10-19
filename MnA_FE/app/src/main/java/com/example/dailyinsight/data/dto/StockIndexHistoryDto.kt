package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

// The top-level response object
data class StockIndexHistoryResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("index")
    val index: String,

    @SerializedName("data")
    val data: List<StockIndexHistoryItem> // Changed from Map to List
)

// Represents a single item in the historical data array
// Also renamed from StockIndexPricePoint for clarity
data class StockIndexHistoryItem(
    @SerializedName("date")
    val date: String,

    @SerializedName("close")
    val close: Double
)