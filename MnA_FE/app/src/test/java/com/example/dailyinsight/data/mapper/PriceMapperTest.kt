package com.example.dailyinsight.data.mapper

import com.example.dailyinsight.data.dto.PriceFinancialInfoDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.model.PricePoint
import org.junit.Assert.*
import org.junit.Test

class PriceMapperTest {

    @Test
    fun extractPricePoints_withValidData_returnsCorrectlySortedList() {
        // Given: Stock detail with unsorted price data
        val priceMap = mapOf(
            "2024-01-15" to 75000.0,
            "2024-01-10" to 70000.0,
            "2024-01-20" to 80000.0,
            "2024-01-05" to 65000.0
        )
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = priceMap)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should be sorted by date in ascending order
        assertEquals(4, result.size)
        assertEquals("2024-01-05", result[0].date)
        assertEquals(65000.0, result[0].close, 0.001)
        assertEquals("2024-01-10", result[1].date)
        assertEquals(70000.0, result[1].close, 0.001)
        assertEquals("2024-01-15", result[2].date)
        assertEquals(75000.0, result[2].close, 0.001)
        assertEquals("2024-01-20", result[3].date)
        assertEquals(80000.0, result[3].close, 0.001)
    }

    @Test
    fun extractPricePoints_withEmptyPriceMap_returnsEmptyList() {
        // Given: Stock detail with empty price map
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = emptyMap())
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractPricePoints_withNullPriceMap_returnsEmptyList() {
        // Given: Stock detail with null price map
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = null)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractPricePoints_withNullPriceFinancialInfo_returnsEmptyList() {
        // Given: Stock detail with null priceFinancialInfo
        val stockDetail = StockDetailDto(priceFinancialInfo = null)

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractPricePoints_withSingleDataPoint_returnsSingleElement() {
        // Given: Stock detail with single price point
        val priceMap = mapOf("2024-01-15" to 75000.0)
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = priceMap)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should return single element
        assertEquals(1, result.size)
        assertEquals("2024-01-15", result[0].date)
        assertEquals(75000.0, result[0].close, 0.001)
    }

    @Test
    fun extractPricePoints_withDecimalPrices_preservesDecimalValues() {
        // Given: Stock detail with decimal price values
        val priceMap = mapOf(
            "2024-01-10" to 70100.50,
            "2024-01-11" to 70200.75,
            "2024-01-12" to 70150.25
        )
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = priceMap)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should preserve decimal values
        assertEquals(3, result.size)
        assertEquals(70100.50, result[0].close, 0.001)
        assertEquals(70200.75, result[1].close, 0.001)
        assertEquals(70150.25, result[2].close, 0.001)
    }

    @Test
    fun extractPricePoints_withMultipleYears_sortsByDateAcrossYears() {
        // Given: Price data spanning multiple years
        val priceMap = mapOf(
            "2024-01-15" to 75000.0,
            "2023-12-20" to 72000.0,
            "2024-02-10" to 78000.0,
            "2023-11-05" to 70000.0
        )
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = priceMap)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should sort chronologically across years
        assertEquals(4, result.size)
        assertEquals("2023-11-05", result[0].date)
        assertEquals("2023-12-20", result[1].date)
        assertEquals("2024-01-15", result[2].date)
        assertEquals("2024-02-10", result[3].date)
    }

    @Test
    fun extractPricePoints_withZeroPrices_includesZeroValues() {
        // Given: Price data including zero values
        val priceMap = mapOf(
            "2024-01-10" to 70000.0,
            "2024-01-11" to 0.0,
            "2024-01-12" to 75000.0
        )
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = priceMap)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should include zero values
        assertEquals(3, result.size)
        assertEquals(0.0, result[1].close, 0.001)
    }

    @Test
    fun extractPricePoints_withNegativePrices_includesNegativeValues() {
        // Given: Price data including negative values (edge case)
        val priceMap = mapOf(
            "2024-01-10" to 70000.0,
            "2024-01-11" to -100.0,
            "2024-01-12" to 75000.0
        )
        val stockDetail = StockDetailDto(
            priceFinancialInfo = PriceFinancialInfoDto(price = priceMap)
        )

        // When: Extract price points
        val result = PriceMapper.extractPricePoints(stockDetail)

        // Then: Should include negative values
        assertEquals(3, result.size)
        assertEquals(-100.0, result[1].close, 0.001)
    }
}
