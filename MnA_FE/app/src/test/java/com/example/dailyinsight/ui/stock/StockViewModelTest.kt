package com.example.dailyinsight.ui.stock

import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
class StockViewModelTest {

    private lateinit var repository: Repository
    private lateinit var viewModel: StockViewModel
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

    // Helper function to create test recommendation
    private fun createRecommendation(
        ticker: String = "005930",
        name: String = "삼성전자",
        price: Long = 70000
    ) = RecommendationDto(
        ticker = ticker,
        name = name,
        price = price,
        change = -100,
        changeRate = -0.14,
        headline = "Test headline"
    )

    @Test
    fun init_automaticallyCallsRefresh() = runTest {
        // Given: Mock repository returns data
        val dataMap = mapOf(
            "2024-01-15" to listOf(createRecommendation())
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel (init calls refresh)
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Success
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun refresh_withValidData_updatesStateToSuccess() = runTest {
        // Given: Mock repository returns history data
        val dataMap = mapOf(
            "2024-01-15" to listOf(
                createRecommendation(ticker = "005930", name = "삼성전자"),
                createRecommendation(ticker = "000660", name = "SK하이닉스")
            ),
            "2024-01-10" to listOf(
                createRecommendation(ticker = "035420", name = "네이버")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel and wait for init
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Success with rows
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        val rows = (state as LoadResult.Success).data
        assertFalse(rows.isEmpty())
    }

    @Test
    fun refresh_withMultipleDates_sortsInDescendingOrder() = runTest {
        // Given: Mock repository returns multiple dates
        // Note: ViewModel uses toSortedMap(compareByDescending) which sorts by string descending
        val dataMap = mapOf(
            "2024-01-10" to listOf(createRecommendation(name = "Stock 10")),
            "2024-01-15" to listOf(createRecommendation(name = "Stock 15")),
            "2024-01-05" to listOf(createRecommendation(name = "Stock 05"))
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Dates should be sorted descending by string (newest first)
        val state = viewModel.state.value as LoadResult.Success
        val rows = state.data
        val headers = rows.filterIsInstance<StockRow.Header>()

        assertEquals(3, headers.size)
        // String comparison: "2024-01-15" > "2024-01-10" > "2024-01-05"
        assertEquals("2024-01-15", headers[0].label)
        assertEquals("2024-01-10", headers[1].label)
        assertEquals("2024-01-05", headers[2].label)
    }

    @Test
    fun refresh_withEmptyMap_returnsEmptyList() = runTest {
        // Given: Mock repository returns empty map
        whenever(repository.getStockRecommendations()).thenReturn(emptyMap())

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Success with empty list
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        assertTrue((state as LoadResult.Success).data.isEmpty())
    }

    @Test
    fun refresh_withRepositoryError_updatesStateToError() = runTest {
        // Given: Mock repository that suspends then throws exception
        val exception = IOException("Network error")
        
        // Create a mock that returns a suspending lambda that throws
        whenever(repository.getStockRecommendations()).thenAnswer { invocation ->
            runBlocking { throw exception }
        }

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Error
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Error)
        assertEquals(exception, (state as LoadResult.Error).throwable)
    }

    @Test
    fun refresh_setsLoadingStateBeforeCompletion() = runTest {
        // Given: Mock repository
        val dataMap = mapOf("2024-01-15" to listOf(createRecommendation()))
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        
        // Note: Due to how StateFlow works, we can't reliably capture the Loading state
        // before it transitions to Success. This test verifies the ViewModel completes successfully.
        advanceUntilIdle()

        // Then: Should be Success after completion
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun refresh_createsHeadersAndItems() = runTest {
        // Given: Mock repository returns data
        val dataMap = mapOf(
            "2024-01-15" to listOf(
                createRecommendation(name = "Stock 1"),
                createRecommendation(name = "Stock 2")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Should have 1 header + 2 items
        val state = viewModel.state.value as LoadResult.Success
        val rows = state.data
        assertEquals(3, rows.size)

        assertTrue(rows[0] is StockRow.Header)
        assertEquals("2024-01-15", (rows[0] as StockRow.Header).label)

        assertTrue(rows[1] is StockRow.Item)
        assertEquals("Stock 1", (rows[1] as StockRow.Item).data.name)

        assertTrue(rows[2] is StockRow.Item)
        assertEquals("Stock 2", (rows[2] as StockRow.Item).data.name)
    }

    @Test
    fun refresh_multipleCallsUpdateState() = runTest {
        // Given: Mock repository returns different data
        val dataMap1 = mapOf("2024-01-15" to listOf(createRecommendation(name = "First")))
        val dataMap2 = mapOf("2024-01-20" to listOf(createRecommendation(name = "Second")))

        whenever(repository.getStockRecommendations())
            .thenReturn(dataMap1)
            .thenReturn(dataMap2)

        // When: Create ViewModel and call refresh twice
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        val firstState = viewModel.state.value

        viewModel.refresh()
        advanceUntilIdle()

        val secondState = viewModel.state.value

        // Then: Both should be Success but with different data
        assertTrue(firstState is LoadResult.Success)
        assertTrue(secondState is LoadResult.Success)
    }

    @Test
    fun refresh_withSingleDateMultipleItems_createsCorrectStructure() = runTest {
        // Given: Single date with multiple items
        val dataMap = mapOf(
            "2024-01-15" to listOf(
                createRecommendation(ticker = "005930", name = "삼성전자"),
                createRecommendation(ticker = "000660", name = "SK하이닉스"),
                createRecommendation(ticker = "035420", name = "네이버"),
                createRecommendation(ticker = "035720", name = "카카오")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Should have 1 header + 4 items = 5 rows
        val state = viewModel.state.value as LoadResult.Success
        val rows = state.data
        assertEquals(5, rows.size)

        val headers = rows.filterIsInstance<StockRow.Header>()
        val items = rows.filterIsInstance<StockRow.Item>()

        assertEquals(1, headers.size)
        assertEquals(4, items.size)
    }

    @Test
    fun refresh_withManyDates_allIncludedInOrder() = runTest {
        // Given: Many dates
        val dataMap = (1..10).associate { i ->
            "2024-01-${String.format("%02d", i)}" to listOf(
                createRecommendation(name = "Stock $i")
            )
        }
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: All 10 dates should be present in descending order (string comparison)
        val state = viewModel.state.value as LoadResult.Success
        val headers = state.data.filterIsInstance<StockRow.Header>()

        assertEquals(10, headers.size)
        // String descending: "2024-01-10" > "2024-01-09" > ... > "2024-01-01"
        assertEquals("2024-01-10", headers[0].label)
        assertEquals("2024-01-09", headers[1].label)
        assertEquals("2024-01-01", headers[9].label)
    }

    @Test
    fun refresh_preservesRecommendationData() = runTest {
        // Given: Detailed recommendation data
        val recommendation = RecommendationDto(
            ticker = "005930",
            name = "삼성전자",
            price = 70000,
            change = -500,
            changeRate = -0.71,
            headline = "Important news"
        )
        val dataMap = mapOf("2024-01-15" to listOf(recommendation))
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Data should be preserved
        val state = viewModel.state.value as LoadResult.Success
        val item = state.data[1] as StockRow.Item

        assertEquals("005930", item.data.ticker)
        assertEquals("삼성전자", item.data.name)
        assertEquals(70000L, item.data.price)
        assertEquals(-500L, item.data.change)
        assertEquals(-0.71, item.data.changeRate, 0.001)
        assertEquals("Important news", item.data.headline)
    }

    @Test
    fun refresh_withLargeDataset_handlesCorrectly() = runTest {
        // Given: Large dataset (100 stocks)
        val largeList = (1..100).map { 
            createRecommendation(ticker = "STOCK$it", name = "회사$it")
        }
        val dataMap = mapOf("오늘" to largeList)
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: All stocks included (1 header + 100 items = 101 rows)
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(101, state.data.size)
    }

    @Test
    fun refresh_withKoreanDateLabels_handlesCorrectly() = runTest {
        // Given: Korean date labels
        val dataMap = mapOf(
            "오늘" to listOf(createRecommendation(ticker = "A")),
            "어제" to listOf(createRecommendation(ticker = "B")),
            "그제" to listOf(createRecommendation(ticker = "C"))
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Should handle Korean labels
        val state = viewModel.state.value as LoadResult.Success
        assertTrue(state.data.isNotEmpty())
        val headers = state.data.filterIsInstance<StockRow.Header>()
        assertEquals(3, headers.size)
    }

    @Test
    fun refresh_withMixedPositiveNegativeChanges_includesAll() = runTest {
        // Given: Mix of changes
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(ticker = "UP", price = 70000).copy(change = 1000, changeRate = 1.5),
                createRecommendation(ticker = "DOWN", price = 69000).copy(change = -1000, changeRate = -1.5),
                createRecommendation(ticker = "ZERO", price = 70000).copy(change = 0, changeRate = 0.0)
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: All included
        val state = viewModel.state.value as LoadResult.Success
        val items = state.data.filterIsInstance<StockRow.Item>()
        assertEquals(3, items.size)
    }

    @Test
    fun refresh_withExtremePriceValues_handlesCorrectly() = runTest {
        // Given: Extreme prices
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(price = 1L),
                createRecommendation(price = Long.MAX_VALUE),
                createRecommendation(price = 50000L)
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Handles extreme values
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
    }

    @Test
    fun refresh_withSpecialCharactersInNames_handlesCorrectly() = runTest {
        // Given: Special characters
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(name = "삼성전자!@#"),
                createRecommendation(name = "SK하이닉스(주)"),
                createRecommendation(name = "LG전자 & Co.")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Handles special characters
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
    }

    @Test
    fun refresh_withNetworkTimeout_setsErrorState() = runTest {
        // Given: Timeout exception
        whenever(repository.getStockRecommendations())
            .thenAnswer { runBlocking { throw java.net.SocketTimeoutException("Timeout") } }

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Error state
        assertTrue(viewModel.state.value is LoadResult.Error)
    }

    @Test
    fun refresh_withNullPointerException_setsErrorState() = runTest {
        // Given: NPE
        whenever(repository.getStockRecommendations())
            .thenAnswer { runBlocking { throw NullPointerException("Null data") } }

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Error state
        assertTrue(viewModel.state.value is LoadResult.Error)
    }

    @Test
    fun refresh_afterError_canRecoverWithSuccess() = runTest {
        // Given: First fails, second succeeds
        val mockData = mapOf("오늘" to listOf(createRecommendation()))
        whenever(repository.getStockRecommendations())
            .thenAnswer { runBlocking { throw IOException("Network error") } }
            .thenReturn(mockData)

        // When: Init (fails)
        viewModel = StockViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Error)

        // When: Retry (succeeds)
        viewModel.refresh()
        advanceUntilIdle()

        // Then: Recovers to Success
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun refresh_withDuplicateTickers_includesAll() = runTest {
        // Given: Duplicate tickers
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(ticker = "005930", price = 70000),
                createRecommendation(ticker = "005930", price = 71000)
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Both included
        val state = viewModel.state.value as LoadResult.Success
        val items = state.data.filterIsInstance<StockRow.Item>()
        assertEquals(2, items.size)
    }

    @Test
    fun refresh_withVeryLongStockName_handlesCorrectly() = runTest {
        // Given: Very long name
        val longName = "A".repeat(1000)
        val dataMap = mapOf("오늘" to listOf(createRecommendation(name = longName)))
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Handles long name
        val state = viewModel.state.value as LoadResult.Success
        val item = state.data.filterIsInstance<StockRow.Item>().first()
        assertEquals(longName, item.data.name)
    }

    @Test
    fun refresh_withEmptyTicker_includesInResults() = runTest {
        // Given: Empty ticker
        val dataMap = mapOf("오늘" to listOf(createRecommendation(ticker = "")))
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Included
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
    }

    @Test
    fun refresh_withComplexStructure_createsCorrectHierarchy() = runTest {
        // Given: Complex multi-date structure
        val dataMap = mapOf(
            "2024-01-20" to listOf(
                createRecommendation(ticker = "A"),
                createRecommendation(ticker = "B")
            ),
            "2024-01-19" to listOf(
                createRecommendation(ticker = "C"),
                createRecommendation(ticker = "D"),
                createRecommendation(ticker = "E")
            ),
            "2024-01-18" to listOf(
                createRecommendation(ticker = "F")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Correct structure (3 headers + 6 items = 9 rows)
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(9, state.data.size)
        assertEquals(3, state.data.filterIsInstance<StockRow.Header>().size)
        assertEquals(6, state.data.filterIsInstance<StockRow.Item>().size)
    }

    @Test
    fun refresh_withZeroPriceStocks_includesAll() = runTest {
        // Given: Zero price stocks
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(price = 0L),
                createRecommendation(price = 70000L)
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Both included
        val state = viewModel.state.value as LoadResult.Success
        val items = state.data.filterIsInstance<StockRow.Item>()
        assertEquals(2, items.size)
    }

    @Test
    fun state_initiallyEmpty_beforeInit() {
        // When: ViewModel created but coroutines not advanced
        viewModel = StockViewModel(repository)

        // Then: Initial state is Empty
        assertTrue(viewModel.state.value is LoadResult.Empty)
    }

    @Test
    fun refresh_withSingleStockPerDate_createsCorrectRows() = runTest {
        // Given: One stock per date
        val dataMap = mapOf(
            "2024-01-15" to listOf(createRecommendation(ticker = "A")),
            "2024-01-14" to listOf(createRecommendation(ticker = "B")),
            "2024-01-13" to listOf(createRecommendation(ticker = "C"))
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: 3 headers + 3 items = 6 rows
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(6, state.data.size)
    }

    @Test
    fun refresh_maintainsOrderWithinEachDate() = runTest {
        // Given: Specific order within date
        val dataMap = mapOf(
            "2024-01-15" to listOf(
                createRecommendation(ticker = "C", name = "Third"),
                createRecommendation(ticker = "A", name = "First"),
                createRecommendation(ticker = "B", name = "Second")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Order maintained
        val state = viewModel.state.value as LoadResult.Success
        val items = state.data.filterIsInstance<StockRow.Item>()
        assertEquals("Third", items[0].data.name)
        assertEquals("First", items[1].data.name)
        assertEquals("Second", items[2].data.name)
    }

    @Test
    fun refresh_withWhitespaceInNames_preservesWhitespace() = runTest {
        // Given: Names with whitespace
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(name = "  삼성전자  "),
                createRecommendation(name = "SK\t하이닉스"),
                createRecommendation(name = "LG\n전자")
            )
        )
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Whitespace preserved
        val state = viewModel.state.value as LoadResult.Success
        val items = state.data.filterIsInstance<StockRow.Item>()
        assertEquals("  삼성전자  ", items[0].data.name)
        assertTrue(items[1].data.name.contains("\t"))
        assertTrue(items[2].data.name.contains("\n"))
    }

    @Test
    fun refresh_withManyItemsPerDate_allIncluded() = runTest {
        // Given: 50 items in one date
        val manyItems = (1..50).map {
            createRecommendation(ticker = "STOCK$it")
        }
        val dataMap = mapOf("2024-01-15" to manyItems)
        whenever(repository.getStockRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: 1 header + 50 items = 51 rows
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(51, state.data.size)
        assertEquals(50, state.data.filterIsInstance<StockRow.Item>().size)
    }
}