package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

data class StockDetailDto(
    @SerializedName("ticker") val ticker: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("industry") val industry: String? = null,

    // 1. "올해" 데이터 (헤더 + 올해 표)
    @SerializedName("current") val current: CurrentData? = null,
    @SerializedName("valuation") val valuation: ValuationData? = null,
    @SerializedName("dividend") val dividend: DividendData? = null,
    @SerializedName("financials") val financials: FinancialsData? = null,

    // 2. "과거" 데이터 (차트 + 작년~ 표)
    @SerializedName("history") val history: List<HistoryItem>? = null,

    // 3. 기타
    @SerializedName("profile") val profile: ProfileData? = null,
    @SerializedName("asOf") val asOf: String? = null // 기준일
)


data class CurrentData(
    @SerializedName("price") val price: Long? = null,
    @SerializedName("change") val change: Long? = null,
    @SerializedName("change_rate") val changeRate: Double? = null,
    @SerializedName("market_cap") val marketCap: Long? = null,
    //@SerialName("shares_outstanding") val sharesOutstanding: Long? = null,
    @SerializedName("date") val date: String? = null // "2025-11-12 00:00:00"
)

data class ValuationData(
    @SerializedName("pe_ttm") val peTtm: Double? = null,
    @SerializedName("pb") val priceToBook: Double? = null, // pbr
    @SerializedName("bps") val bps: Long? = null
)

data class DividendData(
    @SerializedName("yield") val `yield`: Double? = null // div
)

data class FinancialsData(
    @SerializedName("eps") val eps: Long? = null,
    @SerializedName("dps") val dps: Long? = null,
    @SerializedName("roe") val roe: Double? = null
)

data class HistoryItem(
    @SerializedName("date") val date: String,
    @SerializedName("close") val close: Double,
    @SerializedName("market_cap") val marketCap: Long? = null,
    @SerializedName("PER") val per: Double? = null,
    @SerializedName("PBR") val pbr: Double? = null,
    @SerializedName("EPS") val eps: Long? = null,
    @SerializedName("BPS") val bps: Long? = null,
    @SerializedName("DIV") val divYield: Double? = null,
    @SerializedName("DPS") val dps: Long? = null,
    @SerializedName("ROE") val roe: Double? = null
)

data class ProfileData(
    @SerializedName("explanation") val explanation: String? = null
)