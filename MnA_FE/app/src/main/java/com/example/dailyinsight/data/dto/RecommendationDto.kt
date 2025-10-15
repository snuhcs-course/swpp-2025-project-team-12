package com.example.dailyinsight.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendationDto(
    @SerialName("code")        val code: String,     // "005930"
    @SerialName("name")        val name: String,     // "삼성전자"
    @SerialName("price")       val price: Long,      // 1415000
    @SerialName("change")      val change: Long,     // -10000
    @SerialName("change_rate") val changeRate: Double, // -0.29  (← snake_case 매핑)
    @SerialName("time")        val time: String,     // "09:30"
    @SerialName("headline")    val headline: String? = null
)