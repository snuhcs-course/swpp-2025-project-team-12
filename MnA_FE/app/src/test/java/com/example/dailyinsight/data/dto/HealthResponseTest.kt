package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class HealthResponseTest {

    @Test
    fun healthResponse_creates() {
        val response = HealthResponse("ok", "2025-10-15T05:19:00Z")
        assertEquals("ok", response.status)
        assertEquals("2025-10-15T05:19:00Z", response.timestamp)
    }

    @Test
    fun healthResponse_equality() {
        val r1 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val r2 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        assertEquals(r1, r2)
    }

    @Test
    fun healthResponse_copy() {
        val original = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val copied = original.copy(status = "error")
        assertEquals("error", copied.status)
    }

    @Test
    fun healthResponse_toString() {
        val response = HealthResponse("ok", "2025-10-15T05:19:00Z")
        assertNotNull(response.toString())
    }

    @Test
    fun healthResponse_hashCode() {
        val r1 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val r2 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun healthResponse_components() {
        val response = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val (status, timestamp) = response
        assertEquals("ok", status)
        assertEquals("2025-10-15T05:19:00Z", timestamp)
    }

    @Test
    fun healthResponse_errorStatus() {
        val response = HealthResponse("error", "2025-10-15T05:19:00Z")
        assertEquals("error", response.status)
    }

    @Test
    fun healthResponse_emptyStatus() {
        val response = HealthResponse("", "2025-10-15T05:19:00Z")
        assertEquals("", response.status)
    }

    @Test
    fun healthResponse_differentTimestampFormat() {
        val response = HealthResponse("ok", "2025-10-15")
        assertEquals("2025-10-15", response.timestamp)
    }

    @Test
    fun healthResponse_longTimestamp() {
        val longTs = "A".repeat(100)
        val response = HealthResponse("ok", longTs)
        assertEquals(100, response.timestamp.length)
    }
}