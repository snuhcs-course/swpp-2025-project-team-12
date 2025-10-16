package com.example.dailyinsight.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockDetailDto(
    // 헤더
    @SerialName("ticker") val ticker: String,          // 다이어그램: StockInfo.ticker
    @SerialName("name")   val name: String,            // StockInfo.company name
    @SerialName("price")  val price: Long,
    @SerialName("change") val change: Long,
    @SerialName("change_rate") val changeRate: Double,

    // 차트 (예: time=epoch millis, value=종가)
    @SerialName("chart") val chart: List<ChartPoint> = emptyList(),

    // 시세 요약(예: Net Income 표 상단 요약치)
    @SerialName("market_cap") val marketCap: String? = null,
    @SerialName("shares_outstanding") val sharesOutstanding: String? = null,

    // Net Income 표 (연/분기별)
    @SerialName("net_income") val netIncome: NetIncomeSection = NetIncomeSection(),

    // 밸류에이션/유동성/배당
    @SerialName("valuation") val valuation: Valuation = Valuation(),
    @SerialName("solvency")  val solvency:  Solvency  = Solvency(),
    @SerialName("dividend")  val dividend:  Dividend  = Dividend()
) {
    @Serializable data class ChartPoint(@SerialName("t") val t: Long, @SerialName("v") val v: Double)

    @Serializable data class NetIncomeSection(
        @SerialName("annual") val annual: List<Row> = emptyList(),
        @SerialName("quarter") val quarter: List<Row> = emptyList()
    ) { @Serializable data class Row(val period: String, val value: String) }

    @Serializable data class Valuation(
        @SerialName("pe_annual")  val peAnnual: String? = null,
        @SerialName("pe_ttm")     val peTtm: String? = null,
        @SerialName("forward_pe") val forwardPe: String? = null,
        @SerialName("ps_ttm")     val priceToSalesTtm: String? = null,
        @SerialName("pb")         val priceToBook: String? = null,
        @SerialName("pcf_ttm")    val priceToCashflowTtm: String? = null,
        @SerialName("pfcf_ttm")   val priceToFreeCashflowTtm: String? = null
    )

    @Serializable data class Solvency(
        @SerialName("current_ratio") val currentRatio: String? = null,
        @SerialName("quick_ratio")   val quickRatio: String? = null,
        @SerialName("de_ratio")      val debtToEquity: String? = null
    )

    @Serializable data class Dividend(
        @SerialName("payout_ratio")  val payoutRatio: String? = null,
        @SerialName("yield")         val yield: String? = null,
        @SerialName("latest_exdate") val latestExDate: String? = null
    )
}