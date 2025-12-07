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

    // ===== Additional Tests =====

    @Test
    fun stockDetailDto_equality() {
        val dto1 = StockDetailDto(ticker = "005930", name = "삼성전자")
        val dto2 = StockDetailDto(ticker = "005930", name = "삼성전자")
        assertEquals(dto1, dto2)
    }

    @Test
    fun stockDetailDto_hashCode() {
        val dto1 = StockDetailDto(ticker = "005930", name = "삼성전자")
        val dto2 = StockDetailDto(ticker = "005930", name = "삼성전자")
        assertEquals(dto1.hashCode(), dto2.hashCode())
    }

    @Test
    fun stockDetailDto_copy() {
        val original = StockDetailDto(ticker = "005930", name = "삼성전자")
        val copied = original.copy(name = "Samsung Electronics")
        assertEquals("Samsung Electronics", copied.name)
        assertEquals("005930", copied.ticker)
    }

    @Test
    fun stockDetailDto_toString() {
        val dto = StockDetailDto(ticker = "005930", name = "삼성전자")
        val str = dto.toString()
        assertTrue(str.contains("005930"))
        assertTrue(str.contains("삼성전자"))
    }

    @Test
    fun valuationData_equality() {
        val val1 = ValuationData(peTtm = 15.5, priceToBook = 1.2, bps = 60000)
        val val2 = ValuationData(peTtm = 15.5, priceToBook = 1.2, bps = 60000)
        assertEquals(val1, val2)
    }

    @Test
    fun valuationData_copy() {
        val original = ValuationData(peTtm = 15.5, priceToBook = 1.2, bps = 60000)
        val copied = original.copy(peTtm = 16.0)
        assertEquals(16.0, copied.peTtm ?: 0.0, 0.001)
        assertEquals(1.2, copied.priceToBook ?: 0.0, 0.001)
    }

    @Test
    fun dividendData_equality() {
        val div1 = DividendData(`yield` = 2.5)
        val div2 = DividendData(`yield` = 2.5)
        assertEquals(div1, div2)
    }

    @Test
    fun dividendData_copy() {
        val original = DividendData(`yield` = 2.5)
        val copied = original.copy(`yield` = 3.0)
        assertEquals(3.0, copied.`yield` ?: 0.0, 0.001)
    }

    @Test
    fun financialsData_equality() {
        val fin1 = FinancialsData(eps = 4800, dps = 1800, roe = 8.0)
        val fin2 = FinancialsData(eps = 4800, dps = 1800, roe = 8.0)
        assertEquals(fin1, fin2)
    }

    @Test
    fun financialsData_copy() {
        val original = FinancialsData(eps = 4800, dps = 1800, roe = 8.0)
        val copied = original.copy(roe = 9.0)
        assertEquals(9.0, copied.roe ?: 0.0, 0.001)
        assertEquals(4800L, copied.eps)
    }

    @Test
    fun historyItem_equality() {
        val h1 = HistoryItem(date = "2024-01-01", close = 70000.0)
        val h2 = HistoryItem(date = "2024-01-01", close = 70000.0)
        assertEquals(h1, h2)
    }

    @Test
    fun historyItem_hashCode() {
        val h1 = HistoryItem(date = "2024-01-01", close = 70000.0)
        val h2 = HistoryItem(date = "2024-01-01", close = 70000.0)
        assertEquals(h1.hashCode(), h2.hashCode())
    }

    @Test
    fun historyItem_copy() {
        val original = HistoryItem(date = "2024-01-01", close = 70000.0)
        val copied = original.copy(close = 71000.0)
        assertEquals("2024-01-01", copied.date)
        assertEquals(71000.0, copied.close, 0.001)
    }

    @Test
    fun profileData_equality() {
        val p1 = ProfileData(explanation = "반도체 제조업체")
        val p2 = ProfileData(explanation = "반도체 제조업체")
        assertEquals(p1, p2)
    }

    @Test
    fun profileData_copy() {
        val original = ProfileData(explanation = "반도체 제조업체")
        val copied = original.copy(explanation = "Updated")
        assertEquals("Updated", copied.explanation)
    }

    @Test
    fun profileData_nullExplanation() {
        val profile = ProfileData(explanation = null)
        assertNull(profile.explanation)
    }

    @Test
    fun currentData_destructuring() {
        val current = CurrentData(
            price = 72000,
            change = -100,
            changeRate = -0.14,
            marketCap = 1000000,
            date = "2024-01-01"
        )
        val (price, change, changeRate, marketCap, date) = current
        assertEquals(72000L, price)
        assertEquals(-100L, change)
        assertEquals(-0.14, changeRate ?: 0.0, 0.001)
        assertEquals(1000000L, marketCap)
        assertEquals("2024-01-01", date)
    }

    @Test
    fun valuationData_nullFields() {
        val valuation = ValuationData()
        assertNull(valuation.peTtm)
        assertNull(valuation.priceToBook)
        assertNull(valuation.bps)
    }

    @Test
    fun financialsData_nullFields() {
        val financials = FinancialsData()
        assertNull(financials.eps)
        assertNull(financials.dps)
        assertNull(financials.roe)
    }

    @Test
    fun stockDetailDto_industryField() {
        val dto = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            industry = "반도체"
        )
        assertEquals("반도체", dto.industry)
    }
}