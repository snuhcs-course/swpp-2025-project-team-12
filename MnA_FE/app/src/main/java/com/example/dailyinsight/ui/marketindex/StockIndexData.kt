package com.example.dailyinsight.ui.marketindex

import com.google.gson.annotations.SerializedName

data class StockIndexData(
    @SerializedName("name")
    var name: String,

    @SerializedName("close")
    val close: Double,

    @SerializedName("change_amount")
    val changeAmount: Double,

    @SerializedName("change_percent")
    val changePercent: Double,

    @SerializedName("description")
    val description: String
)