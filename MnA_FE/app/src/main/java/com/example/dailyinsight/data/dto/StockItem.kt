package com.example.dailyinsight.data.dto

import android.hardware.camera2.TotalCaptureResult
import android.os.Message
import java.time.ZoneOffset

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

data class PortfolioRequest(
    val portfolio: Set<String>
)

data class PortfolioResponse(
    val message: String
)