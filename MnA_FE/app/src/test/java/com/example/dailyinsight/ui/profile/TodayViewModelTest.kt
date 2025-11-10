package com.example.dailyinsight.ui.profile

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
class TodayViewModelTest {

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
        whenever(repository.getTodayRecommendations()).thenThrow(exception)

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

        // When: Create ViewModel (don't advance idle yet)
        viewModel = TodayViewModel(repository)

        // Then: State should be Loading
        assertTrue(viewModel.state.value is LoadResult.Loading)

        // Advance to complete
        advanceUntilIdle()

        // Now should be Success
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
}
