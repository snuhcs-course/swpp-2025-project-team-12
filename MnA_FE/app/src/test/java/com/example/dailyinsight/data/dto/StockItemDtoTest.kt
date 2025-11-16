package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class StockItemDtoTest {

    // ===== StockItem =====
    @Test
    fun stockItem_createsCorrectly() {
        val item = StockItem("005930", "삼성전자")
        assertEquals("005930", item.ticker)
        assertEquals("삼성전자", item.name)
    }

    @Test
    fun stockItem_equality() {
        val item1 = StockItem("005930", "삼성전자")
        val item2 = StockItem("005930", "삼성전자")
        assertEquals(item1, item2)
    }

    @Test
    fun stockItem_copy() {
        val original = StockItem("005930", "삼성전자")
        val copied = original.copy(name = "Samsung")
        assertEquals("005930", copied.ticker)
        assertEquals("Samsung", copied.name)
    }

    @Test
    fun stockItem_emptyStrings() {
        val item = StockItem("", "")
        assertEquals("", item.ticker)
        assertEquals("", item.name)
    }

    @Test
    fun stockItem_specialCharacters() {
        val item = StockItem("ABC-123", "회사(주)")
        assertEquals("ABC-123", item.ticker)
        assertEquals("회사(주)", item.name)
    }

    // ===== CompanyListResponse =====
    @Test
    fun companyListResponse_createsCorrectly() {
        val items = listOf(
            StockItem("005930", "삼성전자"),
            StockItem("000660", "SK하이닉스")
        )
        val response = CompanyListResponse(
            items = items,
            total = 2,
            limit = 10,
            offset = 0,
            source = "KRX",
            asOf = "2024-01-15"
        )
        
        assertEquals(2, response.items.size)
        assertEquals(2, response.total)
        assertEquals(10, response.limit)
        assertEquals(0, response.offset)
        assertEquals("KRX", response.source)
        assertEquals("2024-01-15", response.asOf)
    }

    @Test
    fun companyListResponse_emptyItems() {
        val response = CompanyListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = "KRX",
            asOf = "2024-01-15"
        )
        
        assertTrue(response.items.isEmpty())
        assertEquals(0, response.total)
    }

    @Test
    fun companyListResponse_largeList() {
        val items = (1..1000).map { StockItem("STOCK$it", "회사$it") }
        val response = CompanyListResponse(
            items = items,
            total = 1000,
            limit = 1000,
            offset = 0,
            source = "API",
            asOf = "2024-01-15"
        )
        
        assertEquals(1000, response.items.size)
        assertEquals(1000, response.total)
    }

    @Test
    fun companyListResponse_withPagination() {
        val items = listOf(StockItem("A", "A"))
        val response = CompanyListResponse(
            items = items,
            total = 100,
            limit = 10,
            offset = 20,
            source = "API",
            asOf = "2024-01-15"
        )
        
        assertEquals(100, response.total)
        assertEquals(10, response.limit)
        assertEquals(20, response.offset)
    }

    // ===== PortfolioRequest =====
    @Test
    fun portfolioRequest_createsCorrectly() {
        val tickers = setOf("005930", "000660", "035420")
        val request = PortfolioRequest(tickers)
        
        assertEquals(3, request.portfolio.size)
        assertTrue(request.portfolio.contains("005930"))
    }

    @Test
    fun portfolioRequest_emptySet() {
        val request = PortfolioRequest(emptySet())
        assertTrue(request.portfolio.isEmpty())
    }

    @Test
    fun portfolioRequest_singleItem() {
        val request = PortfolioRequest(setOf("005930"))
        assertEquals(1, request.portfolio.size)
        assertEquals("005930", request.portfolio.first())
    }

    @Test
    fun portfolioRequest_largeSet() {
        val tickers = (1..100).map { "STOCK$it" }.toSet()
        val request = PortfolioRequest(tickers)
        assertEquals(100, request.portfolio.size)
    }

    @Test
    fun portfolioRequest_duplicatesRemoved() {
        // Set should automatically remove duplicates
        val request = PortfolioRequest(setOf("005930", "005930"))
        assertEquals(1, request.portfolio.size)
    }

    // ===== PortfolioResponse =====
    @Test
    fun portfolioResponse_success() {
        val response = PortfolioResponse("Portfolio updated")
        assertEquals("Portfolio updated", response.message)
    }

    @Test
    fun portfolioResponse_failure() {
        val response = PortfolioResponse("Failed to update")
        assertEquals("Failed to update", response.message)
    }

    @Test
    fun portfolioResponse_emptyMessage() {
        val response = PortfolioResponse("")
        assertEquals("", response.message)
    }

    @Test
    fun portfolioResponse_koreanMessage() {
        val response = PortfolioResponse("포트폴리오 업데이트 완료")
        assertEquals("포트폴리오 업데이트 완료", response.message)
    }

    // ===== Integration Tests =====
    @Test
    fun fullPortfolioFlow() {
        val items = listOf(
            StockItem("005930", "삼성전자"),
            StockItem("000660", "SK하이닉스")
        )
        val listResponse = CompanyListResponse(
            items = items,
            total = 2,
            limit = 10,
            offset = 0,
            source = "API",
            asOf = "2024-01-15"
        )
        
        val request = PortfolioRequest(setOf("005930", "000660"))
        val response = PortfolioResponse("Success")
        
        assertEquals(listResponse.items.size, request.portfolio.size)
        assertNotNull(response.message)
    }

    @Test
    fun stockItem_toString() {
        val item = StockItem("005930", "삼성전자")
        assertNotNull(item.toString())
        assertTrue(item.toString().contains("StockItem"))
    }

    @Test
    fun stockItem_hashCode() {
        val item1 = StockItem("005930", "삼성전자")
        val item2 = StockItem("005930", "삼성전자")
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun companyListResponse_copy() {
        val original = CompanyListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = "API",
            asOf = "2024-01-15"
        )
        val copied = original.copy(total = 100)
        assertEquals(100, copied.total)
        assertEquals(10, copied.limit)
    }
}