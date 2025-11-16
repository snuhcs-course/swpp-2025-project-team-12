package com.example.dailyinsight.data.dto

import org.junit.Assert.*
import org.junit.Test

class StockDetailDtoTest {

    // ===== StockDetailDto (20개) =====
    
    @Test
    fun stockDetailDto_allFieldsNull() {
        val dto = StockDetailDto()
        assertNull(dto.ticker)
        assertNull(dto.name)
        assertNull(dto.price)
        assertNull(dto.marketCap)
    }

    @Test
    fun stockDetailDto_withAllFields() {
        val dto = StockDetailDto(
            ticker = "005930",
            name = "삼성전자",
            price = 70000L,
            change = -100L,
            changeRate = -0.14,
            marketCap = "400조",
            sharesOutstanding = "5억주"
        )
        assertEquals("005930", dto.ticker)
        assertEquals("삼성전자", dto.name)
        assertEquals(70000L, dto.price)
        assertEquals(-100L, dto.change)
        assertEquals(-0.14, dto.changeRate ?: 0.0, 0.001)
        assertEquals("400조", dto.marketCap)
        assertEquals("5억주", dto.sharesOutstanding)
    }

    @Test
    fun stockDetailDto_equality() {
        val d1 = StockDetailDto(ticker = "005930", name = "삼성전자")
        val d2 = StockDetailDto(ticker = "005930", name = "삼성전자")
        assertEquals(d1, d2)
    }

    @Test
    fun stockDetailDto_copy() {
        val original = StockDetailDto(ticker = "005930", price = 70000L)
        val copied = original.copy(price = 80000L)
        assertEquals(80000L, copied.price)
        assertEquals("005930", copied.ticker)
    }

    @Test
    fun stockDetailDto_toString() {
        val dto = StockDetailDto(ticker = "005930")
        assertNotNull(dto.toString())
        assertTrue(dto.toString().contains("StockDetailDto"))
    }

    @Test
    fun stockDetailDto_hashCode() {
        val d1 = StockDetailDto(ticker = "005930")
        val d2 = StockDetailDto(ticker = "005930")
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun stockDetailDto_withValuation() {
        val valuation = Valuation(peAnnual = "15.2", peTtm = "14.8")
        val dto = StockDetailDto(ticker = "005930", valuation = valuation)
        assertEquals("15.2", dto.valuation.peAnnual)
        assertEquals("14.8", dto.valuation.peTtm)
    }

    @Test
    fun stockDetailDto_withSolvency() {
        val solvency = Solvency(currentRatio = "2.1", quickRatio = "1.8")
        val dto = StockDetailDto(ticker = "005930", solvency = solvency)
        assertEquals("2.1", dto.solvency.currentRatio)
        assertEquals("1.8", dto.solvency.quickRatio)
    }

    @Test
    fun stockDetailDto_withDividend() {
        val dividend = Dividend(payoutRatio = "30%", yield = "2.5%")
        val dto = StockDetailDto(ticker = "005930", dividend = dividend)
        assertEquals("30%", dto.dividend.payoutRatio)
        assertEquals("2.5%", dto.dividend.yield)
    }

    @Test
    fun stockDetailDto_withPriceFinancialInfo() {
        val priceInfo = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        val dto = StockDetailDto(ticker = "005930", priceFinancialInfo = priceInfo)
        assertNotNull(dto.priceFinancialInfo)
        assertEquals(70000.0, dto.priceFinancialInfo?.price?.get("2024-01-01") ?: 0.0, 0.001)
    }

    @Test
    fun stockDetailDto_withChart() {
        val chart = listOf(ChartPoint(1234567890L, 70000.0))
        val dto = StockDetailDto(ticker = "005930", chart = chart)
        assertEquals(1, dto.chart?.size)
        assertEquals(1234567890L, dto.chart?.first()?.t)
    }

    @Test
    fun stockDetailDto_withNetIncome() {
        val netIncome = NetIncome(
            annual = listOf(PeriodValue("2024", "10조"))
        )
        val dto = StockDetailDto(ticker = "005930", netIncome = netIncome)
        assertNotNull(dto.netIncome)
        assertEquals(1, dto.netIncome?.annual?.size)
    }

    @Test
    fun stockDetailDto_negativePrice() {
        val dto = StockDetailDto(price = -1000L, change = -100L)
        assertEquals(-1000L, dto.price)
        assertEquals(-100L, dto.change)
    }

    @Test
    fun stockDetailDto_zeroValues() {
        val dto = StockDetailDto(price = 0L, change = 0L, changeRate = 0.0)
        assertEquals(0L, dto.price)
        assertEquals(0L, dto.change)
        assertEquals(0.0, dto.changeRate ?: 0.0, 0.001)
    }

    @Test
    fun stockDetailDto_largeNumbers() {
        val dto = StockDetailDto(price = Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, dto.price)
    }

    @Test
    fun stockDetailDto_emptyStrings() {
        val dto = StockDetailDto(ticker = "", name = "", marketCap = "")
        assertEquals("", dto.ticker)
        assertEquals("", dto.name)
        assertEquals("", dto.marketCap)
    }

    @Test
    fun stockDetailDto_specialCharacters() {
        val dto = StockDetailDto(name = "삼성전자(주)", marketCap = "400조원")
        assertEquals("삼성전자(주)", dto.name)
        assertEquals("400조원", dto.marketCap)
    }

    @Test
    fun stockDetailDto_defaultValuation() {
        val dto = StockDetailDto()
        assertNotNull(dto.valuation)
    }

    @Test
    fun stockDetailDto_defaultSolvency() {
        val dto = StockDetailDto()
        assertNotNull(dto.solvency)
    }

    @Test
    fun stockDetailDto_defaultDividend() {
        val dto = StockDetailDto()
        assertNotNull(dto.dividend)
    }

    // ===== PriceFinancialInfoDto (10개) =====

    @Test
    fun priceFinancialInfo_nullPrice() {
        val dto = PriceFinancialInfoDto(price = null)
        assertNull(dto.price)
    }

    @Test
    fun priceFinancialInfo_emptyMap() {
        val dto = PriceFinancialInfoDto(price = emptyMap())
        assertTrue(dto.price?.isEmpty() == true)
    }

    @Test
    fun priceFinancialInfo_singleEntry() {
        val dto = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        assertEquals(1, dto.price?.size)
        assertEquals(70000.0, dto.price?.get("2024-01-01") ?: 0.0, 0.001)
    }

    @Test
    fun priceFinancialInfo_multipleEntries() {
        val prices = mapOf(
            "2024-01-01" to 70000.0,
            "2024-01-02" to 71000.0,
            "2024-01-03" to 72000.0
        )
        val dto = PriceFinancialInfoDto(price = prices)
        assertEquals(3, dto.price?.size)
    }

    @Test
    fun priceFinancialInfo_equality() {
        val d1 = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        val d2 = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        assertEquals(d1, d2)
    }

    @Test
    fun priceFinancialInfo_copy() {
        val original = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        val copied = original.copy()
        assertEquals(original, copied)
    }

    @Test
    fun priceFinancialInfo_toString() {
        val dto = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        assertNotNull(dto.toString())
    }

    @Test
    fun priceFinancialInfo_hashCode() {
        val d1 = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        val d2 = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.0))
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun priceFinancialInfo_largeDataset() {
        val prices = (1..1000).associate { "2024-$it" to (70000.0 + it) }
        val dto = PriceFinancialInfoDto(price = prices)
        assertEquals(1000, dto.price?.size)
    }

    @Test
    fun priceFinancialInfo_decimalPrecision() {
        val dto = PriceFinancialInfoDto(price = mapOf("2024-01-01" to 70000.123456))
        assertEquals(70000.123456, dto.price?.get("2024-01-01") ?: 0.0, 0.000001)
    }

    // ===== Valuation (15개) =====

    @Test
    fun valuation_allFieldsNull() {
        val v = Valuation()
        assertNull(v.peAnnual)
        assertNull(v.peTtm)
        assertNull(v.forwardPe)
        assertNull(v.psTtm)
        assertNull(v.priceToBook)
        assertNull(v.pcfTtm)
        assertNull(v.pfcfTtm)
    }

    @Test
    fun valuation_allFieldsFilled() {
        val v = Valuation(
            peAnnual = "15.2",
            peTtm = "14.8",
            forwardPe = "13.5",
            psTtm = "1.2",
            priceToBook = "1.5",
            pcfTtm = "10.0",
            pfcfTtm = "12.0"
        )
        assertEquals("15.2", v.peAnnual)
        assertEquals("14.8", v.peTtm)
        assertEquals("13.5", v.forwardPe)
        assertEquals("1.2", v.psTtm)
        assertEquals("1.5", v.priceToBook)
        assertEquals("10.0", v.pcfTtm)
        assertEquals("12.0", v.pfcfTtm)
    }

    @Test
    fun valuation_equality() {
        val v1 = Valuation(peAnnual = "15.2", peTtm = "14.8")
        val v2 = Valuation(peAnnual = "15.2", peTtm = "14.8")
        assertEquals(v1, v2)
    }

    @Test
    fun valuation_copy() {
        val original = Valuation(peAnnual = "15.2")
        val copied = original.copy(peAnnual = "16.0")
        assertEquals("16.0", copied.peAnnual)
    }

    @Test
    fun valuation_toString() {
        val v = Valuation(peAnnual = "15.2")
        assertNotNull(v.toString())
        assertTrue(v.toString().contains("Valuation"))
    }

    @Test
    fun valuation_hashCode() {
        val v1 = Valuation(peAnnual = "15.2")
        val v2 = Valuation(peAnnual = "15.2")
        assertEquals(v1.hashCode(), v2.hashCode())
    }

    @Test
    fun valuation_negativeValues() {
        val v = Valuation(peAnnual = "-5.0", peTtm = "-10.0")
        assertEquals("-5.0", v.peAnnual)
        assertEquals("-10.0", v.peTtm)
    }

    @Test
    fun valuation_zeroValues() {
        val v = Valuation(peAnnual = "0.0", psTtm = "0.0")
        assertEquals("0.0", v.peAnnual)
        assertEquals("0.0", v.psTtm)
    }

    @Test
    fun valuation_emptyStrings() {
        val v = Valuation(peAnnual = "", peTtm = "")
        assertEquals("", v.peAnnual)
        assertEquals("", v.peTtm)
    }

    @Test
    fun valuation_largeNumbers() {
        val v = Valuation(peAnnual = "999999.99")
        assertEquals("999999.99", v.peAnnual)
    }

    @Test
    fun valuation_specialCharacters() {
        val v = Valuation(peAnnual = "N/A", peTtm = "∞")
        assertEquals("N/A", v.peAnnual)
        assertEquals("∞", v.peTtm)
    }

    @Test
    fun valuation_onlyPeAnnual() {
        val v = Valuation(peAnnual = "15.2")
        assertEquals("15.2", v.peAnnual)
        assertNull(v.peTtm)
    }

    @Test
    fun valuation_onlyPeTtm() {
        val v = Valuation(peTtm = "14.8")
        assertNull(v.peAnnual)
        assertEquals("14.8", v.peTtm)
    }

    @Test
    fun valuation_priceToBookAlias() {
        val v = Valuation(priceToBook = "1.5")
        assertEquals("1.5", v.priceToBook)
    }

    @Test
    fun valuation_decimalPrecision() {
        val v = Valuation(peAnnual = "15.123456789")
        assertEquals("15.123456789", v.peAnnual)
    }

    // ===== Solvency (10개) =====

    @Test
    fun solvency_allFieldsNull() {
        val s = Solvency()
        assertNull(s.currentRatio)
        assertNull(s.quickRatio)
        assertNull(s.debtToEquity)
    }

    @Test
    fun solvency_allFieldsFilled() {
        val s = Solvency(
            currentRatio = "2.1",
            quickRatio = "1.8",
            debtToEquity = "0.5"
        )
        assertEquals("2.1", s.currentRatio)
        assertEquals("1.8", s.quickRatio)
        assertEquals("0.5", s.debtToEquity)
    }

    @Test
    fun solvency_equality() {
        val s1 = Solvency(currentRatio = "2.1", quickRatio = "1.8")
        val s2 = Solvency(currentRatio = "2.1", quickRatio = "1.8")
        assertEquals(s1, s2)
    }

    @Test
    fun solvency_copy() {
        val original = Solvency(currentRatio = "2.1")
        val copied = original.copy(currentRatio = "2.5")
        assertEquals("2.5", copied.currentRatio)
    }

    @Test
    fun solvency_toString() {
        val s = Solvency(currentRatio = "2.1")
        assertNotNull(s.toString())
    }

    @Test
    fun solvency_hashCode() {
        val s1 = Solvency(currentRatio = "2.1")
        val s2 = Solvency(currentRatio = "2.1")
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun solvency_highRatios() {
        val s = Solvency(currentRatio = "10.0", quickRatio = "8.0")
        assertEquals("10.0", s.currentRatio)
        assertEquals("8.0", s.quickRatio)
    }

    @Test
    fun solvency_lowRatios() {
        val s = Solvency(currentRatio = "0.5", quickRatio = "0.3")
        assertEquals("0.5", s.currentRatio)
        assertEquals("0.3", s.quickRatio)
    }

    @Test
    fun solvency_debtToEquityHigh() {
        val s = Solvency(debtToEquity = "5.0")
        assertEquals("5.0", s.debtToEquity)
    }

    @Test
    fun solvency_emptyStrings() {
        val s = Solvency(currentRatio = "", quickRatio = "")
        assertEquals("", s.currentRatio)
        assertEquals("", s.quickRatio)
    }

    // ===== Dividend (10개) =====

    @Test
    fun dividend_allFieldsNull() {
        val d = Dividend()
        assertNull(d.payoutRatio)
        assertNull(d.yield)
        assertNull(d.latestExDate)
    }

    @Test
    fun dividend_allFieldsFilled() {
        val d = Dividend(
            payoutRatio = "30%",
            yield = "2.5%",
            latestExDate = "2024-01-15"
        )
        assertEquals("30%", d.payoutRatio)
        assertEquals("2.5%", d.yield)
        assertEquals("2024-01-15", d.latestExDate)
    }

    @Test
    fun dividend_equality() {
        val d1 = Dividend(payoutRatio = "30%", yield = "2.5%")
        val d2 = Dividend(payoutRatio = "30%", yield = "2.5%")
        assertEquals(d1, d2)
    }

    @Test
    fun dividend_copy() {
        val original = Dividend(payoutRatio = "30%")
        val copied = original.copy(payoutRatio = "40%")
        assertEquals("40%", copied.payoutRatio)
    }

    @Test
    fun dividend_toString() {
        val d = Dividend(payoutRatio = "30%")
        assertNotNull(d.toString())
    }

    @Test
    fun dividend_hashCode() {
        val d1 = Dividend(payoutRatio = "30%")
        val d2 = Dividend(payoutRatio = "30%")
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun dividend_highYield() {
        val d = Dividend(yield = "10.0%")
        assertEquals("10.0%", d.yield)
    }

    @Test
    fun dividend_lowYield() {
        val d = Dividend(yield = "0.5%")
        assertEquals("0.5%", d.yield)
    }

    @Test
    fun dividend_pastExDate() {
        val d = Dividend(latestExDate = "2020-01-01")
        assertEquals("2020-01-01", d.latestExDate)
    }

    @Test
    fun dividend_futureExDate() {
        val d = Dividend(latestExDate = "2030-12-31")
        assertEquals("2030-12-31", d.latestExDate)
    }

    // ===== ChartPoint (10개) =====

    @Test
    fun chartPoint_creates() {
        val cp = ChartPoint(1234567890L, 70000.0)
        assertEquals(1234567890L, cp.t)
        assertEquals(70000.0, cp.v, 0.001)
    }

    @Test
    fun chartPoint_equality() {
        val cp1 = ChartPoint(1234567890L, 70000.0)
        val cp2 = ChartPoint(1234567890L, 70000.0)
        assertEquals(cp1, cp2)
    }

    @Test
    fun chartPoint_copy() {
        val original = ChartPoint(1234567890L, 70000.0)
        val copied = original.copy(v = 80000.0)
        assertEquals(80000.0, copied.v, 0.001)
    }

    @Test
    fun chartPoint_toString() {
        val cp = ChartPoint(1234567890L, 70000.0)
        assertNotNull(cp.toString())
    }

    @Test
    fun chartPoint_hashCode() {
        val cp1 = ChartPoint(1234567890L, 70000.0)
        val cp2 = ChartPoint(1234567890L, 70000.0)
        assertEquals(cp1.hashCode(), cp2.hashCode())
    }

    @Test
    fun chartPoint_negativeTimestamp() {
        val cp = ChartPoint(-1L, 70000.0)
        assertEquals(-1L, cp.t)
    }

    @Test
    fun chartPoint_zeroTimestamp() {
        val cp = ChartPoint(0L, 70000.0)
        assertEquals(0L, cp.t)
    }

    @Test
    fun chartPoint_maxTimestamp() {
        val cp = ChartPoint(Long.MAX_VALUE, 70000.0)
        assertEquals(Long.MAX_VALUE, cp.t)
    }

    @Test
    fun chartPoint_negativeValue() {
        val cp = ChartPoint(1234567890L, -1000.0)
        assertEquals(-1000.0, cp.v, 0.001)
    }

    @Test
    fun chartPoint_decimalPrecision() {
        val cp = ChartPoint(1234567890L, 70000.123456)
        assertEquals(70000.123456, cp.v, 0.000001)
    }

    // ===== NetIncome (5개) =====

    @Test
    fun netIncome_bothNull() {
        val ni = NetIncome(annual = null, quarter = null)
        assertNull(ni.annual)
        assertNull(ni.quarter)
    }

    @Test
    fun netIncome_onlyAnnual() {
        val annual = listOf(PeriodValue("2024", "10조"))
        val ni = NetIncome(annual = annual, quarter = null)
        assertEquals(1, ni.annual?.size)
        assertNull(ni.quarter)
    }

    @Test
    fun netIncome_onlyQuarter() {
        val quarter = listOf(PeriodValue("2024Q1", "2.5조"))
        val ni = NetIncome(annual = null, quarter = quarter)
        assertNull(ni.annual)
        assertEquals(1, ni.quarter?.size)
    }

    @Test
    fun netIncome_bothFilled() {
        val annual = listOf(PeriodValue("2024", "10조"))
        val quarter = listOf(PeriodValue("2024Q1", "2.5조"))
        val ni = NetIncome(annual = annual, quarter = quarter)
        assertEquals(1, ni.annual?.size)
        assertEquals(1, ni.quarter?.size)
    }

    @Test
    fun netIncome_equality() {
        val annual = listOf(PeriodValue("2024", "10조"))
        val ni1 = NetIncome(annual = annual)
        val ni2 = NetIncome(annual = annual)
        assertEquals(ni1, ni2)
    }

    // ===== PeriodValue (10개) =====

    @Test
    fun periodValue_creates() {
        val pv = PeriodValue("2024", "10조")
        assertEquals("2024", pv.period)
        assertEquals("10조", pv.value)
    }

    @Test
    fun periodValue_equality() {
        val pv1 = PeriodValue("2024", "10조")
        val pv2 = PeriodValue("2024", "10조")
        assertEquals(pv1, pv2)
    }

    @Test
    fun periodValue_copy() {
        val original = PeriodValue("2024", "10조")
        val copied = original.copy(value = "20조")
        assertEquals("20조", copied.value)
        assertEquals("2024", copied.period)
    }

    @Test
    fun periodValue_toString() {
        val pv = PeriodValue("2024", "10조")
        assertNotNull(pv.toString())
    }

    @Test
    fun periodValue_hashCode() {
        val pv1 = PeriodValue("2024", "10조")
        val pv2 = PeriodValue("2024", "10조")
        assertEquals(pv1.hashCode(), pv2.hashCode())
    }

    @Test
    fun periodValue_quarterPeriod() {
        val pv = PeriodValue("2024Q1", "2.5조")
        assertEquals("2024Q1", pv.period)
    }

    @Test
    fun periodValue_negativeValue() {
        val pv = PeriodValue("2024", "-5조")
        assertEquals("-5조", pv.value)
    }

    @Test
    fun periodValue_emptyStrings() {
        val pv = PeriodValue("", "")
        assertEquals("", pv.period)
        assertEquals("", pv.value)
    }

    @Test
    fun periodValue_largeValue() {
        val pv = PeriodValue("2024", "999조원")
        assertEquals("999조원", pv.value)
    }

    @Test
    fun periodValue_specialCharacters() {
        val pv = PeriodValue("2024년", "10조 원")
        assertEquals("2024년", pv.period)
        assertEquals("10조 원", pv.value)
    }
}