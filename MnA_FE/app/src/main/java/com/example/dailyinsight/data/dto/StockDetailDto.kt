package com.example.dailyinsight.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
@Serializable
data class StockDetailDto(
    @SerialName("market_cap") val marketCap: String? = null,
    @SerialName("shares_outstanding") val sharesOutstanding: String? = null,

    @SerialName("valuation") val valuation: Valuation = Valuation(),
    @SerialName("solvency")  val solvency: Solvency  = Solvency(),
    @SerialName("dividend")  val dividend: Dividend  = Dividend(),


    // price 시계열 맵(날짜→값)을 담아올 자리
    @JsonNames("price_financial_info", "priceFinancialInfo")
    val priceFinancialInfo: PriceFinancialInfoDto? = null,

    // (옵션) 상단 헤더/차트/표용 보강 필드들
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("name")   val name: String? = null,
    @SerialName("price")  val price: Long? = null,
    @SerialName("change") val change: Long? = null,
    @SerialName("change_rate") val changeRate: Double? = null,
    @SerialName("chart") val chart: List<ChartPoint>? = null,
    @SerialName("net_income") val netIncome: NetIncome? = null
)

@Serializable
data class PriceFinancialInfoDto(
    // pandas.Series가 { "YYYY-MM-DD": 70100, ... } 형태로 올 때 수용
    @SerialName("price") val price: Map<String, Double>? = null
)

@Serializable
data class Valuation(
    @SerialName("pe_annual") val peAnnual: String? = null,
    @SerialName("pe_ttm")    val peTtm: String? = null,
    @SerialName("forward_pe") val forwardPe: String? = null,
    @SerialName("ps_ttm")    val psTtm: String? = null,
    @SerialName("pb")        val priceToBook: String? = null,   // ← JSON의 pb
    @SerialName("pcf_ttm")   val pcfTtm: String? = null,
    @SerialName("pfcf_ttm")  val pfcfTtm: String? = null
)

@Serializable
data class Solvency(
    @SerialName("current_ratio") val currentRatio: String? = null,
    @SerialName("quick_ratio")   val quickRatio: String? = null,
    @SerialName("de_ratio")      val debtToEquity: String? = null // ← JSON의 de_ratio
)

@Serializable
data class Dividend(
    @SerialName("payout_ratio")  val payoutRatio: String? = null,
    @SerialName("yield")         val `yield`: String? = null,
    @SerialName("latest_exdate") val latestExDate: String? = null // ← JSON의 latest_exdate
)

@Serializable
data class ChartPoint(
    @SerialName("t") val t: Long,
    @SerialName("v") val v: Double
)

@Serializable
data class NetIncome(
    @SerialName("annual")  val annual: List<PeriodValue>? = null,
    @SerialName("quarter") val quarter: List<PeriodValue>? = null
)

@Serializable
data class PeriodValue(
    @SerialName("period") val period: String,
    @SerialName("value")  val value: String
)