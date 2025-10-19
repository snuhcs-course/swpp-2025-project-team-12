package com.example.dailyinsight.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val data: T,
    val status: String? = null,   // 선택(기본값)
    val message: String? = null   // 선택(기본값)
)