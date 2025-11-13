package com.example.dailyinsight.data.dto

import android.os.Message

data class ChangeNameRequest(
    val name: String
)

data class ChangePasswordRequest(
    val password: String
)

data class UserProfileResponse(
    val message: String
)