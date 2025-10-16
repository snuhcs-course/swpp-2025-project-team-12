package com.example.dailyinsight.ui.marketindex

data class StockIndexData(
    val name: String,
    val close: Double,
    val changeAmount: Double,
    val changePercent: Double,
    val description: String
)
