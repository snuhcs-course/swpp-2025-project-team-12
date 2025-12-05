package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class ApiResponseModelTest {

    // ===== ApiResponse Tests =====

    @Test
    fun apiResponse_withAllFields_createsCorrectly() {
        val response = ApiResponse(
            items = listOf("item1", "item2"),
            total = 100,
            limit = 10,
            offset = 0,
            asOf = "2024-01-15",
            source = "api",
            marketDate = "2024-01-15",
            personalized = true
        )

        assertEquals(listOf("item1", "item2"), response.items)
        assertEquals(100, response.total)
        assertEquals(10, response.limit)
        assertEquals(0, response.offset)
        assertEquals("2024-01-15", response.asOf)
        assertEquals("api", response.source)
        assertEquals("2024-01-15", response.marketDate)
        assertTrue(response.personalized!!)
    }

    @Test
    fun apiResponse_withDefaultNullValues() {
        val response = ApiResponse<String>()

        assertNull(response.items)
        assertNull(response.total)
        assertNull(response.limit)
        assertNull(response.offset)
        assertNull(response.asOf)
        assertNull(response.source)
        assertNull(response.marketDate)
        assertNull(response.personalized)
    }

    @Test
    fun apiResponse_withEmptyItems() {
        val response = ApiResponse(
            items = emptyList<String>(),
            total = 0
        )

        assertNotNull(response.items)
        assertTrue(response.items!!.isEmpty())
        assertEquals(0, response.total)
    }

    @Test
    fun apiResponse_withPartialValues() {
        val response = ApiResponse(
            items = listOf(1, 2, 3),
            total = 3
        )

        assertEquals(3, response.items?.size)
        assertEquals(3, response.total)
        assertNull(response.limit)
        assertNull(response.offset)
    }

    @Test
    fun apiResponse_genericType_withInt() {
        val response = ApiResponse(
            items = listOf(1, 2, 3, 4, 5),
            total = 5
        )

        assertEquals(5, response.items?.size)
        assertEquals(1, response.items?.get(0))
    }

    @Test
    fun apiResponse_genericType_withCustomObject() {
        data class Stock(val ticker: String, val price: Long)
        val stocks = listOf(Stock("AAPL", 150), Stock("GOOG", 2800))

        val response = ApiResponse(
            items = stocks,
            total = 2
        )

        assertEquals(2, response.items?.size)
        assertEquals("AAPL", response.items?.get(0)?.ticker)
    }

    @Test
    fun apiResponse_equality() {
        val response1 = ApiResponse(items = listOf("a"), total = 1)
        val response2 = ApiResponse(items = listOf("a"), total = 1)

        assertEquals(response1, response2)
    }

    @Test
    fun apiResponse_inequality() {
        val response1 = ApiResponse(items = listOf("a"), total = 1)
        val response2 = ApiResponse(items = listOf("b"), total = 1)

        assertNotEquals(response1, response2)
    }

    @Test
    fun apiResponse_hashCode() {
        val response1 = ApiResponse(items = listOf("a"), total = 1)
        val response2 = ApiResponse(items = listOf("a"), total = 1)

        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun apiResponse_copy() {
        val original = ApiResponse(items = listOf("a"), total = 1, personalized = false)
        val copied = original.copy(personalized = true)

        assertTrue(copied.personalized!!)
        assertEquals(listOf("a"), copied.items)
    }

    @Test
    fun apiResponse_pagination() {
        val response = ApiResponse<String>(
            total = 100,
            limit = 20,
            offset = 40
        )

        assertEquals(100, response.total)
        assertEquals(20, response.limit)
        assertEquals(40, response.offset)
    }

    // ===== HealthResponse Tests =====

    @Test
    fun healthResponse_withAllFields() {
        val response = HealthResponse(
            api = "healthy",
            s3 = mapOf("status" to "ok"),
            db = mapOf("status" to "connected"),
            asOf = "2024-01-15T12:00:00"
        )

        assertEquals("healthy", response.api)
        assertNotNull(response.s3)
        assertNotNull(response.db)
        assertEquals("2024-01-15T12:00:00", response.asOf)
    }

    @Test
    fun healthResponse_withDefaultNullValues() {
        val response = HealthResponse()

        assertNull(response.api)
        assertNull(response.s3)
        assertNull(response.db)
        assertNull(response.asOf)
    }

    @Test
    fun healthResponse_withPartialValues() {
        val response = HealthResponse(api = "ok")

        assertEquals("ok", response.api)
        assertNull(response.s3)
        assertNull(response.db)
        assertNull(response.asOf)
    }

    @Test
    fun healthResponse_s3AsString() {
        val response = HealthResponse(s3 = "connected")

        assertEquals("connected", response.s3)
    }

    @Test
    fun healthResponse_s3AsMap() {
        val s3Status = mapOf("bucket" to "available", "latency" to 50)
        val response = HealthResponse(s3 = s3Status)

        assertTrue(response.s3 is Map<*, *>)
    }

    @Test
    fun healthResponse_dbAsBoolean() {
        val response = HealthResponse(db = true)

        assertEquals(true, response.db)
    }

    @Test
    fun healthResponse_equality() {
        val response1 = HealthResponse(api = "ok", asOf = "2024-01-15")
        val response2 = HealthResponse(api = "ok", asOf = "2024-01-15")

        assertEquals(response1, response2)
    }

    @Test
    fun healthResponse_inequality() {
        val response1 = HealthResponse(api = "ok")
        val response2 = HealthResponse(api = "error")

        assertNotEquals(response1, response2)
    }

    @Test
    fun healthResponse_hashCode() {
        val response1 = HealthResponse(api = "ok")
        val response2 = HealthResponse(api = "ok")

        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun healthResponse_copy() {
        val original = HealthResponse(api = "ok", asOf = "2024-01-15")
        val copied = original.copy(api = "error")

        assertEquals("error", copied.api)
        assertEquals("2024-01-15", copied.asOf)
    }

    @Test
    fun healthResponse_toString() {
        val response = HealthResponse(api = "healthy")
        val str = response.toString()

        assertTrue(str.contains("healthy"))
    }
}
