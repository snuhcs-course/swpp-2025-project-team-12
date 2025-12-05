package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class LLMSummaryDtoTest {

    @Test
    fun llmSummaryResponse_createsSuccessfully() {
        val response = LLMSummaryResponse(
            llmOutput = "Test LLM output string"
        )

        assertEquals("Test LLM output string", response.llmOutput)
    }

    @Test
    fun marketSummary_withAllFields_createsSuccessfully() {
        val summary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "bullish",
            confidence = 0.85,
            summary = "Market is showing positive momentum",
            signals = listOf("signal1", "signal2"),
            drivers = listOf("driver1", "driver2"),
            risks = listOf("risk1", "risk2")
        )

        assertEquals("KOSPI", summary.market)
        assertEquals("2024-01-15", summary.asofDate)
        assertEquals("bullish", summary.label)
        assertEquals(0.85, summary.confidence, 0.001)
        assertEquals("Market is showing positive momentum", summary.summary)
        assertEquals(2, summary.signals.size)
        assertEquals(2, summary.drivers.size)
        assertEquals(2, summary.risks.size)
    }

    @Test
    fun marketSummary_withEmptyLists_createsSuccessfully() {
        val summary = MarketSummary(
            market = "KOSDAQ",
            asofDate = "2024-01-15",
            label = "neutral",
            confidence = 0.5,
            summary = "Market is stable",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )

        assertTrue(summary.signals.isEmpty())
        assertTrue(summary.drivers.isEmpty())
        assertTrue(summary.risks.isEmpty())
    }

    @Test
    fun llmSummaryData_withAllFields_createsSuccessfully() {
        val kospiSummary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "bullish",
            confidence = 0.8,
            summary = "KOSPI summary",
            signals = listOf("signal"),
            drivers = listOf("driver"),
            risks = listOf("risk")
        )

        val kosdaqSummary = MarketSummary(
            market = "KOSDAQ",
            asofDate = "2024-01-15",
            label = "bearish",
            confidence = 0.7,
            summary = "KOSDAQ summary",
            signals = listOf("signal"),
            drivers = listOf("driver"),
            risks = listOf("risk")
        )

        val data = LLMSummaryData(
            asofDate = "2024-01-15",
            regime = "bull",
            overview = listOf("overview1", "overview2"),
            kospi = kospiSummary,
            kosdaq = kosdaqSummary,
            newsUsed = listOf("news1", "news2"),
            basicOverview = "Basic overview text",
            newsOverview = "News overview text"
        )

        assertEquals("2024-01-15", data.asofDate)
        assertEquals("bull", data.regime)
        assertEquals(2, data.overview.size)
        assertEquals("KOSPI", data.kospi.market)
        assertEquals("KOSDAQ", data.kosdaq.market)
        assertEquals(2, data.newsUsed.size)
        assertEquals("Basic overview text", data.basicOverview)
        assertEquals("News overview text", data.newsOverview)
    }

    @Test
    fun llmSummaryData_withNullOptionalFields_createsSuccessfully() {
        val kospiSummary = MarketSummary(
            market = "KOSPI",
            asofDate = "2024-01-15",
            label = "neutral",
            confidence = 0.5,
            summary = "Summary",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )

        val kosdaqSummary = MarketSummary(
            market = "KOSDAQ",
            asofDate = "2024-01-15",
            label = "neutral",
            confidence = 0.5,
            summary = "Summary",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )

        val data = LLMSummaryData(
            asofDate = "2024-01-15",
            regime = "neutral",
            overview = emptyList(),
            kospi = kospiSummary,
            kosdaq = kosdaqSummary,
            newsUsed = emptyList(),
            basicOverview = null,
            newsOverview = null
        )

        assertNull(data.basicOverview)
        assertNull(data.newsOverview)
    }

    @Test
    fun marketSummary_confidenceBoundaries_work() {
        val lowConfidence = MarketSummary(
            market = "TEST",
            asofDate = "2024-01-15",
            label = "uncertain",
            confidence = 0.0,
            summary = "Low confidence",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )

        val highConfidence = MarketSummary(
            market = "TEST",
            asofDate = "2024-01-15",
            label = "certain",
            confidence = 1.0,
            summary = "High confidence",
            signals = emptyList(),
            drivers = emptyList(),
            risks = emptyList()
        )

        assertEquals(0.0, lowConfidence.confidence, 0.001)
        assertEquals(1.0, highConfidence.confidence, 0.001)
    }

    // ===== Additional Tests =====

    @Test
    fun llmSummaryResponse_equality() {
        val response1 = LLMSummaryResponse("output")
        val response2 = LLMSummaryResponse("output")
        assertEquals(response1, response2)
    }

    @Test
    fun llmSummaryResponse_hashCode() {
        val response1 = LLMSummaryResponse("output")
        val response2 = LLMSummaryResponse("output")
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun llmSummaryResponse_copy() {
        val original = LLMSummaryResponse("original")
        val copied = original.copy(llmOutput = "copied")
        assertEquals("copied", copied.llmOutput)
    }

    @Test
    fun llmSummaryResponse_destructuring() {
        val response = LLMSummaryResponse("output")
        val (output) = response
        assertEquals("output", output)
    }

    @Test
    fun marketSummary_equality() {
        val summary1 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        val summary2 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        assertEquals(summary1, summary2)
    }

    @Test
    fun marketSummary_hashCode() {
        val summary1 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        val summary2 = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        assertEquals(summary1.hashCode(), summary2.hashCode())
    }

    @Test
    fun marketSummary_copy() {
        val original = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        val copied = original.copy(label = "bearish")
        assertEquals("bearish", copied.label)
        assertEquals("KOSPI", copied.market)
    }

    @Test
    fun marketSummary_toString() {
        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        val str = summary.toString()
        assertTrue(str.contains("KOSPI"))
        assertTrue(str.contains("bullish"))
    }

    @Test
    fun marketSummary_destructuring() {
        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf("s1"), listOf("d1"), listOf("r1"))
        val (market, asofDate, label, confidence, summaryText, signals, drivers, risks) = summary

        assertEquals("KOSPI", market)
        assertEquals("2024-01-15", asofDate)
        assertEquals("bullish", label)
        assertEquals(0.8, confidence, 0.001)
        assertEquals("summary", summaryText)
        assertEquals(listOf("s1"), signals)
        assertEquals(listOf("d1"), drivers)
        assertEquals(listOf("r1"), risks)
    }

    @Test
    fun llmSummaryData_copy() {
        val kospiSummary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.8, "summary", listOf(), listOf(), listOf())
        val kosdaqSummary = MarketSummary("KOSDAQ", "2024-01-15", "bearish", 0.7, "summary", listOf(), listOf(), listOf())

        val original = LLMSummaryData("2024-01-15", "bull", listOf(), kospiSummary, kosdaqSummary, listOf(), null, null)
        val copied = original.copy(regime = "bear")

        assertEquals("bear", copied.regime)
        assertEquals("2024-01-15", copied.asofDate)
    }

    @Test
    fun marketSummary_manySignals() {
        val signals = (1..10).map { "signal$it" }
        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.9, "summary", signals, listOf(), listOf())

        assertEquals(10, summary.signals.size)
        assertEquals("signal1", summary.signals[0])
        assertEquals("signal10", summary.signals[9])
    }

    @Test
    fun marketSummary_manyDriversAndRisks() {
        val drivers = listOf("AI발전", "금리인하", "수출증가", "정책지원")
        val risks = listOf("인플레이션", "지정학적리스크", "환율변동")

        val summary = MarketSummary("KOSPI", "2024-01-15", "bullish", 0.85, "summary", listOf(), drivers, risks)

        assertEquals(4, summary.drivers.size)
        assertEquals(3, summary.risks.size)
        assertTrue(summary.drivers.contains("AI발전"))
        assertTrue(summary.risks.contains("환율변동"))
    }
}
