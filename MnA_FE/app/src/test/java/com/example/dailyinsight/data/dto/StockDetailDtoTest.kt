package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class StockDetailDtoTest {

    @Test
    fun stockDetailDto_withCompleteData_createsSuccessfully() {
        val dto = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            current = CurrentData(
                price = 72000,
                change = -100,
                changeRate = -0.14,
                marketCap = 1000000,
                date = "2024-01-01"
            ),
            valuation = ValuationData(
                peTtm = 15.5,
                priceToBook = 1.2,
                bps = 60000
            ),
            dividend = DividendData(
                `yield` = 2.5
            ),
            financials = FinancialsData(
                eps = 4800,
                dps = 1800,
                roe = 8.0
            ),
            history = listOf(
                HistoryItem(
                    date = "2024-01-01",
                    close = 70000.0,
                    marketCap = 980000,
                    per = 15.2,
                    pbr = 1.1,
                    eps = 4600,
                    bps = 59000,
                    divYield = 2.4,
                    dps = 1700,
                    roe = 7.8
                )
            ),
            profile = ProfileData(
                explanation = "반도체 제조업체"
            ),
            asOf = "2024-01-01"
        )

        assertEquals("005930", dto.ticker)
        assertEquals("삼성전자", dto.name)
        assertNotNull(dto.current)
        assertNotNull(dto.valuation)
        assertNotNull(dto.dividend)
        assertNotNull(dto.financials)
        assertNotNull(dto.history)
        assertNotNull(dto.profile)
    }

    @Test
    fun currentData_withAllFields_createsSuccessfully() {
        val current = CurrentData(
            price = 72000,
            change = -100,
            changeRate = -0.14,
            marketCap = 1000000,
            date = "2024-01-01"
        )

        assertEquals(72000L, current.price)
        assertEquals(-100L, current.change)
        assertEquals(-0.14, current.changeRate ?: 0.0, 0.001)
        assertEquals(1000000L, current.marketCap)
        assertEquals("2024-01-01", current.date)
    }

    @Test
    fun currentData_withNullFields_createsSuccessfully() {
        val current = CurrentData()

        assertNull(current.price)
        assertNull(current.change)
        assertNull(current.changeRate)
        assertNull(current.marketCap)
        assertNull(current.date)
    }

    @Test
    fun valuationData_withAllFields_createsSuccessfully() {
        val valuation = ValuationData(
            peTtm = 15.5,
            priceToBook = 1.2,
            bps = 60000
        )

        assertEquals(15.5, valuation.peTtm ?: 0.0, 0.001)
        assertEquals(1.2, valuation.priceToBook ?: 0.0, 0.001)
        assertEquals(60000L, valuation.bps)
    }

    @Test
    fun dividendData_withYield_createsSuccessfully() {
        val dividend = DividendData(`yield` = 2.5)

        assertEquals(2.5, dividend.`yield` ?: 0.0, 0.001)
    }

    @Test
    fun financialsData_withAllFields_createsSuccessfully() {
        val financials = FinancialsData(
            eps = 4800,
            dps = 1800,
            roe = 8.0
        )

        assertEquals(4800L, financials.eps)
        assertEquals(1800L, financials.dps)
        assertEquals(8.0, financials.roe ?: 0.0, 0.001)
    }

    @Test
    fun historyItem_withAllFields_createsSuccessfully() {
        val history = HistoryItem(
            date = "2024-01-01",
            close = 70000.0,
            marketCap = 980000,
            per = 15.2,
            pbr = 1.1,
            eps = 4600,
            bps = 59000,
            divYield = 2.4,
            dps = 1700,
            roe = 7.8
        )

        assertEquals("2024-01-01", history.date)
        assertEquals(70000.0, history.close, 0.001)
        assertEquals(980000L, history.marketCap)
        assertEquals(15.2, history.per ?: 0.0, 0.001)
        assertEquals(1.1, history.pbr ?: 0.0, 0.001)
        assertEquals(4600L, history.eps)
        assertEquals(59000L, history.bps)
        assertEquals(2.4, history.divYield ?: 0.0, 0.001)
        assertEquals(1700L, history.dps)
        assertEquals(7.8, history.roe ?: 0.0, 0.001)
    }

    @Test
    fun historyItem_withRequiredFieldsOnly_createsSuccessfully() {
        val history = HistoryItem(
            date = "2024-01-01",
            close = 70000.0
        )

        assertEquals("2024-01-01", history.date)
        assertEquals(70000.0, history.close, 0.001)
        assertNull(history.marketCap)
        assertNull(history.per)
    }

    @Test
    fun profileData_withExplanation_createsSuccessfully() {
        val profile = ProfileData(explanation = "반도체 제조업체")

        assertEquals("반도체 제조업체", profile.explanation)
    }

    @Test
    fun stockDetailDto_withMinimalData_createsSuccessfully() {
        val dto = StockDetailDto(
            ticker = "005930",
            name = "삼성전자"
        )

        assertEquals("005930", dto.ticker)
        assertEquals("삼성전자", dto.name)
        assertNull(dto.current)
        assertNull(dto.valuation)
        assertNull(dto.dividend)
        assertNull(dto.financials)
        assertNull(dto.history)
        assertNull(dto.profile)
        assertNull(dto.asOf)
    }

    @Test
    fun stockDetailDto_withEmptyHistory_createsSuccessfully() {
        val dto = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            history = emptyList()
        )

        assertNotNull(dto.history)
        assertTrue(dto.history!!.isEmpty())
    }

    @Test
    fun stockDetailDto_withMultipleHistoryItems_createsSuccessfully() {
        val dto = StockDetailDto(
            ticker = "005930",
            history = listOf(
                HistoryItem(date = "2024-01-01", close = 70000.0),
                HistoryItem(date = "2024-01-02", close = 71000.0),
                HistoryItem(date = "2024-01-03", close = 72000.0)
            )
        )

        assertEquals(3, dto.history?.size)
        assertEquals("2024-01-01", dto.history?.get(0)?.date)
        assertEquals(72000.0, dto.history?.get(2)?.close ?: 0.0, 0.001)
    }

    @Test
    fun currentData_withNegativeChange_createsSuccessfully() {
        val current = CurrentData(
            price = 72000,
            change = -1000,
            changeRate = -1.37
        )

        assertEquals(-1000L, current.change)
        assertEquals(-1.37, current.changeRate ?: 0.0, 0.001)
    }

    @Test
    fun currentData_withPositiveChange_createsSuccessfully() {
        val current = CurrentData(
            price = 72000,
            change = 1000,
            changeRate = 1.41
        )

        assertEquals(1000L, current.change)
        assertEquals(1.41, current.changeRate ?: 0.0, 0.001)
    }

    @Test
    fun currentData_withZeroValues_createsSuccessfully() {
        val dto = StockDetailDto(
            ticker = "005930",
            current = CurrentData(
                price = 0,
                change = 0,
                changeRate = 0.0
            )
        )

        assertEquals(0L, dto.current?.price)
        assertEquals(0L, dto.current?.change)
        assertEquals(0.0, dto.current?.changeRate ?: -1.0, 0.001)
    }

    @Test
    fun stockDetailDto_dataClasses_supportsEquality() {
        val current1 = CurrentData(price = 72000, change = -100)
        val current2 = CurrentData(price = 72000, change = -100)

        assertEquals(current1, current2)
    }

    @Test
    fun stockDetailDto_dataClasses_supportsCopy() {
        val original = CurrentData(price = 72000, change = -100)
        val copy = original.copy(price = 73000)

        assertEquals(73000L, copy.price)
        assertEquals(-100L, copy.change)
        assertEquals(72000L, original.price)
    }

    @Test
    fun historyItem_sortedByDate_maintainsOrder() {
        val items = listOf(
            HistoryItem(date = "2024-01-03", close = 72000.0),
            HistoryItem(date = "2024-01-01", close = 70000.0),
            HistoryItem(date = "2024-01-02", close = 71000.0)
        )

        val sorted = items.sortedBy { it.date }

        assertEquals("2024-01-01", sorted[0].date)
        assertEquals("2024-01-02", sorted[1].date)
        assertEquals("2024-01-03", sorted[2].date)
    }
}