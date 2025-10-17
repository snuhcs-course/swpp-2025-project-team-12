package com.example.dailyinsight.model

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
