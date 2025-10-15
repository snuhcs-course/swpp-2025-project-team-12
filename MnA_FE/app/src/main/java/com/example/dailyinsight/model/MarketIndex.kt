package com.example.dailyinsight.model

data class MarketIndex(
    val name: String,          // "KOSPI"
    val value: Double,         // 3435.17
    val diff: Double,          // +2.30 (절대변화)
    val diffPct: Double,       // +0.09 (%)
    val timeLabel: String,     // "09:30"
    val series: List<Float> = emptyList() // 간단 스파크라인용(지금은 placeholder)
)