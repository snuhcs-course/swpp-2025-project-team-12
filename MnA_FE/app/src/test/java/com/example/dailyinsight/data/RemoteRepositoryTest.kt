package com.example.dailyinsight.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.data.database.BriefingCardCache
import com.example.dailyinsight.data.database.BriefingDao
import com.example.dailyinsight.data.database.FavoriteTicker
import com.example.dailyinsight.data.database.StockDetailCache
import com.example.dailyinsight.data.database.StockDetailDao
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.dto.BriefingItemDto
import com.example.dailyinsight.data.dto.BriefingListResponse
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * Unit tests for RemoteRepository using Robolectric and Mockito.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RemoteRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Mock
    private lateinit var mockApi: ApiService

    @Mock
    private lateinit var mockBriefingDao: BriefingDao

    @Mock
    private lateinit var mockStockDetailDao: StockDetailDao

    private lateinit var context: Context
    private lateinit var repository: RemoteRepository
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        repository = RemoteRepository(
            api = mockApi,
            briefingDao = mockBriefingDao,
            stockDetailDao = mockStockDetailDao,
            context = context
        )
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    // ===== getBriefingFlow Tests =====

    @Test
    fun getBriefingFlow_returnsDaoFlow() {
        // Given
        val expectedList = listOf(
            createBriefingCardCache("AAPL", "Apple"),
            createBriefingCardCache("GOOGL", "Google")
        )
        whenever(mockBriefingDao.getNormalListFlow()).thenReturn(flowOf(expectedList))

        // When
        val flow = repository.getBriefingFlow()

        // Then
        verify(mockBriefingDao).getNormalListFlow()
    }

    // ===== getFavoriteFlow Tests =====

    @Test
    fun getFavoriteFlow_returnsDaoFavoriteFlow() {
        // Given
        val expectedList = listOf(createBriefingCardCache("TSLA", "Tesla", isFavorite = true))
        whenever(mockBriefingDao.getFavoriteListFlow()).thenReturn(flowOf(expectedList))

        // When
        val flow = repository.getFavoriteFlow()

        // Then
        verify(mockBriefingDao).getFavoriteListFlow()
    }

    // ===== getStockReport Tests =====

    @Test
    fun getStockReport_withCache_returnsCachedData() = runTest {
        // Given
        val ticker = "AAPL"
        val cachedJson = """{"ticker":"AAPL","name":"Apple Inc"}"""
        val cachedDetail = StockDetailCache(ticker, cachedJson, System.currentTimeMillis())
        whenever(mockStockDetailDao.getDetail(ticker)).thenReturn(cachedDetail)

        // When
        val result = repository.getStockReport(ticker)

        // Then
        verify(mockStockDetailDao).getDetail(ticker)
        verify(mockApi, never()).getStockReport(any())
        assertEquals("AAPL", result.ticker)
    }

    @Test
    fun getStockReport_withoutCache_callsApiAndCaches() = runTest {
        // Given
        val ticker = "AAPL"
        val apiResponse = StockDetailDto(ticker = "AAPL", name = "Apple Inc")
        whenever(mockStockDetailDao.getDetail(ticker)).thenReturn(null)
        whenever(mockApi.getStockReport(ticker)).thenReturn(apiResponse)
        whenever(mockBriefingDao.getCard(ticker)).thenReturn(null)

        // When
        val result = repository.getStockReport(ticker)

        // Then
        verify(mockApi).getStockReport(ticker)
        verify(mockStockDetailDao).insertDetail(any())
        assertEquals("AAPL", result.ticker)
    }

    // ===== getStockOverview Tests =====

    @Test
    fun getStockOverview_callsApi() = runTest {
        // Given
        val ticker = "AAPL"
        val apiResponse = StockOverviewDto(summary = "Apple makes iPhones")
        whenever(mockApi.getStockOverview(ticker)).thenReturn(apiResponse)

        // When
        val result = repository.getStockOverview(ticker)

        // Then
        verify(mockApi).getStockOverview(ticker)
        assertEquals("Apple makes iPhones", result.summary)
    }

    // ===== toggleFavorite Tests =====

    @Test
    fun toggleFavorite_addFavorite_insertsToDao() = runTest {
        // Given
        val ticker = "AAPL"
        // Setup DataStore with guest user (no server sync)

        // When
        val result = repository.toggleFavorite(ticker, true)

        // Then
        assertTrue(result)
        verify(mockBriefingDao).insertFavorite(any())
        verify(mockBriefingDao).syncFavorites(any())
    }

    @Test
    fun toggleFavorite_removeFavorite_deletesFromDao() = runTest {
        // Given
        val ticker = "AAPL"

        // When
        val result = repository.toggleFavorite(ticker, false)

        // Then
        assertTrue(result)
        verify(mockBriefingDao).deleteFavorite(eq(ticker), any())
        verify(mockBriefingDao).syncFavorites(any())
    }

    // ===== clearUserData Tests =====

    @Test
    fun clearUserData_clearsAllFavorites() = runTest {
        // When
        repository.clearUserData()

        // Then
        verify(mockBriefingDao).clearAllFavorites()
        verify(mockBriefingDao).uncheckAllFavorites()
    }

    // ===== fetchAndSaveBriefing Tests =====

    @Test
    fun fetchAndSaveBriefing_success_returnsAsOf() = runTest {
        // Given
        val items = listOf(
            BriefingItemDto(
                ticker = "AAPL",
                name = "Apple",
                close = "150",
                change = "5",
                changeRate = "3.5",
                summary = "Tech company",
                overview = null,
                marketCap = 1000000L
            )
        )
        val response = BriefingListResponse(
            items = items,
            total = 1,
            limit = 10,
            offset = 0,
            source = null,
            asOf = "2024-01-01"
        )
        whenever(mockApi.getBriefingList(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(response)
        whenever(mockBriefingDao.getFavoriteTickers(any())).thenReturn(emptyList())
        whenever(mockBriefingDao.getCard(any())).thenReturn(null)

        // When
        val result = repository.fetchAndSaveBriefing(offset = 0, clear = false)

        // Then
        assertEquals("2024-01-01", result)
        verify(mockBriefingDao).insertCards(any())
    }

    @Test
    fun fetchAndSaveBriefing_withClear_resetsRanks() = runTest {
        // Given
        val response = BriefingListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = null,
            asOf = "2024-01-01"
        )
        whenever(mockApi.getBriefingList(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(response)
        whenever(mockBriefingDao.getFavoriteTickers(any())).thenReturn(emptyList())

        // When
        repository.fetchAndSaveBriefing(offset = 0, clear = true)

        // Then
        verify(mockBriefingDao).resetRanks()
        verify(mockBriefingDao).deleteGarbage()
    }

    @Test
    fun fetchAndSaveBriefing_apiError_returnsNull() = runTest {
        // Given
        whenever(mockApi.getBriefingList(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException("Network error"))
        whenever(mockBriefingDao.getFavoriteTickers(any())).thenReturn(emptyList())

        // When
        val result = repository.fetchAndSaveBriefing(offset = 0, clear = false)

        // Then
        assertNull(result)
    }

    @Test
    fun fetchAndSaveBriefing_preservesFavoriteStatus() = runTest {
        // Given
        val items = listOf(
            BriefingItemDto(
                ticker = "AAPL",
                name = "Apple",
                close = "150",
                change = "5",
                changeRate = "3.5",
                summary = "Tech company",
                overview = null,
                marketCap = 1000000L
            )
        )
        val response = BriefingListResponse(
            items = items,
            total = 1,
            limit = 10,
            offset = 0,
            source = null,
            asOf = "2024-01-01"
        )
        whenever(mockApi.getBriefingList(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(response)
        whenever(mockBriefingDao.getFavoriteTickers(any())).thenReturn(listOf("AAPL"))
        whenever(mockBriefingDao.getCard("AAPL")).thenReturn(null)

        // When
        repository.fetchAndSaveBriefing(offset = 0, clear = false)

        // Then
        verify(mockBriefingDao).insertCards(argThat { cards ->
            cards.any { it.ticker == "AAPL" && it.isFavorite }
        })
    }

    // ===== syncFavorites Tests =====

    @Test
    fun syncFavorites_guestUser_doesNotCallApi() = runTest {
        // Given: Default context has no username (guest)

        // When
        repository.syncFavorites()

        // Then
        verify(mockApi, never()).getPortfolio()
    }

    // ===== Helper Functions =====

    private fun createBriefingCardCache(
        ticker: String,
        name: String,
        isFavorite: Boolean = false
    ): BriefingCardCache {
        return BriefingCardCache(
            ticker = ticker,
            name = name,
            price = 100L,
            change = 5L,
            changeRate = 2.5,
            headline = "Test headline",
            label = null,
            confidence = null,
            rank = 0,
            fetchedAt = System.currentTimeMillis(),
            marketCap = 1000000L,
            industry = "Technology",
            isFavorite = isFavorite
        )
    }
}
