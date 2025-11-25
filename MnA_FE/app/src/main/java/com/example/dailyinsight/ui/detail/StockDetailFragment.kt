package com.example.dailyinsight.ui.detail

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.CurrentData
import com.example.dailyinsight.data.dto.DividendData
import com.example.dailyinsight.data.dto.FinancialsData
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import com.example.dailyinsight.data.dto.HistoryItem
import com.example.dailyinsight.data.dto.ValuationData
import com.example.dailyinsight.databinding.FragmentStockDetailBinding
import com.example.dailyinsight.ui.common.LoadResult
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToInt
import android.util.TypedValue
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.components.AxisBase
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import android.text.TextUtils
import android.view.LayoutInflater
import kotlinx.coroutines.delay
import android.util.Log


class StockDetailFragment : Fragment(R.layout.fragment_stock_detail) {

    private var _binding: FragmentStockDetailBinding? = null
    private val binding get() = _binding!!

    private val args: StockDetailFragmentArgs by navArgs()
    private val viewModel: StockDetailViewModel by viewModels()

    private var currentChartUi: PriceChartUi? = null
    private var currentRange = Range.M6

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val xSdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.KOREA)
    private var currentXAxisFormat = "MM/dd" // 선택된 X축 날짜 포맷
    // 차트 기간
    private enum class Range { W1, M1, M3, M6, YTD, Y1, Y3, Y5 }

    // ---------- format helpers ----------
    private val dfPrice = DecimalFormat("#,##0")
    private val dfChange = DecimalFormat("#,##0")
    private val dfRate = DecimalFormat("#0.##")
    private fun rateWithComma(r: Double): String = dfRate.format(kotlin.math.abs(r)).replace('.', ',')

    // DTO의 숫자 문자열을 파싱하고 포맷팅하는 헬퍼
    private fun TextView.setNumberOrDash(number: Long?, unit: String, isFraction: Boolean = false) {
        setNumberOrDash(number?.toDouble(), unit, isFraction)
    }
    private fun TextView.setNumberOrDash(number: Double?, unit: String, isFraction: Boolean = false) {
        if (number == null) {
            text = "–"
            return
        }

        val value = if (isFraction) number * 100 else number
        val format = if (value == value.toLong().toDouble() && !isFraction) {
            DecimalFormat("#,##0")
        } else {
            DecimalFormat("#,##0.0")
        }

        // 음수도 그대로 표시
        text = "${format.format(value)}$unit"
    }

    private fun TextView.setOrDash(v: String?) { text = v?.takeIf { it.isNotBlank() } ?: "–" }

    private fun TextView.setOrDash(v: Long?)   { text = v?.let { dfPrice.format(it) } ?: "–" }
    private fun TextView.setOrDash(v: Double?) { text = v?.let { dfRate.format(it) } ?: "–" }
    private fun TextView.setPercentOrDash(v: Double?) { text = v?.let { "${dfRate.format(it)}%" } ?: "–" }

    private fun applyChangeColors(isUpOrFlat: Boolean) {
        val up = ContextCompat.getColor(requireContext(), R.color.price_up)
        val down = ContextCompat.getColor(requireContext(), R.color.price_down)
        val color = if (isUpOrFlat) up else down
        binding.tvChange.setTextColor(color) // 가격은 기본색 유지
    }

    // "123456789000" -> "123.46억", "1.23조"
    private fun formatKrwShort(v: Long?, isShare: Boolean = false): String {
        if (v == null) return "–"

        if (isShare) {
            val hundredMillion = 100_000_000L
            return String.format("%.2f억 주", v / hundredMillion.toDouble())
        }

        val trillion = 1_000_000_000_000L
        val hundredMillion = 100_000_000L
        return when {
            kotlin.math.abs(v) >= trillion -> String.format("%.1f조 원", v / trillion.toDouble())
            kotlin.math.abs(v) >= hundredMillion -> String.format("%.1f억 원", v / hundredMillion.toDouble())
            else -> DecimalFormat("#,##0").format(v) + "원"
        }
    }

    // DTO의 날짜("2025-11-13")를 "2025년 11월 13일"로 변환
    private val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    private val dateFormatter = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
    private fun formatDisplayDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "–"
        return try {
            val date = dateParser.parse(rawDate)
            dateFormatter.format(date!!)
        } catch (e: Exception) {
            rawDate // 파싱 실패 시 원본 반환
        }
    }

    // 프로퍼티
    private var chartData: List<Entry> = emptyList() // Entry 리스트로 변경
    private var chartLabels: List<String> = emptyList()
    // 데이터 로딩 상태를 체크할 변수
    private var isReportReady = false
    private var isOverviewReady = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 초기화: 로딩 화면 보이기 (XML에서 visible로 했지만 확실하게)
        binding.loadingOverlay.visibility = View.VISIBLE

        val ticker = args.item.ticker
        Log.d("StockDetail", "Loading started for ticker: $ticker")
        viewModel.load(ticker)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 2. 상세 정보(차트/가격) 관찰
                launch {
                    viewModel.state.collect { state ->
                        if (_binding == null) return@collect // 🚨 뷰 없으면 중단

                        when (state) {
                            is LoadResult.Success -> {
                                Log.d("StockDetail", "Report Loaded: Success")
                                bindDetail(state.data)
                                isReportReady = true
                                checkAllLoaded()
                            }
                            is LoadResult.Error -> {
                                Log.e("StockDetail", "Report Failed: ${state.throwable.message}")
                                isReportReady = true
                                checkAllLoaded()
                            }
                            is LoadResult.Loading -> Log.d("StockDetail", "Report: Loading...")
                            else -> {}
                        }
                    }
                }

                // 3. 개요 정보(요약 텍스트) 관찰
                launch {
                    viewModel.overviewState.collect { state ->
                        if (_binding == null) return@collect // 🚨 뷰 없으면 중단

                        when (state) {
                            is LoadResult.Success -> {
                                Log.d("StockDetail", "Overview Loaded: Success")
                                bindOverview(state.data)
                                isOverviewReady = true
                                checkAllLoaded()
                            }
                            is LoadResult.Error -> {
                                Log.e("StockDetail", "Overview Failed: ${state.throwable.message}")
                                isOverviewReady = true
                                checkAllLoaded()
                            }
                            is LoadResult.Loading -> Log.d("StockDetail", "Overview: Loading...")
                            else -> {}
                        }
                    }
                }

                // 4. 차트 데이터 관찰
                launch {
                    viewModel.priceState.collect { state ->
                        if (_binding == null) return@collect
                        if (state is LoadResult.Success) {
                            // 데이터 꺼내기 (MPAndroidChart 구조에 맞춰 안전하게 추출)
                            val lineData = state.data.chart.data
                            if (lineData.dataSetCount > 0) {
                                val set = lineData.getDataSetByIndex(0) as LineDataSet

                                // 1. 변수에 저장 (나중에 버튼 누를 때 씀)
                                chartData = set.values.toList() // 복사해서 저장
                                chartLabels = state.data.chart.xLabels

                                // 2. 버튼 활성화 및 그리기
                                binding.btnGroupRange.isEnabled = true
                                renderChart(currentRange)

                                Log.d("StockDetail", "Chart Loaded: Success")
                            }
                        }
                    }
                }
            }
        }
/*
        // 🚨 5. [안전장치] 5초가 지나도 로딩이 안 끝나면 강제로 화면 보여주기
        viewLifecycleOwner.lifecycleScope.launch {
            delay(5000) // 5초 대기
            if (binding.loadingOverlay.visibility == View.VISIBLE) {
                Log.w("StockDetail", "Force hiding loader due to timeout")
                binding.loadingOverlay.visibility = View.GONE
            }
        }*/
        // 하단 여백 보정
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = navInsets.bottom + dp(80))
            insets
        }
        setupChart()
        setupRangeButtons() // 버튼 연결
    }
    // 로딩 완료 판별 함수
    private fun checkAllLoaded() {
        // 두 데이터가 모두 준비되었을 때만 로딩 화면 제거
        if (isReportReady && isOverviewReady) {
            // 부드럽게 사라지게 애니메이션 적용
            binding.loadingOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    if (_binding != null) {
                        binding.loadingOverlay.visibility = View.GONE
                    }
                }
                .start()
        }
    }

    // --- 3. 요약/분석/뉴스 바인딩 함수 ---
    // 🚨 Overview 데이터를 화면 뷰에 꽂아주는 함수 (없으면 추가하세요!)
    // --- 3. 요약/분석/뉴스 바인딩 함수 ---
    private fun bindOverview(overview: StockOverviewDto) = with(binding) {
        // 1. 요약
        overview.summary?.takeIf { it.isNotBlank() }?.let {
            cardSummary.isVisible = true
            tvSummary.text = it
        }
        // 2. 기본적 분석
        overview.fundamental?.takeIf { it.isNotBlank() }?.let {
            cardFundamental.isVisible = true
            tvFundamental.text = it
        }
        // 3. 기술적 분석
        overview.technical?.takeIf { it.isNotBlank() }?.let {
            cardTechnical.isVisible = true
            tvTechnical.text = it
        }
        // 4. 관련 뉴스
        overview.news?.takeIf { it.isNotEmpty() }?.let { newsList ->
            cardNews.isVisible = true
            // 뉴스를 "• 항목 1\n• 항목 2" 형태로 변환
            tvNews.text = newsList.joinToString("\n") { "• $it" }
        }
    }


    /** 상단 헤더(요약) */
    private fun renderHeader(item: RecommendationDto) = with(binding) {
        val sign = if (item.change >= 0) "+" else "-"
        tvName.text = item.name
        tvticker.setOrDash(item.ticker)

        tvPrice.text = dfPrice.format(item.price)
        tvChange.text = "${sign}${dfChange.format(abs(item.change))} (${sign}${rateWithComma(item.changeRate)}%)"

        applyChangeColors(item.change >= 0)
    }

    // ===== 차트 / 버튼 =====

    private fun setupChart() = with(binding.lineChart) {
        setNoDataText("")
        legend.isEnabled = false
        description.isEnabled = false
        setTouchEnabled(false)
        isDragEnabled = false
        setScaleEnabled(false)
        setPinchZoom(false)
        setDrawGridBackground(false)
        setMinOffset(12f)
        setExtraOffsets(8f, 6f, 12f, 16f)
        axisRight.isEnabled = false

        // Y축(왼쪽)
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
        // X축(아래)
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


    /** 버튼(세그먼트) — 선택 상태 스타일 + 실제 기간 렌더 */
    private fun setupRangeButtons() = with(binding) {
        val checkedBg   = ContextCompat.getColor(requireContext(), R.color.black)
        val checkedText = ContextCompat.getColor(requireContext(), android.R.color.white)
        val normalBg    = MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurfaceVariant)
        val normalText  = MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurfaceVariant)

        fun style(btn: com.google.android.material.button.MaterialButton, checked: Boolean) {
            btn.setTextColor(if (checked) checkedText else normalText)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (checked) checkedBg else normalBg)
            btn.strokeWidth = 0
            btn.elevation = 0f
        }

        val all = listOf(btn1W, btn1M, btn3M, btn6M, btnYTD, btn1Y, btn3Y, btn5Y)
        all.forEach { style(it, it.isChecked) }

        btnGroupRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            all.forEach { style(it, it.id == checkedId) }
            when (checkedId) {
                btn1W.id -> renderChart(Range.W1)
                btn1M.id -> renderChart(Range.M1)
                btn3M.id -> renderChart(Range.M3)
                btn6M.id -> renderChart(Range.M6)
                btnYTD.id -> renderChart(Range.YTD)
                btn1Y.id -> renderChart(Range.Y1)
                btn3Y.id -> renderChart(Range.Y3)
                btn5Y.id -> renderChart(Range.Y5)
            }
        }
    }

    /** 실제 데이터로 차트 렌더 */
    private fun renderChart(range: Range) = with(binding.lineChart) {
        // 프래그먼트에 저장된 chartData 사용
        val (pts, labels) = filterByRange(chartData, chartLabels, range)
        if (pts.isEmpty()) { data = null; invalidate(); return@with }

        val entries = pts.mapIndexed { i, p -> Entry(i.toFloat(), p.y, p.data) }
        val minY = entries.minOf { it.y }
        val maxY = entries.maxOf { it.y }
        val span = maxY - minY
        val fewPoints = entries.size < 8
        val almostFlat = span < 1e-3f

        // 축 범위(데이터 때마다 갱신)
        val pad = if (span == 0f) 1f else span * 0.05f
        axisLeft.axisMinimum = minY - pad
        axisLeft.axisMaximum = maxY + pad

        // X 라벨(시작/중간/끝만)
        xAxis.valueFormatter = object : IndexAxisValueFormatter(labels) {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val i = value.toInt()
                val n = labels.lastIndex
                //  yyyy/MM 포맷 적용
                val sdf = SimpleDateFormat(currentXAxisFormat, Locale.KOREA)
                val labelDate = sdf.format(Date(pts[i].data as Long))

                return if (i in labels.indices && (i == 0 || i == n || i == n/2)) labelDate else ""
            }
        }
        val set = LineDataSet(entries, "").apply {
            mode = if (fewPoints || almostFlat) LineDataSet.Mode.LINEAR
            else LineDataSet.Mode.CUBIC_BEZIER
            color = ContextCompat.getColor(requireContext(), R.color.price_up)
            lineWidth = 3f
            if (fewPoints) {
                setDrawCircles(true)
                circleRadius = 3f
            } else {
                setDrawCircles(false)
            }
            setDrawValues(false)

            val drawFill = !fewPoints && !almostFlat
            setDrawFilled(drawFill)
            if (drawFill) {
                fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart_fill)
                fillFormatter = IFillFormatter { _, _ -> minY } // 0 기준 삼각형 방지
            } else {
                fillFormatter = IFillFormatter { _, _ -> minY }
            }
            highLightColor = android.graphics.Color.TRANSPARENT
        }

        data = LineData(set)
        invalidate()
    }


    /** 범위 필터 —범위 필터 — 실제 날짜(타임스탬프) 기준 */
    private fun filterByRange(
        rawEntries: List<Entry>,
        rawLabels: List<String>,
        range: Range
    ): Pair<List<Entry>, List<String>> {
        if (rawEntries.isEmpty()) return Pair(emptyList(), emptyList())
        val now = Calendar.getInstance() // KST (디바이스 기본값)
        val cal = Calendar.getInstance()

        // YTD (연중) 기준일 계산
        cal.time = now.time
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val ytdStartTimestamp = cal.timeInMillis

        // 기간별 시작 타임스탬프 계산
        val startTimestamp = when (range) {
            Range.W1 -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            Range.M1 -> Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
            Range.M3 -> Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis
            Range.M6 -> Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
            Range.YTD -> ytdStartTimestamp
            Range.Y1 -> Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
            Range.Y3 -> Calendar.getInstance().apply { add(Calendar.YEAR, -3) }.timeInMillis
            Range.Y5 -> Calendar.getInstance().apply { add(Calendar.YEAR, -5) }.timeInMillis
        }
        currentXAxisFormat = if (range == Range.Y1 ||range == Range.Y3 || range == Range.Y5) "yyyy/MM" else "MM/dd"
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

    // ───── 표 ─────

    /** "규모" 테이블 렌더링 */
    private fun renderSizeTable(
        table: TableLayout,
        header: List<String>,
        today: HistoryItem?,
        yLast: HistoryItem?,
        yBefore: HistoryItem?,
        calculatedShares: Long?
    ) {
        table.removeAllViews()
        table.addView(rowHeader(header))
        val rowMarketCap = rowBody(listOf("시가총액", "–", "–", "–"), false)
        (rowMarketCap.getChildAt(1) as? TextView)?.text = formatKrwShort(today?.marketCap)
        (rowMarketCap.getChildAt(2) as? TextView)?.text = formatKrwShort(yLast?.marketCap)
        (rowMarketCap.getChildAt(3) as? TextView)?.text = formatKrwShort(yBefore?.marketCap)
        table.addView(rowMarketCap)

        val rowShares = rowBody(listOf("상장 주식수", "–", "–", "–"), false)
        // TO의 sharesOutstanding 대신 계산된 값(calculatedShares)을 사용
        (rowShares.getChildAt(1) as? TextView)?.text = formatKrwShort(calculatedShares, true)
        (rowShares.getChildAt(2) as? TextView)?.text = formatKrwShort(calculatedShares, true)
        (rowShares.getChildAt(3) as? TextView)?.text = formatKrwShort(calculatedShares, true)
        table.addView(rowShares)
    }

    /** "가치" 테이블 렌더링 */
    private fun renderValueTable(
        table: TableLayout,
        header: List<String>,
        today: HistoryItem?,
        yLast: HistoryItem?,
        yBefore: HistoryItem?
    ) {
        table.removeAllViews()
        table.addView(rowHeader(header))

        val rowBps = rowBody(listOf("주당순자산가치", "–", "–", "–"), false)
        (rowBps.getChildAt(1) as? TextView)?.setNumberOrDash(today?.bps, " 원")
        (rowBps.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.bps, " 원")
        (rowBps.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.bps, " 원")
        table.addView(rowBps)

        val rowPer = rowBody(listOf("주가수익률", "–", "–", "–"), false)
        (rowPer.getChildAt(1) as? TextView)?.setNumberOrDash(today?.per, " 배")
        (rowPer.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.per, " 배")
        (rowPer.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.per, " 배")
        table.addView(rowPer)

        val rowPbr = rowBody(listOf("주가순자산비율", "–", "–", "–"), false)
        (rowPbr.getChildAt(1) as? TextView)?.setNumberOrDash(today?.pbr, " 배")
        (rowPbr.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.pbr, " 배")
        (rowPbr.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.pbr, " 배")
        table.addView(rowPbr)
    }

    /** "수익성" 테이블 렌더링 */
    private fun renderProfitabilityTable(
        table: TableLayout,
        header: List<String>,
        today: HistoryItem?,
        yLast: HistoryItem?,
        yBefore: HistoryItem?
    ) {
        table.removeAllViews()
        table.addView(rowHeader(header))

        val rowEps = rowBody(listOf("주당순이익", "–", "–", "–"), false)
        (rowEps.getChildAt(1) as? TextView)?.setNumberOrDash(today?.eps, " 원")
        (rowEps.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.eps, " 원")
        (rowEps.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.eps, " 원")
        table.addView(rowEps)

        val rowRoe = rowBody(listOf("자기자본이익률", "–", "–", "–"), false)
        (rowRoe.getChildAt(1) as? TextView)?.setNumberOrDash(today?.roe, "%", true)
        (rowRoe.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.roe, "%", true)
        (rowRoe.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.roe, "%", true)
        table.addView(rowRoe)
    }

    /** "배당" 테이블 렌더링 */
    private fun renderDividendTable(
        table: TableLayout,
        header: List<String>,
        today: HistoryItem?,
        yLast: HistoryItem?,
        yBefore: HistoryItem?
    ) {
        table.removeAllViews()
        table.addView(rowHeader(header))

        val rowDps = rowBody(listOf("주당배당금", "–", "–", "–"), false)
        (rowDps.getChildAt(1) as? TextView)?.setNumberOrDash(today?.dps, " 원")
        (rowDps.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.dps, " 원")
        (rowDps.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.dps, " 원")
        table.addView(rowDps)

        val rowDiv = rowBody(listOf("배당 수익률", "–", "–", "–"), false)
        (rowDiv.getChildAt(1) as? TextView)?.setNumberOrDash(today?.divYield, "%")
        (rowDiv.getChildAt(2) as? TextView)?.setNumberOrDash(yLast?.divYield, "%")
        (rowDiv.getChildAt(3) as? TextView)?.setNumberOrDash(yBefore?.divYield, "%")
        table.addView(rowDiv)
    }

    // 공용 빌더들 ─────────────────────────────────────────────────────────
    private fun rowHeader(texts: List<String>): TableRow = TableRow(requireContext()).apply {
        layoutParams = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        texts.forEachIndexed { i, t -> addView(cell(t, index = i, header = true, center = true)) }
    }

    private fun rowBody(texts: List<String>, emphasizeFirst: Boolean): TableRow =
        TableRow(requireContext()).apply {
            layoutParams = TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            texts.forEachIndexed { i, t ->
                val isPeriodColumn = (i == 0)
                addView(
                    cell(
                        text = t,
                        index = i,
                        bold = isPeriodColumn,
                        alignEnd = !isPeriodColumn,
                        center = isPeriodColumn
                    )
                )
            }
        }

    private fun cell(
        text: String?,
        index: Int,
        header: Boolean = false,
        bold: Boolean = false,
        alignEnd: Boolean = false,
        center: Boolean = false
    ) = TextView(requireContext()).apply {
        this.text = text ?: "–"
        setBackgroundResource(R.drawable.bg_table_cell)
        setPadding(
            dp(8),  // left
            dp(6),  // top
            dp(8),  // right
            dp(6)   // bottom
        )
        minHeight = dp(40)

        if (header) {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            paint.isFakeBoldText = true
        } else {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }

        gravity = when {
            center -> Gravity.CENTER
            alignEnd -> Gravity.END or Gravity.CENTER_VERTICAL
            else -> Gravity.START or Gravity.CENTER_VERTICAL
        }

        if (bold) paint.isFakeBoldText = true

        // 가중치를 index(컬럼) 기준으로 통일
        val weight =  1.0f
        layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
        maxLines = 1 //  모든 셀은 1줄
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** 상세 데이터 바인딩 */
    private fun bindDetail(d: StockDetailDto) = with(binding) {
        // === 1. 헤더 바인딩 ===
        tvticker.setOrDash(d.ticker)
        tvName.setOrDash(d.name)
        d.current?.let {
            tvPrice.text = dfPrice.format(it.price)
            val sign = if (it.change ?: 0L >= 0) "+" else "-"
            tvChange.text = "${sign}${dfChange.format(abs(it.change ?: 0L))} (${sign}${rateWithComma(it.changeRate ?: 0.0)}%)"
            applyChangeColors(it.change ?: 0L >= 0)
        }

        tvDate.text = formatDisplayDate(d.current?.date) // 날짜
        // === 2. 테이블 데이터 준비 ===
        val financials = d.history.orEmpty()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR) // 예: 2025
        val lastYearStr = (currentYear - 1).toString() // "2024"
        val twoYearsAgoStr = (currentYear - 2).toString() // "2023"

        val yToday = financials.lastOrNull() // "오늘" 데이터
        // 작년(2024년) 12월 31일(또는 마지막 거래일) 데이터 찾기
        val yLast = financials.filter { it.date.startsWith(lastYearStr) }.lastOrNull()
        // 재작년(2023년) 12월 31일(또는 마지막 거래일) 데이터 찾기
        val yBefore = financials.filter { it.date.startsWith(twoYearsAgoStr) }.lastOrNull()
        val dynamicHeader = listOf("연도", currentYear.toString(), lastYearStr, twoYearsAgoStr)
        // === 3. [수정] 상장 주식수 계산 ===
        val marketCapLong = d.current?.marketCap
        val currentPrice = d.current?.price
        val calculatedShares: Long? = if (marketCapLong != null && currentPrice != null && currentPrice > 0) {
            (marketCapLong / currentPrice)
        } else {
            null // 계산 불가 시 null
        }

        // === 4. 테이블 렌더링 ===
        renderSizeTable(tblSize, dynamicHeader, yToday, yLast, yBefore, calculatedShares)
        renderValueTable(tblValue, dynamicHeader, yToday, yLast, yBefore)
        renderProfitabilityTable(tblProfitability, dynamicHeader, yToday, yLast, yBefore)
        renderDividendTable(tblDividend, dynamicHeader, yToday, yLast, yBefore)

        // === 5. "기업 overview" 바인딩 ===
        d.profile?.explanation?.takeIf { it.isNotBlank() }?.let {
            cardExplanation.isVisible = true
            tvExplanation.text = it
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}