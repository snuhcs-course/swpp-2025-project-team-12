package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

data class StockOverviewDto(
    @SerializedName("asof_date") val asOfDate: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("fundamental_analysis") val fundamental: String? = null,
    @SerializedName("technical_analysis") val technical: String? = null,
    @SerializedName("news") val news: List<String>? = null
)