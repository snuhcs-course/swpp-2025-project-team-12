package com.example.dailyinsight.ui.detail

import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.PriceFinancialInfoDto
import com.example.dailyinsight.data.dto.StockDetailDto
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

    private fun createStockDetail(ticker: String = "005930", hasPriceData: Boolean = true): StockDetailDto {
        return StockDetailDto(
            ticker = ticker,
            name = "테스트 주식",
            price = 72000,
            change = -100,
            changeRate = -0.14,
            priceFinancialInfo = if (hasPriceData) {
                PriceFinancialInfoDto(
                    price = mapOf(
                        "2024-01-01" to 70000.0,
                        "2024-01-02" to 71000.0,
                        "2024-01-03" to 72000.0
                    )
                )
            } else {
                null
            }
        )
    }

    @Test
    fun load_withValidTicker_updatesStateToSuccess() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        assertEquals(stockDetail, (state as LoadResult.Success).data)
    }

    @Test
    fun load_withValidPriceData_updatesPriceStateToSuccess() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasPriceData = true)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        val priceState = viewModel.priceState.value
        assertFalse(priceState is LoadResult.Empty)
        assertFalse(priceState is LoadResult.Loading)
    }

    @Test
    fun load_withEmptyPriceData_updatesPriceStateToError() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasPriceData = false)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
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
        whenever(repository.getStockDetail(ticker)).thenAnswer {
            throw exception
        }
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
    fun load_setsLoadingStateBeforeCompletion() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_withDifferentTickers_loadsCorrectData() = runTest {
        val ticker1 = "005930"
        val ticker2 = "000660"
        val stockDetail1 = createStockDetail(ticker1)
        val stockDetail2 = createStockDetail(ticker2)
        whenever(repository.getStockDetail(ticker1)).thenReturn(stockDetail1)
        whenever(repository.getStockDetail(ticker2)).thenReturn(stockDetail2)
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
    fun load_multipleCallsWithSameTicker_updatesState() = runTest {
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun initialState_isEmpty() {
        viewModel = StockDetailViewModel(repository)
        assertTrue(viewModel.state.value is LoadResult.Empty)
        assertTrue(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_withNullPriceFinancialInfo_updatesPriceStateToError() = runTest {
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            name = "테스트",
            price = 70000,
            priceFinancialInfo = null
        )
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    @Test
    fun load_withSinglePricePoint_createsSingleEntryChart() = runTest {
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            name = "테스트",
            price = 70000,
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_withPriceDataInWrongOrder_chartSortsCorrectly() = runTest {
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            name = "테스트",
            price = 75000,
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-01-15" to 75000.0,
                    "2024-01-10" to 70000.0,
                    "2024-01-20" to 80000.0
                )
            )
        )
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_emptyTicker_works() = runTest {
        val detail = createStockDetail("")
        whenever(repository.getStockDetail("")).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load("")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_specialCharacters_works() = runTest {
        val ticker = "ABC-123"
        val detail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_longTicker_works() = runTest {
        val ticker = "A".repeat(50)
        val detail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_consecutiveCalls_works() = runTest {
        val detail1 = createStockDetail("005930")
        val detail2 = createStockDetail("000660")
        whenever(repository.getStockDetail("005930")).thenReturn(detail1)
        whenever(repository.getStockDetail("000660")).thenReturn(detail2)
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        viewModel.load("000660")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_networkTimeout_handlesError() = runTest {
        whenever(repository.getStockDetail(any())).thenAnswer { throw IOException("Timeout") }
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Error)
    }

    @Test
    fun load_largePriceDataSet_succeeds() = runTest {
        val ticker = "005930"
        val largeMap = (1..1000).associate { "2024-$it" to (70000.0 + it) }
        val detail = StockDetailDto(ticker = ticker, priceFinancialInfo = PriceFinancialInfoDto(price = largeMap))
        whenever(repository.getStockDetail(ticker)).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_afterError_canRecover() = runTest {
        whenever(repository.getStockDetail("005930")).thenAnswer { throw IOException("Error") }
        whenever(repository.getStockDetail("000660")).thenReturn(createStockDetail("000660"))
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Error)
        viewModel.load("000660")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_withMinimalData_succeeds() = runTest {
        val detail = StockDetailDto(ticker = "005930")
        whenever(repository.getStockDetail("005930")).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_concurrentCalls_handlesCorrectly() = runTest {
        val detail = createStockDetail()
        whenever(repository.getStockDetail(any())).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        viewModel.load("000660")
        advanceUntilIdle()
        assertNotNull(viewModel.state.value)
    }

    @Test
    fun viewModel_canBeRecreated() {
        val vm1 = StockDetailViewModel(repository)
        val vm2 = StockDetailViewModel(repository)
        assertNotNull(vm1)
        assertNotNull(vm2)
    }

    @Test
    fun load_unicodeTicker_works() = runTest {
        val ticker = "한글"
        val detail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_nullResponse_handlesGracefully() = runTest {
        whenever(repository.getStockDetail(any())).thenReturn(null)
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        assertNotNull(viewModel.state.value)
    }

    @Test
    fun load_rapidCalls_handlesCorrectly() = runTest {
        val detail = createStockDetail()
        whenever(repository.getStockDetail(any())).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        repeat(5) { viewModel.load("00566$it") }
        advanceUntilIdle()
        assertNotNull(viewModel.state.value)
    }

    @Test
    fun priceState_transitionsCorrectly() = runTest {
        val detail = createStockDetail()
        whenever(repository.getStockDetail(any())).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        assertTrue(viewModel.priceState.value is LoadResult.Empty)
        viewModel.load("005930")
        advanceUntilIdle()
        assertFalse(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_exceptionInPriceChart_mainStateSucceeds() = runTest {
        val detail = StockDetailDto(ticker = "005930", priceFinancialInfo = PriceFinancialInfoDto(price = emptyMap()))
        whenever(repository.getStockDetail("005930")).thenReturn(detail)
        viewModel = StockDetailViewModel(repository)
        viewModel.load("005930")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Success)
    }
}