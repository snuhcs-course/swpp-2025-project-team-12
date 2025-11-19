package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class AuthDtoTest {

    // ===== LogInRequest =====
    @Test
    fun logInRequest_createsCorrectly() {
        val request = LogInRequest("user123", "password")
        assertEquals("user123", request.id)
        assertEquals("password", request.password)
    }

    @Test
    fun logInRequest_equality() {
        val r1 = LogInRequest("user", "pass")
        val r2 = LogInRequest("user", "pass")
        assertEquals(r1, r2)
    }

    @Test
    fun logInRequest_copy() {
        val original = LogInRequest("user", "pass")
        val copied = original.copy(password = "newpass")
        assertEquals("user", copied.id)
        assertEquals("newpass", copied.password)
    }

    // ===== SignUpRequest =====
    @Test
    fun signUpRequest_createsCorrectly() {
        val request = SignUpRequest("newuser", "newpass")
        assertEquals("newuser", request.id)
        assertEquals("newpass", request.password)
    }

    @Test
    fun signUpRequest_emptyStrings() {
        val request = SignUpRequest("", "")
        assertEquals("", request.id)
        assertEquals("", request.password)
    }

    // ===== InterestsList =====
    @Test
    fun interestsList_createsCorrectly() {
        val list = arrayListOf("tech", "finance")
        val interests = InterestsList(list)
        assertEquals(2, interests.interests.size)
        assertEquals("tech", interests.interests[0])
    }

    @Test
    fun interestsList_emptyList() {
        val interests = InterestsList(arrayListOf())
        assertTrue(interests.interests.isEmpty())
    }

    @Test
    fun interestsList_largeList() {
        val list = ArrayList((1..100).map { "interest$it" })
        val interests = InterestsList(list)
        assertEquals(100, interests.interests.size)
    }

    // ===== Strategy =====
    @Test
    fun strategy_createsCorrectly() {
        val strategy = Strategy("AGGRESSIVE")
        assertEquals("AGGRESSIVE", strategy.strategy)
    }

    @Test
    fun strategy_differentValues() {
        val s1 = Strategy("STABLE")
        val s2 = Strategy("NEUTRAL")
        assertNotEquals(s1.strategy, s2.strategy)
    }

    // ===== SetStyleRequest =====
    @Test
    fun setStyleRequest_createsCorrectly() {
        val interests = InterestsList(arrayListOf("tech"))
        val strategy = Strategy("STABLE")
        val request = SetStyleRequest(interests, strategy)
        
        assertEquals(1, request.interests.interests.size)
        assertEquals("STABLE", request.strategy.strategy)
    }

    @Test
    fun setStyleRequest_withMultipleInterests() {
        val interests = InterestsList(arrayListOf("tech", "finance", "energy"))
        val strategy = Strategy("AGGRESSIVE")
        val request = SetStyleRequest(interests, strategy)
        
        assertEquals(3, request.interests.interests.size)
        assertEquals("AGGRESSIVE", request.strategy.strategy)
    }

    // ===== CsrfResponse =====
    @Test
    fun csrfResponse_createsCorrectly() {
        val response = CsrfResponse("CSRF token generated")
        assertEquals("CSRF token generated", response.message)
    }

    @Test
    fun csrfResponse_emptyMessage() {
        val response = CsrfResponse("")
        assertEquals("", response.message)
    }

    // ===== LogInResponse =====
    @Test
    fun logInResponse_success() {
        val response = LogInResponse("Login successful")
        assertEquals("Login successful", response.message)
    }

    @Test
    fun logInResponse_failure() {
        val response = LogInResponse("Invalid credentials")
        assertEquals("Invalid credentials", response.message)
    }

    // ===== SignUpResponse =====
    @Test
    fun signUpResponse_success() {
        val response = SignUpResponse("User created")
        assertEquals("User created", response.message)
    }

    @Test
    fun signUpResponse_withSpecialChars() {
        val response = SignUpResponse("회원가입 성공!")
        assertEquals("회원가입 성공!", response.message)
    }

    // ===== SetStyleResponse =====
    @Test
    fun setStyleResponse_success() {
        val response = SetStyleResponse("Style updated")
        assertEquals("Style updated", response.message)
    }

    @Test
    fun setStyleResponse_longMessage() {
        val longMsg = "A".repeat(1000)
        val response = SetStyleResponse(longMsg)
        assertEquals(1000, response.message.length)
    }

    // ===== UserNameResponse =====
    @Test
    fun userNameResponse_createsCorrectly() {
        val response = UserNameResponse("John Doe")
        assertEquals("John Doe", response.name)
    }

    @Test
    fun userNameResponse_koreanName() {
        val response = UserNameResponse("김철수")
        assertEquals("김철수", response.name)
    }

    @Test
    fun userNameResponse_emptyName() {
        val response = UserNameResponse("")
        assertEquals("", response.name)
    }

    // ===== Integration Tests =====
    @Test
    fun fullAuthFlow_allDtosWork() {
        val signUpReq = SignUpRequest("user", "pass")
        val logInReq = LogInRequest("user", "pass")
        val interests = InterestsList(arrayListOf("tech"))
        val strategy = Strategy("STABLE")
        val styleReq = SetStyleRequest(interests, strategy)
        
        assertNotNull(signUpReq)
        assertNotNull(logInReq)
        assertNotNull(styleReq)
    }

    @Test
    fun dataClasses_toString() {
        val request = LogInRequest("user", "pass")
        assertNotNull(request.toString())
        assertTrue(request.toString().contains("LogInRequest"))
    }

    @Test
    fun dataClasses_hashCode() {
        val r1 = SignUpRequest("user", "pass")
        val r2 = SignUpRequest("user", "pass")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun interestsList_mutable() {
        val interests = InterestsList(arrayListOf("tech"))
        interests.interests.add("finance")
        assertEquals(2, interests.interests.size)
    }
}