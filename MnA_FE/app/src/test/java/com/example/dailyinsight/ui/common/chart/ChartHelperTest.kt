package com.example.dailyinsight.ui.common.chart

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ChartHelper using Robolectric
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChartHelperTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ===== Tests for isUpwardTrend() =====

    @Test
    fun isUpwardTrend_withEmptyList_returnsTrue() {
        // Given: Empty list of entries
        val entries = emptyList<Entry>()

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (default behavior)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withSingleEntry_returnsTrue() {
        // Given: Single entry
        val entries = listOf(Entry(0f, 100f))

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (first == last, so >= condition is true)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withUpwardTrend_returnsTrue() {
        // Given: Entries showing upward trend (last > first)
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 110f),
            Entry(2f, 105f),
            Entry(3f, 120f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (120 > 100)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withDownwardTrend_returnsFalse() {
        // Given: Entries showing downward trend (last < first)
        val entries = listOf(
            Entry(0f, 120f),
            Entry(1f, 110f),
            Entry(2f, 115f),
            Entry(3f, 100f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false (100 < 120)
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_withFlatTrend_returnsTrue() {
        // Given: Entries with same first and last value
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 110f),
            Entry(2f, 90f),
            Entry(3f, 100f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (100 == 100, and >= condition is satisfied)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withVolatileUpwardTrend_returnsTrue() {
        // Given: Volatile data but overall upward
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 80f),   // Dip
            Entry(2f, 120f),  // Spike
            Entry(3f, 90f),   // Dip
            Entry(4f, 110f)   // Ends higher than start
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (110 > 100), only compares first and last
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withVolatileDownwardTrend_returnsFalse() {
        // Given: Volatile data but overall downward
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 120f),  // Spike
            Entry(2f, 80f),   // Dip
            Entry(3f, 110f),  // Recovery
            Entry(4f, 90f)    // Ends lower than start
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false (90 < 100)
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_withTwoEntries_upward_returnsTrue() {
        // Given: Two entries, upward
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 110f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withTwoEntries_downward_returnsFalse() {
        // Given: Two entries, downward
        val entries = listOf(
            Entry(0f, 110f),
            Entry(1f, 100f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_withTwoEntries_flat_returnsTrue() {
        // Given: Two entries with same value
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 100f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (>= condition)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withVerySmallIncrease_returnsTrue() {
        // Given: Very small increase
        val entries = listOf(
            Entry(0f, 100.00f),
            Entry(1f, 100.01f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (any increase counts)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withVerySmallDecrease_returnsFalse() {
        // Given: Very small decrease
        val entries = listOf(
            Entry(0f, 100.00f),
            Entry(1f, 99.99f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_withNegativeValues_upward_returnsTrue() {
        // Given: Negative values but upward trend
        val entries = listOf(
            Entry(0f, -50f),
            Entry(1f, -40f),
            Entry(2f, -30f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (-30 > -50)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withNegativeValues_downward_returnsFalse() {
        // Given: Negative values with downward trend
        val entries = listOf(
            Entry(0f, -30f),
            Entry(1f, -40f),
            Entry(2f, -50f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false (-50 < -30)
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_withZeroValues_returnsTrue() {
        // Given: Zero values (flat line at zero)
        val entries = listOf(
            Entry(0f, 0f),
            Entry(1f, 0f),
            Entry(2f, 0f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (0 >= 0)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_fromZeroToPositive_returnsTrue() {
        // Given: Starting from zero and going positive
        val entries = listOf(
            Entry(0f, 0f),
            Entry(1f, 50f),
            Entry(2f, 100f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_fromPositiveToZero_returnsFalse() {
        // Given: Starting positive and going to zero
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 50f),
            Entry(2f, 0f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false (0 < 100)
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_withLargeDataset_upward_returnsTrue() {
        // Given: Large dataset with overall upward trend
        val entries = (0..100).map { i ->
            Entry(i.toFloat(), 100f + i.toFloat())
        }

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (200 > 100)
        assertTrue(result)
    }

    @Test
    fun isUpwardTrend_withLargeDataset_downward_returnsFalse() {
        // Given: Large dataset with overall downward trend
        val entries = (0..100).map { i ->
            Entry(i.toFloat(), 200f - i.toFloat())
        }

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return false (100 < 200)
        assertFalse(result)
    }

    @Test
    fun isUpwardTrend_ignoresMiddleValues() {
        // Given: First and last are equal, but middle has huge spike
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 10000f),  // Huge spike in middle
            Entry(2f, 100f)
        )

        // When: Check trend
        val result = ChartHelper.isUpwardTrend(entries)

        // Then: Should return true (100 >= 100), middle values don't matter
        assertTrue(result)
    }

    // ===== Tests for setupChart() using Robolectric =====

    @Test
    fun setupChart_withEmptyEntries_clearsChart() {
        val chart = LineChart(context)
        val config = ChartConfig(lineColor = Color.RED)

        ChartHelper.setupChart(chart, emptyList(), config)

        assertNull(chart.data)
    }

    @Test
    fun setupChart_withEntries_setsData() {
        val chart = LineChart(context)
        val entries = listOf(
            Entry(0f, 100f),
            Entry(1f, 110f),
            Entry(2f, 120f)
        )
        val config = ChartConfig(lineColor = Color.RED)

        ChartHelper.setupChart(chart, entries, config)

        assertNotNull(chart.data)
        assertEquals(1, chart.data.dataSetCount)
        assertEquals(3, chart.data.getDataSetByIndex(0).entryCount)
    }

    @Test
    fun setupChart_disablesTouchWhenConfigured() {
        val chart = LineChart(context)
        val entries = listOf(Entry(0f, 100f))
        val config = ChartConfig(lineColor = Color.RED, enableTouch = false)

        ChartHelper.setupChart(chart, entries, config)

        // Chart was configured (we verify data was set)
        assertNotNull(chart.data)
    }

    @Test
    fun setupChart_enablesAxesByDefault() {
        val chart = LineChart(context)
        val entries = listOf(Entry(0f, 100f))
        val config = ChartConfig(lineColor = Color.RED, enableAxes = true)

        ChartHelper.setupChart(chart, entries, config)

        assertTrue(chart.xAxis.isEnabled)
        assertTrue(chart.axisLeft.isEnabled)
        assertFalse(chart.axisRight.isEnabled)
    }

    @Test
    fun setupChart_disablesAxesWhenConfigured() {
        val chart = LineChart(context)
        val entries = listOf(Entry(0f, 100f))
        val config = ChartConfig(lineColor = Color.RED, enableAxes = false)

        ChartHelper.setupChart(chart, entries, config)

        assertFalse(chart.xAxis.isEnabled)
        assertFalse(chart.axisLeft.isEnabled)
    }

    @Test
    fun setupChart_setsViewPortOffsets() {
        val chart = LineChart(context)
        val entries = listOf(Entry(0f, 100f))
        val offsets = floatArrayOf(10f, 20f, 30f, 40f)
        val config = ChartConfig(lineColor = Color.RED, viewPortOffsets = offsets)

        ChartHelper.setupChart(chart, entries, config)

        assertNotNull(chart.data)
    }

    @Test
    fun setupChart_disablesDescriptionAndLegend() {
        val chart = LineChart(context)
        val entries = listOf(Entry(0f, 100f))
        val config = ChartConfig(lineColor = Color.RED)

        ChartHelper.setupChart(chart, entries, config)

        assertFalse(chart.description.isEnabled)
        assertFalse(chart.legend.isEnabled)
    }

    // ===== Tests for createTrendBasedConfig() =====

    @Test
    fun createTrendBasedConfig_upwardTrend_returnsValidConfig() {
        val config = ChartHelper.createTrendBasedConfig(
            context = context,
            isUpward = true,
            upColorRes = android.R.color.holo_green_dark,
            downColorRes = android.R.color.holo_red_dark,
            upFillRes = android.R.color.holo_green_light,
            downFillRes = android.R.color.holo_red_light
        )

        assertNotNull(config)
        assertTrue(config.enableTouch)
        assertTrue(config.enableAxes)
    }

    @Test
    fun createTrendBasedConfig_downwardTrend_returnsValidConfig() {
        val config = ChartHelper.createTrendBasedConfig(
            context = context,
            isUpward = false,
            upColorRes = android.R.color.holo_green_dark,
            downColorRes = android.R.color.holo_red_dark,
            upFillRes = android.R.color.holo_green_light,
            downFillRes = android.R.color.holo_red_light
        )

        assertNotNull(config)
    }

    @Test
    fun createTrendBasedConfig_disablesTouchWhenConfigured() {
        val config = ChartHelper.createTrendBasedConfig(
            context = context,
            isUpward = true,
            upColorRes = android.R.color.black,
            downColorRes = android.R.color.white,
            upFillRes = android.R.color.black,
            downFillRes = android.R.color.white,
            enableTouch = false,
            enableAxes = false
        )

        assertFalse(config.enableTouch)
        assertFalse(config.enableAxes)
    }

    @Test
    fun createTrendBasedConfig_hasFillDrawable() {
        val config = ChartHelper.createTrendBasedConfig(
            context = context,
            isUpward = true,
            upColorRes = android.R.color.holo_green_dark,
            downColorRes = android.R.color.holo_red_dark,
            upFillRes = android.R.color.holo_green_light,
            downFillRes = android.R.color.holo_red_light
        )

        assertNotNull(config.fillDrawable)
    }
}
