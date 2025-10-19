package com.example.dailyinsight.data.network

import com.example.dailyinsight.ui.marketindex.StockIndexData
import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: Map<String, StockIndexData>
)