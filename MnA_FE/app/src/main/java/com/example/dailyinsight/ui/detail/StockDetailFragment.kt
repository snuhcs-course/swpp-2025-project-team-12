package com.example.dailyinsight.ui.detail

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentStockDetailBinding
import java.text.DecimalFormat
import kotlin.math.abs

class StockDetailFragment : Fragment(R.layout.fragment_stock_detail) {

    private var _binding: FragmentStockDetailBinding? = null
    private val binding get() = _binding!!

    // Safe Args
    private val args: StockDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStockDetailBinding.bind(view)

        val item = args.item

        val dfPrice = DecimalFormat("#,##0")
        val dfChange = DecimalFormat("#,##0")
        val dfRate = DecimalFormat("#0.##")

        val sign = if (item.change >= 0) "+" else "-"
        val priceStr = dfPrice.format(item.price)
        val changeStr = "${sign}${dfChange.format(abs(item.change))} (${sign}${dfRate.format(abs(item.changeRate))}%)"

        binding.tvName.text = item.name
        binding.tvCode.text = item.code
        binding.tvPrice.text = priceStr
        binding.tvChange.text = changeStr
        binding.tvTime.text = item.time
        binding.tvHeadline.text = item.headline ?: getString(R.string.no_headline)

        val colorRes = if (item.change >= 0) R.color.price_up else R.color.price_down
        binding.tvChange.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}