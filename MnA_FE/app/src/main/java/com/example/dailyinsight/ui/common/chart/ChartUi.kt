package com.example.dailyinsight.ui.common.chart

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData

/**
 * 차트에 필요한 데이터를 한데 모은 UI 전용 모델.
 * - entries: y값 시계열(혹은 순서형) 데이터
 * - xLabels: X축 라벨(날짜/분기/카테고리 등)
 * - lineData: 데이터셋까지 구성된 객체(바로 setData 가능)
 */
data class ChartUi(
    val xLabels: List<String>,
    val lineData: LineData
)