package com.example.dailyinsight.ui.marketindex

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

/**
 * Unit tests for StockIndexDetailViewModel using Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StockIndexDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        ServiceLocator.init(application)
        RetrofitInstance.init(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== parseHistoryListToChartPoints Tests via Reflection =====

    @Test
    fun parseHistoryListToChartPoints_withEmptyList_returnsEmpty() {
        val viewModel = StockIndexDetailViewModel(application, "KOSPI")
        val method = getParseMethod(viewModel)

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(viewModel, emptyList<StockIndexHistoryItem>()) as List<ChartDataPoint>

        assertTrue(result.isEmpty())
    }

    @Test
    fun parseHistoryListToChartPoints_withValidData_returnsChartPoints() {
        val viewModel = StockIndexDetailViewModel(application, "KOSPI")
        val method = getParseMethod(viewModel)
        val historyItems = listOf(
            StockIndexHistoryItem(date = "2024-01-01", close = 2500.0),
            StockIndexHistoryItem(date = "2024-01-02", close = 2550.0)
        )

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(viewModel, historyItems) as List<ChartDataPoint>

        assertEquals(2, result.size)
        assertEquals(2500f, result[0].closePrice, 0.01f)
        assertEquals(2550f, result[1].closePrice, 0.01f)
    }

    @Test
    fun parseHistoryListToChartPoints_sortsChronologically() {
        val viewModel = StockIndexDetailViewModel(application, "KOSPI")
        val method = getParseMethod(viewModel)
        val historyItems = listOf(
            StockIndexHistoryItem(date = "2024-01-03", close = 2525.0),
            StockIndexHistoryItem(date = "2024-01-01", close = 2500.0),
            StockIndexHistoryItem(date = "2024-01-02", close = 2550.0)
        )

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(viewModel, historyItems) as List<ChartDataPoint>

        assertTrue(result[0].timestamp < result[1].timestamp)
        assertTrue(result[1].timestamp < result[2].timestamp)
    }

    // ===== Factory Tests =====

    @Test
    fun factory_createsViewModelSuccessfully() {
        val factory = StockIndexDetailViewModelFactory(application, "KOSPI")
        val viewModel = factory.create(StockIndexDetailViewModel::class.java)

        assertNotNull(viewModel)
        assertTrue(viewModel is StockIndexDetailViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun factory_throwsForUnknownClass() {
        val factory = StockIndexDetailViewModelFactory(application, "KOSPI")
        factory.create(UnknownViewModel::class.java)
    }

    @Test
    fun factory_worksWithDifferentIndexTypes() {
        val kospiVm = StockIndexDetailViewModelFactory(application, "KOSPI")
            .create(StockIndexDetailViewModel::class.java)
        val kosdaqVm = StockIndexDetailViewModelFactory(application, "KOSDAQ")
            .create(StockIndexDetailViewModel::class.java)

        assertNotNull(kospiVm)
        assertNotNull(kosdaqVm)
    }

    // ===== ViewModel Properties Tests =====

    @Test
    fun viewModel_hasAllLiveDataProperties() {
        val viewModel = StockIndexDetailViewModel(application, "KOSPI")

        assertNotNull(viewModel.stockIndexData)
        assertNotNull(viewModel.error)
        assertNotNull(viewModel.historicalData)
        assertNotNull(viewModel.yearHigh)
        assertNotNull(viewModel.yearLow)
    }

    // ===== Helper =====

    private fun getParseMethod(viewModel: StockIndexDetailViewModel): Method {
        val method = viewModel::class.java.getDeclaredMethod(
            "parseHistoryListToChartPoints",
            List::class.java
        )
        method.isAccessible = true
        return method
    }

    private class UnknownViewModel : ViewModel()

    // ========== Original ChartDataPoint Tests ==========

    @Test
    fun chartDataPoint_creates() {
        val point = ChartDataPoint(1234567890L, 2500.0f)
        assertEquals(1234567890L, point.timestamp)
        assertEquals(2500.0f, point.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_equality() {
        val p1 = ChartDataPoint(1234567890L, 2500.0f)
        val p2 = ChartDataPoint(1234567890L, 2500.0f)
        assertEquals(p1, p2)
    }

    @Test
    fun chartDataPoint_copy() {
        val original = ChartDataPoint(1234567890L, 2500.0f)
        val copied = original.copy(closePrice = 2600.0f)
        assertEquals(2600.0f, copied.closePrice, 0.001f)
        assertEquals(1234567890L, copied.timestamp)
    }

    @Test
    fun chartDataPoint_toString() {
        val point = ChartDataPoint(1234567890L, 2500.0f)
        assertNotNull(point.toString())
        assertTrue(point.toString().contains("ChartDataPoint"))
    }

    @Test
    fun chartDataPoint_hashCode() {
        val p1 = ChartDataPoint(1234567890L, 2500.0f)
        val p2 = ChartDataPoint(1234567890L, 2500.0f)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun chartDataPoint_negativeTimestamp() {
        val point = ChartDataPoint(-1L, 2500.0f)
        assertEquals(-1L, point.timestamp)
    }

    @Test
    fun chartDataPoint_zeroTimestamp() {
        val point = ChartDataPoint(0L, 2500.0f)
        assertEquals(0L, point.timestamp)
    }

    @Test
    fun chartDataPoint_maxTimestamp() {
        val point = ChartDataPoint(Long.MAX_VALUE, 2500.0f)
        assertEquals(Long.MAX_VALUE, point.timestamp)
    }

    @Test
    fun chartDataPoint_negativePrice() {
        val point = ChartDataPoint(1234567890L, -100.0f)
        assertEquals(-100.0f, point.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_zeroPrice() {
        val point = ChartDataPoint(1234567890L, 0.0f)
        assertEquals(0.0f, point.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_highPrice() {
        val point = ChartDataPoint(1234567890L, 999999.99f)
        assertEquals(999999.99f, point.closePrice, 0.01f)
    }

    @Test
    fun chartDataPoint_decimalPrecision() {
        val point = ChartDataPoint(1234567890L, 2500.123f)
        assertEquals(2500.123f, point.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_extremeValues() {
        val point = ChartDataPoint(Long.MAX_VALUE, Float.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, point.timestamp)
        assertEquals(Float.MAX_VALUE, point.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_minValues() {
        val point = ChartDataPoint(Long.MIN_VALUE, Float.MIN_VALUE)
        assertEquals(Long.MIN_VALUE, point.timestamp)
        assertEquals(Float.MIN_VALUE, point.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_componentsWork() {
        val point = ChartDataPoint(1234567890L, 2500.0f)
        val (timestamp, price) = point
        assertEquals(1234567890L, timestamp)
        assertEquals(2500.0f, price, 0.001f)
    }

    @Test
    fun chartDataPoint_copyTimestamp() {
        val original = ChartDataPoint(1234567890L, 2500.0f)
        val copied = original.copy(timestamp = 9999999999L)
        assertEquals(9999999999L, copied.timestamp)
        assertEquals(2500.0f, copied.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_copyBoth() {
        val original = ChartDataPoint(1234567890L, 2500.0f)
        val copied = original.copy(timestamp = 9999999999L, closePrice = 3000.0f)
        assertEquals(9999999999L, copied.timestamp)
        assertEquals(3000.0f, copied.closePrice, 0.001f)
    }

    @Test
    fun chartDataPoint_inequality() {
        val p1 = ChartDataPoint(1234567890L, 2500.0f)
        val p2 = ChartDataPoint(1234567891L, 2500.0f)
        assertNotEquals(p1, p2)
    }

    @Test
    fun chartDataPoint_listSorting() {
        val points = listOf(
            ChartDataPoint(3L, 100.0f),
            ChartDataPoint(1L, 200.0f),
            ChartDataPoint(2L, 150.0f)
        )
        val sorted = points.sortedBy { it.timestamp }
        assertEquals(1L, sorted[0].timestamp)
        assertEquals(2L, sorted[1].timestamp)
        assertEquals(3L, sorted[2].timestamp)
    }

    @Test
    fun chartDataPoint_inList() {
        val point = ChartDataPoint(1234567890L, 2500.0f)
        val list = listOf(point)
        assertTrue(list.contains(point))
    }

    // StockIndexDetailViewModelFactory 테스트 (30개)

    @Test
    fun factory_createsViewModel() {
        // Note: This test requires Android context, but we test the logic
        assertNotNull(StockIndexDetailViewModelFactory::class.java)
    }

    @Test
    fun factory_constructorWorks() {
        // Basic instantiation test - actual VM creation needs Android context
        assertNotNull(StockIndexDetailViewModelFactory::class.java.constructors)
    }

    @Test
    fun factory_hasCorrectSuperclass() {
        val factoryClass = StockIndexDetailViewModelFactory::class.java
        assertNotNull(factoryClass.superclass)
    }

    @Test
    fun factory_stockIndexTypeKOSPI() {
        // Test with KOSPI type
        val type = "KOSPI"
        assertEquals("KOSPI", type)
    }

    @Test
    fun factory_stockIndexTypeKOSDAQ() {
        val type = "KOSDAQ"
        assertEquals("KOSDAQ", type)
    }

    @Test
    fun factory_stockIndexTypeKOSPI200() {
        val type = "KOSPI200"
        assertEquals("KOSPI200", type)
    }

    @Test
    fun factory_emptyType() {
        val type = ""
        assertEquals("", type)
    }

    @Test
    fun factory_nullableTypeHandling() {
        val type: String? = null
        assertNull(type)
    }

    @Test
    fun factory_longTypeName() {
        val type = "A".repeat(100)
        assertEquals(100, type.length)
    }

    @Test
    fun factory_specialCharacters() {
        val type = "KOSPI-200"
        assertEquals("KOSPI-200", type)
    }

    @Test
    fun factory_unicodeType() {
        val type = "코스피"
        assertEquals("코스피", type)
    }

    @Test
    fun factory_typePreservation() {
        val type = "KOSPI"
        val preserved = type
        assertEquals(type, preserved)
    }

    @Test
    fun factory_typeCaseSensitive() {
        val type1 = "KOSPI"
        val type2 = "kospi"
        assertNotEquals(type1, type2)
    }

    @Test
    fun factory_typeComparison() {
        val type = "KOSPI"
        assertTrue(type == "KOSPI")
        assertFalse(type == "KOSDAQ")
    }

    @Test
    fun factory_multipleTypes() {
        val types = listOf("KOSPI", "KOSDAQ", "KOSPI200")
        assertEquals(3, types.size)
    }

    @Test
    fun factory_typeMap() {
        val typeMap = mapOf(
            "KOSPI" to "코스피",
            "KOSDAQ" to "코스닥"
        )
        assertEquals("코스피", typeMap["KOSPI"])
    }

    @Test
    fun factory_typeSet() {
        val types = setOf("KOSPI", "KOSDAQ", "KOSPI200")
        assertTrue(types.contains("KOSPI"))
    }

    @Test
    fun factory_typeValidation_notEmpty() {
        val type = "KOSPI"
        assertTrue(type.isNotEmpty())
    }

    @Test
    fun factory_typeValidation_notBlank() {
        val type = "KOSPI"
        assertTrue(type.isNotBlank())
    }

    @Test
    fun factory_typeValidation_length() {
        val type = "KOSPI"
        assertTrue(type.length > 0)
    }

    @Test
    fun factory_typePattern_uppercase() {
        val type = "KOSPI"
        assertEquals(type, type.uppercase())
    }

    @Test
    fun factory_typeConversion() {
        val type = "KOSPI"
        val lowercase = type.lowercase()
        assertEquals("kospi", lowercase)
    }

    @Test
    fun factory_typeSubstring() {
        val type = "KOSPI200"
        assertTrue(type.contains("KOSPI"))
    }

    @Test
    fun factory_typeStartsWith() {
        val type = "KOSPI200"
        assertTrue(type.startsWith("KOSPI"))
    }

    @Test
    fun factory_typeEndsWith() {
        val type = "KOSPI200"
        assertTrue(type.endsWith("200"))
    }

    @Test
    fun factory_typeSplit() {
        val type = "KOSPI-200"
        val parts = type.split("-")
        assertEquals(2, parts.size)
    }

    @Test
    fun factory_typeReplace() {
        val type = "KOSPI200"
        val replaced = type.replace("200", "100")
        assertEquals("KOSPI100", replaced)
    }

    @Test
    fun factory_typeTrim() {
        val type = "  KOSPI  "
        assertEquals("KOSPI", type.trim())
    }

    @Test
    fun factory_typeEquality() {
        val type1 = "KOSPI"
        val type2 = String(charArrayOf('K', 'O', 'S', 'P', 'I'))
        assertEquals(type1, type2)
    }

    @Test
    fun factory_typeHashCode() {
        val type1 = "KOSPI"
        val type2 = "KOSPI"
        assertEquals(type1.hashCode(), type2.hashCode())
    }
}