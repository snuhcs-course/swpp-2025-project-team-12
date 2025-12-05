package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class AuthDtoExtendedTest {

    // ===== UserApiResponse Tests =====

    @Test
    fun userApiResponse_createsCorrectly() {
        val response = UserApiResponse(message = "Success")
        assertEquals("Success", response.message)
    }

    @Test
    fun userApiResponse_equality() {
        val response1 = UserApiResponse("Success")
        val response2 = UserApiResponse("Success")
        assertEquals(response1, response2)
    }

    @Test
    fun userApiResponse_inequality() {
        val response1 = UserApiResponse("Success")
        val response2 = UserApiResponse("Failure")
        assertNotEquals(response1, response2)
    }

    @Test
    fun userApiResponse_hashCode() {
        val response1 = UserApiResponse("Success")
        val response2 = UserApiResponse("Success")
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun userApiResponse_copy() {
        val original = UserApiResponse("Original")
        val copied = original.copy(message = "Copied")
        assertEquals("Copied", copied.message)
    }

    @Test
    fun userApiResponse_emptyMessage() {
        val response = UserApiResponse("")
        assertEquals("", response.message)
    }

    @Test
    fun userApiResponse_koreanMessage() {
        val response = UserApiResponse("로그아웃 성공")
        assertEquals("로그아웃 성공", response.message)
    }

    @Test
    fun userApiResponse_toString() {
        val response = UserApiResponse("Test message")
        val str = response.toString()
        assertTrue(str.contains("Test message"))
    }

    @Test
    fun userApiResponse_destructuring() {
        val response = UserApiResponse("Success")
        val (message) = response
        assertEquals("Success", message)
    }

    // ===== Additional LogInRequest Tests =====

    @Test
    fun logInRequest_destructuring() {
        val request = LogInRequest("user123", "pass123")
        val (id, password) = request
        assertEquals("user123", id)
        assertEquals("pass123", password)
    }

    @Test
    fun logInRequest_toString() {
        val request = LogInRequest("user123", "pass123")
        val str = request.toString()
        assertTrue(str.contains("user123"))
    }

    // ===== Additional SignUpRequest Tests =====

    @Test
    fun signUpRequest_destructuring() {
        val request = SignUpRequest("newuser", "newpass")
        val (id, password) = request
        assertEquals("newuser", id)
        assertEquals("newpass", password)
    }

    @Test
    fun signUpRequest_inequality() {
        val request1 = SignUpRequest("user1", "pass")
        val request2 = SignUpRequest("user2", "pass")
        assertNotEquals(request1, request2)
    }

    // ===== Additional InterestsList Tests =====

    @Test
    fun interestsList_addInterest() {
        val interests = InterestsList(arrayListOf("tech"))
        interests.interests.add("finance")
        interests.interests.add("healthcare")

        assertEquals(3, interests.interests.size)
        assertTrue(interests.interests.contains("tech"))
        assertTrue(interests.interests.contains("finance"))
        assertTrue(interests.interests.contains("healthcare"))
    }

    @Test
    fun interestsList_removeInterest() {
        val interests = InterestsList(arrayListOf("tech", "finance", "healthcare"))
        interests.interests.remove("finance")

        assertEquals(2, interests.interests.size)
        assertFalse(interests.interests.contains("finance"))
    }

    @Test
    fun interestsList_clearInterests() {
        val interests = InterestsList(arrayListOf("tech", "finance"))
        interests.interests.clear()

        assertTrue(interests.interests.isEmpty())
    }

    @Test
    fun interestsList_destructuring() {
        val interests = InterestsList(arrayListOf("tech", "finance"))
        val (list) = interests
        assertEquals(2, list.size)
    }

    // ===== Additional Strategy Tests =====

    @Test
    fun strategy_destructuring() {
        val strategy = Strategy("AGGRESSIVE")
        val (strategyValue) = strategy
        assertEquals("AGGRESSIVE", strategyValue)
    }

    @Test
    fun strategy_equality() {
        val s1 = Strategy("STABLE")
        val s2 = Strategy("STABLE")
        assertEquals(s1, s2)
    }

    @Test
    fun strategy_hashCode() {
        val s1 = Strategy("NEUTRAL")
        val s2 = Strategy("NEUTRAL")
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun strategy_copy() {
        val original = Strategy("STABLE")
        val copied = original.copy(strategy = "AGGRESSIVE")
        assertEquals("AGGRESSIVE", copied.strategy)
    }

    // ===== Additional SetStyleRequest Tests =====

    @Test
    fun setStyleRequest_destructuring() {
        val interests = InterestsList(arrayListOf("tech"))
        val strategy = Strategy("STABLE")
        val request = SetStyleRequest(interests, strategy)

        val (interestsPart, strategyPart) = request
        assertEquals(1, interestsPart.interests.size)
        assertEquals("STABLE", strategyPart.strategy)
    }

    @Test
    fun setStyleRequest_equality() {
        val interests = InterestsList(arrayListOf("tech"))
        val strategy = Strategy("STABLE")

        val request1 = SetStyleRequest(interests, strategy)
        val request2 = SetStyleRequest(interests, strategy)

        assertEquals(request1, request2)
    }

    @Test
    fun setStyleRequest_copy() {
        val interests = InterestsList(arrayListOf("tech"))
        val strategy = Strategy("STABLE")
        val original = SetStyleRequest(interests, strategy)

        val newStrategy = Strategy("AGGRESSIVE")
        val copied = original.copy(strategy = newStrategy)

        assertEquals("AGGRESSIVE", copied.strategy.strategy)
        assertEquals("tech", copied.interests.interests[0])
    }

    // ===== Response Classes Tests =====

    @Test
    fun csrfResponse_destructuring() {
        val response = CsrfResponse("token123")
        val (message) = response
        assertEquals("token123", message)
    }

    @Test
    fun logInResponse_destructuring() {
        val response = LogInResponse("Login successful")
        val (message) = response
        assertEquals("Login successful", message)
    }

    @Test
    fun signUpResponse_destructuring() {
        val response = SignUpResponse("Signup successful")
        val (message) = response
        assertEquals("Signup successful", message)
    }

    @Test
    fun setStyleResponse_destructuring() {
        val response = SetStyleResponse("Style updated")
        val (message) = response
        assertEquals("Style updated", message)
    }

    @Test
    fun userNameResponse_destructuring() {
        val response = UserNameResponse("John Doe")
        val (name) = response
        assertEquals("John Doe", name)
    }

    @Test
    fun userNameResponse_copy() {
        val original = UserNameResponse("John")
        val copied = original.copy(name = "Jane")
        assertEquals("Jane", copied.name)
    }
}
