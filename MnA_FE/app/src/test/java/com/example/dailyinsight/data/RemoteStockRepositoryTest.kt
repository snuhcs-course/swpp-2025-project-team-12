package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.CompanyListResponse
import com.example.dailyinsight.data.dto.PortfolioRequest
import com.example.dailyinsight.data.dto.PortfolioResponse
import com.example.dailyinsight.data.dto.StockItem
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response
import java.io.IOException

@ExperimentalCoroutinesApi
class RemoteStockRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var repository: RemoteStockRepository

    @Before
    fun setup() {
        api = mock()
        repository = RemoteStockRepository(api)
    }

    // ===== fetchStocks Tests =====

    @Test
    fun fetchStocks_success_returnsStockList() = runTest {
        val stockItems = listOf(
            StockItem("005930", "삼성전자"),
            StockItem("000660", "SK하이닉스")
        )
        val response = Response.success(
            CompanyListResponse(
                items = stockItems,
                total = 2,
                limit = 100,
                offset = 0,
                source = "api",
                asOf = "2024-01-01"
            )
        )
        whenever(api.getCompanyList(100, 0)).thenReturn(response)

        val result = repository.fetchStocks()

        assertEquals(2, result.size)
        assertEquals("005930", result[0].ticker)
        assertEquals("삼성전자", result[0].name)
    }

    @Test
    fun fetchStocks_emptyList_returnsEmptyList() = runTest {
        val response = Response.success(
            CompanyListResponse(
                items = emptyList(),
                total = 0,
                limit = 100,
                offset = 0,
                source = "api",
                asOf = "2024-01-01"
            )
        )
        whenever(api.getCompanyList(100, 0)).thenReturn(response)

        val result = repository.fetchStocks()

        assertTrue(result.isEmpty())
    }

    @Test
    fun fetchStocks_apiError_returnsEmptyList() = runTest {
        val errorResponse = Response.error<CompanyListResponse>(
            500,
            "Server Error".toResponseBody()
        )
        whenever(api.getCompanyList(100, 0)).thenReturn(errorResponse)

        val result = repository.fetchStocks()

        assertTrue(result.isEmpty())
    }

    @Test
    fun fetchStocks_networkException_returnsEmptyList() = runTest {
        whenever(api.getCompanyList(100, 0)).thenAnswer { throw IOException("Network error") }

        val result = repository.fetchStocks()

        assertTrue(result.isEmpty())
    }

    @Test
    fun fetchStocks_runtimeException_returnsEmptyList() = runTest {
        whenever(api.getCompanyList(100, 0)).thenAnswer { throw RuntimeException("Unexpected error") }

        val result = repository.fetchStocks()

        assertTrue(result.isEmpty())
    }

    // ===== submitSelectedStocks Tests =====

    @Test
    fun submitSelectedStocks_success_returnsTrue() = runTest {
        val selected = setOf("005930", "000660")
        val response = Response.success(PortfolioResponse(message = "success"))
        whenever(api.setPortfolio(any())).thenReturn(response)

        val result = repository.submitSelectedStocks(selected)

        assertTrue(result)
        verify(api).setPortfolio(argThat<PortfolioRequest> { portfolio == selected })
    }

    @Test
    fun submitSelectedStocks_emptySet_returnsTrue() = runTest {
        val selected = emptySet<String>()
        val response = Response.success(PortfolioResponse(message = "success"))
        whenever(api.setPortfolio(any())).thenReturn(response)

        val result = repository.submitSelectedStocks(selected)

        assertTrue(result)
    }

    @Test
    fun submitSelectedStocks_apiError_returnsFalse() = runTest {
        val selected = setOf("005930")
        val errorResponse = Response.error<PortfolioResponse>(
            400,
            "Bad Request".toResponseBody()
        )
        whenever(api.setPortfolio(any())).thenReturn(errorResponse)

        val result = repository.submitSelectedStocks(selected)

        assertFalse(result)
    }

    @Test
    fun submitSelectedStocks_networkException_returnsFalse() = runTest {
        val selected = setOf("005930")
        whenever(api.setPortfolio(any())).thenAnswer { throw IOException("Network error") }

        val result = repository.submitSelectedStocks(selected)

        assertFalse(result)
    }

    @Test
    fun submitSelectedStocks_serverError_returnsFalse() = runTest {
        val selected = setOf("005930")
        val errorResponse = Response.error<PortfolioResponse>(
            500,
            "Internal Server Error".toResponseBody()
        )
        whenever(api.setPortfolio(any())).thenReturn(errorResponse)

        val result = repository.submitSelectedStocks(selected)

        assertFalse(result)
    }

    @Test
    fun submitSelectedStocks_runtimeException_returnsFalse() = runTest {
        val selected = setOf("005930")
        whenever(api.setPortfolio(any())).thenAnswer { throw RuntimeException("Unexpected error") }

        val result = repository.submitSelectedStocks(selected)

        assertFalse(result)
    }
}