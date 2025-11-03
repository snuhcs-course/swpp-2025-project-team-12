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
import com.example.dailyinsight.data.dto.NetIncome
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.ChartPoint
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
import com.example.dailyinsight.ui.common.chart.ChartConfigurator
import com.example.dailyinsight.ui.common.chart.ChartUi
import kotlin.collections.isNotEmpty
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.components.AxisBase

class StockDetailFragment : Fragment(R.layout.fragment_stock_detail) {

    private var _binding: FragmentStockDetailBinding? = null
    private val binding get() = _binding!!

    private val args: StockDetailFragmentArgs by navArgs()
    private val viewModel: StockDetailViewModel by viewModels()

    private val xSdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.KOREA)

    // 차트 기간
    private enum class Range { W1, M3, M6, M9, Y1 }

    // 최신 차트 데이터 보관

    private val chartConfigurator = ChartConfigurator()
    private val labelFmt = java.text.SimpleDateFormat("MM/dd", java.util.Locale.US)

    // ---------- format helpers ----------
    private val dfPrice = DecimalFormat("#,##0")
    private val dfChange = DecimalFormat("#,##0")
    private val dfRate = DecimalFormat("#0.##")
    private fun rateWithComma(r: Double): String = dfRate.format(kotlin.math.abs(r)).replace('.', ',')

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
    private fun formatKrwShort(raw: String?): String {
        if (raw.isNullOrBlank()) return "–"
        val cleaned = raw.replace(",", "").trim()
        val v = cleaned.toLongOrNull() ?: return raw
        val trillion = 1_000_000_000_000L
        val hundredMillion = 100_000_000L
        return when {
            kotlin.math.abs(v) >= trillion -> String.format("%.2f조", v / trillion.toDouble())
            kotlin.math.abs(v) >= hundredMillion -> String.format("%.2f억", v / hundredMillion.toDouble())
            else -> DecimalFormat("#,##0").format(v) + "원"
        }
    }

    // 프로퍼티
    private var chartData: List<ChartPoint> = emptyList()

    // Map<String, Double> -> List<ChartPoint>
    private fun mapToChartPoints(price: Map<String, Double>?): List<ChartPoint> {
        if (price.isNullOrEmpty()) return emptyList()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC"); isLenient = false
        }
        val sorted: List<Map.Entry<String, Double>> = price.entries.toList().sortedBy { it.key }
        return sorted.mapIndexed { idx, e ->
            val t = try { sdf.parse(e.key)?.time ?: (idx * 86_400_000L) }
            catch (_: Throwable) { idx * 86_400_000L }
            ChartPoint(t = t, v = e.value)
        }
    }
    // ------------------------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStockDetailBinding.bind(view)

        // Header
        renderHeader(args.item)

        // 상세 로드
        viewModel.load(args.item.ticker)

        // 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { st ->
                    when (st) {
                        is LoadResult.Loading -> {
                            binding.progress.isVisible = true
                            binding.tvError.isVisible = false
                        }
                        is LoadResult.Error -> {
                            binding.progress.isVisible = false
                            binding.tvError.isVisible = true
                            binding.tvError.text = st.throwable.message ?: getString(R.string.error_generic)
                        }
                        is LoadResult.Success -> {
                            binding.progress.isVisible = false
                            binding.tvError.isVisible = false
                            bindDetail(st.data)
                        }
                        else -> Unit
                    }
                }
            }
        }

        // 하단 여백 보정
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = navInsets.bottom + dp(24))
            insets
        }

        setupChart()
        setupRangeButtons()
    }

    /** 상단 헤더(요약) */
    private fun renderHeader(item: RecommendationDto) = with(binding) {
        val sign = if (item.change >= 0) "+" else "-"
        tvName.text = item.name
        tvticker.setOrDash(item.ticker)

        tvPrice.text = dfPrice.format(item.price)
        tvChange.text = "${sign}${dfChange.format(abs(item.change))} (${sign}${rateWithComma(item.changeRate)}%)"
        tvTime.text = item.time
        tvHeadline.setOrDash(item.headline ?: getString(R.string.no_headline))

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
        // ※ 그래도 잘리면 아래 고정 오프셋을 사용(위 두 줄 대신):
        // setViewPortOffsets(32f, 12f, 24f, 36f)

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

        val all = listOf(btn1W, btn3M, btn6M, btn9M, btn1Y)
        all.forEach { style(it, it.isChecked) }

        btnGroupRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            all.forEach { style(it, it.id == checkedId) }
            when (checkedId) {
                btn1W.id -> renderChart(Range.W1)
                btn3M.id -> renderChart(Range.M3)
                btn6M.id -> renderChart(Range.M6)
                btn9M.id -> renderChart(Range.M9)
                btn1Y.id -> renderChart(Range.Y1)
            }
        }
    }

    /** 실제 데이터로 차트 렌더 */
    private fun renderChart(range: Range) = with(binding.lineChart) {
        val pts = filterByRange(chartData, range)
        if (pts.isEmpty()) { data = null; invalidate(); return@with }

        val entries = pts.mapIndexed { i, p -> Entry(i.toFloat(), p.v.toFloat()) }
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
        val labels = pts.map { p ->
            java.text.SimpleDateFormat("MM/dd", java.util.Locale.KOREA)
                .format(java.util.Date(p.t))
        }
        xAxis.valueFormatter = object : IndexAxisValueFormatter(labels) {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val i = value.toInt()
                val n = labels.lastIndex
                return if (i in labels.indices && (i == 0 || i == n || i == n/2)) labels[i] else ""
            }
        }

        val set = LineDataSet(entries, "").apply {
            mode = if (fewPoints || almostFlat) LineDataSet.Mode.LINEAR
            else LineDataSet.Mode.CUBIC_BEZIER
            color = ContextCompat.getColor(requireContext(), R.color.price_up)
            lineWidth = 3f
            setDrawCircles(fewPoints)
            circleRadius = if (fewPoints) 3f else 0f
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


    /** 범위 필터 — 마지막 N개 단순 슬라이스(균등 간격 가정) */
    private fun filterByRange(
        raw: List<com.example.dailyinsight.data.dto.ChartPoint>,
        range: Range
    ): List<com.example.dailyinsight.data.dto.ChartPoint> {
        val n = when (range) {
            Range.W1 -> 20
            Range.M3 -> 60
            Range.M6 -> 120
            Range.M9 -> 180
            Range.Y1 -> 240
        }
        return if (raw.size <= n) raw else raw.takeLast(n)
    }

    // ───── Net Income 표 (동일) ─────

    private fun parseQuarterPeriod(raw: String, fallbackYear: Int?): Pair<Int?, String?> {
        val year = Regex("""\b(20\d{2})\b""").find(raw)?.value?.toIntOrNull() ?: fallbackYear
        val q = Regex("""\bQ[1-4]\b""", RegexOption.IGNORE_CASE).find(raw)?.value?.uppercase()
        if (raw.contains("TTM", ignoreCase = true)) return year to null
        return year to q
    }

    private fun normalizeValue(v: String?): String = v?.takeIf { it.isNotBlank() } ?: "–"

    private fun renderNetIncomeTable(net: NetIncome?) = with(binding) {
        val annual  = net?.annual.orEmpty()
        val quarter = net?.quarter.orEmpty()
        val ttmItem = (annual + quarter).firstOrNull { it.period.contains("TTM", ignoreCase = true) }

        tblNetIncome.removeAllViews()

        val years = annual.mapNotNull { it.period.toIntOrNull() }.sortedDescending().take(4)
        tblNetIncome.isVisible = years.isNotEmpty() || quarter.isNotEmpty()
        if (!tblNetIncome.isVisible) return@with

        tblNetIncome.addView(rowHeader(listOf("PERIOD") + years.map { it.toString() }))

        val latestYear = years.firstOrNull()
        val qMap = mutableMapOf<Int, MutableMap<String, String>>()
        quarter.forEach { pv ->
            val (yr, qLabel) = parseQuarterPeriod(pv.period, latestYear)
            if (yr != null && qLabel != null) {
                qMap.getOrPut(yr) { mutableMapOf() }[qLabel] = normalizeValue(pv.value)
            }
        }

        listOf("Q1","Q2","Q3","Q4").forEach { q ->
            val row = mutableListOf(q)
            years.forEach { y -> row += (qMap[y]?.get(q) ?: "–") }
            tblNetIncome.addView(rowBody(row, emphasizeFirst = false))
        }

        val aMap = annual.associate { it.period to normalizeValue(it.value) }
        val annualRow = mutableListOf("Annual") + years.map { y -> aMap[y.toString()] ?: "–" }
        tblNetIncome.addView(rowBody(annualRow, emphasizeFirst = true))

        val serverLabelAndValue: Pair<String, String?>? = ttmItem?.let {
            val qInTtm = Regex("""Q[1-4]""", RegexOption.IGNORE_CASE).find(it.period)?.value?.uppercase()
            val label = if (qInTtm != null) "TTM ($qInTtm)" else "TTM"
            label to normalizeValue(it.value)
        }.takeIf { it?.second?.trim().isNullOrEmpty().not() }

        val computedTtm = computeLatestTTM(qMap)?.let { (label, sum) -> label to (sum?.let { formatB(it) }) }
        val computedYtd = computeYTD(qMap)?.let { (label, sum) -> label to (sum?.let { formatB(it) }) }

        val (finalLabel, finalValue) = serverLabelAndValue ?: computedTtm ?: computedYtd ?: ("TTM" to null)
        val ttmRow = MutableList(1 + years.size) { "–" }
        ttmRow[0] = finalLabel
        if (latestYear != null && !finalValue.isNullOrBlank()) ttmRow[1] = finalValue
        tblNetIncome.addView(rowBody(ttmRow, emphasizeFirst = true))
    }

    private fun parseB(v: String?): Double? {
        if (v.isNullOrBlank()) return null
        val s = v.trim().replace(",", ".")
        if (s == "-" || s == "–") return null
        return s.removeSuffix("B").trim().toDoubleOrNull()
    }

    private fun formatB(d: Double?): String =
        if (d == null) "–" else String.format("%.3f B", d).replace('.', ',')

    private fun qIndex(q: String) = when (q.uppercase()) {
        "Q1" -> 1; "Q2" -> 2; "Q3" -> 3; "Q4" -> 4; else -> null
    }

    private fun computeLatestTTM(qMap: Map<Int, Map<String, String>>): Pair<String, Double?>? {
        if (qMap.isEmpty()) return null
        val timeline = mutableListOf<Pair<Pair<Int,Int>, Double>>()
        qMap.toSortedMap().forEach { (year, qs) ->
            qs.forEach { (qlabel, vStr) ->
                val qi = qIndex(qlabel) ?: return@forEach
                val v  = parseB(vStr) ?: return@forEach
                timeline += (year to qi) to v
            }
        }
        if (timeline.isEmpty()) return null
        val sorted  = timeline.sortedWith(compareBy({ it.first.first }, { it.first.second }))
        val lastKey = sorted.last().first
        val lastNum = lastKey.first * 10 + lastKey.second
        val window  = sorted.filter { (yq, _) ->
            val n = yq.first * 10 + yq.second
            n in (lastNum - 3)..lastNum
        }
        val label = "TTM (Q${lastKey.second})"
        val sum   = if (window.size < 4) null else window.sumOf { it.second }
        return label to sum
    }

    private fun computeYTD(qMap: Map<Int, Map<String, String>>): Pair<String, Double?>? {
        if (qMap.isEmpty()) return null
        val latestYear = qMap.keys.maxOrNull() ?: return null
        val qs = qMap[latestYear].orEmpty()
        val lastQ = listOf("Q4","Q3","Q2","Q1").firstOrNull { qs.containsKey(it) } ?: return null
        val upto = when (lastQ) { "Q4"->4; "Q3"->3; "Q2"->2; "Q1"->1; else->1 }
        val sum = (1..upto).mapNotNull { qi -> parseB(qs["Q$qi"]) }.takeIf { it.isNotEmpty() }?.sum()
        return "YTD ($lastQ)" to sum
    }

    // ───── Key-Value 2열 표 (Valuation / Solvency / Dividend) ─────
    private fun renderKeyValueTables(d: StockDetailDto) = with(binding) {
        tblValuation.removeAllViews()
        tblSolvency.removeAllViews()
        tblDividend.removeAllViews()

        addKeyValueRows(
            tblValuation,
            "Current PE Ratio (Annualized)" to d.valuation.peAnnual,
            "Current PE Ratio (TTM)"        to d.valuation.peTtm,
            "Forward PE Ratio"              to d.valuation.forwardPe,
            "Current Price to Sales (TTM)"  to d.valuation.psTtm,
            "Current Price to Book Value"   to d.valuation.priceToBook,
            "Current Price to Cashflow (TTM)" to d.valuation.pcfTtm,
            "Current Price To Free Cashflow (TTM)" to d.valuation.pfcfTtm
        )

        addKeyValueRows(
            tblSolvency,
            "Current Ratio (Quarter)" to d.solvency.currentRatio,
            "Quick Ratio (Quarter)"   to d.solvency.quickRatio,
            "Debt to Equity Ratio (Quarter)" to d.solvency.debtToEquity
        )

        addKeyValueRows(
            tblDividend,
            "Dividend Yield"             to d.dividend.`yield`,
            "Payout Ratio"               to d.dividend.payoutRatio,
            "Latest Dividend Ex-Date"    to d.dividend.latestExDate
        )
    }

    // 공용 빌더들 ─────────────────────────────────────────────────────────
    private fun rowHeader(texts: List<String>): TableRow = TableRow(requireContext()).apply {
        layoutParams = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        texts.forEachIndexed { i, t -> addView(cell(t, header = true, alignEnd = i > 0)) }
    }

    private fun rowBody(texts: List<String>, emphasizeFirst: Boolean): TableRow =
        TableRow(requireContext()).apply {
            layoutParams = TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            texts.forEachIndexed { i, t ->
                addView(cell(t, bold = emphasizeFirst && i == 0, alignEnd = i > 0, emphasizeValue = i > 0))
            }
        }

    private fun cell(
        text: String?,
        header: Boolean = false,
        bold: Boolean = false,
        alignEnd: Boolean = false,
        emphasizeValue: Boolean = false,
        isLabel: Boolean = false,
        withFrame: Boolean = true
    ) = TextView(requireContext()).apply {
        this.text = text ?: "–"
        if (withFrame) setBackgroundResource(R.drawable.bg_table_cell)
        else setBackgroundColor(android.graphics.Color.TRANSPARENT)

        val hPad = if (isLabel) dp(6) else dp(8)
        setPadding(hPad, dp(6), dp(8), dp(6))

        if (header) {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            paint.isFakeBoldText = true
        } else {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }

        if (isLabel) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            isSingleLine = true
            maxLines = 1
            ellipsize = null
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.4f)
            gravity = Gravity.START
        } else {
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = if (alignEnd) Gravity.END else Gravity.START
        }
        if (bold || emphasizeValue) paint.isFakeBoldText = true
    }

    // 디바이더 (colorOutline 12% 알파)
    private fun makeDivider(): View = View(requireContext()).apply {
        layoutParams = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ).also { it.setMargins(0, dp(6), 0, 0) }
        val base  = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
        val alpha = (0.12f * 255).roundToInt()
        setBackgroundColor(ColorUtils.setAlphaComponent(base, alpha))
    }

    // Key-Value 표 빌더
    private fun addKeyValueRows(table: TableLayout, vararg pairs: Pair<String, String?>) {
        pairs.forEachIndexed { index, (k, v) ->
            table.addView(
                TableRow(requireContext()).apply {
                    layoutParams = TableLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    addView(cell(k, isLabel = true, withFrame = false))
                    addView(cell(v?.ifBlank { "–" }, alignEnd = true, emphasizeValue = true, withFrame = false))
                }
            )
            if (index != pairs.lastIndex) table.addView(makeDivider())
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** 상세 데이터 바인딩 */
    private fun bindDetail(d: StockDetailDto) = with(binding) {
        d.ticker?.let { tvticker.setOrDash(it) }
        d.name?.let { tvName.setOrDash(it) }
        d.price?.let { tvPrice.setOrDash(it) }

        if (d.change != null && d.changeRate != null) {
            val sign = if (d.change >= 0) "+" else "-"
            tvChange.text = "${sign}${dfChange.format(abs(d.change))} (${sign}${rateWithComma(d.changeRate)}%)"
            applyChangeColors(d.change >= 0)
        }

        tvMarketCap.text = formatKrwShort(d.marketCap)
        tvSharesOutstanding.text = formatKrwShort(d.sharesOutstanding)

        // 표/메타
        renderNetIncomeTable(d.netIncome)
        renderKeyValueTables(d)

        // 1) 맵 → 포인트 우선, 2) 비어있으면 배열형(chart)로 대체
        chartData = mapToChartPoints(d.priceFinancialInfo?.price)
            .takeIf { it.isNotEmpty() }
            ?: d.chart.orEmpty()

        if (chartData.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.setNoDataText(getString(R.string.no_chart_data))
            binding.btnGroupRange.isEnabled = false
        } else {
            binding.btnGroupRange.isEnabled = true
            renderChart(Range.M6)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}