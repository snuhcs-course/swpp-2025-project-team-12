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

    // Helper function to create test stock detail with price data
    private fun createStockDetail(ticker: String = "005930", hasPriceData: Boolean = true): StockDetailDto {
        val priceData = if (hasPriceData) {
            mapOf(
                "2024-01-01" to 70000.0,
                "2024-01-02" to 71000.0,
                "2024-01-03" to 72000.0
            )
        } else {
            emptyMap()
        }

        return StockDetailDto(
            ticker = ticker,
            name = "테스트 주식",
            price = 72000,
            priceFinancialInfo = if (hasPriceData) PriceFinancialInfoDto(price = priceData) else null
        )
    }

    @Test
    fun load_withValidTicker_updatesStateToSuccess() = runTest {
        // Given: Mock repository returns valid stock detail
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Create ViewModel and load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: State should be Success with correct data
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        assertEquals(stockDetail, (state as LoadResult.Success).data)
    }

    @Test
    fun load_withValidPriceData_updatesPriceStateToSuccess() = runTest {
        // Given: Mock repository returns stock detail with price data
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasPriceData = true)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Create ViewModel and load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Price state should be Success with chart data
        val priceState = viewModel.priceState.value
        assertTrue(priceState is LoadResult.Success)
        val chartUi = (priceState as LoadResult.Success).data
        assertNotNull(chartUi)
        assertNotNull(chartUi.chart)
        assertEquals(3, chartUi.chart.xLabels.size)
    }

    @Test
    fun load_withEmptyPriceData_updatesPriceStateToError() = runTest {
        // Given: Mock repository returns stock detail with no price data
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker, hasPriceData = false)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Create ViewModel and load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Main state should be Success but price state should be Error
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    @Test
    fun load_withRepositoryError_updatesStateToError() = runTest {
        // Given: Mock repository throws exception
        val ticker = "INVALID"
        val exception = IOException("Network error")
        whenever(repository.getStockDetail(ticker)).thenThrow(exception)

        // When: Create ViewModel and load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Both states should be Error
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Error)
        assertEquals(exception, (state as LoadResult.Error).throwable)

        val priceState = viewModel.priceState.value
        assertTrue(priceState is LoadResult.Error)
        assertEquals(exception, (priceState as LoadResult.Error).throwable)
    }

    @Test
    fun load_setsLoadingStateBeforeCompletion() = runTest {
        // Given: Mock repository with delayed response
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Create ViewModel and start load (don't advance idle yet)
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)

        // Then: State should be Loading before coroutine completes
        assertTrue(viewModel.state.value is LoadResult.Loading)
        assertTrue(viewModel.priceState.value is LoadResult.Loading)

        // Advance to complete
        advanceUntilIdle()

        // Now should be Success
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun load_withDifferentTickers_loadsCorrectData() = runTest {
        // Given: Mock repository with different stock details
        val ticker1 = "005930"
        val ticker2 = "000660"
        val stockDetail1 = createStockDetail(ticker1)
        val stockDetail2 = createStockDetail(ticker2)
        whenever(repository.getStockDetail(ticker1)).thenReturn(stockDetail1)
        whenever(repository.getStockDetail(ticker2)).thenReturn(stockDetail2)

        // When: Load different tickers
        viewModel = StockDetailViewModel(repository)

        viewModel.load(ticker1)
        advanceUntilIdle()
        val state1 = viewModel.state.value

        viewModel.load(ticker2)
        advanceUntilIdle()
        val state2 = viewModel.state.value

        // Then: Should load different data for each ticker
        assertTrue(state1 is LoadResult.Success)
        assertEquals(ticker1, (state1 as LoadResult.Success).data.ticker)

        assertTrue(state2 is LoadResult.Success)
        assertEquals(ticker2, (state2 as LoadResult.Success).data.ticker)
    }

    @Test
    fun load_multipleCallsWithSameTicker_updatesState() = runTest {
        // Given: Mock repository returns stock detail
        val ticker = "005930"
        val stockDetail = createStockDetail(ticker)
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Load same ticker multiple times
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Should still have Success state
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.priceState.value is LoadResult.Success)
    }

    @Test
    fun initialState_isEmpty() = runTest {
        // Given: Newly created ViewModel
        viewModel = StockDetailViewModel(repository)

        // Then: Initial state should be Empty
        assertTrue(viewModel.state.value is LoadResult.Empty)
        assertTrue(viewModel.priceState.value is LoadResult.Empty)
    }

    @Test
    fun load_withNullPriceFinancialInfo_updatesPriceStateToError() = runTest {
        // Given: Stock detail with null priceFinancialInfo
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            name = "테스트",
            priceFinancialInfo = null
        )
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Main state Success, price state Error
        assertTrue(viewModel.state.value is LoadResult.Success)
        assertTrue(viewModel.priceState.value is LoadResult.Error)
    }

    @Test
    fun load_withSinglePricePoint_createsSingleEntryChart() = runTest {
        // Given: Stock detail with single price point
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Price state should have chart with 1 entry
        val priceState = viewModel.priceState.value
        assertTrue(priceState is LoadResult.Success)
        val chart = (priceState as LoadResult.Success).data.chart
        assertEquals(1, chart.xLabels.size)
        assertEquals(1, chart.lineData.dataSets[0].entryCount)
    }

    @Test
    fun load_withPriceDataInWrongOrder_chartSortsCorrectly() = runTest {
        // Given: Stock detail with unsorted price data
        val ticker = "005930"
        val stockDetail = StockDetailDto(
            ticker = ticker,
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-01-15" to 75000.0,
                    "2024-01-10" to 70000.0,
                    "2024-01-20" to 80000.0
                )
            )
        )
        whenever(repository.getStockDetail(ticker)).thenReturn(stockDetail)

        // When: Load data
        viewModel = StockDetailViewModel(repository)
        viewModel.load(ticker)
        advanceUntilIdle()

        // Then: Chart labels should be in sorted order
        val priceState = viewModel.priceState.value
        assertTrue(priceState is LoadResult.Success)
        val labels = (priceState as LoadResult.Success).data.chart.xLabels
        assertEquals("2024-01-10", labels[0])
        assertEquals("2024-01-15", labels[1])
        assertEquals("2024-01-20", labels[2])
    }
}
