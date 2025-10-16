package com.example.dailyinsight.data.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class RecommendationDto(
    @SerialName("code")        val code: String,      // "005930"
    @SerialName("name")        val name: String,      // "삼성전자"
    @SerialName("price")       val price: Long,       // 141500
    @SerialName("change")      val change: Long,      // -100
    @SerialName("change_rate") val changeRate: Double,// -0.29  ← BE가 snake_case라면 유지
    @SerialName("time")        val time: String,      // "09:30"
    @SerialName("headline")    val headline: String? = null
) : Parcelable