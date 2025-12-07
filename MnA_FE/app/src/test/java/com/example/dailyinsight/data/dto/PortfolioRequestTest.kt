package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class PortfolioRequestTest {

    @Test
    fun create_withTickers() {
        val request = PortfolioRequest(listOf("AAPL", "GOOG", "MSFT"))
        assertEquals(3, request.portfolio.size)
        assertEquals("AAPL", request.portfolio[0])
        assertEquals("GOOG", request.portfolio[1])
        assertEquals("MSFT", request.portfolio[2])
    }

    @Test
    fun create_withEmptyList() {
        val request = PortfolioRequest(emptyList())
        assertTrue(request.portfolio.isEmpty())
    }

    @Test
    fun create_withSingleTicker() {
        val request = PortfolioRequest(listOf("AAPL"))
        assertEquals(1, request.portfolio.size)
        assertEquals("AAPL", request.portfolio[0])
    }

    @Test
    fun equality_samePortfolio() {
        val request1 = PortfolioRequest(listOf("AAPL", "GOOG"))
        val request2 = PortfolioRequest(listOf("AAPL", "GOOG"))
        assertEquals(request1, request2)
    }

    @Test
    fun inequality_differentOrder() {
        val request1 = PortfolioRequest(listOf("AAPL", "GOOG"))
        val request2 = PortfolioRequest(listOf("GOOG", "AAPL"))
        assertNotEquals(request1, request2)
    }

    @Test
    fun inequality_differentTickers() {
        val request1 = PortfolioRequest(listOf("AAPL"))
        val request2 = PortfolioRequest(listOf("GOOG"))
        assertNotEquals(request1, request2)
    }

    @Test
    fun inequality_differentSize() {
        val request1 = PortfolioRequest(listOf("AAPL"))
        val request2 = PortfolioRequest(listOf("AAPL", "GOOG"))
        assertNotEquals(request1, request2)
    }

    @Test
    fun hashCode_consistency() {
        val request1 = PortfolioRequest(listOf("AAPL", "GOOG"))
        val request2 = PortfolioRequest(listOf("AAPL", "GOOG"))
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun copy_modifiesPortfolio() {
        val original = PortfolioRequest(listOf("AAPL"))
        val copied = original.copy(portfolio = listOf("GOOG", "MSFT"))
        assertEquals(listOf("GOOG", "MSFT"), copied.portfolio)
    }

    @Test
    fun toString_containsPortfolio() {
        val request = PortfolioRequest(listOf("AAPL", "GOOG"))
        val str = request.toString()
        assertTrue(str.contains("AAPL"))
        assertTrue(str.contains("GOOG"))
    }

    @Test
    fun destructuring() {
        val request = PortfolioRequest(listOf("AAPL", "GOOG"))
        val (portfolio) = request
        assertEquals(listOf("AAPL", "GOOG"), portfolio)
    }

    @Test
    fun portfolio_containsCheck() {
        val request = PortfolioRequest(listOf("AAPL", "GOOG", "MSFT"))
        assertTrue(request.portfolio.contains("AAPL"))
        assertTrue(request.portfolio.contains("GOOG"))
        assertFalse(request.portfolio.contains("TSLA"))
    }

    @Test
    fun portfolio_iterating() {
        val tickers = listOf("AAPL", "GOOG", "MSFT")
        val request = PortfolioRequest(tickers)
        var count = 0
        for (ticker in request.portfolio) {
            assertEquals(tickers[count], ticker)
            count++
        }
        assertEquals(3, count)
    }

    // ===== PortfolioResponse Tests =====

    @Test
    fun portfolioResponse_withTickers() {
        val response = PortfolioResponse(listOf("005930", "000660", "035720"))
        assertNotNull(response.portfolio)
        assertEquals(3, response.portfolio?.size)
        assertEquals("005930", response.portfolio?.get(0))
    }

    @Test
    fun portfolioResponse_withEmptyList() {
        val response = PortfolioResponse(emptyList())
        assertNotNull(response.portfolio)
        assertTrue(response.portfolio!!.isEmpty())
    }

    @Test
    fun portfolioResponse_withNull() {
        val response = PortfolioResponse(null)
        assertNull(response.portfolio)
    }

    @Test
    fun portfolioResponse_defaultValue() {
        val response = PortfolioResponse()
        assertNull(response.portfolio)
    }

    @Test
    fun portfolioResponse_equality() {
        val r1 = PortfolioResponse(listOf("005930", "000660"))
        val r2 = PortfolioResponse(listOf("005930", "000660"))
        assertEquals(r1, r2)
    }

    @Test
    fun portfolioResponse_inequality() {
        val r1 = PortfolioResponse(listOf("005930"))
        val r2 = PortfolioResponse(listOf("000660"))
        assertNotEquals(r1, r2)
    }

    @Test
    fun portfolioResponse_hashCode() {
        val r1 = PortfolioResponse(listOf("005930", "000660"))
        val r2 = PortfolioResponse(listOf("005930", "000660"))
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun portfolioResponse_copy() {
        val original = PortfolioResponse(listOf("005930"))
        val copied = original.copy(portfolio = listOf("000660", "035720"))
        assertEquals(2, copied.portfolio?.size)
        assertEquals("000660", copied.portfolio?.get(0))
    }

    @Test
    fun portfolioResponse_toString() {
        val response = PortfolioResponse(listOf("005930"))
        val str = response.toString()
        assertTrue(str.contains("005930"))
    }

    @Test
    fun portfolioResponse_destructuring() {
        val response = PortfolioResponse(listOf("005930", "000660"))
        val (portfolio) = response
        assertEquals(2, portfolio?.size)
    }

    @Test
    fun portfolioResponse_containsCheck() {
        val response = PortfolioResponse(listOf("005930", "000660", "035720"))
        assertTrue(response.portfolio!!.contains("005930"))
        assertFalse(response.portfolio!!.contains("AAPL"))
    }

    @Test
    fun portfolioResponse_singleTicker() {
        val response = PortfolioResponse(listOf("005930"))
        assertEquals(1, response.portfolio?.size)
        assertEquals("005930", response.portfolio?.get(0))
    }

    @Test
    fun portfolioResponse_largeTickers() {
        val tickers = (1..100).map { String.format("%06d", it) }
        val response = PortfolioResponse(tickers)
        assertEquals(100, response.portfolio?.size)
    }
}
