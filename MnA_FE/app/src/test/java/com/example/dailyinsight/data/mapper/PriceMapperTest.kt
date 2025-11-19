package com.example.dailyinsight.data.mapper

import com.example.dailyinsight.data.dto.HistoryItem
import com.example.dailyinsight.data.dto.StockDetailDto
import org.junit.Assert.*
import org.junit.Test

/**
 * Note: PriceMapper has been removed from the codebase.
 * This test file now tests HistoryItem usage patterns instead.
 */
class PriceMapperTest {

    @Test
    fun historyList_withValidData_canBeAccessed() {
        val dto = StockDetailDto(
            ticker = "005930",
            history = listOf(
                HistoryItem(date = "2024-01-01", close = 70000.0),
                HistoryItem(date = "2024-01-02", close = 71000.0),
                HistoryItem(date = "2024-01-03", close = 72000.0)
            )
        )
        
        val history = dto.history ?: emptyList()
        
        assertEquals(3, history.size)
        assertEquals("2024-01-01", history[0].date)
        assertEquals(70000.0, history[0].close, 0.001)
    }

    @Test
    fun historyList_sortsByDate() {
        val dto = StockDetailDto(
            history = listOf(
                HistoryItem(date = "2024-01-15", close = 75000.0),
                HistoryItem(date = "2024-01-01", close = 70000.0),
                HistoryItem(date = "2024-01-10", close = 72000.0)
            )
        )
        
        val sorted = dto.history?.sortedBy { it.date } ?: emptyList()
        
        assertEquals("2024-01-01", sorted[0].date)
        assertEquals("2024-01-10", sorted[1].date)
        assertEquals("2024-01-15", sorted[2].date)
    }

    @Test
    fun historyList_nullHistory_returnsEmpty() {
        val dto = StockDetailDto(ticker = "005930", history = null)
        
        val history = dto.history ?: emptyList()
        
        assertTrue(history.isEmpty())
    }

    @Test
    fun historyList_emptyList_returnsEmpty() {
        val dto = StockDetailDto(history = emptyList())
        
        val history = dto.history ?: emptyList()
        
        assertTrue(history.isEmpty())
    }

    @Test
    fun historyItem_singleEntry_works() {
        val dto = StockDetailDto(
            history = listOf(
                HistoryItem(date = "2024-01-01", close = 70000.0)
            )
        )
        
        val history = dto.history ?: emptyList()
        
        assertEquals(1, history.size)
        assertEquals("2024-01-01", history[0].date)
        assertEquals(70000.0, history[0].close, 0.001)
    }

    @Test
    fun historyList_largeDataset_works() {
        val items = (1..365).map { 
            HistoryItem(
                date = "2024-${it.toString().padStart(3, '0')}",
                close = 70000.0 + it
            )
        }
        val dto = StockDetailDto(history = items)
        
        val history = dto.history ?: emptyList()
        
        assertEquals(365, history.size)
    }

    @Test
    fun historyItem_preservesValues() {
        val dto = StockDetailDto(
            history = listOf(
                HistoryItem(date = "2024-01-01", close = 70123.456)
            )
        )
        
        val history = dto.history ?: emptyList()
        
        assertEquals(70123.456, history[0].close, 0.001)
    }

    @Test
    fun historyItem_allOptionalFields_accessible() {
        val item = HistoryItem(
            date = "2024-01-01",
            close = 70000.0,
            marketCap = 1000000,
            per = 15.5,
            pbr = 1.2,
            eps = 4800,
            bps = 60000,
            divYield = 2.5,
            dps = 1800,
            roe = 8.0
        )
        
        assertEquals("2024-01-01", item.date)
        assertEquals(70000.0, item.close, 0.001)
        assertEquals(1000000L, item.marketCap)
        assertEquals(15.5, item.per ?: 0.0, 0.001)
        assertEquals(1.2, item.pbr ?: 0.0, 0.001)
        assertEquals(4800L, item.eps)
        assertEquals(60000L, item.bps)
        assertEquals(2.5, item.divYield ?: 0.0, 0.001)
        assertEquals(1800L, item.dps)
        assertEquals(8.0, item.roe ?: 0.0, 0.001)
    }
}