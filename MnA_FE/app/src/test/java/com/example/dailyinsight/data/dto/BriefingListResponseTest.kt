package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class BriefingListResponseTest {

    @Test
    fun briefingItemDto_withAllFields_createsSuccessfully() {
        val item = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            summary = "AI 반도체 수요 증가로 실적 기대",
            overview = null,
            marketCap = 430000000000000L
        )

        assertEquals("005930", item.ticker)
        assertEquals("삼성전자", item.name)
        assertEquals("72000", item.close)
        assertEquals("-100", item.change)
        assertEquals("-0.14", item.changeRate)
        assertEquals("AI 반도체 수요 증가로 실적 기대", item.summary)
        assertNull(item.overview)
        assertEquals(430000000000000L, item.marketCap)
    }

    @Test
    fun briefingItemDto_withNullOptionalFields_createsSuccessfully() {
        val item = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = null,
            change = null,
            changeRate = null,
            summary = null,
            overview = null,
            marketCap = null
        )

        assertEquals("005930", item.ticker)
        assertEquals("삼성전자", item.name)
        assertNull(item.close)
        assertNull(item.change)
        assertNull(item.changeRate)
        assertNull(item.summary)
        assertNull(item.overview)
        assertNull(item.marketCap)
    }

    @Test
    fun briefingItemDto_summaryDefaultsToNull() {
        val item = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "100",
            changeRate = "0.14",
            overview = null,
            marketCap = 430000000000000L
        )

        assertNull(item.summary)
    }

    @Test
    fun briefingListResponse_withItems_createsSuccessfully() {
        val items = listOf(
            BriefingItemDto(
                ticker = "005930",
                name = "삼성전자",
                close = "72000",
                change = "-100",
                changeRate = "-0.14",
                overview = null,
                marketCap = 430000000000000L
            ),
            BriefingItemDto(
                ticker = "000660",
                name = "SK하이닉스",
                close = "180000",
                change = "2000",
                changeRate = "1.12",
                overview = null,
                marketCap = 130000000000000L
            )
        )

        val response = BriefingListResponse(
            items = items,
            total = 100,
            limit = 10,
            offset = 0,
            source = "api",
            asOf = "2024-01-15"
        )

        assertEquals(2, response.items.size)
        assertEquals(100, response.total)
        assertEquals(10, response.limit)
        assertEquals(0, response.offset)
        assertEquals("api", response.source)
        assertEquals("2024-01-15", response.asOf)
    }

    @Test
    fun briefingListResponse_withEmptyItems_createsSuccessfully() {
        val response = BriefingListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = "cache",
            asOf = null
        )

        assertTrue(response.items.isEmpty())
        assertEquals(0, response.total)
        assertNull(response.asOf)
    }

    @Test
    fun briefingListResponse_withNullSourceAndAsOf_createsSuccessfully() {
        val response = BriefingListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = null,
            asOf = null
        )

        assertNull(response.source)
        assertNull(response.asOf)
    }

    @Test
    fun briefingListResponse_pagination_works() {
        val response = BriefingListResponse(
            items = emptyList(),
            total = 100,
            limit = 20,
            offset = 40,
            source = "api",
            asOf = "2024-01-15"
        )

        assertEquals(100, response.total)
        assertEquals(20, response.limit)
        assertEquals(40, response.offset)
    }

    @Test
    fun briefingItemDto_dataClassEquality_works() {
        val item1 = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            overview = null,
            marketCap = 430000000000000L
        )

        val item2 = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            overview = null,
            marketCap = 430000000000000L
        )

        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    // ===== Additional Tests =====

    @Test
    fun briefingItemDto_copy() {
        val original = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            overview = null,
            marketCap = 430000000000000L
        )
        val copied = original.copy(name = "Samsung")
        assertEquals("Samsung", copied.name)
        assertEquals("005930", copied.ticker)
    }

    @Test
    fun briefingItemDto_toString() {
        val item = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            overview = null,
            marketCap = 430000000000000L
        )
        val str = item.toString()
        assertTrue(str.contains("005930"))
        assertTrue(str.contains("삼성전자"))
    }

    @Test
    fun briefingItemDto_destructuring() {
        val item = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            summary = "테스트 요약",
            overview = null,
            marketCap = 430000000000000L
        )
        val (ticker, name, close, change, changeRate, summary, overview, marketCap) = item
        assertEquals("005930", ticker)
        assertEquals("삼성전자", name)
        assertEquals("72000", close)
        assertEquals("-100", change)
        assertEquals("-0.14", changeRate)
        assertEquals("테스트 요약", summary)
        assertNull(overview)
        assertEquals(430000000000000L, marketCap)
    }

    @Test
    fun briefingListResponse_equality() {
        val items = listOf(BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L))
        val response1 = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val response2 = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        assertEquals(response1, response2)
    }

    @Test
    fun briefingListResponse_hashCode() {
        val items = listOf(BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L))
        val response1 = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val response2 = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun briefingListResponse_copy() {
        val items = listOf(BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L))
        val original = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val copied = original.copy(total = 200)
        assertEquals(200, copied.total)
        assertEquals("api", copied.source)
    }

    @Test
    fun briefingListResponse_destructuring() {
        val items = listOf(BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L))
        val response = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val (itemsList, total, limit, offset, source, asOf) = response
        assertEquals(items, itemsList)
        assertEquals(100, total)
        assertEquals(10, limit)
        assertEquals(0, offset)
        assertEquals("api", source)
        assertEquals("2024-01-15", asOf)
    }

    @Test
    fun briefingListResponse_toString() {
        val items = listOf(BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L))
        val response = BriefingListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val str = response.toString()
        assertTrue(str.contains("100"))
        assertTrue(str.contains("api"))
    }

    @Test
    fun briefingItemDto_withOverview() {
        val overview = StockOverviewDto(
            asOfDate = "2024-01-15",
            summary = "AI 반도체 수요 증가로 실적 기대",
            fundamental = "긍정적 분석",
            technical = "상승 추세",
            news = listOf("뉴스1", "뉴스2")
        )
        val item = BriefingItemDto(
            ticker = "005930",
            name = "삼성전자",
            close = "72000",
            change = "-100",
            changeRate = "-0.14",
            overview = overview,
            marketCap = 430000000000000L
        )
        assertNotNull(item.overview)
        assertEquals("2024-01-15", item.overview?.asOfDate)
    }

    @Test
    fun briefingListResponse_filterByTicker() {
        val items = listOf(
            BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L),
            BriefingItemDto("000660", "SK하이닉스", "180000", "2000", "1.12", null, null, 130000000000000L)
        )
        val response = BriefingListResponse(items, 2, 10, 0, "api", "2024-01-15")
        val samsungItems = response.items.filter { it.ticker == "005930" }
        assertEquals(1, samsungItems.size)
        assertEquals("삼성전자", samsungItems[0].name)
    }

    @Test
    fun briefingListResponse_accessByIndex() {
        val items = listOf(
            BriefingItemDto("005930", "삼성전자", "72000", "-100", "-0.14", null, null, 430000000000000L),
            BriefingItemDto("000660", "SK하이닉스", "180000", "2000", "1.12", null, null, 130000000000000L)
        )
        val response = BriefingListResponse(items, 2, 10, 0, "api", "2024-01-15")
        assertEquals("005930", response.items[0].ticker)
        assertEquals("SK하이닉스", response.items[1].name)
    }
}
