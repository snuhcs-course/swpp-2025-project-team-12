package com.example.dailyinsight.ui.detail

import com.example.dailyinsight.MainDispatcherRule
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.*
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import java.io.IOException

@ExperimentalCoroutinesApi
class StockDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: Repository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        repository = mock()
    }

    private fun createViewModel(): StockDetailViewModel {
        return StockDetailViewModel(repository, testDispatcher)
    }

    private fun createStockDetail(
        ticker: String = "005930",
        hasHistory: Boolean = true,
        historySize: Int = 3
    ): StockDetailDto {
        return StockDetailDto(
            ticker = ticker,
            name = "테스트 주식",
            current = CurrentData(
                price = 72000,
                change = -100,
                changeRate = -0.14,
                marketCap = 1000000,
                date = "2024-01-01"
            ),
            valuation = ValuationData(
                peTtm = 15.5,
                priceToBook = 1.2,
                bps = 60000
            ),
            dividend = DividendData(
                `yield` = 2.5
            ),
            financials = FinancialsData(
                eps = 4800,
                dps = 1800,
                roe = 8.0
            ),
            history = if (hasHistory) {
                (1..historySize).map {
                    HistoryItem(
                        date = "2024-01-${it.toString().padStart(2, '0')}",
                        close = 70000.0 + it * 1000
                    )
                }
            } else {
                null
            },
            profile = ProfileData(
                explanation = "테스트 회사 설명"
            ),
            asOf = "2024-01-01"
        )
    }

    private fun createStockOverview(code: String = "005930"): StockOverviewDto {
        return StockOverviewDto(
            asOfDate = "2024-01-01",
            summary = "테스트 요약",
            fundamental = "테스트 펀더멘털 분석",
            technical = "테스트 기술적 분석",
            news = listOf("뉴스1", "뉴스2", "뉴스3")
        )
    }

    // ===== Basic Loading Tests =====

    @Test
    fun load_withValidTicker_updatesStateToSuccess() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        assertEquals(stockDetail, (state as LoadResult.Success).data)
    }

    @Test
    fun load_withRepositoryError_updatesStateToError() = runTest {
        val ticker = "INVALID"
        val exception = IOException("Network error")
        whenever(repository.getStockReport(ticker)).thenAnswer { throw exception }
        whenever(repository.getStockOverview(ticker)).thenAnswer { throw exception }
        
        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Error)
        assertEquals(exception, (state as LoadResult.Error).throwable)
    }

    @Test
    fun initialState_isEmpty() {
        val viewModel = createViewModel()
        assertTrue(viewModel.state.value is LoadResult.Empty)
        assertTrue(viewModel.priceState.value is LoadResult.Empty)
        assertTrue(viewModel.overviewState.value is LoadResult.Empty)
    }

    // ===== Overview Tests =====

    @Test
    fun load_overviewLoadsSeparately() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker)
        val overview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenReturn(overview)
        
        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.overviewState.value is LoadResult.Success)
        assertEquals(overview, (viewModel.overviewState.value as LoadResult.Success).data)
    }

    @Test
    fun load_overviewWithNullFields_handlesGracefully() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker)
        val overview = StockOverviewDto(
            asOfDate = null,
            summary = null,
            fundamental = null,
            technical = null,
            news = null
        )
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenReturn(overview)
        
        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.overviewState.value is LoadResult.Success)
    }

    // ===== Error Recovery Tests =====

    @Test
    fun load_networkTimeout_handlesError() = runTest {
        whenever(repository.getStockReport(any())).thenAnswer { throw IOException("Timeout") }
        whenever(repository.getStockOverview(any())).thenAnswer { throw IOException("Timeout") }
        
        val viewModel = createViewModel()
        viewModel.load("005930")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Error)
    }

    // ===== Edge Cases =====

    @Test
    fun load_emptyTicker_works() = runTest {
        val detail = createStockDetail("")
        val overview = createStockOverview("")
        whenever(repository.getStockReport("")).thenReturn(detail)
        whenever(repository.getStockOverview("")).thenReturn(overview)
        
        val viewModel = createViewModel()
        viewModel.load("")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_specialCharacters_works() = runTest {
        val ticker = "ABC-123"
        val detail = createStockDetail(ticker)
        val overview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenReturn(overview)
        
        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    // ===== ViewModel Lifecycle =====

    @Test
    fun viewModel_canBeRecreated() {
        val vm1 = createViewModel()
        val vm2 = createViewModel()
        assertNotNull(vm1)
        assertNotNull(vm2)
    }

    // ===== Price Chart Tests =====

    @Test
    fun load_withHistory_updatesPriceStateToSuccess() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker, hasHistory = true, historySize = 5)
        val overview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenReturn(overview)

        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()

        assertTrue(viewModel.priceState.value is LoadResult.Success)
    }

    @Test
    fun load_withEmptyHistory_handlesPriceChartGracefully() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker, hasHistory = true, historySize = 0)
        val overview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenReturn(overview)

        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()

        // 빈 히스토리도 성공으로 처리
        assertTrue(viewModel.priceState.value is LoadResult.Success)
    }

    @Test
    fun load_withNullHistory_handlesPriceChartGracefully() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker, hasHistory = false)
        val overview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenReturn(overview)

        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()

        // null 히스토리도 성공으로 처리 (빈 차트)
        assertTrue(viewModel.priceState.value is LoadResult.Success)
    }

    @Test
    fun load_reportError_updatesPriceStateToError() = runTest {
        val ticker = "005930"
        whenever(repository.getStockReport(ticker)).thenAnswer { throw IOException("Error") }
        whenever(repository.getStockOverview(ticker)).thenReturn(createStockOverview(ticker))

        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()

        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    @Test
    fun load_overviewError_stateStillSuccess() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenAnswer { throw IOException("Overview error") }

        val viewModel = createViewModel()
        viewModel.load(ticker)
        advanceUntilIdle()

        // report는 성공, overview만 실패
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.overviewState.value is LoadResult.Error)
    }
}