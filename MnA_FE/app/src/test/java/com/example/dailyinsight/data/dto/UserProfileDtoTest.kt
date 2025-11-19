package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class UserProfileDtoTest {

    // ===== ChangeNameRequest =====
    @Test
    fun changeNameRequest_createsCorrectly() {
        val request = ChangeNameRequest("John Doe")
        assertEquals("John Doe", request.name)
    }

    @Test
    fun changeNameRequest_koreanName() {
        val request = ChangeNameRequest("김철수")
        assertEquals("김철수", request.name)
    }

    @Test
    fun changeNameRequest_emptyName() {
        val request = ChangeNameRequest("")
        assertEquals("", request.name)
    }

    @Test
    fun changeNameRequest_longName() {
        val longName = "A".repeat(1000)
        val request = ChangeNameRequest(longName)
        assertEquals(1000, request.name.length)
    }

    @Test
    fun changeNameRequest_specialCharacters() {
        val request = ChangeNameRequest("O'Neil-Smith")
        assertEquals("O'Neil-Smith", request.name)
    }

    @Test
    fun changeNameRequest_equality() {
        val r1 = ChangeNameRequest("John")
        val r2 = ChangeNameRequest("John")
        assertEquals(r1, r2)
    }

    @Test
    fun changeNameRequest_copy() {
        val original = ChangeNameRequest("John")
        val copied = original.copy(name = "Jane")
        assertEquals("Jane", copied.name)
    }

    // ===== ChangePasswordRequest =====
    @Test
    fun changePasswordRequest_createsCorrectly() {
        val request = ChangePasswordRequest("newPassword123")
        assertEquals("newPassword123", request.password)
    }

    @Test
    fun changePasswordRequest_emptyPassword() {
        val request = ChangePasswordRequest("")
        assertEquals("", request.password)
    }

    @Test
    fun changePasswordRequest_complexPassword() {
        val request = ChangePasswordRequest("P@ssw0rd!2024#$%")
        assertEquals("P@ssw0rd!2024#$%", request.password)
    }

    @Test
    fun changePasswordRequest_equality() {
        val r1 = ChangePasswordRequest("pass")
        val r2 = ChangePasswordRequest("pass")
        assertEquals(r1, r2)
    }

    // ===== UserProfileResponse =====
    @Test
    fun userProfileResponse_success() {
        val response = UserProfileResponse("Profile updated successfully")
        assertEquals("Profile updated successfully", response.message)
    }

    @Test
    fun userProfileResponse_failure() {
        val response = UserProfileResponse("Update failed")
        assertEquals("Update failed", response.message)
    }

    @Test
    fun userProfileResponse_koreanMessage() {
        val response = UserProfileResponse("프로필이 업데이트되었습니다")
        assertEquals("프로필이 업데이트되었습니다", response.message)
    }

    @Test
    fun userProfileResponse_emptyMessage() {
        val response = UserProfileResponse("")
        assertEquals("", response.message)
    }
}