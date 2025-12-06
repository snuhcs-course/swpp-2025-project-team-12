package com.example.dailyinsight.ui.common.chart

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for data classes in ChartViewController.kt
 * These can be unit tested without Android dependencies.
 */
class ChartViewControllerDataTest {

    // ===== ChartRange Tests =====

    @Test
    fun chartRange_W1_has20DataPoints() {
        assertEquals(20, ChartRange.W1.dataPointCount)
    }

    @Test
    fun chartRange_M3_has60DataPoints() {
        assertEquals(60, ChartRange.M3.dataPointCount)
    }

    @Test
    fun chartRange_M6_has120DataPoints() {
        assertEquals(120, ChartRange.M6.dataPointCount)
    }

    @Test
    fun chartRange_M9_has180DataPoints() {
        assertEquals(180, ChartRange.M9.dataPointCount)
    }

    @Test
    fun chartRange_Y1_has240DataPoints() {
        assertEquals(240, ChartRange.Y1.dataPointCount)
    }

    @Test
    fun chartRange_values_returnsAllRanges() {
        val values = ChartRange.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(ChartRange.W1))
        assertTrue(values.contains(ChartRange.M3))
        assertTrue(values.contains(ChartRange.M6))
        assertTrue(values.contains(ChartRange.M9))
        assertTrue(values.contains(ChartRange.Y1))
    }

    @Test
    fun chartRange_valueOf_W1() {
        assertEquals(ChartRange.W1, ChartRange.valueOf("W1"))
    }

    @Test
    fun chartRange_valueOf_M3() {
        assertEquals(ChartRange.M3, ChartRange.valueOf("M3"))
    }

    @Test
    fun chartRange_valueOf_M6() {
        assertEquals(ChartRange.M6, ChartRange.valueOf("M6"))
    }

    @Test
    fun chartRange_valueOf_M9() {
        assertEquals(ChartRange.M9, ChartRange.valueOf("M9"))
    }

    @Test
    fun chartRange_valueOf_Y1() {
        assertEquals(ChartRange.Y1, ChartRange.valueOf("Y1"))
    }

    @Test
    fun chartRange_name() {
        assertEquals("W1", ChartRange.W1.name)
        assertEquals("M3", ChartRange.M3.name)
        assertEquals("M6", ChartRange.M6.name)
        assertEquals("M9", ChartRange.M9.name)
        assertEquals("Y1", ChartRange.Y1.name)
    }

    @Test
    fun chartRange_ordinal() {
        assertEquals(0, ChartRange.W1.ordinal)
        assertEquals(1, ChartRange.M3.ordinal)
        assertEquals(2, ChartRange.M6.ordinal)
        assertEquals(3, ChartRange.M9.ordinal)
        assertEquals(4, ChartRange.Y1.ordinal)
    }

    @Test
    fun chartRange_increasingDataPoints() {
        val ranges = ChartRange.values()
        for (i in 0 until ranges.size - 1) {
            assertTrue(ranges[i].dataPointCount < ranges[i + 1].dataPointCount)
        }
    }

    // ===== ChartDataPoint Tests =====

    @Test
    fun chartDataPoint_create() {
        val point = ChartDataPoint(1234567890L, 2500.5f)
        assertEquals(1234567890L, point.timestamp)
        assertEquals(2500.5f, point.value, 0.001f)
    }

    @Test
    fun chartDataPoint_equality() {
        val p1 = ChartDataPoint(1000L, 100f)
        val p2 = ChartDataPoint(1000L, 100f)
        assertEquals(p1, p2)
    }

    @Test
    fun chartDataPoint_inequality_timestamp() {
        val p1 = ChartDataPoint(1000L, 100f)
        val p2 = ChartDataPoint(2000L, 100f)
        assertNotEquals(p1, p2)
    }

    @Test
    fun chartDataPoint_inequality_value() {
        val p1 = ChartDataPoint(1000L, 100f)
        val p2 = ChartDataPoint(1000L, 200f)
        assertNotEquals(p1, p2)
    }

    @Test
    fun chartDataPoint_hashCode() {
        val p1 = ChartDataPoint(1000L, 100f)
        val p2 = ChartDataPoint(1000L, 100f)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun chartDataPoint_copy() {
        val original = ChartDataPoint(1000L, 100f)
        val copied = original.copy(value = 200f)
        assertEquals(1000L, copied.timestamp)
        assertEquals(200f, copied.value, 0.001f)
    }

    @Test
    fun chartDataPoint_copy_timestamp() {
        val original = ChartDataPoint(1000L, 100f)
        val copied = original.copy(timestamp = 2000L)
        assertEquals(2000L, copied.timestamp)
        assertEquals(100f, copied.value, 0.001f)
    }

    @Test
    fun chartDataPoint_toString() {
        val point = ChartDataPoint(1000L, 100f)
        assertTrue(point.toString().contains("ChartDataPoint"))
    }

    @Test
    fun chartDataPoint_destructuring() {
        val point = ChartDataPoint(1234L, 56.78f)
        val (timestamp, value) = point
        assertEquals(1234L, timestamp)
        assertEquals(56.78f, value, 0.001f)
    }

    @Test
    fun chartDataPoint_zeroValues() {
        val point = ChartDataPoint(0L, 0f)
        assertEquals(0L, point.timestamp)
        assertEquals(0f, point.value, 0.001f)
    }

    @Test
    fun chartDataPoint_negativeValue() {
        val point = ChartDataPoint(1000L, -50.5f)
        assertEquals(-50.5f, point.value, 0.001f)
    }

    @Test
    fun chartDataPoint_maxValues() {
        val point = ChartDataPoint(Long.MAX_VALUE, Float.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, point.timestamp)
        assertEquals(Float.MAX_VALUE, point.value, 0.001f)
    }

    @Test
    fun chartDataPoint_sorting() {
        val points = listOf(
            ChartDataPoint(3000L, 300f),
            ChartDataPoint(1000L, 100f),
            ChartDataPoint(2000L, 200f)
        )
        val sorted = points.sortedBy { it.timestamp }
        assertEquals(1000L, sorted[0].timestamp)
        assertEquals(2000L, sorted[1].timestamp)
        assertEquals(3000L, sorted[2].timestamp)
    }

    // ===== ChartViewConfig Tests =====

    @Test
    fun chartViewConfig_create_withRequiredFields() {
        val config = ChartViewConfig(
            lineColorRes = 0xFF0000,
            fillDrawableRes = 0x123456
        )
        assertEquals(0xFF0000, config.lineColorRes)
        assertEquals(0x123456, config.fillDrawableRes)
    }

    @Test
    fun chartViewConfig_defaultValues() {
        val config = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2)
        assertTrue(config.enableTouch)
        assertTrue(config.enableAxes)
        assertNull(config.viewPortOffsets)
        assertNull(config.xAxisFormatter)
        assertNull(config.yAxisFormatter)
        assertEquals(ChartRange.M6, config.defaultRange)
    }

    @Test
    fun chartViewConfig_customEnableTouch() {
        val config = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, enableTouch = false)
        assertFalse(config.enableTouch)
    }

    @Test
    fun chartViewConfig_customEnableAxes() {
        val config = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, enableAxes = false)
        assertFalse(config.enableAxes)
    }

    @Test
    fun chartViewConfig_customDefaultRange() {
        val config = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, defaultRange = ChartRange.Y1)
        assertEquals(ChartRange.Y1, config.defaultRange)
    }

    @Test
    fun chartViewConfig_viewPortOffsets() {
        val offsets = floatArrayOf(10f, 20f, 30f, 40f)
        val config = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, viewPortOffsets = offsets)
        assertNotNull(config.viewPortOffsets)
        assertEquals(10f, config.viewPortOffsets!![0], 0.001f)
        assertEquals(20f, config.viewPortOffsets!![1], 0.001f)
        assertEquals(30f, config.viewPortOffsets!![2], 0.001f)
        assertEquals(40f, config.viewPortOffsets!![3], 0.001f)
    }

    @Test
    fun chartViewConfig_equality() {
        val config1 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, defaultRange = ChartRange.M3)
        val config2 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, defaultRange = ChartRange.M3)
        assertEquals(config1, config2)
    }

    @Test
    fun chartViewConfig_inequality_lineColor() {
        val config1 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2)
        val config2 = ChartViewConfig(lineColorRes = 99, fillDrawableRes = 2)
        assertNotEquals(config1, config2)
    }

    @Test
    fun chartViewConfig_inequality_fillDrawable() {
        val config1 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2)
        val config2 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 99)
        assertNotEquals(config1, config2)
    }

    @Test
    fun chartViewConfig_copy() {
        val original = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2, defaultRange = ChartRange.W1)
        val copied = original.copy(defaultRange = ChartRange.Y1)
        assertEquals(1, copied.lineColorRes)
        assertEquals(2, copied.fillDrawableRes)
        assertEquals(ChartRange.Y1, copied.defaultRange)
    }

    @Test
    fun chartViewConfig_toString() {
        val config = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2)
        assertTrue(config.toString().contains("ChartViewConfig"))
    }

    @Test
    fun chartViewConfig_hashCode() {
        val config1 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2)
        val config2 = ChartViewConfig(lineColorRes = 1, fillDrawableRes = 2)
        assertEquals(config1.hashCode(), config2.hashCode())
    }
}
