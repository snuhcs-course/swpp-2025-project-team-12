package com.example.dailyinsight.model

import java.time.LocalDate

data class StockEntry(
    val date: LocalDate,
    val name: String,
    val score: Int,
    val changeText: String
)

sealed class StockItem {
    data class Header(val dateLabel: String) : StockItem()
    data class Row(val entry: StockEntry) : StockItem()
}
