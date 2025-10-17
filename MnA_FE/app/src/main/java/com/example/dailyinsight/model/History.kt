package com.example.dailyinsight.model

import java.time.LocalDate

data class HistoryEntry(
    val date: LocalDate,
    val name: String,
    val score: Int,
    val changeText: String
)

sealed class HistoryItem {
    data class Header(val dateLabel: String) : HistoryItem()
    data class Row(val entry: HistoryEntry) : HistoryItem()
}
