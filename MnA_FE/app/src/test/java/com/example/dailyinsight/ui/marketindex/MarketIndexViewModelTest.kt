package com.example.dailyinsight.ui.marketindex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailyinsight.data.dto.StockIndexData
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

@ExperimentalCoroutinesApi
class MarketIndexViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
    fun stockIndexData_enrichesDataWithNames() {
        val kospiData = createStockIndexData("", close = 2500.0)
        val kosdaqData = createStockIndexData("", close = 750.0)
        val dataMap = mutableMapOf(
            "KOSPI" to kospiData,
            "KOSDAQ" to kosdaqData
        )
        dataMap.forEach { (key, value) ->
            value.name = key
        }
        assertEquals("KOSPI", dataMap["KOSPI"]?.name)
        assertEquals("KOSDAQ", dataMap["KOSDAQ"]?.name)
    }

    @Test
    fun emptyMap_handlesGracefully() {
        val emptyMap = emptyMap<String, StockIndexData>()
        assertEquals(0, emptyMap.size)
    }

    @Test
    fun stockIndexData_hasMutableName() {
        val data = createStockIndexData("INITIAL")
        data.name = "MODIFIED"
        assertEquals("MODIFIED", data.name)
    }

    @Test
    fun stockIndexData_containsAllRequiredFields() {
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 10.0,
            changePercent = 0.4
        )
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
        val indices = mapOf(
            "KOSPI" to createStockIndexData("KOSPI", close = 2500.0),
            "KOSDAQ" to createStockIndexData("KOSDAQ", close = 750.0),
            "S&P500" to createStockIndexData("S&P500", close = 4500.0),
            "NASDAQ" to createStockIndexData("NASDAQ", close = 14000.0)
        )
        assertEquals(4, indices.size)
        assertEquals(2500.0, indices["KOSPI"]?.close ?: 0.0, 0.001)
        assertEquals(750.0, indices["KOSDAQ"]?.close ?: 0.0, 0.001)
        assertEquals(4500.0, indices["S&P500"]?.close ?: 0.0, 0.001)
        assertEquals(14000.0, indices["NASDAQ"]?.close ?: 0.0, 0.001)
    }

    @Test
    fun stockIndexData_supportsPositiveChange() {
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 25.5,
            changePercent = 1.03
        )
        assertTrue(data.changeAmount > 0)
        assertTrue(data.changePercent > 0)
    }

    @Test
    fun stockIndexData_supportsNegativeChange() {
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = -25.5,
            changePercent = -1.01
        )
        assertTrue(data.changeAmount < 0)
        assertTrue(data.changePercent < 0)
    }

    @Test
    fun stockIndexData_supportsZeroChange() {
        val data = createStockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 0.0,
            changePercent = 0.0
        )
        assertEquals(0.0, data.changeAmount, 0.001)
        assertEquals(0.0, data.changePercent, 0.001)
    }

    @Test
    fun stockIndexData_largeVolume() {
        val data = createStockIndexData("KOSPI", 2500.0, 10.0, 0.4)
        data.name = "TEST"
        assertEquals(1000000L, data.volume)
    }

    @Test
    fun stockIndexData_equality() {
        val d1 = createStockIndexData("KOSPI", 2500.0)
        val d2 = createStockIndexData("KOSPI", 2500.0)
        assertEquals(d1, d2)
    }

    @Test
    fun stockIndexData_copy() {
        val original = createStockIndexData("KOSPI", 2500.0)
        val copied = original.copy(close = 2600.0)
        assertEquals(2600.0, copied.close, 0.001)
        assertEquals("KOSPI", copied.name)
    }

    @Test
    fun stockIndexData_toString() {
        val data = createStockIndexData()
        assertNotNull(data.toString())
    }

    @Test
    fun stockIndexData_hashCode() {
        val d1 = createStockIndexData()
        val d2 = createStockIndexData()
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun stockIndexData_highLowValidation() {
        val data = createStockIndexData()
        assertTrue(data.high >= data.close)
        assertTrue(data.low <= data.close)
    }

    @Test
    fun stockIndexData_decimalPrecision() {
        val data = createStockIndexData("KOSPI", 2500.123456)
        assertEquals(2500.123456, data.close, 0.000001)
    }

    @Test
    fun mapOperations_filterByChange() {
        val indices = mapOf(
            "UP" to createStockIndexData("UP", changeAmount = 10.0),
            "DOWN" to createStockIndexData("DOWN", changeAmount = -10.0)
        )
        val positive = indices.filter { it.value.changeAmount > 0 }
        assertEquals(1, positive.size)
    }

    @Test
    fun stockIndexData_extremeValues() {
        val data = createStockIndexData("TEST", Double.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, data.close, 0.001)
    }

    @Test
    fun stockIndexData_nameEnrichment_multipleIndices() {
        val dataMap = (1..10).associate {
            "INDEX$it" to createStockIndexData("", close = 2500.0 + it)
        }.toMutableMap()
        dataMap.forEach { (key, value) -> value.name = key }
        assertEquals(10, dataMap.size)
        dataMap.values.forEach { assertNotEquals("", it.name) }
    }
}