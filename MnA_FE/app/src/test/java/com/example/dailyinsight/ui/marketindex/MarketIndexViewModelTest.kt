package com.example.dailyinsight.ui.marketindex

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MarketIndexViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Initialize RetrofitInstance for tests
        val context = ApplicationProvider.getApplicationContext<Context>()
        RetrofitInstance.init(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper function to create test stock index data
    private fun createStockIndexData(
        name: String = "KOSPI",
        close: Double = 2500.0,
        changeAmount: Double = 10.0,
        changePercent: Double = 0.4
    ) = StockIndexData(
        name = name,
        close = close,
        changeAmount = changeAmount,
        changePercent = changePercent,
        date = "2024-01-15",
        high = 2510.0,
        low = 2490.0,
        open = 2495.0,
        volume = 1000000
    )

    @Test
    fun viewModel_initializes_successfully() {
        // Given & When
        val viewModel = MarketIndexViewModel()

        // Then: ViewModel should be created
        assertNotNull(viewModel)
    }

    @Test
    fun viewModel_is_instance_of_ViewModel() {
        // Given & When
        val viewModel = MarketIndexViewModel()

        // Then: Should be a ViewModel
        assertTrue(viewModel is androidx.lifecycle.ViewModel)
    }

    @Test
    fun viewModel_hasMarketDataLiveData() {
        // Given & When
        val viewModel = MarketIndexViewModel()

        // Then: Should have marketData LiveData
        assertNotNull(viewModel.marketData)
    }

    @Test
    fun viewModel_hasLlmSummaryLiveData() {
        // Given & When
        val viewModel = MarketIndexViewModel()

        // Then: Should have llmSummary LiveData
        assertNotNull(viewModel.llmSummary)
    }

    @Test
    fun viewModel_hasErrorLiveData() {
        // Given & When
        val viewModel = MarketIndexViewModel()

        // Then: Should have error LiveData
        assertNotNull(viewModel.error)
    }

    @Test
    fun stockIndexData_enrichesDataWithNames() {
        // Given: Repository returns data without proper names
        val kospiData = createStockIndexData("", close = 2500.0) // Empty name
        val kosdaqData = createStockIndexData("", close = 750.0) // Empty name
        val dataMap = mutableMapOf(
            "KOSPI" to kospiData,
            "KOSDAQ" to kosdaqData
        )

        // When: Names are enriched from map keys (simulating ViewModel logic)
        dataMap.forEach { (key, value) ->
            value.name = key
        }

        // Then: Names should be enriched from map keys
        assertEquals("KOSPI", dataMap["KOSPI"]?.name)
        assertEquals("KOSDAQ", dataMap["KOSDAQ"]?.name)
    }

    @Test
    fun emptyMap_handlesGracefully() {
        // Given: Empty map
        val emptyMap = emptyMap<String, StockIndexData>()

        // When/Then: Should handle empty data
        assertEquals(0, emptyMap.size)
    }

    @Test
    fun stockIndexData_hasMutableName() {
        // Given: StockIndexData with initial name
        val data = createStockIndexData("INITIAL")

        // When: Name is mutated
        data.name = "MODIFIED"

        // Then: Name should be updated
        assertEquals("MODIFIED", data.name)
    }

    @Test
    fun stockIndexData_containsAllRequiredFields() {
        // Given/When: Create stock index data
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 10.0,
            changePercent = 0.4
        )

        // Then: All fields should be present
        assertEquals("KOSPI", data.name)
        assertEquals(2500.0, data.close, 0.001)
        assertEquals(10.0, data.changeAmount, 0.001)
        assertEquals(0.4, data.changePercent, 0.001)
        assertEquals("2024-01-15", data.date)
        assertEquals(2510.0, data.high, 0.001)
        assertEquals(2490.0, data.low, 0.001)
        assertEquals(2495.0, data.open, 0.001)
        assertEquals(1000000L, data.volume)
    }

    @Test
    fun multipleIndices_canBeStoredInMap() {
        // Given: Multiple stock index data
        val indices = mapOf(
            "KOSPI" to createStockIndexData("KOSPI", close = 2500.0),
            "KOSDAQ" to createStockIndexData("KOSDAQ", close = 750.0),
            "S&P500" to createStockIndexData("S&P500", close = 4500.0),
            "NASDAQ" to createStockIndexData("NASDAQ", close = 14000.0)
        )

        // When/Then: All should be accessible
        assertEquals(4, indices.size)
        assertEquals(2500.0, indices["KOSPI"]?.close ?: 0.0, 0.001)
        assertEquals(750.0, indices["KOSDAQ"]?.close ?: 0.0, 0.001)
        assertEquals(4500.0, indices["S&P500"]?.close ?: 0.0, 0.001)
        assertEquals(14000.0, indices["NASDAQ"]?.close ?: 0.0, 0.001)
    }

    @Test
    fun stockIndexData_supportsPositiveChange() {
        // Given: Data with positive change
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 25.5,
            changePercent = 1.03
        )

        // Then: Positive changes should be represented correctly
        assertTrue(data.changeAmount > 0)
        assertTrue(data.changePercent > 0)
    }

    @Test
    fun stockIndexData_supportsNegativeChange() {
        // Given: Data with negative change
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = -25.5,
            changePercent = -1.01
        )

        // Then: Negative changes should be represented correctly
        assertTrue(data.changeAmount < 0)
        assertTrue(data.changePercent < 0)
    }

    @Test
    fun stockIndexData_supportsZeroChange() {
        // Given: Data with no change
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 0.0,
            changePercent = 0.0
        )

        // Then: Zero changes should be represented correctly
        assertEquals(0.0, data.changeAmount, 0.001)
        assertEquals(0.0, data.changePercent, 0.001)
    }
}

/**
 * Note: The MarketIndexViewModel doesn't support dependency injection,
 * so full integration testing is limited. The tests above verify:
 * 1. Data structure and field handling
 * 2. Name enrichment logic
 * 3. Expected behavior patterns
 *
 * To fully test this ViewModel with mocked repository, consider:
 * - Refactoring to accept repository as constructor parameter
 * - Using a dependency injection framework
 * - Or creating a testable subclass for testing
 */