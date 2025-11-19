package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.*
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class RemoteRepositoryTest {

    private lateinit var repository: RemoteRepository
    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        mockApiService = mock()
        repository = RemoteRepository(mockApiService)
    }

    // ===== getTodayRecommendations Tests =====

    @Test
    fun getTodayRecommendations_success_returnsData() = runTest {
        val mockRecommendations = listOf(
            RecommendationDto(
                ticker = "005930",
                name = "삼성전자",
                price = 72000,
                change = -100,
                changeRate = -0.14,
                headline = "Strong earnings"
            ),
            RecommendationDto(
                ticker = "000660",
                name = "SK하이닉스",
                price = 150000,
                change = 2000,
                changeRate = 1.35,
                headline = "Tech rally"
            )
        )
        val apiResponse = ApiResponse(data = mockRecommendations)
        whenever(mockApiService.getTodayRecommendations()).thenReturn(apiResponse)

        val result = repository.getTodayRecommendations()

        assertEquals(2, result.size)
        assertEquals("삼성전자", result[0].name)
        assertEquals("SK하이닉스", result[1].name)
    }

    @Test
    fun getTodayRecommendations_emptyResponse_returnsEmptyList() = runTest {
        val apiResponse = ApiResponse(data = emptyList<RecommendationDto>())
        whenever(mockApiService.getTodayRecommendations()).thenReturn(apiResponse)

        val result = repository.getTodayRecommendations()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getTodayRecommendations_nullData_returnsEmptyList() = runTest {
        val apiResponse = ApiResponse(data = null as List<RecommendationDto>?)
        whenever(mockApiService.getTodayRecommendations()).thenReturn(apiResponse as ApiResponse<List<RecommendationDto>>)

        val result = repository.getTodayRecommendations()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getTodayRecommendations_withNetworkError_throwsException() = runTest {
        whenever(mockApiService.getTodayRecommendations()).thenAnswer { throw IOException("Network error") }

        try {
            repository.getTodayRecommendations()
            fail("Should throw IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    // ===== getStockRecommendations Tests =====

    @Test
    fun getStockRecommendations_success_returnsMap() = runTest {
        val mockMap = mapOf(
            "오늘" to listOf(
                RecommendationDto(
                    ticker = "005930",
                    name = "삼성전자",
                    price = 72000,
                    change = -100,
                    changeRate = -0.14,
                    headline = "Good performance"
                )
            ),
            "어제" to listOf(
                RecommendationDto(
                    ticker = "000660",
                    name = "SK하이닉스",
                    price = 150000,
                    change = 2000,
                    changeRate = 1.35,
                    headline = "Better outlook"
                )
            )
        )
        val apiResponse = ApiResponse(data = mockMap)
        whenever(mockApiService.getStockRecommendations()).thenReturn(apiResponse)

        val result = repository.getStockRecommendations()

        assertEquals(2, result.size)
        assertTrue(result.containsKey("오늘"))
        assertTrue(result.containsKey("어제"))
        assertEquals(1, result["오늘"]?.size)
        assertEquals("삼성전자", result["오늘"]?.get(0)?.name)
    }

    @Test
    fun getStockRecommendations_emptyMap_returnsEmptyMap() = runTest {
        val apiResponse = ApiResponse(data = emptyMap<String, List<RecommendationDto>>())
        whenever(mockApiService.getStockRecommendations()).thenReturn(apiResponse)

        val result = repository.getStockRecommendations()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getStockRecommendations_nullData_returnsEmptyMap() = runTest {
        val apiResponse = ApiResponse(data = null as Map<String, List<RecommendationDto>>?)
        whenever(mockApiService.getStockRecommendations()).thenReturn(apiResponse as ApiResponse<Map<String, List<RecommendationDto>>>)

        val result = repository.getStockRecommendations()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getStockRecommendations_withMultipleDays_returnsAllDays() = runTest {
        val mockMap = mapOf(
            "오늘" to listOf(RecommendationDto("005930", "삼성전자", 72000, -100, -0.14, "News1")),
            "어제" to listOf(RecommendationDto("000660", "SK하이닉스", 150000, 2000, 1.35, "News2")),
            "그저께" to listOf(RecommendationDto("035720", "카카오", 50000, 500, 1.01, "News3"))
        )
        val apiResponse = ApiResponse(data = mockMap)
        whenever(mockApiService.getStockRecommendations()).thenReturn(apiResponse)

        val result = repository.getStockRecommendations()

        assertEquals(3, result.size)
        assertTrue(result.containsKey("오늘"))
        assertTrue(result.containsKey("어제"))
        assertTrue(result.containsKey("그저께"))
    }

    // ===== getStockReport Tests =====

    @Test
    fun getStockReport_success_returnsDetail() = runTest {
        val mockDetail = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            current = CurrentData(
                price = 72000,
                change = -100,
                changeRate = -0.14
            )
        )
        whenever(mockApiService.getStockReport("005930")).thenReturn(mockDetail)

        val result = repository.getStockReport("005930")

        assertEquals("005930", result.ticker)
        assertEquals("삼성전자", result.name)
        assertEquals(72000L, result.current?.price)
    }

    @Test
    fun getStockReport_differentTickers_returnsDifferentData() = runTest {
        val detail1 = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            current = CurrentData(price = 72000)
        )
        val detail2 = StockDetailDto(
            ticker = "000660",
            name = "SK하이닉스",
            current = CurrentData(price = 150000)
        )
        
        whenever(mockApiService.getStockReport("005930")).thenReturn(detail1)
        whenever(mockApiService.getStockReport("000660")).thenReturn(detail2)

        val result1 = repository.getStockReport("005930")
        val result2 = repository.getStockReport("000660")

        assertEquals("삼성전자", result1.name)
        assertEquals("SK하이닉스", result2.name)
        assertEquals(72000L, result1.current?.price)
        assertEquals(150000L, result2.current?.price)
    }

    @Test
    fun getStockReport_withCompleteData_returnsAllFields() = runTest {
        val mockDetail = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            current = CurrentData(
                price = 72000,
                change = -100,
                changeRate = -0.14,
                marketCap = 1000000,
                date = "2024-01-01"
            ),
            valuation = ValuationData(
                peTtm = 15.5,
                priceToBook = 1.2,
                bps = 60000
            ),
            dividend = DividendData(`yield` = 2.5),
            financials = FinancialsData(
                eps = 4800,
                dps = 1800,
                roe = 8.0
            ),
            history = listOf(
                HistoryItem(date = "2024-01-01", close = 70000.0),
                HistoryItem(date = "2024-01-02", close = 71000.0)
            ),
            profile = ProfileData(explanation = "반도체 제조업체"),
            asOf = "2024-01-01"
        )
        whenever(mockApiService.getStockReport("005930")).thenReturn(mockDetail)

        val result = repository.getStockReport("005930")

        assertNotNull(result.current)
        assertNotNull(result.valuation)
        assertNotNull(result.dividend)
        assertNotNull(result.financials)
        assertNotNull(result.history)
        assertNotNull(result.profile)
        assertEquals(2, result.history?.size)
    }

    @Test
    fun getStockReport_withNetworkError_throwsException() = runTest {
        whenever(mockApiService.getStockReport("005930")).thenAnswer { throw IOException("Network error") }

        try {
            repository.getStockReport("005930")
            fail("Should throw IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    // ===== getStockOverview Tests =====

    @Test
    fun getStockOverview_success_returnsOverview() = runTest {
        val mockOverview = StockOverviewDto(
            asOfDate = "2024-01-01",
            summary = "테스트 요약",
            fundamental = "펀더멘털 분석",
            technical = "기술적 분석",
            news = listOf("뉴스1", "뉴스2")
        )
        whenever(mockApiService.getStockOverview("005930")).thenReturn(mockOverview)

        val result = repository.getStockOverview("005930")

        assertEquals("2024-01-01", result.asOfDate)
        assertEquals("테스트 요약", result.summary)
        assertEquals("펀더멘털 분석", result.fundamental)
        assertEquals("기술적 분석", result.technical)
        assertEquals(2, result.news?.size)
    }

    @Test
    fun getStockOverview_withNullFields_returnsPartialData() = runTest {
        val mockOverview = StockOverviewDto(
            asOfDate = null,
            summary = "요약만 있음",
            fundamental = null,
            technical = null,
            news = null
        )
        whenever(mockApiService.getStockOverview("005930")).thenReturn(mockOverview)

        val result = repository.getStockOverview("005930")

        assertNull(result.asOfDate)
        assertEquals("요약만 있음", result.summary)
        assertNull(result.fundamental)
        assertNull(result.technical)
        assertNull(result.news)
    }

    @Test
    fun getStockOverview_withNetworkError_throwsException() = runTest {
        whenever(mockApiService.getStockOverview("005930")).thenAnswer { throw IOException("Network error") }

        try {
            repository.getStockOverview("005930")
            fail("Should throw IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    @Test
    fun getStockOverview_differentTickers_returnsDifferentData() = runTest {
        val overview1 = StockOverviewDto(summary = "삼성전자 요약")
        val overview2 = StockOverviewDto(summary = "SK하이닉스 요약")
        
        whenever(mockApiService.getStockOverview("005930")).thenReturn(overview1)
        whenever(mockApiService.getStockOverview("000660")).thenReturn(overview2)

        val result1 = repository.getStockOverview("005930")
        val result2 = repository.getStockOverview("000660")

        assertEquals("삼성전자 요약", result1.summary)
        assertEquals("SK하이닉스 요약", result2.summary)
    }
}