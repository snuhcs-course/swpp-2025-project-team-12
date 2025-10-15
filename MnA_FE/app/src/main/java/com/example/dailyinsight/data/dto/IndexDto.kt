package com.example.dailyinsight.data.dto

data class IndexDto(
    val code: String,        // "KOSPI" / "KOSDAQ"
    val name: String,        // "코스피" / "코스닥"
    val price: Double,       // 2564.12
    val change: Double,      // -10.32
    val changeRate: Double,  // -0.40
    val time: String         // "09:30"
)