package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(

    @SerializedName("data")
    val data: T,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null
)