package com.example.dailyinsight.ui.marketindex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.databinding.FragmentMarketIndexBinding
import java.util.Locale

class MarketIndexFragment : Fragment() {

    private var _binding: FragmentMarketIndexBinding? = null
    private val binding get() = _binding!!
    private lateinit var marketIndexViewModel: MarketIndexViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        marketIndexViewModel = ViewModelProvider(this).get(MarketIndexViewModel::class.java)
        _binding = FragmentMarketIndexBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tell the Activity that this fragment has an options menu.
        setHasOptionsMenu(true)

        marketIndexViewModel.marketData.observe(viewLifecycleOwner) { dataMap ->
            // Update KOSPI UI
            dataMap["KOSPI"]?.let { kospiData ->
                updateIndexUI(
                    data = kospiData,
                    // Access TextViews via the included block's ID
                    nameView = binding.kospiBlock.name,
                    valueView = binding.kospiBlock.price,
                    changeView = binding.kospiBlock.priceChange,
                    descriptionView = binding.kospiBlock.description
                )
            }

            // Update KOSDAQ UI
            dataMap["KOSDAQ"]?.let { kosdaqData ->
                updateIndexUI(
                    data = kosdaqData,
                    // Access TextViews via the included block's ID
                    nameView = binding.kosdaqBlock.name,
                    valueView = binding.kosdaqBlock.price,
                    changeView = binding.kosdaqBlock.priceChange,
                    descriptionView = binding.kosdaqBlock.description
                )
            }
        }

        // "전반", "KOSPI", "KOSDAQ"의 모든 LLM 요약 텍스트를 업데이트
        // 로딩 중일 때 표시할 텍스트
        val loadingText = getString(R.string.placeholder_loading)
        // "전반" 텍스트 업데이트 (로딩/실패/성공)
        marketIndexViewModel.llmOverviewText.observe(viewLifecycleOwner) { overviewText ->
            binding.overviewBlock.MarketTitle.text = getString(R.string.label_overview)
            if (overviewText.isBlank()) {
                // API 로딩 중이거나 실패하면 모든 요약 필드를 "불러오는 중..."으로 설정
                binding.overviewBlock.MarketSummary.text = loadingText
                binding.kospiBlock.description.text = loadingText
                binding.kosdaqBlock.description.text = loadingText
            } else {
                // API 성공 시 "전반" 텍스트 설정
                binding.overviewBlock.MarketSummary.text = overviewText
            }
        }

        // KOSPI/KOSDAQ 개별 요약 텍스트 업데이트 (성공 시) (이 옵저버는 API가 성공할 때만 호출됩니다)
        marketIndexViewModel.llmSummary.observe(viewLifecycleOwner) { summaryData ->
            // KOSPI 요약 텍스트 (null이 아니고 비어있지 않으면)
            summaryData.kospi?.summary?.takeIf { it.isNotBlank() }?.let {
                binding.kospiBlock.description.text = it
            }

            // KOSDAQ 요약 텍스트 (null이 아니고 비어있지 않으면)
            summaryData.kosdaq?.summary?.takeIf { it.isNotBlank() }?.let {
                binding.kosdaqBlock.description.text = it
            }
        }

        // Set up click listeners
        binding.kospiBlock.root.setOnClickListener {
            // Navigation logic for KOSPI
            val action = MarketIndexFragmentDirections
                .actionNavigationMarketIndexToStockIndexDetailFragment(stockIndexType = "KOSPI")
            findNavController().navigate(action)
        }
        binding.kosdaqBlock.root.setOnClickListener {
            // Navigation logic for KOSDAQ
            val action = MarketIndexFragmentDirections
                .actionNavigationMarketIndexToStockIndexDetailFragment(stockIndexType = "KOSDAQ")
            findNavController().navigate(action)
        }

        marketIndexViewModel.error.observe(viewLifecycleOwner) { msg ->
            // TODO: Snackbar/Toast로 표시
        }
        /*
        marketIndexViewModel.marketData.observe(viewLifecycleOwner) { dataMap ->
            if (dataMap.isNullOrEmpty()) {
                // TODO: placeholder 노출(“데이터가 없습니다”) 또는 스켈레톤 유지
                return@observe
            }
            // 정확한 키 사용
            dataMap["KOSPI"]?.let { updateIndexUI(it, binding.kospiBlock.name, binding.kospiBlock.price, binding.kospiBlock.priceChange, binding.kospiBlock.description) }
            dataMap["KOSDAQ"]?.let { updateIndexUI(it, binding.kosdaqBlock.name, binding.kosdaqBlock.price, binding.kosdaqBlock.priceChange, binding.kosdaqBlock.description) }
        }*/
    }

    // Inflate the menu resource into the toolbar.
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_toolbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Optional: Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                // Handle notifications icon tap
                true
            }
            R.id.action_profile -> {
                // Handle profile icon tap
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateIndexUI(
        data: StockIndexData,
        nameView: android.widget.TextView,
        valueView: android.widget.TextView,
        changeView: android.widget.TextView,
        descriptionView: android.widget.TextView
    ) {
        nameView.text = data.name
        valueView.text = String.format(Locale.getDefault(), "%.2f", data.close)
        // change this to data.description later
        val sign = if (data.changeAmount >= 0) "+" else ""
        val changeText = String.format(
            Locale.getDefault(),
            "%s%.2f (%.2f%%)",
            sign,
            data.changeAmount,
            data.changePercent,
        )
        changeView.text = changeText

        // Set text color based on positive or negative change
        val colorRes = if (data.changeAmount >= 0) R.color.positive_red else R.color.negative_blue
        changeView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}