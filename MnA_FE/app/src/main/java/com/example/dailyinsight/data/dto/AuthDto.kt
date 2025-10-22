package com.example.dailyinsight.data.dto

// ============ Auth DTOs ============
data class LogInRequest(
    val id: String,
    val password: String
)

data class SignUpRequest(
    val id: String,
    val password: String
)

data class LogInResponse(
    val message: String
)

data class SignUpResponse(
    val message: String
)