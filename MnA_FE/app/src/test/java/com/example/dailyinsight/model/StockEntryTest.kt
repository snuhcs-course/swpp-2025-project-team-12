package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StockEntryTest {

    @Test
    fun stockEntry_createsCorrectly() {
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(
            date = date,
            name = "Samsung",
            score = 85,
            changeText = "+1.5%"
        )
        assertEquals(date, entry.date)
        assertEquals("Samsung", entry.name)
        assertEquals(85, entry.score)
        assertEquals("+1.5%", entry.changeText)
    }

    @Test
    fun stockEntry_equality() {
        val date = LocalDate.of(2024, 1, 15)
        val entry1 = StockEntry(date, "Samsung", 85, "+1.5%")
        val entry2 = StockEntry(date, "Samsung", 85, "+1.5%")
        assertEquals(entry1, entry2)
    }

    @Test
    fun stockEntry_inequality_differentName() {
        val date = LocalDate.of(2024, 1, 15)
        val entry1 = StockEntry(date, "Samsung", 85, "+1.5%")
        val entry2 = StockEntry(date, "Apple", 85, "+1.5%")
        assertNotEquals(entry1, entry2)
    }

    @Test
    fun stockEntry_inequality_differentScore() {
        val date = LocalDate.of(2024, 1, 15)
        val entry1 = StockEntry(date, "Samsung", 85, "+1.5%")
        val entry2 = StockEntry(date, "Samsung", 90, "+1.5%")
        assertNotEquals(entry1, entry2)
    }

    @Test
    fun stockEntry_inequality_differentDate() {
        val entry1 = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val entry2 = StockEntry(LocalDate.of(2024, 1, 16), "Samsung", 85, "+1.5%")
        assertNotEquals(entry1, entry2)
    }

    @Test
    fun stockEntry_copy() {
        val date = LocalDate.of(2024, 1, 15)
        val original = StockEntry(date, "Samsung", 85, "+1.5%")
        val copied = original.copy(score = 90)
        assertEquals(90, copied.score)
        assertEquals("Samsung", copied.name)
    }

    @Test
    fun stockEntry_hashCode() {
        val date = LocalDate.of(2024, 1, 15)
        val entry1 = StockEntry(date, "Samsung", 85, "+1.5%")
        val entry2 = StockEntry(date, "Samsung", 85, "+1.5%")
        assertEquals(entry1.hashCode(), entry2.hashCode())
    }

    @Test
    fun stockEntry_negativeScore() {
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(date, "Stock", -5, "-2.0%")
        assertEquals(-5, entry.score)
    }

    @Test
    fun stockEntry_zeroScore() {
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(date, "Stock", 0, "0.0%")
        assertEquals(0, entry.score)
    }

    @Test
    fun stockEntry_emptyChangeText() {
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(date, "Stock", 50, "")
        assertEquals("", entry.changeText)
    }

    @Test
    fun stockEntry_koreanName() {
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(date, "삼성전자", 85, "+1.5%")
        assertEquals("삼성전자", entry.name)
    }

    @Test
    fun stockEntry_destructuring() {
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(date, "Samsung", 85, "+1.5%")
        val (d, n, s, c) = entry
        assertEquals(date, d)
        assertEquals("Samsung", n)
        assertEquals(85, s)
        assertEquals("+1.5%", c)
    }
}

class StockItemTest {

    @Test
    fun header_createsCorrectly() {
        val header = StockItem.Header("Today")
        assertEquals("Today", header.dateLabel)
    }

    @Test
    fun header_equality() {
        val header1 = StockItem.Header("Today")
        val header2 = StockItem.Header("Today")
        assertEquals(header1, header2)
    }

    @Test
    fun header_inequality() {
        val header1 = StockItem.Header("Today")
        val header2 = StockItem.Header("Yesterday")
        assertNotEquals(header1, header2)
    }

    @Test
    fun row_createsCorrectly() {
        val entry = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val row = StockItem.Row(entry)
        assertEquals(entry, row.entry)
    }

    @Test
    fun row_equality() {
        val entry = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val row1 = StockItem.Row(entry)
        val row2 = StockItem.Row(entry)
        assertEquals(row1, row2)
    }

    @Test
    fun header_isNotEqualToRow() {
        val header = StockItem.Header("Today")
        val entry = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val row = StockItem.Row(entry)
        assertNotEquals(header, row)
    }

    @Test
    fun header_isStockItem() {
        val header: StockItem = StockItem.Header("Today")
        assertTrue(header is StockItem.Header)
    }

    @Test
    fun row_isStockItem() {
        val entry = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val row: StockItem = StockItem.Row(entry)
        assertTrue(row is StockItem.Row)
    }

    @Test
    fun whenExpression_worksWithSealed() {
        val entry = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val items = listOf<StockItem>(
            StockItem.Header("Today"),
            StockItem.Row(entry)
        )

        val labels = items.map { item ->
            when (item) {
                is StockItem.Header -> item.dateLabel
                is StockItem.Row -> item.entry.name
            }
        }

        assertEquals("Today", labels[0])
        assertEquals("Samsung", labels[1])
    }

    @Test
    fun header_copy() {
        val original = StockItem.Header("Today")
        val copied = original.copy(dateLabel = "Yesterday")
        assertEquals("Yesterday", copied.dateLabel)
    }

    @Test
    fun row_copy() {
        val entry1 = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val entry2 = StockEntry(LocalDate.of(2024, 1, 16), "Apple", 90, "+2.0%")
        val original = StockItem.Row(entry1)
        val copied = original.copy(entry = entry2)
        assertEquals(entry2, copied.entry)
    }

    @Test
    fun header_hashCode() {
        val header1 = StockItem.Header("Today")
        val header2 = StockItem.Header("Today")
        assertEquals(header1.hashCode(), header2.hashCode())
    }

    @Test
    fun row_hashCode() {
        val entry = StockEntry(LocalDate.of(2024, 1, 15), "Samsung", 85, "+1.5%")
        val row1 = StockItem.Row(entry)
        val row2 = StockItem.Row(entry)
        assertEquals(row1.hashCode(), row2.hashCode())
    }
}
