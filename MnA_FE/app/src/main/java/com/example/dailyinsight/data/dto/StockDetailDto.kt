package com.example.dailyinsight.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class StockDetailDto(
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("name") val name: String? = null,

    // 1. "오늘" 데이터 (헤더 + 2025년 표)
    @SerialName("current") val current: CurrentData? = null,
    @SerialName("valuation") val valuation: ValuationData? = null,
    @SerialName("dividend") val dividend: DividendData? = null,
    @SerialName("financials") val financials: FinancialsData? = null,

    // 2. "과거" 데이터 (차트 + 2024, 2023년 표)
    @SerialName("history") val history: List<HistoryItem>? = null,

    // 3. 기타
    @SerialName("profile") val profile: ProfileData? = null,
    @SerialName("asOf") val asOf: String? = null // 기준일
)

@Serializable
data class CurrentData(
    @SerialName("price") val price: Long? = null,
    @SerialName("change") val change: Long? = null,
    @SerialName("change_rate") val changeRate: Double? = null,
    @SerialName("market_cap") val marketCap: Long? = null,
    //@SerialName("shares_outstanding") val sharesOutstanding: Long? = null,
    @SerialName("date") val date: String? = null // "2025-11-12 00:00:00"
)

@Serializable
data class ValuationData(
    @SerialName("pe_ttm") val peTtm: Double? = null,
    @SerialName("pb") val priceToBook: Double? = null, // pbr
    @SerialName("bps") val bps: Long? = null
)

@Serializable
data class DividendData(
    @SerialName("yield") val `yield`: Double? = null // div
)

@Serializable
data class FinancialsData(
    @SerialName("eps") val eps: Long? = null,
    @SerialName("dps") val dps: Long? = null,
    @SerialName("roe") val roe: Double? = null
)

@Serializable
data class HistoryItem(
    @SerialName("date") val date: String,
    @SerialName("close") val close: Double,
    @SerialName("market_cap") val marketCap: Long? = null,
    @SerialName("PER") val per: Double? = null,
    @SerialName("PBR") val pbr: Double? = null,
    @SerialName("EPS") val eps: Long? = null,
    @SerialName("BPS") val bps: Long? = null,
    @SerialName("DIV") val divYield: Double? = null,
    @SerialName("DPS") val dps: Long? = null,
    @SerialName("ROE") val roe: Double? = null
)

@Serializable
data class ProfileData(
    @SerialName("explanation") val explanation: String? = null
)