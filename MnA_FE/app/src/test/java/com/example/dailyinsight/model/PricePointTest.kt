package com.example.dailyinsight.data.model

import org.junit.Assert.*
import org.junit.Test

class PricePointTest {

    @Test
    fun pricePoint_creates() {
        val point = PricePoint("2024-01-01", 2500.0)
        assertEquals("2024-01-01", point.date)
        assertEquals(2500.0, point.close, 0.001)
    }

    @Test
    fun pricePoint_equality() {
        val p1 = PricePoint("2024-01-01", 2500.0)
        val p2 = PricePoint("2024-01-01", 2500.0)
        assertEquals(p1, p2)
    }

    @Test
    fun pricePoint_copy() {
        val original = PricePoint("2024-01-01", 2500.0)
        val copied = original.copy(close = 3000.0)
        assertEquals(3000.0, copied.close, 0.001)
    }

    @Test
    fun pricePoint_toString() {
        val point = PricePoint("2024-01-01", 2500.0)
        assertNotNull(point.toString())
    }

    @Test
    fun pricePoint_hashCode() {
        val p1 = PricePoint("2024-01-01", 2500.0)
        val p2 = PricePoint("2024-01-01", 2500.0)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun pricePoint_components() {
        val point = PricePoint("2024-01-01", 2500.0)
        val (date, close) = point
        assertEquals("2024-01-01", date)
        assertEquals(2500.0, close, 0.001)
    }

    @Test
    fun pricePoint_negativeClose() {
        val point = PricePoint("2024-01-01", -100.0)
        assertEquals(-100.0, point.close, 0.001)
    }

    @Test
    fun pricePoint_zeroClose() {
        val point = PricePoint("2024-01-01", 0.0)
        assertEquals(0.0, point.close, 0.001)
    }

    @Test
    fun pricePoint_emptyDate() {
        val point = PricePoint("", 2500.0)
        assertEquals("", point.date)
    }

    @Test
    fun pricePoint_largeValue() {
        val point = PricePoint("2024-01-01", Double.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, point.close, 0.001)
    }
}