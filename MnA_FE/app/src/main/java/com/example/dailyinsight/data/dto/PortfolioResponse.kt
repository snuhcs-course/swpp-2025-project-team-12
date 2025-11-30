package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

data class PortfolioResponse(
    @SerializedName("portfolio")
    val portfolio: List<String>? = null // ["005930", "000660", ...] 형태의 Ticker 리스트
)