package com.example.dailyinsight.ui.common.chart

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis

class ChartConfigurator {
    fun apply(chart: LineChart, ui: ChartUi) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(ui.xLabels)
        }
        chart.axisRight.isEnabled = false
        chart.legend.apply {
            form = Legend.LegendForm.LINE
            isEnabled = true
        }

        chart.data = ui.lineData
        chart.invalidate()
    }
}