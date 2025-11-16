package com.example.dailyinsight.ui.stock

import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.RecommendationDto
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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

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
        val dataMap = mapOf(
            "2024-01-10" to listOf(createRecommendation(name = "Stock 10")),
            "2024-01-15" to listOf(createRecommendation(name = "Stock 15")),
            "2024-01-05" to listOf(createRecommendation(name = "Stock 05"))
        )
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Dates should be sorted descending (newest first)
        val state = viewModel.state.value as LoadResult.Success
        val rows = state.data
        val headers = rows.filterIsInstance<HistoryRow.Header>()

        assertEquals(3, headers.size)
        assertEquals("2024-01-15", headers[0].label)
        assertEquals("2024-01-10", headers[1].label)
        assertEquals("2024-01-05", headers[2].label)
    }

    @Test
    fun refresh_withEmptyMap_returnsEmptyList() = runTest {
        // Given: Mock repository returns empty map
        whenever(repository.getHistoryRecommendations()).thenReturn(emptyMap())

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
        // Given: Mock repository throws exception
        val exception = IOException("Network error")
        whenever(repository.getHistoryRecommendations()).thenThrow(exception)

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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel (don't advance idle yet)
        viewModel = StockViewModel(repository)

        // Then: State should be Loading
        assertTrue(viewModel.state.value is LoadResult.Loading)

        // Advance to complete
        advanceUntilIdle()

        // Now should be Success
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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Should have 1 header + 2 items
        val state = viewModel.state.value as LoadResult.Success
        val rows = state.data
        assertEquals(3, rows.size)

        assertTrue(rows[0] is HistoryRow.Header)
        assertEquals("2024-01-15", (rows[0] as HistoryRow.Header).label)

        assertTrue(rows[1] is HistoryRow.Item)
        assertEquals("Stock 1", (rows[1] as HistoryRow.Item).data.name)

        assertTrue(rows[2] is HistoryRow.Item)
        assertEquals("Stock 2", (rows[2] as HistoryRow.Item).data.name)
    }

    @Test
    fun refresh_multipleCallsUpdateState() = runTest {
        // Given: Mock repository returns different data
        val dataMap1 = mapOf("2024-01-15" to listOf(createRecommendation(name = "First")))
        val dataMap2 = mapOf("2024-01-20" to listOf(createRecommendation(name = "Second")))

        whenever(repository.getHistoryRecommendations())
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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Should have 1 header + 4 items = 5 rows
        val state = viewModel.state.value as LoadResult.Success
        val rows = state.data
        assertEquals(5, rows.size)

        val headers = rows.filterIsInstance<HistoryRow.Header>()
        val items = rows.filterIsInstance<HistoryRow.Item>()

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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: All 10 dates should be present in descending order
        val state = viewModel.state.value as LoadResult.Success
        val headers = state.data.filterIsInstance<HistoryRow.Header>()

        assertEquals(10, headers.size)
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
        whenever(repository.getHistoryRecommendations()).thenReturn(dataMap)

        // When: Create ViewModel
        viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // Then: Data should be preserved
        val state = viewModel.state.value as LoadResult.Success
        val item = state.data[1] as HistoryRow.Item

        assertEquals("005930", item.data.ticker)
        assertEquals("삼성전자", item.data.name)
        assertEquals(70000L, item.data.price)
        assertEquals(-500L, item.data.change)
        assertEquals(-0.71, item.data.changeRate, 0.001)
        assertEquals("Important news", item.data.headline)
    }
}
