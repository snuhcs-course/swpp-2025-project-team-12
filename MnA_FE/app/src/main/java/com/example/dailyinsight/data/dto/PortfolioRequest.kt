package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

data class PortfolioRequest(
    @SerializedName("portfolio")
    val portfolio: List<String>
)