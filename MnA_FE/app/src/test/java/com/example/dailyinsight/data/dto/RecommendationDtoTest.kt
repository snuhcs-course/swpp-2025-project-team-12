package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class RecommendationDtoTest {

    @Test
    fun recommendationDto_withAllFields() {
        val dto = RecommendationDto(
            ticker = "005930",
            name = "삼성전자",
            price = 70000L,
            change = -100L,
            changeRate = -0.14,
            headline = "삼성전자, 실적 발표"
        )
        assertEquals("005930", dto.ticker)
        assertEquals("삼성전자", dto.name)
        assertEquals(70000L, dto.price)
        assertEquals(-100L, dto.change)
        assertEquals(-0.14, dto.changeRate, 0.001)
        assertEquals("삼성전자, 실적 발표", dto.headline)
    }

    @Test
    fun recommendationDto_nullHeadline() {
        val dto = RecommendationDto(
            ticker = "005930",
            name = "삼성전자",
            price = 70000L,
            change = -100L,
            changeRate = -0.14
        )
        assertNull(dto.headline)
    }

    @Test
    fun recommendationDto_equality() {
        val d1 = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        val d2 = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        assertEquals(d1, d2)
    }

    @Test
    fun recommendationDto_copy() {
        val original = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        val copied = original.copy(price = 80000L)
        assertEquals(80000L, copied.price)
        assertEquals("005930", copied.ticker)
    }

    @Test
    fun recommendationDto_toString() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        assertNotNull(dto.toString())
        assertTrue(dto.toString().contains("RecommendationDto"))
    }

    @Test
    fun recommendationDto_hashCode() {
        val d1 = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        val d2 = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun recommendationDto_positiveChange() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, 100L, 0.14)
        assertEquals(100L, dto.change)
        assertEquals(0.14, dto.changeRate, 0.001)
        assertTrue(dto.change > 0)
    }

    @Test
    fun recommendationDto_negativeChange() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, -100L, -0.14)
        assertEquals(-100L, dto.change)
        assertEquals(-0.14, dto.changeRate, 0.001)
        assertTrue(dto.change < 0)
    }

    @Test
    fun recommendationDto_zeroChange() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, 0L, 0.0)
        assertEquals(0L, dto.change)
        assertEquals(0.0, dto.changeRate, 0.001)
    }

    @Test
    fun recommendationDto_emptyTicker() {
        val dto = RecommendationDto("", "회사명", 70000L, 0L, 0.0)
        assertEquals("", dto.ticker)
    }

    @Test
    fun recommendationDto_emptyName() {
        val dto = RecommendationDto("005930", "", 70000L, 0L, 0.0)
        assertEquals("", dto.name)
    }

    @Test
    fun recommendationDto_longTicker() {
        val longTicker = "A".repeat(100)
        val dto = RecommendationDto(longTicker, "회사", 70000L, 0L, 0.0)
        assertEquals(100, dto.ticker.length)
    }

    @Test
    fun recommendationDto_longName() {
        val longName = "회사".repeat(100)
        val dto = RecommendationDto("005930", longName, 70000L, 0L, 0.0)
        assertTrue(dto.name.length > 100)
    }

    @Test
    fun recommendationDto_longHeadline() {
        val longHeadline = "뉴스 ".repeat(1000)
        val dto = RecommendationDto("005930", "삼성전자", 70000L, 0L, 0.0, longHeadline)
        assertTrue(dto.headline!!.length > 1000)
    }

    @Test
    fun recommendationDto_largePrice() {
        val dto = RecommendationDto("005930", "삼성전자", Long.MAX_VALUE, 0L, 0.0)
        assertEquals(Long.MAX_VALUE, dto.price)
    }

    @Test
    fun recommendationDto_largeChange() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, Long.MAX_VALUE, 100.0)
        assertEquals(Long.MAX_VALUE, dto.change)
    }

    @Test
    fun recommendationDto_extremeChangeRate() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, 0L, Double.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, dto.changeRate, 0.001)
    }

    @Test
    fun recommendationDto_negativePrice() {
        val dto = RecommendationDto("005930", "삼성전자", -1000L, 0L, 0.0)
        assertEquals(-1000L, dto.price)
    }

    @Test
    fun recommendationDto_specialCharactersInName() {
        val dto = RecommendationDto("005930", "삼성전자(주)", 70000L, 0L, 0.0)
        assertEquals("삼성전자(주)", dto.name)
    }

    @Test
    fun recommendationDto_unicodeHeadline() {
        val dto = RecommendationDto("005930", "삼성전자", 70000L, 0L, 0.0, "🚀 주가 상승!")
        assertEquals("🚀 주가 상승!", dto.headline)
    }
}