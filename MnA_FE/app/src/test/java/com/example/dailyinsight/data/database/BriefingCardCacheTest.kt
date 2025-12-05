package com.example.dailyinsight.data.database

import org.junit.Assert.*
import org.junit.Test

class BriefingCardCacheTest {

    // ===== toDto Tests =====

    @Test
    fun toDto_convertsAllFields() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = "삼성전자 실적 호조",
            label = "상승",
            confidence = 0.85,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = true
        )

        val dto = cache.toDto()

        assertEquals("005930", dto.ticker)
        assertEquals("삼성전자", dto.name)
        assertEquals(72000L, dto.price)
        assertEquals(1000L, dto.change)
        assertEquals(1.5, dto.changeRate, 0.01)
        assertEquals("삼성전자 실적 호조", dto.headline)
        assertTrue(dto.isFavorite)
    }

    @Test
    fun toDto_withNullHeadline_convertsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val dto = cache.toDto()

        assertNull(dto.headline)
        assertFalse(dto.isFavorite)
    }

    @Test
    fun toDto_withNegativeChange_convertsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "000660",
            name = "SK하이닉스",
            price = 150000L,
            change = -2000L,
            changeRate = -1.3,
            headline = "반도체 시장 둔화",
            label = "하락",
            confidence = 0.75,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val dto = cache.toDto()

        assertEquals(-2000L, dto.change)
        assertEquals(-1.3, dto.changeRate, 0.01)
    }

    @Test
    fun toDto_withZeroValues_convertsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "123456",
            name = "테스트 주식",
            price = 0L,
            change = 0L,
            changeRate = 0.0,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = 0L,
            isFavorite = false
        )

        val dto = cache.toDto()

        assertEquals(0L, dto.price)
        assertEquals(0L, dto.change)
        assertEquals(0.0, dto.changeRate, 0.01)
    }

    @Test
    fun toDto_isFavoriteTrue_preservesValue() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = "테스트",
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = true
        )

        val dto = cache.toDto()

        assertTrue(dto.isFavorite)
    }

    @Test
    fun toDto_isFavoriteFalse_preservesValue() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = "테스트",
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val dto = cache.toDto()

        assertFalse(dto.isFavorite)
    }

    // ===== Data Class Default Values =====

    @Test
    fun defaultIsFavorite_isFalse() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis()
        )

        assertFalse(cache.isFavorite)
    }

    // ===== Equality Tests =====

    @Test
    fun equality_sameTicker_areEqual() {
        val cache1 = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = "테스트",
            label = null,
            confidence = null,
            fetchedAt = 1000L,
            isFavorite = true
        )
        val cache2 = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = "테스트",
            label = null,
            confidence = null,
            fetchedAt = 1000L,
            isFavorite = true
        )

        assertEquals(cache1, cache2)
    }

    @Test
    fun equality_differentTicker_areNotEqual() {
        val cache1 = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = 1000L
        )
        val cache2 = BriefingCardCache(
            ticker = "000660",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = 1000L
        )

        assertNotEquals(cache1, cache2)
    }

    // ===== New Fields Tests =====

    @Test
    fun marketCap_defaultsToNull() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis()
        )

        assertNull(cache.marketCap)
    }

    @Test
    fun marketCap_withValue_createsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            marketCap = 430000000000000L
        )

        assertEquals(430000000000000L, cache.marketCap)
    }

    @Test
    fun rank_defaultsToNull() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis()
        )

        assertNull(cache.rank)
    }

    @Test
    fun rank_withValue_createsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            rank = 1
        )

        assertEquals(1, cache.rank)
    }

    @Test
    fun industry_defaultsToNull() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis()
        )

        assertNull(cache.industry)
    }

    @Test
    fun industry_withValue_createsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            industry = "전기.전자"
        )

        assertEquals("전기.전자", cache.industry)
    }

    @Test
    fun allOptionalFields_withValues_createsCorrectly() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = "테스트 헤드라인",
            label = "상승",
            confidence = 0.95,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = true,
            marketCap = 430000000000000L,
            rank = 1,
            industry = "전기.전자"
        )

        assertEquals("테스트 헤드라인", cache.headline)
        assertEquals("상승", cache.label)
        assertEquals(0.95, cache.confidence!!, 0.01)
        assertTrue(cache.isFavorite)
        assertEquals(430000000000000L, cache.marketCap)
        assertEquals(1, cache.rank)
        assertEquals("전기.전자", cache.industry)
    }

    @Test
    fun copy_modifiesRank() {
        val original = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            rank = 1
        )

        val copied = original.copy(rank = 2)

        assertEquals(2, copied.rank)
        assertEquals("005930", copied.ticker)
    }

    @Test
    fun copy_modifiesIsFavorite() {
        val original = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val copied = original.copy(isFavorite = true)

        assertTrue(copied.isFavorite)
    }

    @Test
    fun hashCode_consistency() {
        val cache1 = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = 1000L
        )
        val cache2 = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = 1000L
        )

        assertEquals(cache1.hashCode(), cache2.hashCode())
    }

    @Test
    fun toString_containsFields() {
        val cache = BriefingCardCache(
            ticker = "005930",
            name = "삼성전자",
            price = 72000L,
            change = 1000L,
            changeRate = 1.5,
            headline = null,
            label = null,
            confidence = null,
            fetchedAt = 1000L
        )

        val str = cache.toString()
        assertTrue(str.contains("005930"))
        assertTrue(str.contains("삼성전자"))
    }
}