package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class ApiResponseTest {

    @Test
    fun apiResponse_withString() {
        val response = ApiResponse(data = "test")
        assertEquals("test", response.data)
        assertNull(response.status)
        assertNull(response.message)
    }

    @Test
    fun apiResponse_withAllFields() {
        val response = ApiResponse(
            data = "test",
            status = "success",
            message = "ok"
        )
        assertEquals("test", response.data)
        assertEquals("success", response.status)
        assertEquals("ok", response.message)
    }

    @Test
    fun apiResponse_withList() {
        val list = listOf(1, 2, 3)
        val response = ApiResponse(data = list)
        assertEquals(3, response.data.size)
    }

    @Test
    fun apiResponse_withMap() {
        val map = mapOf("key" to "value")
        val response = ApiResponse(data = map)
        assertEquals("value", response.data["key"])
    }

    @Test
    fun apiResponse_equality() {
        val r1 = ApiResponse(data = "test", status = "ok")
        val r2 = ApiResponse(data = "test", status = "ok")
        assertEquals(r1, r2)
    }

    @Test
    fun apiResponse_copy() {
        val original = ApiResponse(data = "test")
        val copied = original.copy(status = "success")
        assertEquals("test", copied.data)
        assertEquals("success", copied.status)
    }

    @Test
    fun apiResponse_toString() {
        val response = ApiResponse(data = "test")
        assertNotNull(response.toString())
    }

    @Test
    fun apiResponse_hashCode() {
        val r1 = ApiResponse(data = "test")
        val r2 = ApiResponse(data = "test")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun apiResponse_nullData() {
        val response = ApiResponse<String?>(data = null)
        assertNull(response.data)
    }

    @Test
    fun apiResponse_emptyStatus() {
        val response = ApiResponse(data = "test", status = "")
        assertEquals("", response.status)
    }

    @Test
    fun apiResponse_components() {
        val response = ApiResponse(data = "test", status = "ok", message = "done")
        val (data, status, message) = response
        assertEquals("test", data)
        assertEquals("ok", status)
        assertEquals("done", message)
    }

    @Test
    fun apiResponse_withDto() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        val response = ApiResponse(data = dto)
        assertEquals("005930", response.data.ticker)
    }

    @Test
    fun apiResponse_genericType() {
        val response1: ApiResponse<String> = ApiResponse(data = "test")
        val response2: ApiResponse<Int> = ApiResponse(data = 123)
        assertEquals("test", response1.data)
        assertEquals(123, response2.data)
    }

    @Test
    fun apiResponse_longMessage() {
        val longMsg = "A".repeat(1000)
        val response = ApiResponse(data = "test", message = longMsg)
        assertEquals(1000, response.message?.length)
    }

    @Test
    fun apiResponse_specialCharacters() {
        val response = ApiResponse(
            data = "test",
            status = "성공",
            message = "완료됨"
        )
        assertEquals("성공", response.status)
        assertEquals("완료됨", response.message)
    }
}