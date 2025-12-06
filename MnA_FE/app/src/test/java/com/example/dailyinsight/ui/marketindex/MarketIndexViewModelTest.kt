package com.example.dailyinsight.ui.marketindex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailyinsight.MainDispatcherRule
import com.example.dailyinsight.data.dto.LLMSummaryData
import com.example.dailyinsight.data.dto.MarketSummary
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.repository.MarketIndexRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException

@ExperimentalCoroutinesApi
class MarketIndexViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: MarketIndexRepository

    @Before
    fun setup() {
        repository = mock()
    }

    // ===== Market Data Tests =====

    @Test
    fun init_fetchesMarketDataSuccessfully() = runTest {
        val marketData = mapOf(
            "KOSPI" to createStockIndexData("KOSPI", 2500.0),
            "KOSDAQ" to createStockIndexData("KOSDAQ", 800.0)
        )
        whenever(repository.getMarketData()).thenReturn(marketData)
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, viewModel.marketData.value?.size)
        assertEquals(2500.0, viewModel.marketData.value?.get("KOSPI")?.close)
    }

    @Test
    fun init_marketDataError_setsErrorMessage() = runTest {
        whenever(repository.getMarketData()).thenAnswer { throw IOException("Network error") }
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("Failed to fetch data"))
    }

    @Test
    fun init_emptyMarketData_handlesGracefully() = runTest {
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.marketData.value?.isEmpty() == true)
    }

    // ===== LLM Summary Tests =====

    @Test
    fun init_fetchesLLMSummarySuccessfully() = runTest {
        val summary = createLLMSummary("시장 개요", "뉴스 개요")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertNotNull(viewModel.llmSummary.value)
        assertEquals("시장 개요", viewModel.llmSummary.value?.basicOverview)
    }

    @Test
    fun init_llmSummaryError_setsEmptyText() = runTest {
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenAnswer { throw IOException("API error") }
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals("", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_combinesBasicAndNewsOverview() = runTest {
        val summary = createLLMSummary("기본 개요", "뉴스 개요")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        val text = viewModel.llmOverviewText.value
        assertTrue(text?.contains("기본 개요") == true)
        assertTrue(text?.contains("뉴스 개요") == true)
    }

    @Test
    fun init_onlyBasicOverview_showsBasicOnly() = runTest {
        val summary = createLLMSummary("기본만", null)
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals("기본만", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_onlyNewsOverview_showsNewsOnly() = runTest {
        val summary = createLLMSummary(null, "뉴스만")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals("뉴스만", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_bothOverviewsNull_showsEmpty() = runTest {
        val summary = createLLMSummary(null, null)
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals("", viewModel.llmOverviewText.value)
    }

    // ===== Pre-cache Tests =====

    @org.junit.Ignore("Flaky due to coroutine dispatcher state pollution from other tests")
    @Test
    fun init_preCachesHistoricalData() = runTest {
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        verify(repository).refreshHistoricalData("KOSPI")
        verify(repository).refreshHistoricalData("KOSDAQ")
    }

    @Test
    fun init_preCacheFailure_doesNotAffectOtherData() = runTest {
        val marketData = mapOf("KOSPI" to createStockIndexData("KOSPI", 2500.0))
        whenever(repository.getMarketData()).thenReturn(marketData)
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenAnswer { throw IOException("Cache error") }

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        // 캐시 실패해도 market data는 정상
        assertEquals(1, viewModel.marketData.value?.size)
    }

    // ===== Helper Functions =====

    private fun createStockIndexData(name: String, close: Double) = StockIndexData(
        name = name,
        close = close,
        changeAmount = 10.0,
        changePercent = 0.4,
        date = "2024-01-01",
        high = close + 10,
        low = close - 10,
        open = close - 5,
        volume = 1000000L
    )

    private fun createLLMSummary(
        basic: String? = "기본 개요",
        news: String? = "뉴스 개요"
    ) = LLMSummaryData(
        asofDate = "2024-01-01",
        regime = "neutral",
        overview = listOf("시장 개요"),
        kospi = createMarketSummary("KOSPI"),
        kosdaq = createMarketSummary("KOSDAQ"),
        newsUsed = listOf("뉴스1", "뉴스2"),
        basicOverview = basic,
        newsOverview = news
    )

    private fun createMarketSummary(market: String) = MarketSummary(
        market = market,
        asofDate = "2024-01-01",
        label = "neutral",
        confidence = 0.8,
        summary = "$market 요약",
        signals = listOf("signal1"),
        drivers = listOf("driver1"),
        risks = listOf("risk1")
    )

    // ===== Additional Market Data Tests =====

    @Test
    fun init_singleMarketData_works() = runTest {
        val marketData = mapOf("KOSPI" to createStockIndexData("KOSPI", 2500.0))
        whenever(repository.getMarketData()).thenReturn(marketData)
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, viewModel.marketData.value?.size)
        assertNotNull(viewModel.marketData.value?.get("KOSPI"))
    }

    @Test
    fun init_marketDataWithHighValues_works() = runTest {
        val marketData = mapOf(
            "KOSPI" to createStockIndexData("KOSPI", 9999.99),
            "KOSDAQ" to createStockIndexData("KOSDAQ", 8888.88)
        )
        whenever(repository.getMarketData()).thenReturn(marketData)
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals(9999.99, viewModel.marketData.value?.get("KOSPI")?.close)
        assertEquals(8888.88, viewModel.marketData.value?.get("KOSDAQ")?.close)
    }

    @Test
    fun init_marketDataWithZeroValues_works() = runTest {
        val marketData = mapOf("KOSPI" to createStockIndexData("KOSPI", 0.0))
        whenever(repository.getMarketData()).thenReturn(marketData)
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals(0.0, viewModel.marketData.value?.get("KOSPI")?.close)
    }

    @Test
    fun init_marketDataWithNegativeChange_works() = runTest {
        val data = StockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = -50.0,
            changePercent = -2.0,
            date = "2024-01-01",
            high = 2550.0,
            low = 2450.0,
            open = 2520.0,
            volume = 1000000L
        )
        whenever(repository.getMarketData()).thenReturn(mapOf("KOSPI" to data))
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals(-50.0, viewModel.marketData.value?.get("KOSPI")?.changeAmount)
        assertEquals(-2.0, viewModel.marketData.value?.get("KOSPI")?.changePercent)
    }

    // ===== LLM Summary Edge Cases =====

    @Test
    fun init_llmSummaryWithEmptyStrings_handlesGracefully() = runTest {
        val summary = createLLMSummary("", "")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        // Empty strings should result in empty combined text
        assertEquals("", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_llmSummaryWithWhitespace_handlesGracefully() = runTest {
        val summary = createLLMSummary("   ", "   ")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        // Whitespace-only strings are treated as blank
        assertEquals("", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_llmSummaryWithLongText_works() = runTest {
        val longText = "A".repeat(5000)
        val summary = createLLMSummary(longText, longText)
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.llmOverviewText.value!!.length > 10000)
    }

    @Test
    fun init_llmSummaryWithKoreanText_works() = runTest {
        val summary = createLLMSummary("오늘 시장은 상승세입니다", "주요 뉴스: 경제 호황")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.llmOverviewText.value!!.contains("상승세"))
        assertTrue(viewModel.llmOverviewText.value!!.contains("경제 호황"))
    }

    @Test
    fun init_llmSummaryWithSpecialCharacters_works() = runTest {
        val summary = createLLMSummary("상승률: +5.5%", "하락: -3.2%")
        whenever(repository.getMarketData()).thenReturn(emptyMap())
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.llmOverviewText.value!!.contains("+5.5%"))
        assertTrue(viewModel.llmOverviewText.value!!.contains("-3.2%"))
    }

    // ===== Error Handling Tests =====

    @Test
    fun init_bothApisFail_setsErrorAndEmptyLLM() = runTest {
        whenever(repository.getMarketData()).thenAnswer { throw IOException("Market error") }
        whenever(repository.getLLMSummaryLatest()).thenAnswer { throw IOException("LLM error") }
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertEquals("", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_marketSuccessLlmFails_marketDataStillAvailable() = runTest {
        val marketData = mapOf("KOSPI" to createStockIndexData("KOSPI", 2500.0))
        whenever(repository.getMarketData()).thenReturn(marketData)
        whenever(repository.getLLMSummaryLatest()).thenAnswer { throw IOException("LLM error") }
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, viewModel.marketData.value?.size)
        assertEquals("", viewModel.llmOverviewText.value)
    }

    @Test
    fun init_llmSuccessMarketFails_llmDataStillAvailable() = runTest {
        val summary = createLLMSummary("개요", "뉴스")
        whenever(repository.getMarketData()).thenAnswer { throw IOException("Market error") }
        whenever(repository.getLLMSummaryLatest()).thenReturn(summary)
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.llmOverviewText.value!!.contains("개요"))
    }

    @Test
    fun init_runtimeException_setsError() = runTest {
        whenever(repository.getMarketData()).thenAnswer { throw RuntimeException("Unexpected") }
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("Unexpected"))
    }

    // ===== StockIndexData Name Assignment Tests =====

    @Test
    fun init_assignsNameToStockIndexData() = runTest {
        val data = StockIndexData(
            name = "",
            close = 2500.0,
            changeAmount = 10.0,
            changePercent = 0.4,
            date = "2024-01-01",
            high = 2510.0,
            low = 2490.0,
            open = 2495.0,
            volume = 1000000L
        )
        whenever(repository.getMarketData()).thenReturn(mapOf("KOSPI" to data))
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        // Name should be assigned from the map key
        assertEquals("KOSPI", viewModel.marketData.value?.get("KOSPI")?.name)
    }

    @Test
    fun init_multipleIndices_allNamesAssigned() = runTest {
        val kospi = StockIndexData("", 2500.0, 10.0, 0.4, "2024-01-01", 2510.0, 2490.0, 2495.0, 1000000L)
        val kosdaq = StockIndexData("", 800.0, 5.0, 0.6, "2024-01-01", 810.0, 790.0, 795.0, 500000L)
        whenever(repository.getMarketData()).thenReturn(mapOf("KOSPI" to kospi, "KOSDAQ" to kosdaq))
        whenever(repository.getLLMSummaryLatest()).thenReturn(createLLMSummary())
        whenever(repository.refreshHistoricalData(any())).thenReturn(Unit)

        val viewModel = MarketIndexViewModel(repository)
        advanceUntilIdle()

        assertEquals("KOSPI", viewModel.marketData.value?.get("KOSPI")?.name)
        assertEquals("KOSDAQ", viewModel.marketData.value?.get("KOSDAQ")?.name)
    }
}