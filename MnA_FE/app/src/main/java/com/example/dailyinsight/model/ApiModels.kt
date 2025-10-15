package com.example.dailyinsight.data.model

data class ApiResponse<T>(
    val items: List<T>? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val asOf: String? = null,
    val source: String? = null,
    val marketDate: String? = null,
    val personalized: Boolean? = null
)

data class HealthResponse(
    val api: String? = null,
    val s3: Any? = null,
    val db: Any? = null,
    val asOf: String? = null
)

data class Recommendation(
    val ticker: String,
    val name: String,
    val news: List<String> = emptyList(),
    val reason: List<String> = emptyList(),
    val rank: Int? = null
)