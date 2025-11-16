package com.example.dailyinsight.ui.stock

import com.example.dailyinsight.data.dto.RecommendationDto
import org.junit.Assert.*
import org.junit.Test

class StockRowsTest {

    // Helper function to create test recommendation
    private fun createRecommendation(
        ticker: String = "005930",
        name: String = "삼성전자",
        price: Long = 70000,
        change: Long = -100,
        changeRate: Double = -0.14,
        headline: String? = "Test headline"
    ) = RecommendationDto(ticker, name, price, change, changeRate, headline)

    // ===== Tests for List<RecommendationDto>.toRows() =====

    @Test
    fun listToRows_withNonEmptyList_returnsHeaderPlusItems() {
        // Given: A list of recommendations
        val recommendations = listOf(
            createRecommendation(ticker = "005930", name = "삼성전자"),
            createRecommendation(ticker = "000660", name = "SK하이닉스")
        )

        // When: Convert to rows with default title
        val result = recommendations.toRows()

        // Then: Should have header + 2 items (3 total)
        assertEquals(3, result.size)
        assertTrue(result[0] is StockRow.Header)
        assertEquals("오늘", (result[0] as StockRow.Header).label)
        assertTrue(result[1] is StockRow.Item)
        assertEquals("삼성전자", (result[1] as StockRow.Item).data.name)
        assertTrue(result[2] is StockRow.Item)
        assertEquals("SK하이닉스", (result[2] as StockRow.Item).data.name)
    }

    @Test
    fun listToRows_withCustomTitle_usesCustomTitle() {
        // Given: A list of recommendations
        val recommendations = listOf(createRecommendation())

        // When: Convert to rows with custom title
        val result = recommendations.toRows("어제")

        // Then: Should use custom title
        assertEquals(2, result.size)
        assertTrue(result[0] is StockRow.Header)
        assertEquals("어제", (result[0] as StockRow.Header).label)
    }

    @Test
    fun listToRows_withEmptyList_returnsEmptyList() {
        // Given: An empty list
        val recommendations = emptyList<RecommendationDto>()

        // When: Convert to rows
        val result = recommendations.toRows()

        // Then: Should return empty list (no header for empty content)
        assertTrue(result.isEmpty())
    }

    @Test
    fun listToRows_withSingleItem_returnsHeaderPlusOneItem() {
        // Given: A single recommendation
        val recommendations = listOf(createRecommendation(name = "카카오"))

        // When: Convert to rows
        val result = recommendations.toRows("2024-01-15")

        // Then: Should have header + 1 item
        assertEquals(2, result.size)
        assertTrue(result[0] is StockRow.Header)
        assertEquals("2024-01-15", (result[0] as StockRow.Header).label)
        assertTrue(result[1] is StockRow.Item)
        assertEquals("카카오", (result[1] as StockRow.Item).data.name)
    }

    // ===== Tests for Map<String, List<RecommendationDto>>.toRows() =====

    @Test
    fun mapToRows_withTodayOnly_returnsTodaySection() {
        // Given: Map with only "오늘" data
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(name = "삼성전자"),
                createRecommendation(name = "SK하이닉스")
            )
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Should have header "오늘" + 2 items
        assertEquals(3, result.size)
        assertTrue(result[0] is StockRow.Header)
        assertEquals("오늘", (result[0] as StockRow.Header).label)
        assertEquals("삼성전자", (result[1] as StockRow.Item).data.name)
        assertEquals("SK하이닉스", (result[2] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_withYesterdayOnly_returnsYesterdaySection() {
        // Given: Map with only "어제" data
        val dataMap = mapOf(
            "어제" to listOf(
                createRecommendation(name = "네이버"),
                createRecommendation(name = "카카오")
            )
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Should have header "어제" + 2 items
        assertEquals(3, result.size)
        assertTrue(result[0] is StockRow.Header)
        assertEquals("어제", (result[0] as StockRow.Header).label)
        assertEquals("네이버", (result[1] as StockRow.Item).data.name)
        assertEquals("카카오", (result[2] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_withTodayAndYesterday_returnsBothInCorrectOrder() {
        // Given: Map with both "오늘" and "어제"
        val dataMap = mapOf(
            "어제" to listOf(createRecommendation(name = "어제 주식")),
            "오늘" to listOf(createRecommendation(name = "오늘 주식"))
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: "오늘" should come first, then "어제"
        assertEquals(4, result.size)
        assertEquals("오늘", (result[0] as StockRow.Header).label)
        assertEquals("오늘 주식", (result[1] as StockRow.Item).data.name)
        assertEquals("어제", (result[2] as StockRow.Header).label)
        assertEquals("어제 주식", (result[3] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_withDateStrings_sortsInDescendingOrder() {
        // Given: Map with date strings (older dates)
        val dataMap = mapOf(
            "2024-01-10" to listOf(createRecommendation(name = "Stock 10")),
            "2024-01-15" to listOf(createRecommendation(name = "Stock 15")),
            "2024-01-05" to listOf(createRecommendation(name = "Stock 05"))
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Dates should be in descending order (newest first)
        assertEquals(6, result.size)
        assertEquals("2024-01-15", (result[0] as StockRow.Header).label)
        assertEquals("Stock 15", (result[1] as StockRow.Item).data.name)
        assertEquals("2024-01-10", (result[2] as StockRow.Header).label)
        assertEquals("Stock 10", (result[3] as StockRow.Item).data.name)
        assertEquals("2024-01-05", (result[4] as StockRow.Header).label)
        assertEquals("Stock 05", (result[5] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_withMixedKeys_todayAndYesterdayFirst() {
        // Given: Map with "오늘", "어제", and date strings
        val dataMap = mapOf(
            "2024-01-15" to listOf(createRecommendation(name = "Date Stock")),
            "어제" to listOf(createRecommendation(name = "Yesterday Stock")),
            "2024-01-10" to listOf(createRecommendation(name = "Old Date Stock")),
            "오늘" to listOf(createRecommendation(name = "Today Stock"))
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Order should be "오늘", "어제", then dates descending
        assertEquals(8, result.size)
        assertEquals("오늘", (result[0] as StockRow.Header).label)
        assertEquals("Today Stock", (result[1] as StockRow.Item).data.name)
        assertEquals("어제", (result[2] as StockRow.Header).label)
        assertEquals("Yesterday Stock", (result[3] as StockRow.Item).data.name)
        assertEquals("2024-01-15", (result[4] as StockRow.Header).label)
        assertEquals("Date Stock", (result[5] as StockRow.Item).data.name)
        assertEquals("2024-01-10", (result[6] as StockRow.Header).label)
        assertEquals("Old Date Stock", (result[7] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_withEmptyMap_returnsEmptyList() {
        // Given: Empty map
        val dataMap = emptyMap<String, List<RecommendationDto>>()

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun mapToRows_withEmptyLists_skipsEmptySections() {
        // Given: Map with some empty lists
        val dataMap = mapOf(
            "오늘" to emptyList(),
            "어제" to listOf(createRecommendation(name = "Yesterday Stock")),
            "2024-01-10" to emptyList()
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Should only include "어제" section (non-empty)
        assertEquals(2, result.size)
        assertEquals("어제", (result[0] as StockRow.Header).label)
        assertEquals("Yesterday Stock", (result[1] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_withNullLists_treatsAsEmpty() {
        // Given: Map that might have null values
        val dataMap: Map<String, List<RecommendationDto>?> = mapOf(
            "오늘" to null,
            "어제" to listOf(createRecommendation(name = "Valid Stock"))
        )

        // When: Convert to rows (using safe call)
        val result = dataMap.mapValues { it.value.orEmpty() }.toRows()

        // Then: Should skip null section
        assertEquals(2, result.size)
        assertEquals("어제", (result[0] as StockRow.Header).label)
    }

    @Test
    fun mapToRows_withMultipleItemsPerDate_includesAllItems() {
        // Given: Multiple recommendations per date
        val dataMap = mapOf(
            "오늘" to listOf(
                createRecommendation(ticker = "005930", name = "삼성전자"),
                createRecommendation(ticker = "000660", name = "SK하이닉스"),
                createRecommendation(ticker = "035420", name = "네이버")
            )
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Should have header + 3 items
        assertEquals(4, result.size)
        assertEquals("오늘", (result[0] as StockRow.Header).label)
        assertEquals("삼성전자", (result[1] as StockRow.Item).data.name)
        assertEquals("SK하이닉스", (result[2] as StockRow.Item).data.name)
        assertEquals("네이버", (result[3] as StockRow.Item).data.name)
    }

    @Test
    fun mapToRows_preservesRecommendationData() {
        // Given: Map with detailed recommendation data
        val recommendation = createRecommendation(
            ticker = "005930",
            name = "삼성전자",
            price = 70000,
            change = -500,
            changeRate = -0.71,
            headline = "Important news"
        )
        val dataMap = mapOf("오늘" to listOf(recommendation))

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Should preserve all recommendation data
        val item = result[1] as StockRow.Item
        assertEquals("005930", item.data.ticker)
        assertEquals("삼성전자", item.data.name)
        assertEquals(70000L, item.data.price)
        assertEquals(-500L, item.data.change)
        assertEquals(-0.71, item.data.changeRate, 0.001)
        assertEquals("Important news", item.data.headline)
    }

    @Test
    fun mapToRows_withComplexScenario_correctlyOrganizes() {
        // Given: Complex realistic scenario
        val dataMap = mapOf(
            "2024-01-01" to listOf(createRecommendation(name = "New Year Stock")),
            "오늘" to listOf(
                createRecommendation(name = "Today 1"),
                createRecommendation(name = "Today 2")
            ),
            "2024-01-20" to listOf(createRecommendation(name = "Recent Stock")),
            "어제" to listOf(createRecommendation(name = "Yesterday 1")),
            "2024-01-10" to listOf(createRecommendation(name = "Mid Month Stock"))
        )

        // When: Convert to rows
        val result = dataMap.toRows()

        // Then: Verify complete order and structure
        val headers = result.filterIsInstance<StockRow.Header>()
        assertEquals(5, headers.size)
        assertEquals("오늘", headers[0].label)
        assertEquals("어제", headers[1].label)
        assertEquals("2024-01-20", headers[2].label)
        assertEquals("2024-01-10", headers[3].label)
        assertEquals("2024-01-01", headers[4].label)

        // Verify total count (5 headers + 6 items = 11)
        assertEquals(11, result.size)
    }
}
