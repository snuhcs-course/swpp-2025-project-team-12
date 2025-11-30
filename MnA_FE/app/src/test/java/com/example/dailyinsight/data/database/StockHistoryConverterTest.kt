package com.example.dailyinsight.data.database

import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StockHistoryConverterTest {

    private lateinit var converter: StockHistoryConverter

    @Before
    fun setup() {
        converter = StockHistoryConverter()
    }

    // ===== fromStockHistoryItemList Tests =====

    @Test
    fun fromStockHistoryItemList_convertsListToJson() {
        val list = listOf(
            StockIndexHistoryItem("2024-01-01", 2500.0),
            StockIndexHistoryItem("2024-01-02", 2520.0)
        )

        val json = converter.fromStockHistoryItemList(list)

        assertTrue(json.contains("2024-01-01"))
        assertTrue(json.contains("2500.0"))
        assertTrue(json.contains("2024-01-02"))
        assertTrue(json.contains("2520.0"))
    }

    @Test
    fun fromStockHistoryItemList_emptyList_returnsEmptyJsonArray() {
        val list = emptyList<StockIndexHistoryItem>()

        val json = converter.fromStockHistoryItemList(list)

        assertEquals("[]", json)
    }

    @Test
    fun fromStockHistoryItemList_singleItem_convertsCorrectly() {
        val list = listOf(StockIndexHistoryItem("2024-01-01", 2500.0))

        val json = converter.fromStockHistoryItemList(list)

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("2024-01-01"))
    }

    // ===== toStockHistoryItemList Tests =====

    @Test
    fun toStockHistoryItemList_convertsJsonToList() {
        val json = """[{"date":"2024-01-01","close":2500.0},{"date":"2024-01-02","close":2520.0}]"""

        val list = converter.toStockHistoryItemList(json)

        assertEquals(2, list.size)
        assertEquals("2024-01-01", list[0].date)
        assertEquals(2500.0, list[0].close, 0.01)
        assertEquals("2024-01-02", list[1].date)
        assertEquals(2520.0, list[1].close, 0.01)
    }

    @Test
    fun toStockHistoryItemList_emptyJsonArray_returnsEmptyList() {
        val json = "[]"

        val list = converter.toStockHistoryItemList(json)

        assertTrue(list.isEmpty())
    }

    @Test
    fun toStockHistoryItemList_singleItem_convertsCorrectly() {
        val json = """[{"date":"2024-01-01","close":2500.0}]"""

        val list = converter.toStockHistoryItemList(json)

        assertEquals(1, list.size)
        assertEquals("2024-01-01", list[0].date)
    }

    // ===== Round-trip Tests =====

    @Test
    fun roundTrip_listToJsonAndBack_preservesData() {
        val originalList = listOf(
            StockIndexHistoryItem("2024-01-01", 2500.0),
            StockIndexHistoryItem("2024-01-02", 2520.0),
            StockIndexHistoryItem("2024-01-03", 2480.0)
        )

        val json = converter.fromStockHistoryItemList(originalList)
        val restoredList = converter.toStockHistoryItemList(json)

        assertEquals(originalList.size, restoredList.size)
        for (i in originalList.indices) {
            assertEquals(originalList[i].date, restoredList[i].date)
            assertEquals(originalList[i].close, restoredList[i].close, 0.01)
        }
    }

    @Test
    fun roundTrip_emptyList_preservesData() {
        val originalList = emptyList<StockIndexHistoryItem>()

        val json = converter.fromStockHistoryItemList(originalList)
        val restoredList = converter.toStockHistoryItemList(json)

        assertTrue(restoredList.isEmpty())
    }

    @Test
    fun roundTrip_largeList_preservesData() {
        val originalList = (1..365).map { day ->
            StockIndexHistoryItem(
                date = "2024-${(day / 30 + 1).toString().padStart(2, '0')}-${(day % 28 + 1).toString().padStart(2, '0')}",
                close = 2500.0 + day
            )
        }

        val json = converter.fromStockHistoryItemList(originalList)
        val restoredList = converter.toStockHistoryItemList(json)

        assertEquals(365, restoredList.size)
        assertEquals(originalList.first().date, restoredList.first().date)
        assertEquals(originalList.last().close, restoredList.last().close, 0.01)
    }

    // ===== Edge Cases =====

    @Test
    fun fromStockHistoryItemList_negativeValues_convertsCorrectly() {
        val list = listOf(
            StockIndexHistoryItem("2024-01-01", -100.0),
            StockIndexHistoryItem("2024-01-02", -50.5)
        )

        val json = converter.fromStockHistoryItemList(list)
        val restored = converter.toStockHistoryItemList(json)

        assertEquals(-100.0, restored[0].close, 0.01)
        assertEquals(-50.5, restored[1].close, 0.01)
    }

    @Test
    fun fromStockHistoryItemList_zeroValues_convertsCorrectly() {
        val list = listOf(StockIndexHistoryItem("2024-01-01", 0.0))

        val json = converter.fromStockHistoryItemList(list)
        val restored = converter.toStockHistoryItemList(json)

        assertEquals(0.0, restored[0].close, 0.01)
    }

    @Test
    fun fromStockHistoryItemList_largeValues_convertsCorrectly() {
        val list = listOf(StockIndexHistoryItem("2024-01-01", 999999999.99))

        val json = converter.fromStockHistoryItemList(list)
        val restored = converter.toStockHistoryItemList(json)

        assertEquals(999999999.99, restored[0].close, 0.01)
    }
}