package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class StockOverviewDtoTest {

    @Test
    fun create_withAllFields() {
        val dto = StockOverviewDto(
            asOfDate = "2024-01-15",
            summary = "Stock is performing well",
            fundamental = "Strong fundamentals",
            technical = "Bullish trend",
            news = listOf("News 1", "News 2")
        )
        assertEquals("2024-01-15", dto.asOfDate)
        assertEquals("Stock is performing well", dto.summary)
        assertEquals("Strong fundamentals", dto.fundamental)
        assertEquals("Bullish trend", dto.technical)
        assertEquals(listOf("News 1", "News 2"), dto.news)
    }

    @Test
    fun create_withDefaultNullValues() {
        val dto = StockOverviewDto()
        assertNull(dto.asOfDate)
        assertNull(dto.summary)
        assertNull(dto.fundamental)
        assertNull(dto.technical)
        assertNull(dto.news)
    }

    @Test
    fun create_withPartialValues() {
        val dto = StockOverviewDto(
            summary = "Summary only"
        )
        assertNull(dto.asOfDate)
        assertEquals("Summary only", dto.summary)
        assertNull(dto.fundamental)
        assertNull(dto.technical)
        assertNull(dto.news)
    }

    @Test
    fun news_emptyList() {
        val dto = StockOverviewDto(news = emptyList())
        assertNotNull(dto.news)
        assertTrue(dto.news!!.isEmpty())
    }

    @Test
    fun news_multipleItems() {
        val newsList = listOf("Breaking news", "Market update", "Analyst report")
        val dto = StockOverviewDto(news = newsList)
        assertEquals(3, dto.news!!.size)
        assertEquals("Breaking news", dto.news!![0])
        assertEquals("Market update", dto.news!![1])
        assertEquals("Analyst report", dto.news!![2])
    }

    @Test
    fun equality_sameValues() {
        val dto1 = StockOverviewDto("2024-01-15", "summary", "fund", "tech", listOf("news"))
        val dto2 = StockOverviewDto("2024-01-15", "summary", "fund", "tech", listOf("news"))
        assertEquals(dto1, dto2)
    }

    @Test
    fun inequality_differentDate() {
        val dto1 = StockOverviewDto(asOfDate = "2024-01-15")
        val dto2 = StockOverviewDto(asOfDate = "2024-01-16")
        assertNotEquals(dto1, dto2)
    }

    @Test
    fun inequality_differentSummary() {
        val dto1 = StockOverviewDto(summary = "Summary 1")
        val dto2 = StockOverviewDto(summary = "Summary 2")
        assertNotEquals(dto1, dto2)
    }

    @Test
    fun hashCode_consistency() {
        val dto1 = StockOverviewDto("2024-01-15", "summary", null, null, null)
        val dto2 = StockOverviewDto("2024-01-15", "summary", null, null, null)
        assertEquals(dto1.hashCode(), dto2.hashCode())
    }

    @Test
    fun copy_modifiesAsOfDate() {
        val original = StockOverviewDto(asOfDate = "2024-01-15")
        val copied = original.copy(asOfDate = "2024-01-16")
        assertEquals("2024-01-16", copied.asOfDate)
    }

    @Test
    fun copy_modifiesSummary() {
        val original = StockOverviewDto(summary = "Original")
        val copied = original.copy(summary = "Updated")
        assertEquals("Updated", copied.summary)
    }

    @Test
    fun copy_preservesOtherFields() {
        val original = StockOverviewDto(
            asOfDate = "2024-01-15",
            summary = "summary",
            fundamental = "fundamental",
            technical = "technical",
            news = listOf("news")
        )
        val copied = original.copy(summary = "new summary")
        assertEquals("2024-01-15", copied.asOfDate)
        assertEquals("fundamental", copied.fundamental)
        assertEquals("technical", copied.technical)
        assertEquals(listOf("news"), copied.news)
    }

    @Test
    fun toString_containsFields() {
        val dto = StockOverviewDto(asOfDate = "2024-01-15", summary = "test")
        val str = dto.toString()
        assertTrue(str.contains("2024-01-15"))
        assertTrue(str.contains("test"))
    }

    @Test
    fun destructuring() {
        val dto = StockOverviewDto("date", "sum", "fund", "tech", listOf("n"))
        val (date, sum, fund, tech, news) = dto
        assertEquals("date", date)
        assertEquals("sum", sum)
        assertEquals("fund", fund)
        assertEquals("tech", tech)
        assertEquals(listOf("n"), news)
    }

    // ===== Additional Tests =====

    @Test
    fun koreanContent_handledCorrectly() {
        val dto = StockOverviewDto(
            asOfDate = "2024-01-15",
            summary = "삼성전자 주가가 상승세를 보이고 있습니다.",
            fundamental = "기업 실적이 개선되고 있습니다.",
            technical = "RSI 지표가 상승 추세를 나타냅니다.",
            news = listOf("반도체 시장 전망 긍정적", "AI 수요 증가")
        )
        assertEquals("삼성전자 주가가 상승세를 보이고 있습니다.", dto.summary)
        assertEquals(2, dto.news?.size)
    }

    @Test
    fun longContent_handledCorrectly() {
        val longSummary = "A".repeat(10000)
        val dto = StockOverviewDto(summary = longSummary)
        assertEquals(10000, dto.summary?.length)
    }

    @Test
    fun news_filterOperation() {
        val dto = StockOverviewDto(
            news = listOf("긍정 뉴스", "부정 뉴스", "중립 뉴스")
        )
        val filteredNews = dto.news?.filter { it.contains("긍정") }
        assertEquals(1, filteredNews?.size)
        assertEquals("긍정 뉴스", filteredNews?.get(0))
    }

    @Test
    fun news_mapOperation() {
        val dto = StockOverviewDto(
            news = listOf("news1", "news2", "news3")
        )
        val upperCaseNews = dto.news?.map { it.uppercase() }
        assertEquals(listOf("NEWS1", "NEWS2", "NEWS3"), upperCaseNews)
    }

    @Test
    fun fundamentalOnly() {
        val dto = StockOverviewDto(fundamental = "Strong balance sheet")
        assertNull(dto.asOfDate)
        assertNull(dto.summary)
        assertEquals("Strong balance sheet", dto.fundamental)
        assertNull(dto.technical)
        assertNull(dto.news)
    }

    @Test
    fun technicalOnly() {
        val dto = StockOverviewDto(technical = "Golden cross pattern")
        assertNull(dto.asOfDate)
        assertNull(dto.summary)
        assertNull(dto.fundamental)
        assertEquals("Golden cross pattern", dto.technical)
        assertNull(dto.news)
    }

    @Test
    fun copy_allFields() {
        val original = StockOverviewDto("2024-01-15", "sum", "fund", "tech", listOf("n"))
        val copied = original.copy(
            asOfDate = "2024-01-16",
            summary = "new sum",
            fundamental = "new fund",
            technical = "new tech",
            news = listOf("new news")
        )
        assertEquals("2024-01-16", copied.asOfDate)
        assertEquals("new sum", copied.summary)
        assertEquals("new fund", copied.fundamental)
        assertEquals("new tech", copied.technical)
        assertEquals(listOf("new news"), copied.news)
    }

    @Test
    fun specialCharacters_inContent() {
        val dto = StockOverviewDto(
            summary = "Price: $100.50 (+5.5%)",
            fundamental = "P/E ratio: 15.5",
            technical = "Support level: ₩70,000"
        )
        assertEquals("Price: $100.50 (+5.5%)", dto.summary)
        assertEquals("P/E ratio: 15.5", dto.fundamental)
        assertEquals("Support level: ₩70,000", dto.technical)
    }

    @Test
    fun emptyStrings_handledCorrectly() {
        val dto = StockOverviewDto(
            asOfDate = "",
            summary = "",
            fundamental = "",
            technical = "",
            news = emptyList()
        )
        assertEquals("", dto.asOfDate)
        assertEquals("", dto.summary)
        assertEquals("", dto.fundamental)
        assertEquals("", dto.technical)
        assertTrue(dto.news!!.isEmpty())
    }
}
