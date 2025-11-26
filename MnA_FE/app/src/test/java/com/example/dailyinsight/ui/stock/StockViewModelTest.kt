package com.example.dailyinsight.ui.stock

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.database.BriefingCardCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class StockViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: Repository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        whenever(repository.getBriefingFlow()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== Initialization Tests =====

    @Test
    fun init_callsRefresh() = runTest {
        whenever(repository.fetchAndSaveBriefing(eq(0), eq(true))).thenReturn("2024-01-01T12:00:00")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        verify(repository).fetchAndSaveBriefing(offset = 0, clear = true)
    }

    @Test
    fun init_updatesAsOfTimeOnSuccess() = runTest {
        val expectedAsOf = "2024-01-01T12:00:00"
        whenever(repository.fetchAndSaveBriefing(eq(0), eq(true))).thenReturn(expectedAsOf)

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        assertEquals(expectedAsOf, viewModel.asOfTime.value)
    }

    // ===== Refresh Tests =====

    @Test
    fun refresh_callsRepositoryWithOffsetZeroAndClearTrue() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // init에서 1번, refresh에서 1번
        verify(repository, times(2)).fetchAndSaveBriefing(offset = 0, clear = true)
    }

    @Test
    fun refresh_updatesAsOfTime() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any()))
            .thenReturn("2024-01-01T12:00:00")
            .thenReturn("2024-01-02T12:00:00")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()
        assertEquals("2024-01-01T12:00:00", viewModel.asOfTime.value)

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals("2024-01-02T12:00:00", viewModel.asOfTime.value)
    }

    // ===== LoadNextPage Tests =====

    @Test
    fun loadNextPage_incrementsOffsetBy10() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()
        verify(repository).fetchAndSaveBriefing(offset = 10, clear = false)

        viewModel.loadNextPage()
        advanceUntilIdle()
        verify(repository).fetchAndSaveBriefing(offset = 20, clear = false)

        viewModel.loadNextPage()
        advanceUntilIdle()
        verify(repository).fetchAndSaveBriefing(offset = 30, clear = false)
    }

    @Test
    fun loadNextPage_doesNotClearDb() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        verify(repository).fetchAndSaveBriefing(offset = 10, clear = false)
    }

    // ===== Error Handling Tests =====

    @Test
    fun refresh_handlesNullAsOfGracefully() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any())).thenReturn(null)

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // null이어도 크래시 없이 처리
        assertNull(viewModel.asOfTime.value)
    }

    @Test
    fun loadNextPage_handlesErrorGracefully() = runTest {
        whenever(repository.fetchAndSaveBriefing(eq(0), eq(true))).thenReturn("2024-01-01")
        whenever(repository.fetchAndSaveBriefing(eq(10), eq(false))).thenReturn(null)

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        // 에러 발생해도 크래시 없음
        assertNotNull(viewModel)
    }

    // ===== Helper Functions =====

    private fun createBriefingCard(ticker: String, name: String) = BriefingCardCache(
        ticker = ticker,
        name = name,
        price = 70000L,
        change = 1000L,
        changeRate = 1.5,
        headline = "테스트 요약",
        label = null,
        confidence = null,
        fetchedAt = System.currentTimeMillis()
    )
}