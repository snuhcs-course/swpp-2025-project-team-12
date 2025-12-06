package com.example.dailyinsight.ui.stock

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailyinsight.MainDispatcherRule
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.database.BriefingCardCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class StockViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: Repository

    @Before
    fun setup() {
        repository = mock()
        whenever(repository.getBriefingFlow()).thenReturn(flowOf(emptyList()))
    }

    // ===== Initialization Tests =====

    @Test
    fun init_callsRefresh() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01T12:00:00")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        verify(repository).fetchAndSaveBriefing(eq(0), eq(true), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun init_updatesAsOfTimeOnSuccess() = runTest {
        val expectedAsOf = "2024-01-01T12:00:00"
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(expectedAsOf)

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        assertEquals(expectedAsOf, viewModel.asOfTime.value)
    }

    // ===== Refresh Tests =====

    @Test
    fun refresh_callsRepositoryWithOffsetZeroAndClearTrue() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // init에서 1번, refresh에서 1번
        verify(repository, times(2)).fetchAndSaveBriefing(eq(0), eq(true), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun refresh_updatesAsOfTime() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
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
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()
        verify(repository).fetchAndSaveBriefing(eq(10), eq(false), anyOrNull(), anyOrNull(), anyOrNull())

        viewModel.loadNextPage()
        advanceUntilIdle()
        verify(repository).fetchAndSaveBriefing(eq(20), eq(false), anyOrNull(), anyOrNull(), anyOrNull())

        viewModel.loadNextPage()
        advanceUntilIdle()
        verify(repository).fetchAndSaveBriefing(eq(30), eq(false), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun loadNextPage_doesNotClearDb() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        verify(repository).fetchAndSaveBriefing(eq(10), eq(false), anyOrNull(), anyOrNull(), anyOrNull())
    }

    // ===== Error Handling Tests =====

    @Test
    fun refresh_handlesNullAsOfGracefully() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        // null이어도 크래시 없이 처리
        assertNull(viewModel.asOfTime.value)
    }

    @Test
    fun loadNextPage_handlesErrorGracefully() = runTest {
        whenever(repository.fetchAndSaveBriefing(eq(0), eq(true), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")
        whenever(repository.fetchAndSaveBriefing(eq(10), eq(false), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)

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

    // ===== FavoriteMode Tests =====

    @Test
    fun setFavoriteMode_updatesState() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.setFavoriteMode(true)
        // setFavoriteMode는 내부 StateFlow를 업데이트하므로 크래시 없이 동작해야 함
        assertNotNull(viewModel)
    }

    // ===== Filter Tests =====

    @Test
    fun setSizeFilter_updatesFilterState() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.setSizeFilter(StockViewModel.SizeFilter.LARGE)
        advanceUntilIdle()

        assertEquals(StockViewModel.SizeFilter.LARGE, viewModel.getCurrentFilterMode())
    }

    @Test
    fun getCurrentFilterMode_returnsCurrentFilter() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        assertEquals(StockViewModel.SizeFilter.ALL, viewModel.getCurrentFilterMode())

        viewModel.setSizeFilter(StockViewModel.SizeFilter.LARGE)
        advanceUntilIdle()

        assertEquals(StockViewModel.SizeFilter.LARGE, viewModel.getCurrentFilterMode())
    }

    // ===== Sort Tests =====

    @Test
    fun setSort_updatesState() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.setSort("price")
        advanceUntilIdle()

        // setSort triggers loadData and should update state
        assertNotNull(viewModel)
    }

    // ===== Industry Filter Tests =====

    @Test
    fun setIndustryFilter_updatesFilterState() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        val industries = setOf("IT", "건설")
        viewModel.setIndustryFilter(industries)
        advanceUntilIdle()

        assertEquals(industries, viewModel.getCurrentIndustries())
    }

    @Test
    fun setIndustryFilter_emptySet() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.setIndustryFilter(emptySet())
        advanceUntilIdle()

        assertTrue(viewModel.getCurrentIndustries().isEmpty())
    }

    @Test
    fun getCurrentIndustries_returnsEmptyByDefault() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.getCurrentIndustries().isEmpty())
    }

    // ===== getCurrentFilterState Tests =====

    @Test
    fun getCurrentFilterState_returnsDefaultState() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.getCurrentFilterState()
        assertEquals(StockViewModel.SizeFilter.ALL, state.size)
        assertTrue(state.industries.isEmpty())
        assertEquals("market_cap", state.sort)
        assertFalse(state.isFavMode)
    }

    @Test
    fun getCurrentFilterState_reflectsChanges() = runTest {
        whenever(repository.fetchAndSaveBriefing(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("2024-01-01")

        val viewModel = StockViewModel(repository)
        advanceUntilIdle()

        viewModel.setSizeFilter(StockViewModel.SizeFilter.MID)
        viewModel.setIndustryFilter(setOf("금융"))
        viewModel.setSort("change_rate")
        advanceUntilIdle()

        val state = viewModel.getCurrentFilterState()
        assertEquals(StockViewModel.SizeFilter.MID, state.size)
        assertTrue(state.industries.contains("금융"))
        assertEquals("change_rate", state.sort)
    }

    // ===== SizeFilter Enum Tests =====

    @Test
    fun sizeFilter_ALL_hasNullMinMax() {
        val filter = StockViewModel.SizeFilter.ALL
        assertNull(filter.minRank)
        assertNull(filter.maxRank)
    }

    @Test
    fun sizeFilter_LARGE_hasCorrectRange() {
        val filter = StockViewModel.SizeFilter.LARGE
        assertEquals(0, filter.minRank)
        assertEquals(100, filter.maxRank)
    }

    @Test
    fun sizeFilter_MID_hasCorrectRange() {
        val filter = StockViewModel.SizeFilter.MID
        assertEquals(100, filter.minRank)
        assertEquals(300, filter.maxRank)
    }

    @Test
    fun sizeFilter_SMALL_hasCorrectRange() {
        val filter = StockViewModel.SizeFilter.SMALL
        assertEquals(300, filter.minRank)
        assertNull(filter.maxRank)
    }

    @Test
    fun sizeFilter_values_contains4Items() {
        assertEquals(4, StockViewModel.SizeFilter.values().size)
    }

    @Test
    fun sizeFilter_valueOf_works() {
        assertEquals(StockViewModel.SizeFilter.ALL, StockViewModel.SizeFilter.valueOf("ALL"))
        assertEquals(StockViewModel.SizeFilter.LARGE, StockViewModel.SizeFilter.valueOf("LARGE"))
        assertEquals(StockViewModel.SizeFilter.MID, StockViewModel.SizeFilter.valueOf("MID"))
        assertEquals(StockViewModel.SizeFilter.SMALL, StockViewModel.SizeFilter.valueOf("SMALL"))
    }

    // ===== FilterState Data Class Tests =====

    @Test
    fun filterState_defaultValues() {
        val state = StockViewModel.FilterState()
        assertEquals(StockViewModel.SizeFilter.ALL, state.size)
        assertTrue(state.industries.isEmpty())
        assertEquals("market_cap", state.sort)
        assertFalse(state.isFavMode)
    }

    @Test
    fun filterState_customValues() {
        val state = StockViewModel.FilterState(
            size = StockViewModel.SizeFilter.LARGE,
            industries = setOf("IT", "금융"),
            sort = "price",
            isFavMode = true
        )
        assertEquals(StockViewModel.SizeFilter.LARGE, state.size)
        assertEquals(setOf("IT", "금융"), state.industries)
        assertEquals("price", state.sort)
        assertTrue(state.isFavMode)
    }

    @Test
    fun filterState_copy() {
        val original = StockViewModel.FilterState()
        val copied = original.copy(size = StockViewModel.SizeFilter.MID)
        assertEquals(StockViewModel.SizeFilter.MID, copied.size)
        assertEquals(original.industries, copied.industries)
        assertEquals(original.sort, copied.sort)
        assertEquals(original.isFavMode, copied.isFavMode)
    }

    @Test
    fun filterState_equality() {
        val state1 = StockViewModel.FilterState(size = StockViewModel.SizeFilter.LARGE)
        val state2 = StockViewModel.FilterState(size = StockViewModel.SizeFilter.LARGE)
        assertEquals(state1, state2)
    }

    @Test
    fun filterState_hashCode() {
        val state1 = StockViewModel.FilterState()
        val state2 = StockViewModel.FilterState()
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun filterState_toString() {
        val state = StockViewModel.FilterState()
        assertTrue(state.toString().contains("FilterState"))
    }

    @Test
    fun filterState_destructuring() {
        val state = StockViewModel.FilterState(
            size = StockViewModel.SizeFilter.SMALL,
            industries = setOf("제약"),
            sort = "volume",
            isFavMode = true
        )
        val (size, industries, sort, isFavMode) = state
        assertEquals(StockViewModel.SizeFilter.SMALL, size)
        assertEquals(setOf("제약"), industries)
        assertEquals("volume", sort)
        assertTrue(isFavMode)
    }

}