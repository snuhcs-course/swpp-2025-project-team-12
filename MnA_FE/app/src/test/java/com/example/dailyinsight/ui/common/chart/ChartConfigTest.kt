package com.example.dailyinsight.ui.common.chart

import org.junit.Assert.*
import org.junit.Test

class ChartConfigTest {

    @Test
    fun create_withRequiredField() {
        val config = ChartConfig(lineColor = 0xFF0000)
        assertEquals(0xFF0000, config.lineColor)
    }

    @Test
    fun create_withDefaultValues() {
        val config = ChartConfig(lineColor = 0xFF0000)
        assertNull(config.fillDrawable)
        assertEquals(2f, config.lineWidth)
        assertTrue(config.enableTouch)
        assertTrue(config.enableAxes)
        assertNull(config.xAxisFormatter)
        assertNull(config.yAxisFormatter)
        assertFalse(config.showValues)
        assertNull(config.valueFormatter)
        assertNull(config.viewPortOffsets)
    }

    @Test
    fun create_withCustomLineWidth() {
        val config = ChartConfig(lineColor = 0xFF0000, lineWidth = 3.5f)
        assertEquals(3.5f, config.lineWidth)
    }

    @Test
    fun create_withTouchDisabled() {
        val config = ChartConfig(lineColor = 0xFF0000, enableTouch = false)
        assertFalse(config.enableTouch)
    }

    @Test
    fun create_withAxesDisabled() {
        val config = ChartConfig(lineColor = 0xFF0000, enableAxes = false)
        assertFalse(config.enableAxes)
    }

    @Test
    fun create_withShowValues() {
        val config = ChartConfig(lineColor = 0xFF0000, showValues = true)
        assertTrue(config.showValues)
    }

    @Test
    fun create_withViewPortOffsets() {
        val offsets = floatArrayOf(10f, 20f, 30f, 40f)
        val config = ChartConfig(lineColor = 0xFF0000, viewPortOffsets = offsets)
        assertNotNull(config.viewPortOffsets)
        assertEquals(10f, config.viewPortOffsets!![0])
        assertEquals(20f, config.viewPortOffsets!![1])
        assertEquals(30f, config.viewPortOffsets!![2])
        assertEquals(40f, config.viewPortOffsets!![3])
    }

    @Test
    fun equality_sameValues() {
        val config1 = ChartConfig(lineColor = 0xFF0000, lineWidth = 2f, enableTouch = true)
        val config2 = ChartConfig(lineColor = 0xFF0000, lineWidth = 2f, enableTouch = true)
        assertEquals(config1, config2)
    }

    @Test
    fun inequality_differentLineColor() {
        val config1 = ChartConfig(lineColor = 0xFF0000)
        val config2 = ChartConfig(lineColor = 0x00FF00)
        assertNotEquals(config1, config2)
    }

    @Test
    fun inequality_differentLineWidth() {
        val config1 = ChartConfig(lineColor = 0xFF0000, lineWidth = 2f)
        val config2 = ChartConfig(lineColor = 0xFF0000, lineWidth = 3f)
        assertNotEquals(config1, config2)
    }

    @Test
    fun inequality_differentEnableTouch() {
        val config1 = ChartConfig(lineColor = 0xFF0000, enableTouch = true)
        val config2 = ChartConfig(lineColor = 0xFF0000, enableTouch = false)
        assertNotEquals(config1, config2)
    }

    @Test
    fun hashCode_consistency() {
        val config1 = ChartConfig(lineColor = 0xFF0000, lineWidth = 2f)
        val config2 = ChartConfig(lineColor = 0xFF0000, lineWidth = 2f)
        assertEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun copy_modifiesLineColor() {
        val original = ChartConfig(lineColor = 0xFF0000)
        val copied = original.copy(lineColor = 0x00FF00)
        assertEquals(0x00FF00, copied.lineColor)
    }

    @Test
    fun copy_modifiesLineWidth() {
        val original = ChartConfig(lineColor = 0xFF0000, lineWidth = 2f)
        val copied = original.copy(lineWidth = 4f)
        assertEquals(4f, copied.lineWidth)
    }

    @Test
    fun copy_preservesOtherFields() {
        val original = ChartConfig(
            lineColor = 0xFF0000,
            lineWidth = 2.5f,
            enableTouch = false,
            enableAxes = false,
            showValues = true
        )
        val copied = original.copy(lineColor = 0x00FF00)
        assertEquals(2.5f, copied.lineWidth)
        assertFalse(copied.enableTouch)
        assertFalse(copied.enableAxes)
        assertTrue(copied.showValues)
    }

    @Test
    fun toString_containsLineColor() {
        val config = ChartConfig(lineColor = 16711680)  // 0xFF0000
        val str = config.toString()
        assertTrue(str.contains("16711680"))
    }

    @Test
    fun destructuring() {
        val config = ChartConfig(
            lineColor = 0xFF0000,
            fillDrawable = null,
            lineWidth = 2f,
            enableTouch = true,
            enableAxes = false,
            xAxisFormatter = null,
            yAxisFormatter = null,
            showValues = true,
            valueFormatter = null,
            viewPortOffsets = null
        )
        val (lineColor, fillDrawable, lineWidth, enableTouch, enableAxes,
             xAxisFormatter, yAxisFormatter, showValues, valueFormatter, viewPortOffsets) = config
        assertEquals(0xFF0000, lineColor)
        assertNull(fillDrawable)
        assertEquals(2f, lineWidth)
        assertTrue(enableTouch)
        assertFalse(enableAxes)
        assertNull(xAxisFormatter)
        assertNull(yAxisFormatter)
        assertTrue(showValues)
        assertNull(valueFormatter)
        assertNull(viewPortOffsets)
    }
}
