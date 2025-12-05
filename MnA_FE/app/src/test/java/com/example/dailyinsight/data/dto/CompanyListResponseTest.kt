package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class CompanyListResponseTest {

    // ===== StockItem Tests =====

    @Test
    fun stockItem_createsCorrectly() {
        val item = StockItem(
            ticker = "005930",
            name = "삼성전자"
        )

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
    fun stockItem_inequality_differentTicker() {
        val item1 = StockItem("005930", "삼성전자")
        val item2 = StockItem("000660", "삼성전자")

        assertNotEquals(item1, item2)
    }

    @Test
    fun stockItem_inequality_differentName() {
        val item1 = StockItem("005930", "삼성전자")
        val item2 = StockItem("005930", "Samsung Electronics")

        assertNotEquals(item1, item2)
    }

    @Test
    fun stockItem_hashCode() {
        val item1 = StockItem("005930", "삼성전자")
        val item2 = StockItem("005930", "삼성전자")

        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun stockItem_copy() {
        val original = StockItem("005930", "삼성전자")
        val copied = original.copy(name = "Samsung")

        assertEquals("005930", copied.ticker)
        assertEquals("Samsung", copied.name)
    }

    @Test
    fun stockItem_toString() {
        val item = StockItem("005930", "삼성전자")
        val str = item.toString()

        assertTrue(str.contains("005930"))
        assertTrue(str.contains("삼성전자"))
    }

    @Test
    fun stockItem_destructuring() {
        val item = StockItem("005930", "삼성전자")
        val (ticker, name) = item

        assertEquals("005930", ticker)
        assertEquals("삼성전자", name)
    }

    @Test
    fun stockItem_emptyValues() {
        val item = StockItem("", "")

        assertEquals("", item.ticker)
        assertEquals("", item.name)
    }

    // ===== CompanyListResponse Tests =====

    @Test
    fun companyListResponse_createsCorrectly() {
        val items = listOf(
            StockItem("005930", "삼성전자"),
            StockItem("000660", "SK하이닉스")
        )

        val response = CompanyListResponse(
            items = items,
            total = 100,
            limit = 10,
            offset = 0,
            source = "api",
            asOf = "2024-01-15"
        )

        assertEquals(2, response.items.size)
        assertEquals(100, response.total)
        assertEquals(10, response.limit)
        assertEquals(0, response.offset)
        assertEquals("api", response.source)
        assertEquals("2024-01-15", response.asOf)
    }

    @Test
    fun companyListResponse_withEmptyItems() {
        val response = CompanyListResponse(
            items = emptyList(),
            total = 0,
            limit = 10,
            offset = 0,
            source = "cache",
            asOf = "2024-01-15"
        )

        assertTrue(response.items.isEmpty())
        assertEquals(0, response.total)
    }

    @Test
    fun companyListResponse_pagination() {
        val response = CompanyListResponse(
            items = emptyList(),
            total = 100,
            limit = 20,
            offset = 40,
            source = "api",
            asOf = "2024-01-15"
        )

        assertEquals(100, response.total)
        assertEquals(20, response.limit)
        assertEquals(40, response.offset)
    }

    @Test
    fun companyListResponse_equality() {
        val items = listOf(StockItem("005930", "삼성전자"))

        val response1 = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val response2 = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")

        assertEquals(response1, response2)
    }

    @Test
    fun companyListResponse_inequality() {
        val items = listOf(StockItem("005930", "삼성전자"))

        val response1 = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val response2 = CompanyListResponse(items, 200, 10, 0, "api", "2024-01-15")

        assertNotEquals(response1, response2)
    }

    @Test
    fun companyListResponse_hashCode() {
        val items = listOf(StockItem("005930", "삼성전자"))

        val response1 = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val response2 = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")

        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun companyListResponse_copy() {
        val items = listOf(StockItem("005930", "삼성전자"))
        val original = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")
        val copied = original.copy(total = 200)

        assertEquals(200, copied.total)
        assertEquals("api", copied.source)
    }

    @Test
    fun companyListResponse_accessItems() {
        val items = listOf(
            StockItem("005930", "삼성전자"),
            StockItem("000660", "SK하이닉스"),
            StockItem("035720", "카카오")
        )

        val response = CompanyListResponse(items, 3, 10, 0, "api", "2024-01-15")

        assertEquals("005930", response.items[0].ticker)
        assertEquals("SK하이닉스", response.items[1].name)
        assertEquals("035720", response.items[2].ticker)
    }

    @Test
    fun companyListResponse_filterItems() {
        val items = listOf(
            StockItem("005930", "삼성전자"),
            StockItem("000660", "SK하이닉스"),
            StockItem("035720", "카카오")
        )

        val response = CompanyListResponse(items, 3, 10, 0, "api", "2024-01-15")

        val samsungItems = response.items.filter { it.name.contains("삼성") }
        assertEquals(1, samsungItems.size)
        assertEquals("005930", samsungItems[0].ticker)
    }

    @Test
    fun companyListResponse_destructuring() {
        val items = listOf(StockItem("005930", "삼성전자"))
        val response = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")

        val (itemsList, total, limit, offset, source, asOf) = response

        assertEquals(items, itemsList)
        assertEquals(100, total)
        assertEquals(10, limit)
        assertEquals(0, offset)
        assertEquals("api", source)
        assertEquals("2024-01-15", asOf)
    }

    @Test
    fun companyListResponse_toString() {
        val items = listOf(StockItem("005930", "삼성전자"))
        val response = CompanyListResponse(items, 100, 10, 0, "api", "2024-01-15")

        val str = response.toString()

        assertTrue(str.contains("100"))
        assertTrue(str.contains("api"))
    }
}
