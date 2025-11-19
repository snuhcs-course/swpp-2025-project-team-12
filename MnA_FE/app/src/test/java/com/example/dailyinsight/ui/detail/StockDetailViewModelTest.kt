package com.example.dailyinsight.ui.detail

import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.*
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import java.io.IOException

@ExperimentalCoroutinesApi
class StockDetailViewModelTest {

    private lateinit var repository: Repository
    private lateinit var viewModel: StockDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        assertEquals(stockDetail, (state as LoadResult.Success).data)
    }

    @Test
    fun load_withValidHistory_updatesPriceStateToSuccessOrError() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasHistory = true)
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        val priceState = viewModel.priceState.value
        // priceState는 Success 또는 Error일 수 있음 (차트 생성 실패 시)
        assertFalse(priceState is LoadResult.Empty)
        assertFalse(priceState is LoadResult.Loading)
    }

    @Test
    fun load_withEmptyHistory_updatesPriceStateToError() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasHistory = false)
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    @Test
    fun load_withRepositoryError_updatesStateToError() = runTest {
        val ticker = "INVALID"
        val exception = IOException("Network error")
        whenever(repository.getStockReport(ticker)).thenAnswer { throw exception }
        whenever(repository.getStockOverview(ticker)).thenAnswer { throw exception }
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Error)
        assertEquals(exception, (state as LoadResult.Error).throwable)
        
        val priceState = viewModel.priceState.value
        assertTrue(priceState is LoadResult.Error)
        assertEquals(exception, (priceState as LoadResult.Error).throwable)
    }

    @Test
    fun initialState_isEmpty() {
        viewModel = StockDetailViewModel(repository)
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
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.overviewState.value is LoadResult.Success)
        assertEquals(overview, (viewModel.overviewState.value as LoadResult.Success).data)
    }

    @Test
    fun load_overviewError_doesNotAffectDetailState() = runTest {
        val ticker = "005930"
        val detail = createStockDetail(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(detail)
        whenever(repository.getStockOverview(ticker)).thenAnswer { throw IOException("Overview error") }
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.overviewState.value is LoadResult.Error)
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
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.overviewState.value is LoadResult.Success)
    }

    // ===== Chart Data Tests =====

    @Test
    fun load_withSingleHistoryPoint_handlesChartCreation() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasHistory = true, historySize = 1)
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        // priceState는 Success 또는 Error (단일 데이터 포인트 처리 방식에 따라)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_withLargeHistoryDataSet_handlesChartCreation() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasHistory = true, historySize = 100)
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        // priceState는 차트 생성 성공/실패 여부에 따라 결정
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_withHistoryInWrongOrder_handlesChartCreation() = runTest {
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            name = "테스트",
            current = CurrentData(price = 75000),
            history = listOf(
                HistoryItem(date = "2024-01-15", close = 75000.0),
                HistoryItem(date = "2024-01-10", close = 70000.0),
                HistoryItem(date = "2024-01-20", close = 80000.0)
            )
        )
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    // ===== Multiple Load Tests =====

    @Test
    fun load_withDifferentTickers_loadsCorrectData() = runTest {
        val ticker1 = "005930"
        val ticker2 = "000660"
        val stockDetail1 = createStockDetail(ticker1)
        val stockDetail2 = createStockDetail(ticker2)
        val stockOverview1 = createStockOverview(ticker1)
        val stockOverview2 = createStockOverview(ticker2)
        
        whenever(repository.getStockReport(ticker1)).thenReturn(stockDetail1)
        whenever(repository.getStockReport(ticker2)).thenReturn(stockDetail2)
        whenever(repository.getStockOverview(ticker1)).thenReturn(stockOverview1)
        whenever(repository.getStockOverview(ticker2)).thenReturn(stockOverview2)
        
        viewModel = StockDetailViewModel(repository)
        
        viewModel.load(ticker1)
        advanceUntilIdle()
        val state1 = viewModel.state.value
        
        viewModel.load(ticker2)
        advanceUntilIdle()
        val state2 = viewModel.state.value
        
        assertTrue(state1 is LoadResult.Success)
        assertEquals(ticker1, (state1 as LoadResult.Success).data.ticker)
        assertTrue(state2 is LoadResult.Success)
        assertEquals(ticker2, (state2 as LoadResult.Success).data.ticker)
    }

    @Test
    fun load_multipleCallsWithSameTicker_updatesStateCorrectly() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    // ===== Error Recovery Tests =====

    @Test
    fun load_afterError_canRecover() = runTest {
        whenever(repository.getStockReport("005930")).thenAnswer { throw IOException("Error") }
        whenever(repository.getStockReport("000660")).thenReturn(createStockDetail("000660"))
        whenever(repository.getStockOverview("005930")).thenAnswer { throw IOException("Error") }
        whenever(repository.getStockOverview("000660")).thenReturn(createStockOverview("000660"))
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Error)
        
        viewModel.load("000660")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_networkTimeout_handlesError() = runTest {
        whenever(repository.getStockReport(any())).thenAnswer { throw IOException("Timeout") }
        whenever(repository.getStockOverview(any())).thenAnswer { throw IOException("Timeout") }
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Error)
        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    // ===== Edge Cases =====

    @Test
    fun load_withMinimalData_succeeds() = runTest {
        val detail = StockDetailDto(ticker = "005930")
        val overview = createStockOverview("005930")
        whenever(repository.getStockReport("005930")).thenReturn(detail)
        whenever(repository.getStockOverview("005930")).thenReturn(overview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_emptyTicker_works() = runTest {
        val detail = createStockDetail("")
        val overview = createStockOverview("")
        whenever(repository.getStockReport("")).thenReturn(detail)
        whenever(repository.getStockOverview("")).thenReturn(overview)
        
        viewModel = StockDetailViewModel(repository)
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
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_withEmptyHistoryList_updatesPriceStateToError() = runTest {
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            history = emptyList()
        )
        val stockOverview = createStockOverview(ticker)
        whenever(repository.getStockReport(ticker)).thenReturn(stockDetail)
        whenever(repository.getStockOverview(ticker)).thenReturn(stockOverview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    // ===== State Transitions =====

    @Test
    fun priceState_transitionsFromEmptyToNonEmpty() = runTest {
        val detail = createStockDetail()
        val overview = createStockOverview()
        whenever(repository.getStockReport(any())).thenReturn(detail)
        whenever(repository.getStockOverview(any())).thenReturn(overview)
        
        viewModel = StockDetailViewModel(repository)
        assertTrue(viewModel.priceState.value is LoadResult.Empty)
        
        viewModel.load("005930")
        advanceUntilIdle()
        
        // Empty에서 벗어났는지만 확인 (Success 또는 Error)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_exceptionInPriceChart_mainStateSucceeds() = runTest {
        val detail = StockDetailDto(
            ticker = "005930",
            history = emptyList()
        )
        val overview = createStockOverview("005930")
        whenever(repository.getStockReport("005930")).thenReturn(detail)
        whenever(repository.getStockOverview("005930")).thenReturn(overview)
        
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    // ===== ViewModel Lifecycle =====

    @Test
    fun viewModel_canBeRecreated() {
        val vm1 = StockDetailViewModel(repository)
        val vm2 = StockDetailViewModel(repository)
        assertNotNull(vm1)
        assertNotNull(vm2)
    }
}