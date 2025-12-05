package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class MarketIndexDtoTest {

    @Test
    fun stockIndexData_withAllFields_createsSuccessfully() {
        val data = StockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 25.0,
            changePercent = 1.01,
            date = "2024-01-15",
            high = 2520.0,
            low = 2480.0,
            open = 2490.0,
            volume = 1000000L
        )

        assertEquals("KOSPI", data.name)
        assertEquals(2500.0, data.close, 0.001)
        assertEquals(25.0, data.changeAmount, 0.001)
        assertEquals(1.01, data.changePercent, 0.001)
        assertEquals("2024-01-15", data.date)
        assertEquals(2520.0, data.high, 0.001)
        assertEquals(2480.0, data.low, 0.001)
        assertEquals(2490.0, data.open, 0.001)
        assertEquals(1000000L, data.volume)
    }

    @Test
    fun stockIndexData_withNegativeChange_createsSuccessfully() {
        val data = StockIndexData(
            name = "KOSDAQ",
            close = 800.0,
            changeAmount = -10.0,
            changePercent = -1.23,
            date = "2024-01-15",
            high = 810.0,
            low = 795.0,
            open = 808.0,
            volume = 500000L
        )

        assertEquals(-10.0, data.changeAmount, 0.001)
        assertEquals(-1.23, data.changePercent, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_createsSuccessfully() {
        val item = StockIndexHistoryItem(
            date = "2024-01-15",
            close = 2500.0
        )

        assertEquals("2024-01-15", item.date)
        assertEquals(2500.0, item.close, 0.001)
    }

    @Test
    fun stockIndexHistoryResponse_withData_createsSuccessfully() {
        val items = listOf(
            StockIndexHistoryItem("2024-01-13", 2480.0),
            StockIndexHistoryItem("2024-01-14", 2490.0),
            StockIndexHistoryItem("2024-01-15", 2500.0)
        )

        val response = StockIndexHistoryResponse(
            status = "success",
            index = "KOSPI",
            data = items
        )

        assertEquals("success", response.status)
        assertEquals("KOSPI", response.index)
        assertEquals(3, response.data.size)
        assertEquals(2480.0, response.data[0].close, 0.001)
        assertEquals(2500.0, response.data[2].close, 0.001)
    }

    @Test
    fun stockIndexHistoryResponse_withEmptyData_createsSuccessfully() {
        val response = StockIndexHistoryResponse(
            status = "success",
            index = "KOSPI",
            data = emptyList()
        )

        assertEquals("success", response.status)
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun stockIndexLatestResponse_withSingleIndex_createsSuccessfully() {
        val kospiData = StockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 25.0,
            changePercent = 1.01,
            date = "2024-01-15",
            high = 2520.0,
            low = 2480.0,
            open = 2490.0,
            volume = 1000000L
        )

        val response = StockIndexLatestResponse(
            status = "success",
            data = mapOf("KOSPI" to kospiData)
        )

        assertEquals("success", response.status)
        assertEquals(1, response.data.size)
        assertNotNull(response.data["KOSPI"])
        assertEquals(2500.0, response.data["KOSPI"]?.close ?: 0.0, 0.001)
    }

    @Test
    fun stockIndexLatestResponse_withMultipleIndices_createsSuccessfully() {
        val kospiData = StockIndexData(
            name = "KOSPI",
            close = 2500.0,
            changeAmount = 25.0,
            changePercent = 1.01,
            date = "2024-01-15",
            high = 2520.0,
            low = 2480.0,
            open = 2490.0,
            volume = 1000000L
        )

        val kosdaqData = StockIndexData(
            name = "KOSDAQ",
            close = 800.0,
            changeAmount = -10.0,
            changePercent = -1.23,
            date = "2024-01-15",
            high = 810.0,
            low = 795.0,
            open = 808.0,
            volume = 500000L
        )

        val response = StockIndexLatestResponse(
            status = "success",
            data = mapOf(
                "KOSPI" to kospiData,
                "KOSDAQ" to kosdaqData
            )
        )

        assertEquals(2, response.data.size)
        assertNotNull(response.data["KOSPI"])
        assertNotNull(response.data["KOSDAQ"])
    }

    @Test
    fun stockIndexData_nameMutability_works() {
        val data = StockIndexData(
            name = "OLD_NAME",
            close = 2500.0,
            changeAmount = 25.0,
            changePercent = 1.01,
            date = "2024-01-15",
            high = 2520.0,
            low = 2480.0,
            open = 2490.0,
            volume = 1000000L
        )

        data.name = "NEW_NAME"
        assertEquals("NEW_NAME", data.name)
    }

    @Test
    fun stockIndexHistoryItem_sortedByDate_maintainsOrder() {
        val items = listOf(
            StockIndexHistoryItem("2024-01-15", 2500.0),
            StockIndexHistoryItem("2024-01-13", 2480.0),
            StockIndexHistoryItem("2024-01-14", 2490.0)
        )

        val sorted = items.sortedBy { it.date }

        assertEquals("2024-01-13", sorted[0].date)
        assertEquals("2024-01-14", sorted[1].date)
        assertEquals("2024-01-15", sorted[2].date)
    }

    // ===== Additional Tests =====

    @Test
    fun stockIndexHistoryItem_equality() {
        val item1 = StockIndexHistoryItem("2024-01-15", 2500.0)
        val item2 = StockIndexHistoryItem("2024-01-15", 2500.0)
        assertEquals(item1, item2)
    }

    @Test
    fun stockIndexHistoryItem_inequality() {
        val item1 = StockIndexHistoryItem("2024-01-15", 2500.0)
        val item2 = StockIndexHistoryItem("2024-01-15", 2600.0)
        assertNotEquals(item1, item2)
    }

    @Test
    fun stockIndexHistoryItem_hashCode() {
        val item1 = StockIndexHistoryItem("2024-01-15", 2500.0)
        val item2 = StockIndexHistoryItem("2024-01-15", 2500.0)
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun stockIndexHistoryItem_copy() {
        val original = StockIndexHistoryItem("2024-01-15", 2500.0)
        val copied = original.copy(close = 2600.0)
        assertEquals("2024-01-15", copied.date)
        assertEquals(2600.0, copied.close, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_destructuring() {
        val item = StockIndexHistoryItem("2024-01-15", 2500.0)
        val (date, close) = item
        assertEquals("2024-01-15", date)
        assertEquals(2500.0, close, 0.001)
    }

    @Test
    fun stockIndexHistoryItem_toString() {
        val item = StockIndexHistoryItem("2024-01-15", 2500.0)
        val str = item.toString()
        assertTrue(str.contains("2024-01-15"))
        assertTrue(str.contains("2500"))
    }

    @Test
    fun stockIndexData_equality() {
        val data1 = StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L)
        val data2 = StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L)
        assertEquals(data1, data2)
    }

    @Test
    fun stockIndexData_hashCode() {
        val data1 = StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L)
        val data2 = StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L)
        assertEquals(data1.hashCode(), data2.hashCode())
    }

    @Test
    fun stockIndexData_copy() {
        val original = StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L)
        val copied = original.copy(close = 2600.0)
        assertEquals("KOSPI", copied.name)
        assertEquals(2600.0, copied.close, 0.001)
    }

    @Test
    fun stockIndexData_destructuring() {
        val data = StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L)
        val (name, close, changeAmount, changePercent, date, high, low, open, volume) = data
        assertEquals("KOSPI", name)
        assertEquals(2500.0, close, 0.001)
        assertEquals(25.0, changeAmount, 0.001)
        assertEquals(1.0, changePercent, 0.001)
        assertEquals("2024-01-15", date)
        assertEquals(2520.0, high, 0.001)
        assertEquals(2480.0, low, 0.001)
        assertEquals(2490.0, open, 0.001)
        assertEquals(500000000L, volume)
    }

    @Test
    fun stockIndexData_zeroChange() {
        val data = StockIndexData("KOSPI", 2500.0, 0.0, 0.0, "2024-01-15", 2500.0, 2500.0, 2500.0, 500000000L)
        assertEquals(0.0, data.changeAmount, 0.001)
        assertEquals(0.0, data.changePercent, 0.001)
    }

    @Test
    fun stockIndexLatestResponse_emptyData() {
        val response = StockIndexLatestResponse("success", emptyMap())
        assertEquals("success", response.status)
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun stockIndexLatestResponse_equality() {
        val data = mapOf("KOSPI" to StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L))
        val response1 = StockIndexLatestResponse("success", data)
        val response2 = StockIndexLatestResponse("success", data)
        assertEquals(response1, response2)
    }

    @Test
    fun stockIndexLatestResponse_copy() {
        val data = mapOf("KOSPI" to StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L))
        val original = StockIndexLatestResponse("success", data)
        val copied = original.copy(status = "error")
        assertEquals("error", copied.status)
        assertEquals(data, copied.data)
    }

    @Test
    fun stockIndexLatestResponse_destructuring() {
        val data = mapOf("KOSPI" to StockIndexData("KOSPI", 2500.0, 25.0, 1.0, "2024-01-15", 2520.0, 2480.0, 2490.0, 500000000L))
        val response = StockIndexLatestResponse("success", data)
        val (status, dataMap) = response
        assertEquals("success", status)
        assertEquals(data, dataMap)
    }

    @Test
    fun stockIndexHistoryResponse_equality() {
        val items = listOf(StockIndexHistoryItem("2024-01-15", 2500.0))
        val response1 = StockIndexHistoryResponse("success", "KOSPI", items)
        val response2 = StockIndexHistoryResponse("success", "KOSPI", items)
        assertEquals(response1, response2)
    }

    @Test
    fun stockIndexHistoryResponse_copy() {
        val items = listOf(StockIndexHistoryItem("2024-01-15", 2500.0))
        val original = StockIndexHistoryResponse("success", "KOSPI", items)
        val copied = original.copy(index = "KOSDAQ")
        assertEquals("KOSDAQ", copied.index)
        assertEquals("success", copied.status)
    }

    @Test
    fun stockIndexHistoryResponse_destructuring() {
        val items = listOf(StockIndexHistoryItem("2024-01-15", 2500.0))
        val response = StockIndexHistoryResponse("success", "KOSPI", items)
        val (status, index, data) = response
        assertEquals("success", status)
        assertEquals("KOSPI", index)
        assertEquals(items, data)
    }

    @Test
    fun stockIndexHistoryResponse_largeDataSet() {
        val items = (1..365).map { day ->
            StockIndexHistoryItem("2024-${(day / 30 + 1).coerceAtMost(12)}-${(day % 28 + 1)}", 2500.0 + day)
        }
        val response = StockIndexHistoryResponse("success", "KOSPI", items)
        assertEquals(365, response.data.size)
    }
}
