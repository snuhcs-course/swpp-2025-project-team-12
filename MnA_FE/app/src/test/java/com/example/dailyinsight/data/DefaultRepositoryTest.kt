package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.ApiResponse
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class DefaultRepositoryTest {

    private lateinit var repository: DefaultRepository
    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        mockApiService = mock()
        repository = DefaultRepository(mockApiService)
    }

    // ===== getTodayRecommendations Tests =====

    @Test
    fun getTodayRecommendations_success_returnsData() = runTest {
        // Given: API returns recommendations wrapped in ApiResponse
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

        // When: Get today's recommendations
        val result = repository.getTodayRecommendations()

        // Then: Should return API data
        assertEquals(2, result.size)
        assertEquals("삼성전자", result[0].name)
        assertEquals("SK하이닉스", result[1].name)
    }

    @Test
    fun getTodayRecommendations_emptyResponse_returnsEmptyList() = runTest {
        // Given: API returns empty list wrapped in ApiResponse
        val apiResponse = ApiResponse(data = emptyList<RecommendationDto>())
        whenever(mockApiService.getTodayRecommendations()).thenReturn(apiResponse)

        // When: Get today's recommendations
        val result = repository.getTodayRecommendations()

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun getTodayRecommendations_nullData_returnsEmptyList() = runTest {
        // Given: API returns null data
        val apiResponse = ApiResponse(data = null as List<RecommendationDto>?)
        whenever(mockApiService.getTodayRecommendations()).thenReturn(apiResponse as ApiResponse<List<RecommendationDto>>)

        // When: Get today's recommendations
        val result = repository.getTodayRecommendations()

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    // ===== getStockRecommendations Tests =====

    @Test
    fun getStockRecommendations_success_returnsMap() = runTest {
        // Given: API returns recommendations map wrapped in ApiResponse
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

        // When: Get stock recommendations
        val result = repository.getStockRecommendations()

        // Then: Should return map with data
        assertEquals(2, result.size)
        assertTrue(result.containsKey("오늘"))
        assertTrue(result.containsKey("어제"))
        assertEquals(1, result["오늘"]?.size)
        assertEquals("삼성전자", result["오늘"]?.get(0)?.name)
    }

    @Test
    fun getStockRecommendations_emptyMap_returnsEmptyMap() = runTest {
        // Given: API returns empty map wrapped in ApiResponse
        val apiResponse = ApiResponse(data = emptyMap<String, List<RecommendationDto>>())
        whenever(mockApiService.getStockRecommendations()).thenReturn(apiResponse)

        // When: Get stock recommendations
        val result = repository.getStockRecommendations()

        // Then: Should return empty map
        assertTrue(result.isEmpty())
    }

    @Test
    fun getStockRecommendations_nullData_returnsEmptyMap() = runTest {
        // Given: API returns null data
        val apiResponse = ApiResponse(data = null as Map<String, List<RecommendationDto>>?)
        whenever(mockApiService.getStockRecommendations()).thenReturn(apiResponse as ApiResponse<Map<String, List<RecommendationDto>>>)

        // When: Get stock recommendations
        val result = repository.getStockRecommendations()

        // Then: Should return empty map
        assertTrue(result.isEmpty())
    }

    // ===== getStockDetail Tests =====

    @Test
    fun getStockDetail_success_returnsDetail() = runTest {
        // Given: API returns stock detail wrapped in ApiResponse
        val mockDetail = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            price = 72000
        )
        val apiResponse = ApiResponse(data = mockDetail)
        whenever(mockApiService.getStockDetail("005930")).thenReturn(apiResponse)

        // When: Get stock detail
        val result = repository.getStockDetail("005930")

        // Then: Should return detail data
        assertEquals("005930", result.ticker)
        assertEquals("삼성전자", result.name)
        assertEquals(72000L, result.price)
    }

    @Test
    fun getStockDetail_nullData_throwsException() = runTest {
        // Given: API returns null data
        val apiResponse = ApiResponse(data = null as StockDetailDto?)
        whenever(mockApiService.getStockDetail("005930")).thenReturn(apiResponse as ApiResponse<StockDetailDto>)

        // When/Then: Should throw NoSuchElementException
        try {
            repository.getStockDetail("005930")
            fail("Should throw NoSuchElementException")
        } catch (e: NoSuchElementException) {
            assertTrue(e.message?.contains("005930") == true)
        }
    }

    @Test
    fun getStockDetail_differentTickers_returnsDifferentData() = runTest {
        // Given: API returns different data for different tickers
        val detail1 = StockDetailDto(ticker = "005930", name = "삼성전자", price = 72000)
        val detail2 = StockDetailDto(ticker = "000660", name = "SK하이닉스", price = 150000)
        
        val apiResponse1 = ApiResponse(data = detail1)
        val apiResponse2 = ApiResponse(data = detail2)
        
        whenever(mockApiService.getStockDetail("005930")).thenReturn(apiResponse1)
        whenever(mockApiService.getStockDetail("000660")).thenReturn(apiResponse2)

        // When: Get different stock details
        val result1 = repository.getStockDetail("005930")
        val result2 = repository.getStockDetail("000660")

        // Then: Should return different data
        assertEquals("삼성전자", result1.name)
        assertEquals("SK하이닉스", result2.name)
        assertEquals(72000L, result1.price)
        assertEquals(150000L, result2.price)
    }
}