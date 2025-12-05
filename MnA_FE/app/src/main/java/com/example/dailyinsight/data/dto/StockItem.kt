package com.example.dailyinsight.data.dto

data class StockItem(
    val ticker: String,
    val name: String
)

data class CompanyListResponse(
    val items: List<StockItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val source: String,
    val asOf: String
)