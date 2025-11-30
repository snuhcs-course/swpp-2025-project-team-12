package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

// 전체 응답 래퍼
data class BriefingListResponse(
    @SerializedName("items") val items: List<BriefingItemDto>, // 👈 List<BriefingItemDto>
    @SerializedName("total") val total: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("offset") val offset: Int,
    @SerializedName("source") val source: String?,
    @SerializedName("asOf") val asOf: String?
)

data class BriefingItemDto(
    @SerializedName("ticker") val ticker: String,
    @SerializedName("name") val name: String,
    @SerializedName("close") val close: String?,
    @SerializedName("change") val change: String?,
    @SerializedName("change_rate") val changeRate: String?,
    @SerializedName("summary") val summary: String? = null, // LLM summary
    @SerializedName("overview") val overview: StockOverviewDto? // 우리는 overview가 필요함
)