package com.example.dailyinsight.ui.common.chart

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Configuration for chart styling and behavior
 */
data class ChartConfig(
    val lineColor: Int,
    val fillDrawable: Drawable? = null,
    val lineWidth: Float = 2f,
    val enableTouch: Boolean = true,
    val enableAxes: Boolean = true,
    val xAxisFormatter: ValueFormatter? = null,
    val yAxisFormatter: ValueFormatter? = null,
    val showValues: Boolean = false,
    val valueFormatter: ValueFormatter? = null,
    val viewPortOffsets: FloatArray? = null
)

/**
 * Simplified chart helper for setting up LineChart with common configurations
 */
object ChartHelper {

    /**
     * Setup and render a LineChart with the given entries and configuration
     */
    fun setupChart(
        chart: LineChart,
        entries: List<Entry>,
        config: ChartConfig
    ) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }

        // Create dataset with styling
        val dataSet = LineDataSet(entries, "").apply {
            color = config.lineColor
            lineWidth = config.lineWidth
            setDrawCircles(false)
            setDrawValues(config.showValues)
            mode = LineDataSet.Mode.CUBIC_BEZIER

            // Fill configuration
            if (config.fillDrawable != null) {
                setDrawFilled(true)
                fillDrawable = config.fillDrawable
            }

            // Value formatter for labels
            if (config.valueFormatter != null) {
                valueFormatter = config.valueFormatter
            }
        }

        val lineData = LineData(dataSet)

        // Apply chart configuration
        chart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false

            // Touch interaction
            setTouchEnabled(config.enableTouch)
            isDragEnabled = config.enableTouch
            setScaleEnabled(config.enableTouch)

            // Viewport offsets
            if (config.viewPortOffsets != null) {
                setViewPortOffsets(
                    config.viewPortOffsets[0],
                    config.viewPortOffsets[1],
                    config.viewPortOffsets[2],
                    config.viewPortOffsets[3]
                )
            }

            // Axes configuration
            xAxis.apply {
                isEnabled = config.enableAxes
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                if (config.xAxisFormatter != null) {
                    valueFormatter = config.xAxisFormatter
                }
            }

            axisLeft.apply {
                isEnabled = config.enableAxes
                setDrawGridLines(false)
                if (config.yAxisFormatter != null) {
                    valueFormatter = config.yAxisFormatter
                }
            }

            axisRight.isEnabled = false

            invalidate()
        }
    }

    /**
     * Determine trend direction (upward/downward) from entries
     */
    fun isUpwardTrend(entries: List<Entry>): Boolean {
        if (entries.isEmpty()) return true
        return entries.last().y >= entries.first().y
    }

    /**
     * Create a simple ChartConfig with trend-based coloring
     */
    fun createTrendBasedConfig(
        context: Context,
        isUpward: Boolean,
        upColorRes: Int,
        downColorRes: Int,
        upFillRes: Int,
        downFillRes: Int,
        enableTouch: Boolean = true,
        enableAxes: Boolean = true,
        xAxisFormatter: ValueFormatter? = null
    ): ChartConfig {
        val colorRes = if (isUpward) upColorRes else downColorRes
        val fillRes = if (isUpward) upFillRes else downFillRes

        return ChartConfig(
            lineColor = ContextCompat.getColor(context, colorRes),
            fillDrawable = ContextCompat.getDrawable(context, fillRes),
            enableTouch = enableTouch,
            enableAxes = enableAxes,
            xAxisFormatter = xAxisFormatter
        )
    }
}
