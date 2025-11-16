package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StockTest {

    @Test
    fun stockEntry_createsCorrectly() {
        // Given: Stock entry data
        val date = LocalDate.of(2024, 1, 15)
        val entry = StockEntry(
            date = date,
            name = "삼성전자",
            score = 85,
            changeText = "+2.5%"
        )

        // Then: Properties are set correctly
        assertEquals(date, entry.date)
        assertEquals("삼성전자", entry.name)
        assertEquals(85, entry.score)
        assertEquals("+2.5%", entry.changeText)
    }

    @Test
    fun stockEntry_equalityWorks() {
        // Given: Two identical entries
        val date = LocalDate.of(2024, 1, 15)
        val entry1 = StockEntry(date, "삼성전자", 85, "+2.5%")
        val entry2 = StockEntry(date, "삼성전자", 85, "+2.5%")

        // Then: Should be equal
        assertEquals(entry1, entry2)
        assertEquals(entry1.hashCode(), entry2.hashCode())
    }

    @Test
    fun stockItem_header_createsCorrectly() {
        // Given: Header item
        val header = StockItem.Header("2024-01-15")

        // Then: Should have correct label
        assertEquals("2024-01-15", header.dateLabel)
    }

    @Test
    fun stockItem_row_createsCorrectly() {
        // Given: Row item with entry
        val entry = StockEntry(
            date = LocalDate.of(2024, 1, 15),
            name = "SK하이닉스",
            score = 90,
            changeText = "+5.0%"
        )
        val row = StockItem.Row(entry)

        // Then: Should contain the entry
        assertEquals(entry, row.entry)
        assertEquals("SK하이닉스", row.entry.name)
    }

    @Test
    fun stockItem_sealedClass_canBeEitherHeaderOrRow() {
        // Given: Different stock items
        val header: StockItem = StockItem.Header("Today")
        val row: StockItem = StockItem.Row(
            StockEntry(LocalDate.now(), "Test", 75, "+1.0%")
        )

        // Then: Both are StockItems
        assertTrue(header is StockItem.Header)
        assertTrue(row is StockItem.Row)
        assertFalse(header is StockItem.Row)
        assertFalse(row is StockItem.Header)
    }

    @Test
    fun stockEntry_copyWorks() {
        // Given: Original entry
        val original = StockEntry(
            date = LocalDate.of(2024, 1, 15),
            name = "삼성전자",
            score = 85,
            changeText = "+2.5%"
        )

        // When: Copy with changes
        val modified = original.copy(score = 90, changeText = "+3.0%")

        // Then: Only modified fields change
        assertEquals(original.date, modified.date)
        assertEquals(original.name, modified.name)
        assertEquals(90, modified.score)
        assertEquals("+3.0%", modified.changeText)
    }
}