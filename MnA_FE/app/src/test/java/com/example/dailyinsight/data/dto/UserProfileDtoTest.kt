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

    // ===== Additional Tests =====

    @Test
    fun changeNameRequest_hashCode() {
        val r1 = ChangeNameRequest("John")
        val r2 = ChangeNameRequest("John")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun changeNameRequest_toString() {
        val request = ChangeNameRequest("John")
        val str = request.toString()
        assertTrue(str.contains("John"))
    }

    @Test
    fun changeNameRequest_destructuring() {
        val request = ChangeNameRequest("John")
        val (name) = request
        assertEquals("John", name)
    }

    @Test
    fun changeNameRequest_inequality() {
        val r1 = ChangeNameRequest("John")
        val r2 = ChangeNameRequest("Jane")
        assertNotEquals(r1, r2)
    }

    @Test
    fun changePasswordRequest_hashCode() {
        val r1 = ChangePasswordRequest("pass")
        val r2 = ChangePasswordRequest("pass")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun changePasswordRequest_toString() {
        val request = ChangePasswordRequest("secret")
        val str = request.toString()
        assertTrue(str.contains("secret"))
    }

    @Test
    fun changePasswordRequest_destructuring() {
        val request = ChangePasswordRequest("secret")
        val (password) = request
        assertEquals("secret", password)
    }

    @Test
    fun changePasswordRequest_copy() {
        val original = ChangePasswordRequest("old")
        val copied = original.copy(password = "new")
        assertEquals("new", copied.password)
    }

    @Test
    fun changePasswordRequest_inequality() {
        val r1 = ChangePasswordRequest("pass1")
        val r2 = ChangePasswordRequest("pass2")
        assertNotEquals(r1, r2)
    }

    @Test
    fun changePasswordRequest_longPassword() {
        val longPassword = "A".repeat(256)
        val request = ChangePasswordRequest(longPassword)
        assertEquals(256, request.password.length)
    }

    @Test
    fun userProfileResponse_hashCode() {
        val r1 = UserProfileResponse("Success")
        val r2 = UserProfileResponse("Success")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun userProfileResponse_toString() {
        val response = UserProfileResponse("Success")
        val str = response.toString()
        assertTrue(str.contains("Success"))
    }

    @Test
    fun userProfileResponse_destructuring() {
        val response = UserProfileResponse("Success")
        val (message) = response
        assertEquals("Success", message)
    }

    @Test
    fun userProfileResponse_equality() {
        val r1 = UserProfileResponse("Success")
        val r2 = UserProfileResponse("Success")
        assertEquals(r1, r2)
    }

    @Test
    fun userProfileResponse_copy() {
        val original = UserProfileResponse("Old message")
        val copied = original.copy(message = "New message")
        assertEquals("New message", copied.message)
    }

    @Test
    fun userProfileResponse_inequality() {
        val r1 = UserProfileResponse("Success")
        val r2 = UserProfileResponse("Failure")
        assertNotEquals(r1, r2)
    }
}