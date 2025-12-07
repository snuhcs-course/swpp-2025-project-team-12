package com.example.dailyinsight.ui.marketindex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.databinding.FragmentStockIndexDetailBinding
import com.example.dailyinsight.ui.common.chart.ChartViewController
import com.example.dailyinsight.ui.common.chart.ChartViewConfig
import java.text.SimpleDateFormat
import java.util.Locale
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import java.util.Calendar
import java.util.Date

class StockIndexDetailFragment : Fragment() {

    private lateinit var viewModel: StockIndexDetailViewModel
    private val args: StockIndexDetailFragmentArgs by navArgs()
    private var _binding: FragmentStockIndexDetailBinding? = null
    private val binding get() = _binding!!

    // 🚨 차트 데이터 관리용 (ChartViewController 대신 직접 관리)
    private var fullChartData: List<Entry> = emptyList()
    private var fullChartLabels: List<String> = emptyList()
    private var currentRange = Range.M3 // 기본값 3개월

    private enum class Range { W1, M3, M6, M9, Y1 }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockIndexDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stockIndexType = args.stockIndexType
        val viewModelFactory = StockIndexDetailViewModelFactory(requireActivity().application, stockIndexType)
        viewModel = ViewModelProvider(this, viewModelFactory)[StockIndexDetailViewModel::class.java]

        // 1. 차트 초기화 (축 설정, 터치 등)
        setupChart()
        // 2. 기간 버튼 리스너 연결
        setupRangeButtons()

        // 3. 헤더 데이터 관찰
        viewModel.stockIndexData.observe(viewLifecycleOwner) { data ->
            data?.let { updateIndexUI(it) }
        }

        // 4. 차트 데이터 관찰
        viewModel.historicalData.observe(viewLifecycleOwner) { dataPoints ->
            if (dataPoints.isEmpty()) {
                // 로딩 중 or 데이터 없음
                binding.chartProgressBar.visibility = View.VISIBLE
                binding.chartView.lineChart.visibility = View.INVISIBLE
            } else {
                // 데이터 로드 완료
                binding.chartProgressBar.visibility = View.GONE
                binding.chartView.lineChart.visibility = View.VISIBLE

                // 🚨 데이터를 Entry로 변환하여 저장
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.KOREA)
                fullChartData = dataPoints.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.closePrice, point.timestamp)
                }
                fullChartLabels = dataPoints.map { point ->
                    sdf.format(Date(point.timestamp))
                }

                // 현재 선택된 기간으로 차트 그리기
                renderChart(currentRange)
            }
        }

        // 5. 52주 최고/최저 관찰
        viewModel.yearHigh.observe(viewLifecycleOwner) { high ->
            high?.let { binding.tvYearHighValue.text = String.format(Locale.getDefault(), "%.2f", it) }
        }
        viewModel.yearLow.observe(viewLifecycleOwner) { low ->
            low?.let { binding.tvYearLowValue.text = String.format(Locale.getDefault(), "%.2f", it) }
        }
    }

    // --- 차트 설정 (종목 상세와 동일하게) ---
    private fun setupChart() = with(binding.chartView.lineChart) {
        setNoDataText("")
        legend.isEnabled = false
        description.isEnabled = false

        // 터치/줌 설정
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setScaleXEnabled(true)
        setScaleYEnabled(true)
        setPinchZoom(true)

        setDrawGridBackground(false)
        setMinOffset(12f)
        setExtraOffsets(8f, 6f, 12f, 16f)
        axisRight.isEnabled = false

        // Y축
        axisLeft.apply {
            isEnabled = true
            setDrawGridLines(false)
            xOffset = 6f
            textSize = 11f
            valueFormatter = object : ValueFormatter() {
                private val df = java.text.DecimalFormat("#,##0")
                override fun getAxisLabel(v: Float, a: AxisBase?): String = df.format(v.toLong())
            }
            setLabelCount(4, true)
        }
        // X축
        xAxis.apply {
            isEnabled = true
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            setLabelCount(3, true)
            yOffset = 6f
            setAvoidFirstLastClipping(true)
            textSize = 11f
        }
    }

    // --- 버튼 설정 ---
    private fun setupRangeButtons() = with(binding.chartView) {
        val checkedBg = ContextCompat.getColor(requireContext(), R.color.black)
        val checkedText = ContextCompat.getColor(requireContext(), android.R.color.white)
        val normalBg = MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurfaceVariant)
        val normalText = MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurfaceVariant)

        fun style(btn: com.google.android.material.button.MaterialButton, checked: Boolean) {
            btn.setTextColor(if (checked) checkedText else normalText)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (checked) checkedBg else normalBg)
            btn.strokeWidth = 0
            btn.elevation = 0f
        }

        // 버튼 리스트 (XML ID 확인 필요)
        val buttons = listOf(btn1W, btn3M, btn6M, btn9M, btn1Y) // 필요한 버튼만 리스트업

        // 초기 상태 스타일링
        buttons.forEach { style(it, it.isChecked) }

        btnGroupRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            // 스타일 갱신
            buttons.forEach { style(it, it.id == checkedId) }

            // 범위 변경 및 렌더링
            val range = when (checkedId) {
                btn1W.id -> Range.W1
                btn3M.id -> Range.M3
                btn6M.id -> Range.M6
                btn9M.id -> Range.M9
                btn1Y.id -> Range.Y1
                else -> Range.M3
            }
            currentRange = range
            renderChart(range)
        }
    }

    // --- 차트 그리기 (핵심 로직) ---
    private fun renderChart(range: Range) = with(binding.chartView.lineChart) {
        // 1. 기간 필터링
        var (pts, labels) = filterByRange(fullChartData, fullChartLabels, range)

        if (pts.isEmpty()) {
            data = null
            invalidate()
            return@with
        }

        val isOneWeek = range == Range.W1

        // 🚨 2. 1주일(W1) 데이터 채우기 (휴장일 처리)
        if (isOneWeek && pts.isNotEmpty()) {
            val filledPts = mutableListOf<Entry>()
            val filledLabels = mutableListOf<String>()
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
            val labelSdf = SimpleDateFormat("yyyy/MM/dd", Locale.KOREA)

            val lastTimestamp = pts.last().data as Long
            val calendar = Calendar.getInstance().apply { timeInMillis = lastTimestamp }

            val tempEntries = mutableListOf<Entry>()

            // 7일치 역순 생성
            for (i in 0 until 7) {
                val targetDayStr = sdf.format(calendar.time)
                val existingEntry = pts.find { sdf.format(Date(it.data as Long)) == targetDayStr }

                if (existingEntry != null) {
                    tempEntries.add(existingEntry)
                } else {
                    // 데이터 없음 -> 임시
                    tempEntries.add(Entry(0f, 0f, calendar.timeInMillis))
                }
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            tempEntries.sortBy { it.data as Long }

            // 가격 채우기 (Forward Fill)
            var lastValidPrice = pts.first().y
            tempEntries.forEach { entry ->
                val original = pts.find { sdf.format(Date(it.data as Long)) == sdf.format(Date(entry.data as Long)) }
                if (original != null) {
                    lastValidPrice = original.y
                    filledPts.add(original)
                } else {
                    filledPts.add(Entry(0f, lastValidPrice, entry.data))
                }
                filledLabels.add(labelSdf.format(Date(entry.data as Long)))
            }
            pts = filledPts
            labels = filledLabels
        }

        // 3. 데이터셋 생성
        val entries = pts.mapIndexed { i, p -> Entry(i.toFloat(), p.y, p.data) }
        val minY = entries.minOf { it.y }
        val maxY = entries.maxOf { it.y }
        val span = maxY - minY
        val pad = if (span == 0f) 1f else span * 0.05f

        axisLeft.axisMinimum = minY - pad
        axisLeft.axisMaximum = maxY + pad

        // 4. X축 라벨 (3개 고정)
        xAxis.apply {
            setLabelCount(3, true)
            granularity = 1f
            valueFormatter = object : IndexAxisValueFormatter(labels) {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val i = value.toInt()
                    val n = labels.lastIndex
                    val sdf = SimpleDateFormat("MM/dd", Locale.KOREA)
                    val dateStr = if (i in pts.indices) sdf.format(Date(pts[i].data as Long)) else ""

                    return if (i in labels.indices && (i == 0 || i == n || i == n/2)) dateStr else ""
                }
            }
        }

        // 5. 라인 스타일
        val set = LineDataSet(entries, "").apply {
            mode = if (isOneWeek) LineDataSet.Mode.LINEAR else LineDataSet.Mode.CUBIC_BEZIER
            color = ContextCompat.getColor(requireContext(), R.color.positive_red)
            lineWidth = 2f

            if (isOneWeek) {
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(color)
                setDrawCircleHole(false)
            } else {
                setDrawCircles(false)
            }

            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fade_red)
            fillFormatter = IFillFormatter { _, _ -> axisLeft.axisMinimum }
            highLightColor = android.graphics.Color.TRANSPARENT
        }

        data = LineData(set)
        fitScreen()
        invalidate()
    }

    // --- 기간 필터링 함수 ---
    private fun filterByRange(
        rawEntries: List<Entry>,
        rawLabels: List<String>,
        range: Range
    ): Pair<List<Entry>, List<String>> {
        if (rawEntries.isEmpty()) return Pair(emptyList(), emptyList())

        // 시작 시간 계산
        val startTimestamp = when (range) {
            Range.W1 -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            Range.M3 -> Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis
            Range.M6 -> Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
            Range.M9 -> Calendar.getInstance().apply { add(Calendar.MONTH, -9) }.timeInMillis
            Range.Y1 -> Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
        }

        val filteredEntries = mutableListOf<Entry>()
        val filteredLabels = mutableListOf<String>()

        rawEntries.forEachIndexed { index, entry ->
            if ((entry.data as Long) >= startTimestamp) {
                filteredEntries.add(entry)
                filteredLabels.add(rawLabels[index])
            }
        }
        return Pair(filteredEntries, filteredLabels)
    }

    // --- 기존 UI 업데이트 ---
    private fun updateIndexUI(data: StockIndexData) {
        binding.tvPrice.text = String.format("%.2f", data.close)
        val sign = if (data.changeAmount >= 0) "+" else ""
        binding.tvChange.text = "$sign${data.changeAmount} (${sign}${data.changePercent}%)"
        val color = if (data.changeAmount >= 0) R.color.positive_red else R.color.negative_blue
        binding.tvChange.setTextColor(ContextCompat.getColor(requireContext(), color))

        binding.tvMarketPriceDate.text = formatDate(data.date)
        binding.tvOpenValue.text = "%.2f".format(data.open)
        binding.tvCloseValue.text = "%.2f".format(data.close)
        binding.tvDayHighValue.text = "%.2f".format(data.high)
        binding.tvDayLowValue.text = "%.2f".format(data.low)
        binding.tvVolumeValue.text = formatVolume(data.volume)
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("M월 d일 기준", Locale.KOREA)
            val date = parser.parse(dateStr)
            date?.let { formatter.format(it) } ?: dateStr
        } catch (_: Exception) { dateStr }
    }

    private fun formatVolume(volume: Long): String {
        val eok = volume / 100_000_000
        val man = (volume % 100_000_000) / 10_000
        val result = StringBuilder()
        if (eok > 0) result.append("${eok}억 ")
        if (man > 0 || eok == 0L) result.append("${man}만")
        result.append("주")
        return result.toString()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}