package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class RemainingDtoTest {

    // ===== HealthResponse (10개) =====
    
    @Test
    fun healthResponse_createsCorrectly() {
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
    fun healthResponse_differentStatuses() {
        val ok = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val error = HealthResponse("error", "2025-10-15T05:19:00Z")
        assertNotEquals(ok.status, error.status)
    }

    @Test
    fun healthResponse_emptyStrings() {
        val response = HealthResponse("", "")
        assertEquals("", response.status)
        assertEquals("", response.timestamp)
    }

    @Test
    fun healthResponse_toString() {
        val response = HealthResponse("ok", "2025-10-15T05:19:00Z")
        assertNotNull(response.toString())
        assertTrue(response.toString().contains("HealthResponse"))
    }

    @Test
    fun healthResponse_hashCode() {
        val r1 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val r2 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun healthResponse_differentTimestamps() {
        val r1 = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val r2 = HealthResponse("ok", "2025-10-16T05:19:00Z")
        assertNotEquals(r1, r2)
    }

    @Test
    fun healthResponse_withMilliseconds() {
        val response = HealthResponse("ok", "2025-10-15T05:19:00.123Z")
        assertEquals("2025-10-15T05:19:00.123Z", response.timestamp)
    }

    @Test
    fun healthResponse_statusCaseSensitive() {
        val ok = HealthResponse("ok", "2025-10-15T05:19:00Z")
        val OK = HealthResponse("OK", "2025-10-15T05:19:00Z")
        assertNotEquals(ok.status, OK.status)
    }

    // ===== LLMSummaryResponse (10개) =====

    @Test
    fun llmSummaryResponse_createsCorrectly() {
        val response = LLMSummaryResponse("LLM output text")
        assertEquals("LLM output text", response.llmOutput)
    }

    @Test
    fun llmSummaryResponse_emptyOutput() {
        val response = LLMSummaryResponse("")
        assertEquals("", response.llmOutput)
    }

    @Test
    fun llmSummaryResponse_longOutput() {
        val longText = "A".repeat(10000)
        val response = LLMSummaryResponse(longText)
        assertEquals(10000, response.llmOutput.length)
    }

    @Test
    fun llmSummaryResponse_withJSON() {
        val jsonOutput = """{"key": "value"}"""
        val response = LLMSummaryResponse(jsonOutput)
        assertTrue(response.llmOutput.contains("key"))
    }

    @Test
    fun llmSummaryResponse_equality() {
        val r1 = LLMSummaryResponse("output")
        val r2 = LLMSummaryResponse("output")
        assertEquals(r1, r2)
    }

    @Test
    fun llmSummaryResponse_copy() {
        val original = LLMSummaryResponse("old")
        val copied = original.copy(llmOutput = "new")
        assertEquals("new", copied.llmOutput)
    }

    @Test
    fun llmSummaryResponse_withNewlines() {
        val output = "Line1\nLine2\nLine3"
        val response = LLMSummaryResponse(output)
        assertTrue(response.llmOutput.contains("\n"))
    }

    @Test
    fun llmSummaryResponse_withUnicode() {
        val response = LLMSummaryResponse("한글 출력 🚀")
        assertEquals("한글 출력 🚀", response.llmOutput)
    }

    @Test
    fun llmSummaryResponse_toString() {
        val response = LLMSummaryResponse("output")
        assertNotNull(response.toString())
    }

    @Test
    fun llmSummaryResponse_hashCode() {
        val r1 = LLMSummaryResponse("output")
        val r2 = LLMSummaryResponse("output")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    // ===== MarketSummary (15개) =====

    @Test
    fun marketSummary_createsCorrectly() {
        val summary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "bullish",
            confidence = 0.85,
            summary = "Market is strong",
            signals = listOf("signal1", "signal2"),
            drivers = listOf("driver1"),
            risks = listOf("risk1")
        )
        
        assertEquals("KOSPI", summary.market)
        assertEquals("2024-01-15", summary.asofDate)
        assertEquals("bullish", summary.label)
        assertEquals(0.85, summary.confidence, 0.001)
        assertEquals("Market is strong", summary.summary)
        assertEquals(2, summary.signals.size)
        assertEquals(1, summary.drivers.size)
        assertEquals(1, summary.risks.size)
    }

    @Test
    fun marketSummary_emptyLists() {
        val summary = MarketSummary(
            market = "KOSDAQ",
            asofDate = "2024-01-15",
            label = "neutral",
            confidence = 0.5,
            summary = "Summary",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )
        
        assertTrue(summary.signals.isEmpty())
        assertTrue(summary.drivers.isEmpty())
        assertTrue(summary.risks.isEmpty())
    }

    @Test
    fun marketSummary_highConfidence() {
        val summary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "bullish",
            confidence = 1.0,
            summary = "Very confident",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )
        
        assertEquals(1.0, summary.confidence, 0.001)
    }

    @Test
    fun marketSummary_lowConfidence() {
        val summary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "uncertain",
            confidence = 0.0,
            summary = "Uncertain",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )
        
        assertEquals(0.0, summary.confidence, 0.001)
    }

    @Test
    fun marketSummary_multipleSignals() {
        val signals = (1..10).map { "signal$it" }
        val summary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "bullish",
            confidence = 0.9,
            summary = "Strong",
            signals = signals,
            drivers = emptyList(),
            risks = emptyList()
        )
        
        assertEquals(10, summary.signals.size)
    }

    @Test
    fun marketSummary_equality() {
        val s1 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        val s2 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        assertEquals(s1, s2)
    }

    @Test
    fun marketSummary_copy() {
        val original = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        val copied = original.copy(label = "bearish")
        assertEquals("bearish", copied.label)
        assertEquals("KOSPI", copied.market)
    }

    @Test
    fun marketSummary_differentMarkets() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        assertNotEquals(kospi.market, kosdaq.market)
    }

    @Test
    fun marketSummary_toString() {
        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        assertNotNull(summary.toString())
        assertTrue(summary.toString().contains("MarketSummary"))
    }

    @Test
    fun marketSummary_hashCode() {
        val s1 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        val s2 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Summary", emptyList(), emptyList(), emptyList())
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun marketSummary_withLongSummary() {
        val longSummary = "A".repeat(5000)
        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, longSummary, emptyList(), emptyList(), emptyList())
        assertEquals(5000, summary.summary.length)
    }

    @Test
    fun marketSummary_confidencePrecision() {
        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.123456789, "Summary", emptyList(), emptyList(), emptyList())
        assertEquals(0.123456789, summary.confidence, 0.0000001)
    }

    @Test
    fun marketSummary_allFieldsFilled() {
        val summary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15T10:00:00Z",
            label = "strongly_bullish",
            confidence = 0.95,
            summary = "Very strong market with positive sentiment",
            signals = listOf("high_volume", "price_breakout", "positive_news"),
            drivers = listOf("tech_stocks", "export_growth"),
            risks = listOf("inflation", "geopolitical")
        )
        
        assertNotNull(summary.market)
        assertNotNull(summary.asofDate)
        assertNotNull(summary.label)
        assertTrue(summary.confidence > 0)
        assertFalse(summary.signals.isEmpty())
        assertFalse(summary.drivers.isEmpty())
        assertFalse(summary.risks.isEmpty())
    }

    @Test
    fun marketSummary_koreanText() {
        val summary = MarketSummary(
            market = "코스피",
            asofDate = "2024-01-15",
            label = "상승장",
            confidence = 0.9,
            summary = "시장이 강세를 보이고 있습니다",
            signals = listOf("거래량 증가"),
            drivers = listOf("수출 증가"),
            risks = listOf("인플레이션")
        )
        
        assertEquals("코스피", summary.market)
        assertEquals("상승장", summary.label)
    }

    @Test
    fun marketSummary_emptyStrings() {
        val summary = MarketSummary("", "", "", 0.0, "", emptyList(), emptyList(), emptyList())
        assertEquals("", summary.market)
        assertEquals("", summary.label)
    }

    // ===== LLMSummaryData (10개) =====

    @Test
    fun llmSummaryData_createsCorrectly() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val data = LLMSummaryData(
            asofDate = "2024-01-15",
            regime = "bull_market",
            overview = listOf("overview1", "overview2"),
            kospi = kospi,
            kosdaq = kosdaq,
            newsUsed = listOf("news1", "news2"),
            basicOverview = "Basic overview text",
            newsOverview = "News overview text"
        )
        
        assertEquals("2024-01-15", data.asofDate)
        assertEquals("bull_market", data.regime)
        assertEquals(2, data.overview.size)
        assertEquals("KOSPI", data.kospi.market)
        assertEquals("KOSDAQ", data.kosdaq.market)
        assertEquals(2, data.newsUsed.size)
        assertEquals("Basic overview text", data.basicOverview)
        assertEquals("News overview text", data.newsOverview)
    }

    @Test
    fun llmSummaryData_nullableFields() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val data = LLMSummaryData(
            asofDate = "2024-01-15",
            regime = "bull_market",
            overview = emptyList(),
            kospi = kospi,
            kosdaq = kosdaq,
            newsUsed = emptyList(),
            basicOverview = null,
            newsOverview = null
        )
        
        assertNull(data.basicOverview)
        assertNull(data.newsOverview)
    }

    @Test
    fun llmSummaryData_emptyLists() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val data = LLMSummaryData(
            asofDate = "2024-01-15",
            regime = "bull_market",
            overview = emptyList(),
            kospi = kospi,
            kosdaq = kosdaq,
            newsUsed = emptyList(),
            basicOverview = "Basic",
            newsOverview = "News"
        )
        
        assertTrue(data.overview.isEmpty())
        assertTrue(data.newsUsed.isEmpty())
    }

    @Test
    fun llmSummaryData_manyOverviews() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        val overviews = (1..20).map { "overview$it" }
        
        val data = LLMSummaryData(
            asofDate = "2024-01-15",
            regime = "bull_market",
            overview = overviews,
            kospi = kospi,
            kosdaq = kosdaq,
            newsUsed = emptyList(),
            basicOverview = "Basic",
            newsOverview = "News"
        )
        
        assertEquals(20, data.overview.size)
    }

    @Test
    fun llmSummaryData_differentRegimes() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val bull = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        val bear = LLMSummaryData("2024-01-15", "bear_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        
        assertNotEquals(bull.regime, bear.regime)
    }

    @Test
    fun llmSummaryData_equality() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val d1 = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        val d2 = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        
        assertEquals(d1, d2)
    }

    @Test
    fun llmSummaryData_copy() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val original = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        val copied = original.copy(regime = "bear_market")
        
        assertEquals("bear_market", copied.regime)
        assertEquals("2024-01-15", copied.asofDate)
    }

    @Test
    fun llmSummaryData_toString() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val data = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        
        assertNotNull(data.toString())
        assertTrue(data.toString().contains("LLMSummaryData"))
    }

    @Test
    fun llmSummaryData_hashCode() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        
        val d1 = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        val d2 = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), null, null)
        
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun llmSummaryData_withLongTexts() {
        val kospi = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "Strong", emptyList(), emptyList(), emptyList())
        val kosdaq = MarketSummary("KOSDAQ", "2024-01-15", "neutral", 0.5, "Stable", emptyList(), emptyList(), emptyList())
        val longBasic = "A".repeat(10000)
        val longNews = "B".repeat(10000)
        
        val data = LLMSummaryData("2024-01-15", "bull_market", emptyList(), kospi, kosdaq, emptyList(), longBasic, longNews)
        
        assertEquals(10000, data.basicOverview?.length)
        assertEquals(10000, data.newsOverview?.length)
    }

    // ===== StockIndexData (15개) =====

    @Test
    fun stockIndexData_createsCorrectly() {
        val data = StockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 50.0,
            changePercent = 2.0,
            date = "2024-01-15",
            high = 2520.0,
            low = 2480.0,
            open = 2490.0,
            volume = 1000000L
        )
        
        assertEquals("KOSPI", data.name)
        assertEquals(2500.0, data.close, 0.001)
        assertEquals(50.0, data.changeAmount, 0.001)
        assertEquals(2.0, data.changePercent, 0.001)
        assertEquals("2024-01-15", data.date)
        assertEquals(2520.0, data.high, 0.001)
        assertEquals(2480.0, data.low, 0.001)
        assertEquals(2490.0, data.open, 0.001)
        assertEquals(1000000L, data.volume)
    }

    @Test
    fun stockIndexData_nameIsMutable() {
        val data = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        data.name = "KOSDAQ"
        assertEquals("KOSDAQ", data.name)
    }

    @Test
    fun stockIndexData_negativeChange() {
        val data = StockIndexData("KOSPI", 2500.0, -50.0, -2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        assertEquals(-50.0, data.changeAmount, 0.001)
        assertEquals(-2.0, data.changePercent, 0.001)
    }

    @Test
    fun stockIndexData_zeroChange() {
        val data = StockIndexData("KOSPI", 2500.0, 0.0, 0.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        assertEquals(0.0, data.changeAmount, 0.001)
        assertEquals(0.0, data.changePercent, 0.001)
    }

    @Test
    fun stockIndexData_largeVolume() {
        val data = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, data.volume)
    }

    @Test
    fun stockIndexData_smallDecimals() {
        val data = StockIndexData("KOSPI", 2500.123, 0.001, 0.0001, "2024-01-15", 2500.5, 2499.9, 2500.0, 100L)
        assertEquals(2500.123, data.close, 0.0001)
        assertEquals(0.001, data.changeAmount, 0.0001)
    }

    @Test
    fun stockIndexData_equality() {
        val d1 = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        val d2 = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        assertEquals(d1, d2)
    }

    @Test
    fun stockIndexData_copy() {
        val original = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        val copied = original.copy(close = 2600.0)
        assertEquals(2600.0, copied.close, 0.001)
        assertEquals("KOSPI", copied.name)
    }

    @Test
    fun stockIndexData_toString() {
        val data = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        assertNotNull(data.toString())
        assertTrue(data.toString().contains("StockIndexData"))
    }

    @Test
    fun stockIndexData_hashCode() {
        val d1 = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        val d2 = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun stockIndexData_highLowOpenClose_relationships() {
        val data = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2600.0, 2400.0, 2450.0, 1000000L)
        assertTrue(data.high >= data.close)
        assertTrue(data.low <= data.close)
    }

    @Test
    fun stockIndexData_emptyStrings() {
        val data = StockIndexData("", 0.0, 0.0, 0.0, "", 0.0, 0.0, 0.0, 0L)
        assertEquals("", data.name)
        assertEquals("", data.date)
    }

    @Test
    fun stockIndexData_koreanName() {
        val data = StockIndexData("코스피", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        assertEquals("코스피", data.name)
    }

    @Test
    fun stockIndexData_decimalPrecision() {
        val data = StockIndexData("KOSPI", 2500.123456789, 50.987654321, 2.12345, "2024-01-15", 2520.5, 2480.5, 2490.5, 1000000L)
        assertEquals(2500.123456789, data.close, 0.000000001)
        assertEquals(50.987654321, data.changeAmount, 0.000000001)
    }

    @Test
    fun stockIndexData_differentDates() {
        val d1 = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        val d2 = StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-16", 2520.0, 2480.0, 2490.0, 1000000L)
        assertNotEquals(d1.date, d2.date)
    }

    // ===== StockIndexHistoryItem (10개) =====

    @Test
    fun stockIndexHistoryItem_createsCorrectly() {
        val item = StockIndexHistoryItem("2024-01-15", 2500.0)
        assertEquals("2024-01-15", item.date)
        assertEquals(2500.0, item.close, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_equality() {
        val i1 = StockIndexHistoryItem("2024-01-15", 2500.0)
        val i2 = StockIndexHistoryItem("2024-01-15", 2500.0)
        assertEquals(i1, i2)
    }

    @Test
    fun stockIndexHistoryItem_copy() {
        val original = StockIndexHistoryItem("2024-01-15", 2500.0)
        val copied = original.copy(close = 2600.0)
        assertEquals(2600.0, copied.close, 0.001)
        assertEquals("2024-01-15", copied.date)
    }

    @Test
    fun stockIndexHistoryItem_negativeClose() {
        val item = StockIndexHistoryItem("2024-01-15", -100.0)
        assertEquals(-100.0, item.close, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_zeroClose() {
        val item = StockIndexHistoryItem("2024-01-15", 0.0)
        assertEquals(0.0, item.close, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_largeClose() {
        val item = StockIndexHistoryItem("2024-01-15", 1000000.0)
        assertEquals(1000000.0, item.close, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_toString() {
        val item = StockIndexHistoryItem("2024-01-15", 2500.0)
        assertNotNull(item.toString())
        assertTrue(item.toString().contains("StockIndexHistoryItem"))
    }

    @Test
    fun stockIndexHistoryItem_hashCode() {
        val i1 = StockIndexHistoryItem("2024-01-15", 2500.0)
        val i2 = StockIndexHistoryItem("2024-01-15", 2500.0)
        assertEquals(i1.hashCode(), i2.hashCode())
    }

    @Test
    fun stockIndexHistoryItem_differentDates() {
        val i1 = StockIndexHistoryItem("2024-01-15", 2500.0)
        val i2 = StockIndexHistoryItem("2024-01-16", 2500.0)
        assertNotEquals(i1, i2)
    }

    @Test
    fun stockIndexHistoryItem_emptyDate() {
        val item = StockIndexHistoryItem("", 2500.0)
        assertEquals("", item.date)
    }

    // ===== StockIndexHistoryResponse (5개) =====

    @Test
    fun stockIndexHistoryResponse_createsCorrectly() {
        val items = listOf(
            StockIndexHistoryItem("2024-01-15", 2500.0),
            StockIndexHistoryItem("2024-01-16", 2520.0)
        )
        val response = StockIndexHistoryResponse("ok", "KOSPI", items)
        
        assertEquals("ok", response.status)
        assertEquals("KOSPI", response.index)
        assertEquals(2, response.data.size)
    }

    @Test
    fun stockIndexHistoryResponse_emptyData() {
        val response = StockIndexHistoryResponse("ok", "KOSPI", emptyList())
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun stockIndexHistoryResponse_largeData() {
        val items = (1..365).map {
            StockIndexHistoryItem("2024-01-$it", 2500.0 + it)
        }
        val response = StockIndexHistoryResponse("ok", "KOSPI", items)
        assertEquals(365, response.data.size)
    }

    @Test
    fun stockIndexHistoryResponse_equality() {
        val items = listOf(StockIndexHistoryItem("2024-01-15", 2500.0))
        val r1 = StockIndexHistoryResponse("ok", "KOSPI", items)
        val r2 = StockIndexHistoryResponse("ok", "KOSPI", items)
        assertEquals(r1, r2)
    }

    @Test
    fun stockIndexHistoryResponse_copy() {
        val items = listOf(StockIndexHistoryItem("2024-01-15", 2500.0))
        val original = StockIndexHistoryResponse("ok", "KOSPI", items)
        val copied = original.copy(index = "KOSDAQ")
        assertEquals("KOSDAQ", copied.index)
        assertEquals("ok", copied.status)
    }

    // ===== StockIndexLatestResponse (5개) =====

    @Test
    fun stockIndexLatestResponse_createsCorrectly() {
        val dataMap = mapOf(
            "KOSPI" to StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L),
            "KOSDAQ" to StockIndexData("KOSDAQ", 800.0, 10.0, 1.25, "2024-01-15", 810.0, 795.0, 798.0, 500000L)
        )
        val response = StockIndexLatestResponse("ok", dataMap)
        
        assertEquals("ok", response.status)
        assertEquals(2, response.data.size)
        assertTrue(response.data.containsKey("KOSPI"))
        assertTrue(response.data.containsKey("KOSDAQ"))
    }

    @Test
    fun stockIndexLatestResponse_emptyData() {
        val response = StockIndexLatestResponse("ok", emptyMap())
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun stockIndexLatestResponse_singleIndex() {
        val dataMap = mapOf(
            "KOSPI" to StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        )
        val response = StockIndexLatestResponse("ok", dataMap)
        assertEquals(1, response.data.size)
    }

    @Test
    fun stockIndexLatestResponse_equality() {
        val dataMap = mapOf(
            "KOSPI" to StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        )
        val r1 = StockIndexLatestResponse("ok", dataMap)
        val r2 = StockIndexLatestResponse("ok", dataMap)
        assertEquals(r1, r2)
    }

    @Test
    fun stockIndexLatestResponse_copy() {
        val dataMap = mapOf(
            "KOSPI" to StockIndexData("KOSPI", 2500.0, 50.0, 2.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 1000000L)
        )
        val original = StockIndexLatestResponse("ok", dataMap)
        val copied = original.copy(status = "error")
        assertEquals("error", copied.status)
        assertEquals(dataMap, copied.data)
    }
}