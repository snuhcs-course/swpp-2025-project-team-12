package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class ApiModelsTest {

    // ===== ApiResponse 테스트 =====

    @Test
    fun apiResponse_allFieldsNull_createsCorrectly() {
        // When
        val response = ApiResponse<String>()

        // Then
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
    fun apiResponse_withItems_createsCorrectly() {
        // When
        val items = listOf("item1", "item2", "item3")
        val response = ApiResponse(items = items)

        // Then
        assertEquals(items, response.items)
        assertEquals(3, response.items?.size)
    }

    @Test
    fun apiResponse_withTotal_createsCorrectly() {
        // When
        val response = ApiResponse<String>(total = 100)

        // Then
        assertEquals(100, response.total)
    }

    @Test
    fun apiResponse_withLimit_createsCorrectly() {
        // When
        val response = ApiResponse<String>(limit = 50)

        // Then
        assertEquals(50, response.limit)
    }

    @Test
    fun apiResponse_withOffset_createsCorrectly() {
        // When
        val response = ApiResponse<String>(offset = 10)

        // Then
        assertEquals(10, response.offset)
    }

    @Test
    fun apiResponse_withAsOf_createsCorrectly() {
        // When
        val response = ApiResponse<String>(asOf = "2024-01-15")

        // Then
        assertEquals("2024-01-15", response.asOf)
    }

    @Test
    fun apiResponse_withSource_createsCorrectly() {
        // When
        val response = ApiResponse<String>(source = "API")

        // Then
        assertEquals("API", response.source)
    }

    @Test
    fun apiResponse_withMarketDate_createsCorrectly() {
        // When
        val response = ApiResponse<String>(marketDate = "2024-01-15")

        // Then
        assertEquals("2024-01-15", response.marketDate)
    }

    @Test
    fun apiResponse_withPersonalized_true() {
        // When
        val response = ApiResponse<String>(personalized = true)

        // Then
        assertTrue(response.personalized == true)
    }

    @Test
    fun apiResponse_withPersonalized_false() {
        // When
        val response = ApiResponse<String>(personalized = false)

        // Then
        assertFalse(response.personalized == true)
    }

    @Test
    fun apiResponse_withAllFields_createsCorrectly() {
        // When
        val response = ApiResponse(
            items = listOf("A", "B"),
            total = 2,
            limit = 10,
            offset = 0,
            asOf = "2024-01-15",
            source = "Test",
            marketDate = "2024-01-15",
            personalized = true
        )

        // Then
        assertEquals(2, response.items?.size)
        assertEquals(2, response.total)
        assertEquals(10, response.limit)
        assertEquals(0, response.offset)
        assertEquals("2024-01-15", response.asOf)
        assertEquals("Test", response.source)
        assertEquals("2024-01-15", response.marketDate)
        assertTrue(response.personalized == true)
    }

    @Test
    fun apiResponse_emptyItemsList_createsCorrectly() {
        // When
        val response = ApiResponse<String>(items = emptyList())

        // Then
        assertTrue(response.items?.isEmpty() == true)
    }

    @Test
    fun apiResponse_largeItemsList_createsCorrectly() {
        // When
        val largeList = (1..1000).map { "item$it" }
        val response = ApiResponse(items = largeList)

        // Then
        assertEquals(1000, response.items?.size)
    }

    @Test
    fun apiResponse_zeroTotal_createsCorrectly() {
        // When
        val response = ApiResponse<String>(total = 0)

        // Then
        assertEquals(0, response.total)
    }

    @Test
    fun apiResponse_negativeOffset_createsCorrectly() {
        // When
        val response = ApiResponse<String>(offset = -1)

        // Then
        assertEquals(-1, response.offset)
    }

    @Test
    fun apiResponse_equality_sameValues() {
        // When
        val response1 = ApiResponse(items = listOf("A"), total = 1)
        val response2 = ApiResponse(items = listOf("A"), total = 1)

        // Then
        assertEquals(response1, response2)
    }

    @Test
    fun apiResponse_copy_works() {
        // When
        val original = ApiResponse(items = listOf("A"), total = 1)
        val copied = original.copy(total = 2)

        // Then
        assertEquals(listOf("A"), copied.items)
        assertEquals(2, copied.total)
    }

    @Test
    fun apiResponse_withComplexType_createsCorrectly() {
        // When
        data class TestData(val id: Int, val name: String)
        val items = listOf(TestData(1, "A"), TestData(2, "B"))
        val response = ApiResponse(items = items)

        // Then
        assertEquals(2, response.items?.size)
        assertEquals("A", response.items?.first()?.name)
    }

    @Test
    fun apiResponse_withNullableType_createsCorrectly() {
        // When
        val response = ApiResponse<String?>(items = listOf(null, "A", null))

        // Then
        assertEquals(3, response.items?.size)
        assertNull(response.items?.first())
    }

    @Test
    fun apiResponse_toString_works() {
        // When
        val response = ApiResponse(items = listOf("A"), total = 1)

        // Then
        assertNotNull(response.toString())
        assertTrue(response.toString().contains("ApiResponse"))
    }

    // ===== HealthResponse 테스트 =====

    @Test
    fun healthResponse_allFieldsNull_createsCorrectly() {
        // When
        val response = HealthResponse()

        // Then
        assertNull(response.api)
        assertNull(response.s3)
        assertNull(response.db)
        assertNull(response.asOf)
    }

    @Test
    fun healthResponse_withApi_createsCorrectly() {
        // When
        val response = HealthResponse(api = "healthy")

        // Then
        assertEquals("healthy", response.api)
    }

    @Test
    fun healthResponse_withS3String_createsCorrectly() {
        // When
        val response = HealthResponse(s3 = "connected")

        // Then
        assertEquals("connected", response.s3)
    }

    @Test
    fun healthResponse_withS3Boolean_createsCorrectly() {
        // When
        val response = HealthResponse(s3 = true)

        // Then
        assertEquals(true, response.s3)
    }

    @Test
    fun healthResponse_withS3Map_createsCorrectly() {
        // When
        val s3Status = mapOf("status" to "ok", "latency" to "10ms")
        val response = HealthResponse(s3 = s3Status)

        // Then
        assertEquals(s3Status, response.s3)
    }

    @Test
    fun healthResponse_withDbString_createsCorrectly() {
        // When
        val response = HealthResponse(db = "connected")

        // Then
        assertEquals("connected", response.db)
    }

    @Test
    fun healthResponse_withDbBoolean_createsCorrectly() {
        // When
        val response = HealthResponse(db = false)

        // Then
        assertEquals(false, response.db)
    }

    @Test
    fun healthResponse_withAsOf_createsCorrectly() {
        // When
        val response = HealthResponse(asOf = "2024-01-15T10:00:00Z")

        // Then
        assertEquals("2024-01-15T10:00:00Z", response.asOf)
    }

    @Test
    fun healthResponse_withAllFields_createsCorrectly() {
        // When
        val response = HealthResponse(
            api = "healthy",
            s3 = "connected",
            db = "connected",
            asOf = "2024-01-15"
        )

        // Then
        assertEquals("healthy", response.api)
        assertEquals("connected", response.s3)
        assertEquals("connected", response.db)
        assertEquals("2024-01-15", response.asOf)
    }

    @Test
    fun healthResponse_equality_sameValues() {
        // When
        val response1 = HealthResponse(api = "healthy", s3 = true)
        val response2 = HealthResponse(api = "healthy", s3 = true)

        // Then
        assertEquals(response1, response2)
    }

    @Test
    fun healthResponse_copy_works() {
        // When
        val original = HealthResponse(api = "healthy")
        val copied = original.copy(api = "unhealthy")

        // Then
        assertEquals("unhealthy", copied.api)
    }

    @Test
    fun healthResponse_toString_works() {
        // When
        val response = HealthResponse(api = "healthy")

        // Then
        assertNotNull(response.toString())
        assertTrue(response.toString().contains("HealthResponse"))
    }

    @Test
    fun healthResponse_withEmptyStrings_createsCorrectly() {
        // When
        val response = HealthResponse(api = "", asOf = "")

        // Then
        assertEquals("", response.api)
        assertEquals("", response.asOf)
    }

    @Test
    fun healthResponse_withWhitespace_createsCorrectly() {
        // When
        val response = HealthResponse(api = "  healthy  ")

        // Then
        assertEquals("  healthy  ", response.api)
    }

    @Test
    fun healthResponse_withSpecialCharacters_createsCorrectly() {
        // When
        val response = HealthResponse(api = "健康状態OK")

        // Then
        assertEquals("健康状態OK", response.api)
    }

    // ===== 통합 테스트 =====

    @Test
    fun apiResponse_nestedWithHealthResponse_works() {
        // When
        val health = HealthResponse(api = "healthy")
        val response = ApiResponse(items = listOf(health))

        // Then
        assertEquals(1, response.items?.size)
        assertEquals("healthy", response.items?.first()?.api)
    }

    @Test
    fun apiResponse_multipleTypes_differentInstances() {
        // When
        val stringResponse = ApiResponse<String>(total = 1)
        val intResponse = ApiResponse<Int>(total = 2)

        // Then
        assertNotEquals(stringResponse, intResponse)
    }

    @Test
    fun apiResponse_hashCode_consistency() {
        // When
        val response = ApiResponse(items = listOf("A"), total = 1)
        val hash1 = response.hashCode()
        val hash2 = response.hashCode()

        // Then
        assertEquals(hash1, hash2)
    }

    @Test
    fun healthResponse_hashCode_consistency() {
        // When
        val response = HealthResponse(api = "healthy")
        val hash1 = response.hashCode()
        val hash2 = response.hashCode()

        // Then
        assertEquals(hash1, hash2)
    }
}