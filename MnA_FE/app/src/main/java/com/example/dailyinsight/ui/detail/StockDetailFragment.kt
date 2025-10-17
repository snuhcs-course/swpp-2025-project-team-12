package com.example.dailyinsight.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.databinding.FragmentStockDetailBinding
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.abs
import androidx.core.view.isVisible
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import com.example.dailyinsight.data.dto.NetIncome


class StockDetailFragment : Fragment(R.layout.fragment_stock_detail) {

    private var _binding: FragmentStockDetailBinding? = null
    private val binding get() = _binding!!

    // Safe Args: 목록에서 전달된 요약 아이템
    private val args: StockDetailFragmentArgs by navArgs()

    // 상세 API 로더
    private val viewModel: StockDetailViewModel by viewModels()

    // ---------- format helpers ----------

    private val dfPrice = DecimalFormat("#,##0")
    private val dfChange = DecimalFormat("#,##0")
    private val dfRate = DecimalFormat("#0.##")

    private fun TextView.setOrDash(v: String?) { text = v?.takeIf { it.isNotBlank() } ?: "–" }
    private fun TextView.setOrDash(v: Long?)   { text = v?.let { dfPrice.format(it) } ?: "–" }
    private fun TextView.setOrDash(v: Double?) { text = v?.let { dfRate.format(it) } ?: "–" }
    private fun TextView.setPercentOrDash(v: Double?) { text = v?.let { "${dfRate.format(it)}%" } ?: "–" }
    private fun applyChangeColors(isUpOrFlat: Boolean) {
        val up = ContextCompat.getColor(requireContext(), R.color.price_up)
        val down = ContextCompat.getColor(requireContext(), R.color.price_down)
        val color = if (isUpOrFlat) up else down
        binding.tvPrice.setTextColor(color)
        binding.tvChange.setTextColor(color)
        binding.tvChangeRate.setTextColor(color)
    }

    // "123456789000" -> "123.46억", "1.23조" 등으로 축약
    private fun formatKrwShort(raw: String?): String {
        if (raw.isNullOrBlank()) return "–"
        val cleaned = raw.replace(",", "").trim()
        val v = cleaned.toLongOrNull() ?: return raw  // 숫자 변환 실패 시 원문 반환

        val trillion = 1_000_000_000_000L // 1조
        val hundredMillion = 100_000_000L // 1억

        return when {
            kotlin.math.abs(v) >= trillion ->
                String.format("%.2f조", v / trillion.toDouble())
            kotlin.math.abs(v) >= hundredMillion ->
                String.format("%.2f억", v / hundredMillion.toDouble())
            else ->
                DecimalFormat("#,##0").format(v) + "원"
        }
    }
    // ------------------------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStockDetailBinding.bind(view)

        // 1) 목록에서 받은 요약 즉시 렌더
        val item = args.item
        renderHeader(item)

        // 2) 티커로 상세 데이터 로드
        viewModel.load(item.ticker)

        // 3) 상세 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { st ->
                    when (st) {
                        is LoadResult.Loading -> {
                            // 로딩 중: ProgressBar 보이기, 에러 숨기기, 내용은 그대로 두어도 OK
                            binding.progress.isVisible = true
                            binding.tvError.isVisible = false
                        }

                        is LoadResult.Error -> {
                            // 에러: ProgressBar 숨기고, 에러 텍스트 보여주기
                            binding.progress.isVisible = false
                            binding.tvError.isVisible = true
                            binding.tvError.text =
                                st.throwable.message ?: getString(R.string.error_generic)
                        }

                        is LoadResult.Success -> {
                            // 성공: ProgressBar/에러 숨기고, 데이터 바인딩
                            binding.progress.isVisible = false
                            binding.tvError.isVisible = false
                            bindDetail(st.data)
                        }

                        else -> Unit // Empty 등
                    }
                }
            }
        }
    }

    /** 상단 헤더(요약) 바인딩 */
    private fun renderHeader(item: RecommendationDto) = with(binding) {
        val sign = if (item.change >= 0) "+" else "-"
        val priceStr = dfPrice.format(item.price)
        val changeStr =
            "${sign}${dfChange.format(abs(item.change))} (${sign}${dfRate.format(abs(item.changeRate))}%)"

        tvName.text = item.name
        // 아래는 tvTicker 기준. 만약 tvticker 라면 이름만 바꾸면 됩니다.
        tvticker.setOrDash(item.ticker) // safe-call: 레이아웃에 없으면 무시되도록
        tvPrice.text = priceStr
        tvChange.text = changeStr
        tvTime.text = item.time
        tvHeadline.setOrDash(item.headline ?: getString(R.string.no_headline))

        val colorRes = if (item.change >= 0) R.color.price_up else R.color.price_down
        tvChange.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    // 표 생성: annual 먼저, 그 다음 quarter
    private fun renderNetIncomeTable(net: NetIncome?) = with(binding) {
        val table = tblNetIncome
        table.removeAllViews()  // 리바인딩 시 중복 방지

        val annual = net?.annual.orEmpty()
        val quarter = net?.quarter.orEmpty()

        // 데이터가 하나도 없으면 표 숨김
        table.isVisible = annual.isNotEmpty() || quarter.isNotEmpty()
        if (!table.isVisible) return

        // 1) 헤더
        table.addView(makeHeaderRow(table))

        // 2) Annual 섹션
        if (annual.isNotEmpty()) {
            table.addView(makeSectionRow(table, "Annual"))
            annual.take(MAX_ROWS_EACH).forEachIndexed { idx, pv ->
                table.addView(makeDataRow(table, pv.period, pv.value, zebra = idx % 2 == 1))
            }
        }

        // 3) Quarter 섹션
        if (quarter.isNotEmpty()) {
            table.addView(makeSectionRow(table, "Quarter"))
            quarter.take(MAX_ROWS_EACH).forEachIndexed { idx, pv ->
                table.addView(makeDataRow(table, pv.period, pv.value, zebra = idx % 2 == 1))
            }
        }
    }

    companion object {private const val MAX_ROWS_EACH = 6}  // Annual/Quarter 각각 최대 N행만 노출 (원하면 늘리세요)

// ── Row builders ──────────────────────────────────────────────────────────────

    private fun makeHeaderRow(parent: TableLayout): TableRow = TableRow(requireContext()).apply {
        layoutParams = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(6), 0, dp(2)) }

        addView(makeHeaderCell("Type"))
        addView(makeHeaderCell("Period"))
        addView(makeHeaderCell("Value"))
    }

    private fun makeSectionRow(parent: TableLayout, title: String): TableRow =
        TableRow(requireContext()).apply {
            layoutParams = TableLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(8), 0, dp(2)) }

            // 섹션 셀: Type만 채우고 나머지는 비움
            addView(makeSectionCell(title).apply {
                // 1열만 크고 굵게
            })
            addView(makeSectionCell(""))
            addView(makeSectionCell(""))
        }

    private fun makeDataRow(
        parent: TableLayout,
        period: String,
        value: String,
        zebra: Boolean
    ): TableRow = TableRow(requireContext()).apply {
        layoutParams = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(2), 0, dp(2)) }

        if (zebra) setBackgroundColor(requireContext().getColor(android.R.color.transparent))
        // 필요하면 연한 배경을 넣으세요: R.color.surfaceContainerLowest 등
        // 필요하면 지브라 배경 색 넣기
        // setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surfaceContainerLowest))

        addView(makeBodyCell("•"))         // Type 컬럼은 점(bullet)로 표시
        addView(makeBodyCell(period))
        addView(makeBodyCell(value))
    }

// ── Cell builders ─────────────────────────────────────────────────────────────

    private fun makeHeaderCell(text: String) = TextView(requireContext()).apply {
        this.text = text
        setPadding(dp(8), dp(6), dp(8), dp(6))
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
    }

    private fun makeSectionCell(text: String) = TextView(requireContext()).apply {
        this.text = text
        setPadding(dp(8), dp(6), dp(8), dp(6))
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
    }

    private fun makeBodyCell(text: String) = TextView(requireContext()).apply {
        this.text = text
        setPadding(dp(8), dp(6), dp(8), dp(6))
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
    }

    // ── dp helper ────────────────────────────────────────────────────────────────
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    /** 상세 데이터 바인딩 (지금은 문자열 기반, ‘–’ 기본값) */
    private fun bindDetail(d: StockDetailDto) = with(binding) {
        // ----- 헤더(상세 값이 있으면 요약값을 덮어씀) -----
        d.ticker?.let { tvticker.setOrDash(it) }
        d.name?.let { tvName.setOrDash(it) }
        d.price?.let { tvPrice.setOrDash(it) }
        d.change?.let { tvChange.setOrDash(it) }
        d.changeRate?.let { tvChangeRate.setPercentOrDash(it) }

        // 등락 색상 일관 적용
        d.change?.let { changeValue ->
            applyChangeColors(changeValue >= 0)
        }

        // ----- 요약 메타 -----
        tvMarketCap.setOrDash(d.marketCap)
        tvSharesOutstanding.setOrDash(d.sharesOutstanding)

        // ----- Valuation -----
        // d.valuation은 기본값 객체라 널 아님(네가 준 DTO 기준)
        tvPeAnnual.setOrDash(d.valuation.peAnnual)
        tvPeTtm.setOrDash(d.valuation.peTtm)
        tvForwardPe.setOrDash(d.valuation.forwardPe)
        tvPsTtm.setOrDash(d.valuation.psTtm)
        tvPriceToBook.setOrDash(d.valuation.priceToBook)   // JSON pb
        tvPcfTtm.setOrDash(d.valuation.pcfTtm)
        tvPfcfTtm.setOrDash(d.valuation.pfcfTtm)

        // ----- Solvency -----
        tvCurrentRatio.setOrDash(d.solvency.currentRatio)
        tvQuickRatio.setOrDash(d.solvency.quickRatio)
        tvDebtToEquity.setOrDash(d.solvency.debtToEquity)  // JSON de_ratio

        // ----- Dividend -----
        tvPayoutRatio.setOrDash(d.dividend.payoutRatio)
        tvDividendYield.setOrDash(d.dividend.`yield`)       // ← backtick 필수
        tvLatestExDate.setOrDash(d.dividend.latestExDate)

        // ----- (옵션) Net Income 표시: 최근 연간/분기 1개씩만 요약 -----
        val latestAnnual  = d.netIncome?.annual?.firstOrNull()?.let { "${it.period}: ${it.value}" }
        val latestQuarter = d.netIncome?.quarter?.firstOrNull()?.let { "${it.period}: ${it.value}" }
        tvNetIncomeAnnual?.setOrDash(latestAnnual)   // 레이아웃에 있으면 바인딩, 없으면 무시
        tvNetIncomeQuarter?.setOrDash(latestQuarter)
        renderNetIncomeTable(d.netIncome)   // ← 표 생성
        // 그래프는 제외 → graphPlaceholder는 그대로 둠
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}