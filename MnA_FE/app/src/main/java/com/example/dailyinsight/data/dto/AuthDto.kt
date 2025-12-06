package com.example.dailyinsight.data.dto

import com.example.dailyinsight.model.Tag

// ============ Auth DTOs ============
data class LogInRequest(
    val id: String,
    val password: String
)

data class SignUpRequest(
    val id: String,
    val password: String
)

data class InterestsList(
    val interests: ArrayList<String>
)

data class Strategy(
    val strategy: String
)

data class SetStyleRequest(
    val interests: InterestsList,
    val strategy: Strategy
)

data class CsrfResponse(
    val message: String
)

data class LogInResponse(
    val message: String
)

data class SignUpResponse(
    val message: String
)

data class SetStyleResponse(
    val message: String
)

data class UserNameResponse(
    val name: String
)

data class UserApiResponse(
    val message: String
)