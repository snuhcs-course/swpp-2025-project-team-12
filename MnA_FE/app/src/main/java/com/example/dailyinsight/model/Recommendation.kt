package com.example.dailyinsight.model

data class Recommendation(
    val name: String,
    val price: Double,
    val diff: Double,   // 전일 대비 절대값
    val pct: Double,    // 전일 대비 %
    val summary: String // 한줄 설명
)