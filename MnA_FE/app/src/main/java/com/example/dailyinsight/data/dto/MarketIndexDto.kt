package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

// ============ Market Index DTOs ============

data class StockIndexLatestResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: Map<String, StockIndexData>
)

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

data class StockIndexData(
    @SerializedName("name")
    var name: String,

    @SerializedName("close")
    val close: Double,

    @SerializedName("change_amount")
    val changeAmount: Double,

    @SerializedName("change_percent")
    val changePercent: Double,

    @SerializedName("date")
    val date: String,

    @SerializedName("high")
    val high: Double,

    @SerializedName("low")
    val low: Double,

    @SerializedName("open")
    val open: Double,

    @SerializedName("volume")
    val volume: Long
)