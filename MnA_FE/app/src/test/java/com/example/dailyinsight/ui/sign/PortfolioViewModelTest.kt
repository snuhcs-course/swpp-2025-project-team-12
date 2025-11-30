package com.example.dailyinsight.ui.sign

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailyinsight.data.StockRepository
import com.example.dailyinsight.data.dto.StockItem
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
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@ExperimentalCoroutinesApi
class PortfolioViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: StockRepository
    private lateinit var viewModel: PortfolioViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = PortfolioViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createStockItem(ticker: String = "005930", name: String = "삼성전자") =
        StockItem(ticker = ticker, name = name)

    // ===== Toggle Selection Tests =====

    @Test
    fun toggleSelection_add_addsToSet() {
        viewModel.toggleSelection("005930", true)
        assertTrue(viewModel.selectedTickers.value?.contains("005930") == true)
    }

    @Test
    fun toggleSelection_remove_removesFromSet() {
        viewModel.toggleSelection("005930", true)
        viewModel.toggleSelection("005930", false)
        assertFalse(viewModel.selectedTickers.value?.contains("005930") == true)
    }

    @Test
    fun toggleSelection_multiple() {
        viewModel.toggleSelection("001", true)
        viewModel.toggleSelection("002", true)
        assertEquals(2, viewModel.selectedTickers.value?.size)
    }

    @Test
    fun toggleSelection_sameTickerTwice() {
        viewModel.toggleSelection("005930", true)
        viewModel.toggleSelection("005930", true)
        assertEquals(1, viewModel.selectedTickers.value?.size)
    }

    // ===== Toggle Select None Tests =====

    @Test
    fun toggleSelectNone_true_clearsSelection() {
        viewModel.toggleSelection("001", true)
        viewModel.toggleSelectNone(true)
        assertEquals(0, viewModel.selectedTickers.value?.size)
    }

    @Test
    fun toggleSelectNone_false_restoresPrevious() {
        viewModel.toggleSelection("001", true)
        viewModel.toggleSelectNone(true)
        viewModel.toggleSelectNone(false)
        assertTrue(viewModel.selectedTickers.value?.contains("001") == true)
    }

    @Test
    fun toggleSelectNone_updatesSelectNoneState() {
        viewModel.toggleSelectNone(true)
        assertTrue(viewModel.selectNone.value == true)

        viewModel.toggleSelectNone(false)
        assertFalse(viewModel.selectNone.value == true)
    }

    // ===== Initial State Tests =====

    @Test
    fun viewModel_initialization() {
        assertNotNull(viewModel)
        assertNotNull(viewModel.stockItems)
    }

    @Test
    fun filteredStocks_initiallyEmpty() {
        assertTrue(viewModel.filteredStocks.value?.isEmpty() == true)
    }

    @Test
    fun selectedTickers_initiallyEmpty() {
        assertEquals(0, viewModel.selectedTickers.value?.size)
    }

    @Test
    fun selectNone_initiallyFalse() {
        assertEquals(false, viewModel.selectNone.value)
    }

    // ===== FetchStocks Tests =====

    @Test
    fun fetchStocks_success_updatesStockItems() = runTest {
        val stocks = listOf(
            createStockItem("005930", "삼성전자"),
            createStockItem("000660", "SK하이닉스")
        )
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()

        assertEquals(2, viewModel.stockItems.value?.size)
        assertEquals("삼성전자", viewModel.stockItems.value?.get(0)?.name)
    }

    @Test
    fun fetchStocks_success_updatesFilteredStocks() = runTest {
        val stocks = listOf(createStockItem("005930", "삼성전자"))
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()

        assertEquals(1, viewModel.filteredStocks.value?.size)
    }

    @Test
    fun fetchStocks_error_handlesGracefully() = runTest {
        whenever(repository.fetchStocks()).thenAnswer { throw IOException("Network error") }

        viewModel.fetchStocks()
        advanceUntilIdle()

        // 에러 시 크래시 없이 null 유지
        assertNull(viewModel.stockItems.value)
    }

    @Test
    fun fetchStocks_emptyList_handlesGracefully() = runTest {
        whenever(repository.fetchStocks()).thenReturn(emptyList())

        viewModel.fetchStocks()
        advanceUntilIdle()

        assertTrue(viewModel.stockItems.value?.isEmpty() == true)
    }

    // ===== SearchStocks Tests =====

    @Test
    fun searchStocks_withQuery_filtersResults() = runTest {
        val stocks = listOf(
            createStockItem("005930", "삼성전자"),
            createStockItem("000660", "SK하이닉스"),
            createStockItem("035420", "NAVER")
        )
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()
        viewModel.searchStocks("삼성")

        assertEquals(1, viewModel.filteredStocks.value?.size)
        assertEquals("삼성전자", viewModel.filteredStocks.value?.get(0)?.name)
    }

    @Test
    fun searchStocks_emptyQuery_showsAll() = runTest {
        val stocks = listOf(
            createStockItem("005930", "삼성전자"),
            createStockItem("000660", "SK하이닉스")
        )
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()
        viewModel.searchStocks("")

        assertEquals(2, viewModel.filteredStocks.value?.size)
    }

    @Test
    fun searchStocks_blankQuery_showsAll() = runTest {
        val stocks = listOf(
            createStockItem("005930", "삼성전자"),
            createStockItem("000660", "SK하이닉스")
        )
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()
        viewModel.searchStocks("   ")

        assertEquals(2, viewModel.filteredStocks.value?.size)
    }

    @Test
    fun searchStocks_caseInsensitive() = runTest {
        val stocks = listOf(
            createStockItem("035420", "NAVER"),
            createStockItem("035720", "kakao")
        )
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()
        viewModel.searchStocks("naver")

        assertEquals(1, viewModel.filteredStocks.value?.size)
        assertEquals("NAVER", viewModel.filteredStocks.value?.get(0)?.name)
    }

    @Test
    fun searchStocks_noMatch_returnsEmpty() = runTest {
        val stocks = listOf(createStockItem("005930", "삼성전자"))
        whenever(repository.fetchStocks()).thenReturn(stocks)

        viewModel.fetchStocks()
        advanceUntilIdle()
        viewModel.searchStocks("애플")

        assertTrue(viewModel.filteredStocks.value?.isEmpty() == true)
    }

    @Test
    fun searchStocks_beforeFetch_returnsEmpty() {
        viewModel.searchStocks("삼성")
        assertTrue(viewModel.filteredStocks.value?.isEmpty() == true)
    }

    // ===== SubmitSelectedStocks Tests =====

    @Test
    fun submitSelectedStocks_success_callsCallback() = runTest {
        whenever(repository.submitSelectedStocks(any())).thenReturn(true)

        viewModel.toggleSelection("005930", true)

        var callbackResult: Boolean? = null
        viewModel.submitSelectedStocks { result ->
            callbackResult = result
        }
        advanceUntilIdle()

        assertTrue(callbackResult == true)
        assertTrue(viewModel.submitResult.value == true)
    }

    @Test
    fun submitSelectedStocks_failure_callsCallbackWithFalse() = runTest {
        whenever(repository.submitSelectedStocks(any())).thenReturn(false)

        viewModel.toggleSelection("005930", true)

        var callbackResult: Boolean? = null
        viewModel.submitSelectedStocks { result ->
            callbackResult = result
        }
        advanceUntilIdle()

        assertFalse(callbackResult == true)
        assertFalse(viewModel.submitResult.value == true)
    }

    @Test
    fun submitSelectedStocks_emptySelection_submitsEmptySet() = runTest {
        whenever(repository.submitSelectedStocks(emptySet())).thenReturn(true)

        viewModel.submitSelectedStocks { }
        advanceUntilIdle()

        verify(repository).submitSelectedStocks(emptySet())
    }

    @Test
    fun submitSelectedStocks_error_handlesGracefully() = runTest {
        whenever(repository.submitSelectedStocks(any())).thenAnswer { throw IOException("Error") }

        viewModel.toggleSelection("005930", true)

        // 에러 발생해도 크래시 없음
        viewModel.submitSelectedStocks { }
        advanceUntilIdle()

        assertNotNull(viewModel)
    }
}