package com.example.dailyinsight.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class IndexDto(
    val name: String,       // "KOSPI"
    val value: Double,      // 3435.17
    val change: Double,     // 2.30 (지수 절대 변화)
    val changeRate: Double, // 0.09 (%)
    val time: String        // "09:30"
)