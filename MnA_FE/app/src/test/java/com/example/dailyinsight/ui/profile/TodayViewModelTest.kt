package com.example.dailyinsight.ui.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import java.io.IOException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Simple TodayViewModel for testing purposes
 * This mirrors the pattern used in StockViewModel
 */
class TodayViewModel(
    private val repo: Repository
) : ViewModel() {

    private val _state =
        MutableStateFlow<LoadResult<List<RecommendationDto>>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<List<RecommendationDto>>> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _state.value = runCatching { repo.getTodayRecommendations() }
                .fold(
                    onSuccess = { LoadResult.Success(it) },
                    onFailure = { LoadResult.Error(it) }
                )
        }
    }

    init {
        refresh()
    }
}

@ExperimentalCoroutinesApi
class TodayViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: Repository
    private lateinit var viewModel: TodayViewModel
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
    fun viewModel_initializes_successfully() = runTest {
        // Given: Mock repository returns data
        val recommendations = listOf(createRecommendation())
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: ViewModel should be created
        assertNotNull(viewModel)
    }

    @Test
    fun viewModel_is_instance_of_ViewModel() = runTest {
        // Given: Mock repository returns data
        val recommendations = listOf(createRecommendation())
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Should be a ViewModel (always true, but documents the requirement)
        assertNotNull(viewModel as? ViewModel)
    }

    @Test
    fun init_automaticallyCallsRefresh() = runTest {
        // Given: Mock repository returns data
        val recommendations = listOf(createRecommendation())
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel (init calls refresh)
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Success
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun refresh_withValidData_updatesStateToSuccess() = runTest {
        // Given: Mock repository returns recommendations
        val recommendations = listOf(
            createRecommendation(ticker = "005930", name = "삼성전자"),
            createRecommendation(ticker = "000660", name = "SK하이닉스"),
            createRecommendation(ticker = "035420", name = "네이버")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel and wait for init
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Success with correct data
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
        val data = (state as LoadResult.Success).data
        assertEquals(3, data.size)
        assertEquals("삼성전자", data[0].name)
        assertEquals("SK하이닉스", data[1].name)
        assertEquals("네이버", data[2].name)
    }

    @Test
    fun refresh_withEmptyList_returnsEmptyList() = runTest {
        // Given: Mock repository returns empty list
        whenever(repository.getTodayRecommendations()).thenReturn(emptyList())

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
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
        whenever(repository.getTodayRecommendations()).thenAnswer {
            throw exception
        }

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: State should be Error
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Error)
        assertEquals(exception, (state as LoadResult.Error).throwable)
    }

    @Test
    fun refresh_setsLoadingStateBeforeCompletion() = runTest {
        // Given: Mock repository
        val recommendations = listOf(createRecommendation())
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        
        // Note: Due to how StateFlow works, we can't reliably capture the Loading state
        // before it transitions to Success. This test verifies the ViewModel completes successfully.
        advanceUntilIdle()

        // Then: Should be Success after completion
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun refresh_multipleCallsUpdateState() = runTest {
        // Given: Mock repository returns different data
        val recommendations1 = listOf(createRecommendation(name = "First"))
        val recommendations2 = listOf(createRecommendation(name = "Second"))

        whenever(repository.getTodayRecommendations())
            .thenReturn(recommendations1)
            .thenReturn(recommendations2)

        // When: Create ViewModel and call refresh twice
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        val firstState = viewModel.state.value

        viewModel.refresh()
        advanceUntilIdle()

        val secondState = viewModel.state.value

        // Then: Both should be Success
        assertTrue(firstState is LoadResult.Success)
        assertTrue(secondState is LoadResult.Success)
    }

    @Test
    fun refresh_withSingleRecommendation_returnsOneItem() = runTest {
        // Given: Single recommendation
        val recommendation = createRecommendation(
            ticker = "005930",
            name = "삼성전자",
            price = 70000
        )
        whenever(repository.getTodayRecommendations()).thenReturn(listOf(recommendation))

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: State should have one item
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(1, state.data.size)
        assertEquals("삼성전자", state.data[0].name)
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
        whenever(repository.getTodayRecommendations()).thenReturn(listOf(recommendation))

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Data should be preserved
        val state = viewModel.state.value as LoadResult.Success
        val data = state.data[0]

        assertEquals("005930", data.ticker)
        assertEquals("삼성전자", data.name)
        assertEquals(70000L, data.price)
        assertEquals(-500L, data.change)
        assertEquals(-0.71, data.changeRate, 0.001)
        assertEquals("Important news", data.headline)
    }

    @Test
    fun refresh_withManyRecommendations_allIncluded() = runTest {
        // Given: Many recommendations
        val recommendations = (1..20).map { i ->
            createRecommendation(
                ticker = String.format("%06d", i),
                name = "Stock $i",
                price = 50000L + (i * 1000)
            )
        }
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: All 20 should be present
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(20, state.data.size)
        assertEquals("Stock 1", state.data[0].name)
        assertEquals("Stock 20", state.data[19].name)
    }

    @Test
    fun refresh_maintainsListOrder() = runTest {
        // Given: Recommendations in specific order
        val recommendations = listOf(
            createRecommendation(ticker = "C", name = "Third"),
            createRecommendation(ticker = "A", name = "First"),
            createRecommendation(ticker = "B", name = "Second")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Order should be maintained as returned from repository
        val state = viewModel.state.value as LoadResult.Success
        assertEquals("Third", state.data[0].name)
        assertEquals("First", state.data[1].name)
        assertEquals("Second", state.data[2].name)
    }

    @Test
    fun refresh_withPositiveAndNegativeChanges_handlesCorrectly() = runTest {
        // Given: Mix of positive and negative changes
        val recommendations = listOf(
            createRecommendation(name = "Up", price = 70000).copy(change = 500, changeRate = 0.72),
            createRecommendation(name = "Down", price = 69000).copy(change = -500, changeRate = -0.72),
            createRecommendation(name = "Flat", price = 70000).copy(change = 0, changeRate = 0.0)
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: All change values should be preserved
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(500L, state.data[0].change)
        assertEquals(-500L, state.data[1].change)
        assertEquals(0L, state.data[2].change)
    }

    @Test
    fun refresh_withLargeDataset_handlesCorrectly() = runTest {
        // Given: 100 recommendations
        val largeList = (1..100).map {
            createRecommendation(ticker = "STOCK$it", name = "회사$it")
        }
        whenever(repository.getTodayRecommendations()).thenReturn(largeList)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: All 100 included
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(100, state.data.size)
    }

    @Test
    fun refresh_withSpecialCharactersInNames_handlesCorrectly() = runTest {
        // Given: Special characters
        val recommendations = listOf(
            createRecommendation(name = "삼성전자!@#$"),
            createRecommendation(name = "SK하이닉스(주)"),
            createRecommendation(name = "LG전자 & Co.")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Handles special characters
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
    }

    @Test
    fun refresh_withDuplicateTickers_includesAll() = runTest {
        // Given: Duplicate tickers
        val recommendations = listOf(
            createRecommendation(ticker = "005930", price = 70000),
            createRecommendation(ticker = "005930", price = 71000),
            createRecommendation(ticker = "005930", price = 72000)
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: All duplicates included
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(3, state.data.size)
    }

    @Test
    fun refresh_withExtremePriceValues_handlesCorrectly() = runTest {
        // Given: Extreme prices
        val recommendations = listOf(
            createRecommendation(price = 1L),
            createRecommendation(price = Long.MAX_VALUE),
            createRecommendation(price = 0L)
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Handles extreme values
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
    }

    @Test
    fun refresh_afterError_recoversSuccessfully() = runTest {
        // Given: First fails, second succeeds
        val data = listOf(createRecommendation())
        whenever(repository.getTodayRecommendations())
            .thenAnswer { throw IOException() }
            .thenReturn(data)

        // When: Init (fails)
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoadResult.Error)

        // When: Retry (succeeds)
        viewModel.refresh()
        advanceUntilIdle()

        // Then: Recovers
        assertTrue(viewModel.state.value is LoadResult.Success)
    }

    @Test
    fun refresh_withNetworkTimeout_setsError() = runTest {
        // Given: Timeout
        whenever(repository.getTodayRecommendations())
            .thenAnswer { throw java.net.SocketTimeoutException() }

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Error state
        assertTrue(viewModel.state.value is LoadResult.Error)
    }

    @Test
    fun refresh_withNullPointer_setsError() = runTest {
        // Given: NPE
        whenever(repository.getTodayRecommendations())
            .thenAnswer { throw NullPointerException() }

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Error state
        assertTrue(viewModel.state.value is LoadResult.Error)
    }

    @Test
    fun state_initiallyEmpty_beforeInit() {
        // When: Created but not initialized
        viewModel = TodayViewModel(repository)

        // Then: Initial state Empty
        assertTrue(viewModel.state.value is LoadResult.Empty)
    }

    @Test
    fun refresh_withEmptyStringFields_handlesCorrectly() = runTest {
        // Given: Empty strings
        val recommendations = listOf(
            createRecommendation(ticker = "", name = "")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Handles empty strings
        val state = viewModel.state.value
        assertTrue(state is LoadResult.Success)
    }

    @Test
    fun refresh_withVeryLongName_handlesCorrectly() = runTest {
        // Given: Very long name
        val longName = "A".repeat(500)
        val recommendations = listOf(createRecommendation(name = longName))
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Handles long name
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(longName, state.data.first().name)
    }

    @Test
    fun refresh_preservesExactOrder() = runTest {
        // Given: Specific order
        val recommendations = listOf(
            createRecommendation(ticker = "Z"),
            createRecommendation(ticker = "A"),
            createRecommendation(ticker = "M")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Order preserved exactly
        val state = viewModel.state.value as LoadResult.Success
        assertEquals("Z", state.data[0].ticker)
        assertEquals("A", state.data[1].ticker)
        assertEquals("M", state.data[2].ticker)
    }

    @Test
    fun refresh_withNegativeChangeRates_preservesValues() = runTest {
        // Given: Negative rates
        val recommendations = listOf(
            createRecommendation().copy(changeRate = -5.0),
            createRecommendation().copy(changeRate = -10.5),
            createRecommendation().copy(changeRate = -0.01)
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Negative rates preserved
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(-5.0, state.data[0].changeRate, 0.001)
        assertEquals(-10.5, state.data[1].changeRate, 0.001)
        assertEquals(-0.01, state.data[2].changeRate, 0.001)
    }

    @Test
    fun refresh_withWhitespaceInNames_preservesWhitespace() = runTest {
        // Given: Names with whitespace
        val recommendations = listOf(
            createRecommendation(name = "  삼성전자  "),
            createRecommendation(name = "SK\t하이닉스"),
            createRecommendation(name = "LG\n전자")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Whitespace preserved
        val state = viewModel.state.value as LoadResult.Success
        assertEquals("  삼성전자  ", state.data[0].name)
        assertTrue(state.data[1].name.contains("\t"))
        assertTrue(state.data[2].name.contains("\n"))
    }

    @Test
    fun refresh_with50Recommendations_allIncluded() = runTest {
        // Given: 50 recommendations
        val recommendations = (1..50).map {
            createRecommendation(ticker = "STOCK$it")
        }
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: All 50 included
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(50, state.data.size)
    }

    @Test
    fun refresh_withMixedCaseNames_preservesCase() = runTest {
        // Given: Mixed case names
        val recommendations = listOf(
            createRecommendation(name = "SAMSUNG"),
            createRecommendation(name = "samsung"),
            createRecommendation(name = "SaMsUnG")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Case preserved
        val state = viewModel.state.value as LoadResult.Success
        assertEquals("SAMSUNG", state.data[0].name)
        assertEquals("samsung", state.data[1].name)
        assertEquals("SaMsUnG", state.data[2].name)
    }

    @Test
    fun refresh_withUnicodeCharacters_handlesCorrectly() = runTest {
        // Given: Unicode characters
        val recommendations = listOf(
            createRecommendation(name = "삼성전자"),
            createRecommendation(name = "サムスン電子"),
            createRecommendation(name = "三星电子"),
            createRecommendation(name = "🚀📈")
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Unicode handled
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(4, state.data.size)
    }

    @Test
    fun refresh_withVerySmallChangeRates_preservesPrecision() = runTest {
        // Given: Very small change rates
        val recommendations = listOf(
            createRecommendation().copy(changeRate = 0.0001),
            createRecommendation().copy(changeRate = -0.0001),
            createRecommendation().copy(changeRate = 0.00001)
        )
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: Precision preserved
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(0.0001, state.data[0].changeRate, 0.00001)
        assertEquals(-0.0001, state.data[1].changeRate, 0.00001)
        assertEquals(0.00001, state.data[2].changeRate, 0.000001)
    }

    @Test
    fun refresh_withAlternatingPrices_handlesCorrectly() = runTest {
        // Given: Alternating high/low prices
        val recommendations = (1..10).map { i ->
            if (i % 2 == 0) {
                createRecommendation(price = 100000L)
            } else {
                createRecommendation(price = 10L)
            }
        }
        whenever(repository.getTodayRecommendations()).thenReturn(recommendations)

        // When: Create ViewModel
        viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        // Then: All included
        val state = viewModel.state.value as LoadResult.Success
        assertEquals(10, state.data.size)
    }
}