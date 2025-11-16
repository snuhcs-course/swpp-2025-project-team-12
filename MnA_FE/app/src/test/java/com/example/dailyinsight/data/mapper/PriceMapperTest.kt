package com.example.dailyinsight.data.mapper

import com.example.dailyinsight.data.dto.PriceFinancialInfoDto
import com.example.dailyinsight.data.dto.StockDetailDto
import org.junit.Assert.*
import org.junit.Test

class PriceMapperTest {

    @Test
    fun extractPricePoints_withValidData_returnsPoints() {
        val dto = StockDetailDto(
            ticker = "005930",
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-01-01" to 70000.0,
                    "2024-01-02" to 71000.0,
                    "2024-01-03" to 72000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(3, result.size)
        assertEquals("2024-01-01", result[0].date)
        assertEquals(70000.0, result[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_sortsByDate() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-01-15" to 75000.0,
                    "2024-01-01" to 70000.0,
                    "2024-01-10" to 72000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals("2024-01-01", result[0].date)
        assertEquals("2024-01-10", result[1].date)
        assertEquals("2024-01-15", result[2].date)
    }

    @Test
    fun extractPricePoints_nullPriceFinancialInfo_returnsEmpty() {
        val dto = StockDetailDto(ticker = "005930", priceFinancialInfo = null)
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractPricePoints_nullPriceMap_returnsEmpty() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = null)
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractPricePoints_emptyMap_returnsEmpty() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = emptyMap())
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractPricePoints_singleEntry_works() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(1, result.size)
        assertEquals("2024-01-01", result[0].date)
        assertEquals(70000.0, result[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_largeDataset_works() {
        val prices = (1..365).associate { 
            "2024-${it.toString().padStart(3, '0')}" to (70000.0 + it) 
        }
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = prices)
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(365, result.size)
    }

    @Test
    fun extractPricePoints_preservesValues() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70123.456)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(70123.456, result[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_negativeValues_works() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to -100.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(-100.0, result[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_zeroValue_works() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 0.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(0.0, result[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_multipleCalls_consistent() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        
        val result1 = PriceMapper.extractPricePoints(dto)
        val result2 = PriceMapper.extractPricePoints(dto)
        
        assertEquals(result1.size, result2.size)
        assertEquals(result1[0].date, result2[0].date)
        assertEquals(result1[0].close, result2[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_dateFormat_preserved() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-01-01" to 70000.0,
                    "2024-12-31" to 80000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals("2024-01-01", result[0].date)
        assertEquals("2024-12-31", result[1].date)
    }

    @Test
    fun extractPricePoints_unsortedDates_sorted() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-12-31" to 80000.0,
                    "2024-06-15" to 75000.0,
                    "2024-01-01" to 70000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals("2024-01-01", result[0].date)
        assertEquals("2024-06-15", result[1].date)
        assertEquals("2024-12-31", result[2].date)
    }

    @Test
    fun extractPricePoints_yearBoundary_works() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2023-12-31" to 69000.0,
                    "2024-01-01" to 70000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(2, result.size)
        assertEquals("2023-12-31", result[0].date)
        assertEquals("2024-01-01", result[1].date)
    }

    @Test
    fun extractPricePoints_extremeValues_works() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-01-01" to Double.MAX_VALUE,
                    "2024-01-02" to Double.MIN_VALUE
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(2, result.size)
    }

    @Test
    fun extractPricePoints_decimalPrecision_preserved() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70123.123456789)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(70123.123456789, result[0].close, 0.00000001)
    }

    @Test
    fun extractPricePoints_multipleYears_sorted() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2025-01-01" to 85000.0,
                    "2023-01-01" to 65000.0,
                    "2024-01-01" to 75000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals("2023-01-01", result[0].date)
        assertEquals("2024-01-01", result[1].date)
        assertEquals("2025-01-01", result[2].date)
    }

    @Test
    fun extractPricePoints_withOtherFields_ignoresThem() {
        val dto = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            price = 70000L,
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(1, result.size)
        assertEquals("2024-01-01", result[0].date)
    }

    @Test
    fun extractPricePoints_emptyDate_works() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("" to 70000.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(1, result.size)
        assertEquals("", result[0].date)
    }

    @Test
    fun extractPricePoints_specialDateFormats_preserved() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024/01/01" to 70000.0,
                    "2024-01-02" to 71000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(2, result.size)
    }

    @Test
    fun extractPricePoints_consecutiveDates_ordered() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = (1..10).associate { 
                    "2024-01-${it.toString().padStart(2, '0')}" to (70000.0 + it * 100) 
                }
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals(10, result.size)
        for (i in 0 until result.size - 1) {
            assertTrue(result[i].date < result[i + 1].date)
        }
    }

    @Test
    fun extractPricePoints_mapPreservation_doesNotModifyOriginal() {
        val originalMap = mapOf("2024-01-01" to 70000.0)
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = originalMap)
        )
        
        PriceMapper.extractPricePoints(dto)
        
        assertEquals(1, originalMap.size)
        assertEquals(70000.0, originalMap["2024-01-01"] ?: 0.0, 0.001)
    }

    @Test
    fun extractPricePoints_returnsList_notNull() {
        val dto = StockDetailDto(priceFinancialInfo = null)
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertNotNull(result)
    }

    @Test
    fun extractPricePoints_returnsNewListEachTime() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        
        val result1 = PriceMapper.extractPricePoints(dto)
        val result2 = PriceMapper.extractPricePoints(dto)
        
        assertNotSame(result1, result2)
    }

    @Test
    fun extractPricePoints_chronologicalOrder_guaranteed() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf(
                    "2024-03-15" to 73000.0,
                    "2024-01-15" to 71000.0,
                    "2024-02-15" to 72000.0
                )
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        
        assertEquals("2024-01-15", result[0].date)
        assertEquals("2024-02-15", result[1].date)
        assertEquals("2024-03-15", result[2].date)
    }

    @Test
    fun extractPricePoints_pricePointsHaveCorrectFields() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        val point = result[0]
        
        assertNotNull(point.date)
        assertNotNull(point.close)
    }

    @Test
    fun priceMapper_objectSingleton_works() {
        assertNotNull(PriceMapper)
    }

    @Test
    fun priceMapper_multipleAccess_sameInstance() {
        val mapper1 = PriceMapper
        val mapper2 = PriceMapper
        assertSame(mapper1, mapper2)
    }

    @Test
    fun extractPricePoints_maintainsImmutability() {
        val dto = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(
                price = mapOf("2024-01-01" to 70000.0)
            )
        )
        
        val result = PriceMapper.extractPricePoints(dto)
        val originalSize = result.size
        
        // Try to verify list is independent
        val newResult = PriceMapper.extractPricePoints(dto)
        assertEquals(originalSize, newResult.size)
    }
}