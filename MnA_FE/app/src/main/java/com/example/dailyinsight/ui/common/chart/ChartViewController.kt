package com.example.dailyinsight.ui.common.chart

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.example.dailyinsight.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors

/**
 * Time range options for chart display
 */
enum class ChartRange(val dataPointCount: Int) {
    W1(20),    // 1 week
    M3(60),    // 3 months
    M6(120),   // 6 months
    M9(180),   // 9 months
    Y1(240)    // 1 year
}

/**
 * Data point for charting
 */
data class ChartDataPoint(
    val timestamp: Long,
    val value: Float
)

/**
 * Configuration for ChartViewController
 */
data class ChartViewConfig(
    val lineColorRes: Int,
    val fillDrawableRes: Int,
    val enableTouch: Boolean = true,
    val enableAxes: Boolean = true,
    val viewPortOffsets: FloatArray? = null,
    val xAxisFormatter: ValueFormatter? = null,
    val yAxisFormatter: ValueFormatter? = null,
    val defaultRange: ChartRange = ChartRange.M6
)

/**
 * Modular chart controller that handles chart rendering and range selection
 * Can be reused across different fragments
 */
class ChartViewController(
    private val context: Context,
    private val lineChart: LineChart,
    private val btnGroupRange: MaterialButtonToggleGroup,
    private val btn1W: MaterialButton,
    private val btn3M: MaterialButton,
    private val btn6M: MaterialButton,
    private val btn9M: MaterialButton,
    private val btn1Y: MaterialButton,
    private val config: ChartViewConfig
) {
    private var allDataPoints: List<ChartDataPoint> = emptyList()
    private var currentRange: ChartRange = config.defaultRange

    init {
        setupRangeButtons()
    }

    /**
     * Set the data and render the chart with the current range
     */
    fun setData(dataPoints: List<ChartDataPoint>) {
        allDataPoints = dataPoints
        if (dataPoints.isEmpty()) {
            lineChart.clear()
            btnGroupRange.isEnabled = false
        } else {
            btnGroupRange.isEnabled = true
            renderChart(currentRange)
        }
    }

    /**
     * Clear the chart
     */
    fun clear() {
        allDataPoints = emptyList()
        lineChart.clear()
        btnGroupRange.isEnabled = false
    }

    /**
     * Setup range selection buttons
     */
    private fun setupRangeButtons() {
        val checkedBg = ContextCompat.getColor(context, R.color.black)
        val checkedText = ContextCompat.getColor(context, android.R.color.white)
        val normalBg = MaterialColors.getColor(lineChart, com.google.android.material.R.attr.colorSurfaceVariant)
        val normalText = MaterialColors.getColor(lineChart, com.google.android.material.R.attr.colorOnSurfaceVariant)

        fun styleButton(btn: MaterialButton, checked: Boolean) {
            btn.setTextColor(if (checked) checkedText else normalText)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (checked) checkedBg else normalBg)
            btn.strokeWidth = 0
            btn.elevation = 0f
        }

        val allButtons = listOf(btn1W, btn3M, btn6M, btn9M, btn1Y)
        allButtons.forEach { styleButton(it, it.isChecked) }

        btnGroupRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            allButtons.forEach { styleButton(it, it.id == checkedId) }

            currentRange = when (checkedId) {
                btn1W.id -> ChartRange.W1
                btn3M.id -> ChartRange.M3
                btn6M.id -> ChartRange.M6
                btn9M.id -> ChartRange.M9
                btn1Y.id -> ChartRange.Y1
                else -> ChartRange.M6
            }
            renderChart(currentRange)
        }
    }

    /**
     * Filter data points by range
     */
    private fun filterByRange(data: List<ChartDataPoint>, range: ChartRange): List<ChartDataPoint> {
        return if (data.size <= range.dataPointCount) data else data.takeLast(range.dataPointCount)
    }

    /**
     * Render the chart with filtered data
     */
    private fun renderChart(range: ChartRange) {
        val filteredData = filterByRange(allDataPoints, range)
        if (filteredData.isEmpty()) {
            lineChart.clear()
            return
        }

        // Convert to chart entries
        val entries = filteredData.mapIndexed { i, point ->
            Entry(i.toFloat(), point.value)
        }

        // Calculate Y-axis range
        val minY = entries.minOf { it.y }
        val maxY = entries.maxOf { it.y }
        val pad = if (maxY == minY) 1f else (maxY - minY) * 0.05f

        // Create date labels from timestamps
        val dateFormat = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
        val labels = filteredData.map { point ->
            dateFormat.format(java.util.Date(point.timestamp))
        }

        // Create X-axis formatter that shows only start/middle/end
        val xFormatter = object : com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels) {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                val i = value.toInt()
                val n = labels.lastIndex
                return if (i in labels.indices && (i == 0 || i == n || i == n/2)) labels[i] else ""
            }
        }

        // Create chart config
        val chartConfig = ChartConfig(
            lineColor = ContextCompat.getColor(context, config.lineColorRes),
            fillDrawable = ContextCompat.getDrawable(context, config.fillDrawableRes),
            lineWidth = 2f,
            enableTouch = config.enableTouch,
            enableAxes = config.enableAxes,
            xAxisFormatter = xFormatter,  // Use our custom formatter
            yAxisFormatter = config.yAxisFormatter,
            viewPortOffsets = config.viewPortOffsets
        )

        // Setup chart
        ChartHelper.setupChart(lineChart, entries, chartConfig)

        // Configure Y-axis range
        lineChart.axisLeft.apply {
            axisMinimum = minY - pad
            axisMaximum = maxY + pad
            setLabelCount(4, false)
        }

        lineChart.invalidate()
    }
}
