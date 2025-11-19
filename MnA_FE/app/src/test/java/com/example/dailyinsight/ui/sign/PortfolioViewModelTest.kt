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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
    fun viewModel_initialization() {
        assertNotNull(viewModel)
        assertNotNull(viewModel.stockItems)
    }

    @Test
    fun filteredStocks_initiallyEmpty() {
        assertTrue(viewModel.filteredStocks.value?.isEmpty() == true)
    }

    @Test
    fun toggleSelection_multiple() {
        viewModel.toggleSelection("001", true)
        viewModel.toggleSelection("002", true)
        assertEquals(2, viewModel.selectedTickers.value?.size)
    }

    @Test
    fun selectedTickers_initiallyEmpty() {
        assertEquals(0, viewModel.selectedTickers.value?.size)
    }

    @Test
    fun selectNone_initiallyFalse() {
        assertEquals(false, viewModel.selectNone.value)
    }

    @Test
    fun toggleSelection_sameTickerTwice() {
        viewModel.toggleSelection("005930", true)
        viewModel.toggleSelection("005930", true)
        assertEquals(1, viewModel.selectedTickers.value?.size)
    }
}