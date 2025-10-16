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

    private fun TextView.setOrDash(v: String?) {
        text = v?.takeIf { it.isNotBlank() } ?: "–"
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
                        is LoadResult.Success -> bindDetail(st.data)
                        // 로딩/에러/빈 화면 처리는 필요 시 추가
                        else -> Unit
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
        // 레이아웃 id가 tvTicker / tvCode 등 프로젝트에 맞는 걸로 사용하세요.
        // 아래는 tvTicker 기준. 만약 tvticker 라면 이름만 바꾸면 됩니다.
        tvticker?.setOrDash(item.ticker) // safe-call: 레이아웃에 없으면 무시되도록
        tvPrice.text = priceStr
        tvChange.text = changeStr
        tvTime.text = item.time
        tvHeadline.setOrDash(item.headline ?: getString(R.string.no_headline))

        val colorRes = if (item.change >= 0) R.color.price_up else R.color.price_down
        tvChange.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    /** 상세 데이터 바인딩 (지금은 문자열 기반, ‘–’ 기본값) */
    private fun bindDetail(d: StockDetailDto) = with(binding) {
        // Summary
        tvMarketCap.setOrDash(d.marketCap)
        tvSharesOutstanding.setOrDash(d.sharesOutstanding)

        // Valuation
        tvPeAnnual.setOrDash(d.valuation?.peAnnual)
        tvPb.setOrDash(d.valuation?.priceToBook)

        // Solvency
        tvCurrentRatio.setOrDash(d.solvency?.currentRatio)
        tvDebtToEquity.setOrDash(d.solvency?.debtToEquity)

        // Dividend
        tvDividendYield.setOrDash(d.dividend?.yield)
        tvLatestExDate.setOrDash(d.dividend?.latestExDate)

        // ▼ 이후 섹션/필드 추가 시 라인만 계속 추가
        // ex) tvPer.setOrDash(d.per) ...
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}