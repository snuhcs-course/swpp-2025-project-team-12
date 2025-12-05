package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class PortfolioDtoTest {

    @Test
    fun portfolioRequest_withTickers_createsSuccessfully() {
        val request = PortfolioRequest(
            portfolio = listOf("005930", "000660", "035720")
        )

        assertEquals(3, request.portfolio.size)
        assertEquals("005930", request.portfolio[0])
        assertEquals("000660", request.portfolio[1])
        assertEquals("035720", request.portfolio[2])
    }

    @Test
    fun portfolioRequest_withEmptyList_createsSuccessfully() {
        val request = PortfolioRequest(portfolio = emptyList())

        assertTrue(request.portfolio.isEmpty())
    }

    @Test
    fun portfolioRequest_withSingleTicker_createsSuccessfully() {
        val request = PortfolioRequest(portfolio = listOf("005930"))

        assertEquals(1, request.portfolio.size)
        assertEquals("005930", request.portfolio[0])
    }

    @Test
    fun portfolioResponse_withTickers_createsSuccessfully() {
        val response = PortfolioResponse(
            portfolio = listOf("005930", "000660", "035720")
        )

        assertNotNull(response.portfolio)
        assertEquals(3, response.portfolio?.size)
        assertEquals("005930", response.portfolio?.get(0))
    }

    @Test
    fun portfolioResponse_withNullPortfolio_createsSuccessfully() {
        val response = PortfolioResponse(portfolio = null)

        assertNull(response.portfolio)
    }

    @Test
    fun portfolioResponse_withDefaultNullPortfolio_createsSuccessfully() {
        val response = PortfolioResponse()

        assertNull(response.portfolio)
    }

    @Test
    fun portfolioResponse_withEmptyList_createsSuccessfully() {
        val response = PortfolioResponse(portfolio = emptyList())

        assertNotNull(response.portfolio)
        assertTrue(response.portfolio?.isEmpty() == true)
    }

    @Test
    fun portfolioRequest_dataClassEquality_works() {
        val request1 = PortfolioRequest(listOf("005930", "000660"))
        val request2 = PortfolioRequest(listOf("005930", "000660"))

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun portfolioResponse_dataClassEquality_works() {
        val response1 = PortfolioResponse(listOf("005930", "000660"))
        val response2 = PortfolioResponse(listOf("005930", "000660"))

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }
}
