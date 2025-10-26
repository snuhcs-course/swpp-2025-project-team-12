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
import com.example.dailyinsight.databinding.ViewStockChartWithRangeBinding
import com.example.dailyinsight.ui.common.chart.ChartViewController
import com.example.dailyinsight.ui.common.chart.ChartViewConfig
import java.text.SimpleDateFormat
import java.util.Locale

class StockIndexDetailFragment : Fragment() {

    private lateinit var viewModel: StockIndexDetailViewModel
    private val args: StockIndexDetailFragmentArgs by navArgs()
    private var _binding: FragmentStockIndexDetailBinding? = null
    private val binding get() = _binding!!
    private var chartViewController: ChartViewController? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockIndexDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the stock index type from navigation arguments
        val stockIndexType = args.stockIndexType

        // Create the ViewModel using the Factory, passing in the type
        val viewModelFactory = StockIndexDetailViewModelFactory(requireActivity().application, stockIndexType)
        viewModel = ViewModelProvider(this, viewModelFactory).get(StockIndexDetailViewModel::class.java)

        // Initialize chart controller
        setupChartController()

        // The Fragment now only observes the data. It no longer calls load methods.
        viewModel.stockIndexData.observe(viewLifecycleOwner) { data ->
            data?.let {
                updateIndexUI(it)
            }
        }

        viewModel.historicalData.observe(viewLifecycleOwner) { dataPoints ->
            // Convert to ChartDataPoint and pass to controller
            val chartData = dataPoints.map {
                com.example.dailyinsight.ui.common.chart.ChartDataPoint(
                    timestamp = it.timestamp,
                    value = it.closePrice
                )
            }
            chartViewController?.setData(chartData)
        }

        // --- ADD OBSERVERS FOR 1-YEAR HIGH/LOW ---
        viewModel.yearHigh.observe(viewLifecycleOwner) { high ->
            high?.let {
                binding.tvYearHighValue.text = String.format(Locale.getDefault(), "%.2f", it)
            }
        }

        viewModel.yearLow.observe(viewLifecycleOwner) { low ->
            low?.let {
                binding.tvYearLowValue.text = String.format(Locale.getDefault(), "%.2f", it)
            }
        }
        // --- END ---
    }

    private fun setupChartController() {
        // Bind the included chart view
        val chartBinding = ViewStockChartWithRangeBinding.bind(binding.chartView.root)

        // Config (ChartViewController creates its own X-axis formatter internally)
        val config = ChartViewConfig(
            lineColorRes = R.color.positive_red,
            fillDrawableRes = R.drawable.chart_fade_red,
            enableTouch = true,
            enableAxes = true
        )

        chartViewController = ChartViewController(
            context = requireContext(),
            lineChart = chartBinding.lineChart,
            btnGroupRange = chartBinding.btnGroupRange,
            btn1W = chartBinding.btn1W,
            btn3M = chartBinding.btn3M,
            btn6M = chartBinding.btn6M,
            btn9M = chartBinding.btn9M,
            btn1Y = chartBinding.btn1Y,
            config = config
        )
    }

    // --- UPDATE THIS ENTIRE FUNCTION ---
    private fun updateIndexUI(data: StockIndexData) {
        // Header Price
        binding.stockIndexItem.price.text = String.format(Locale.getDefault(), "%.2f", data.close)

        // Header Price Change
        val sign = if (data.changeAmount >= 0) "+" else ""
        val changeText = String.format(
            Locale.getDefault(),
            "%s%.2f (%.2f%%)",
            sign,
            data.changeAmount,
            data.changePercent
        )
        binding.stockIndexItem.priceChange.text = changeText

        // Header Price Change Color
        val colorRes = if (data.changeAmount >= 0) R.color.positive_red else R.color.negative_blue
        binding.stockIndexItem.priceChange.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // --- "시세" (Market Price) Section ---
        binding.tvMarketPriceDate.text = formatDate(data.date)
        binding.tvOpenValue.text = String.format(Locale.getDefault(), "%.2f", data.open)
        binding.tvCloseValue.text = String.format(Locale.getDefault(), "%.2f", data.close)
        binding.tvDayHighValue.text = String.format(Locale.getDefault(), "%.2f", data.high)
        binding.tvDayLowValue.text = String.format(Locale.getDefault(), "%.2f", data.low)
        binding.tvVolumeValue.text = formatVolume(data.volume)

        // Note: 1-Year High/Low (tvYearHighValue, tvYearLowValue) are set
        // by their own observers since they come from a different data source.
    }

    // --- ADD HELPER FUNCTIONS ---

    /**
     * Formats a "yyyy-MM-dd" date string to "M월 d일 기준".
     */
    private fun formatDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("M월 d일 기준", Locale.KOREA)
            val date = parser.parse(dateStr)
            formatter.format(date)
        } catch (e: Exception) {
            dateStr // Fallback to the original string on error
        }
    }

    /**
     * Formats a volume number into Korean units (e.g., "4억 210만주").
     */
    private fun formatVolume(volume: Long): String {
        val eok = volume / 100_000_000
        val man = (volume % 100_000_000) / 10_000

        val result = StringBuilder()
        if (eok > 0) {
            result.append("${eok}억 ")
        }
        if (man > 0 || eok == 0L) { // Show "만" if it's non-zero, or if "억" is zero
            result.append("${man}만")
        }
        result.append("주")
        return result.toString()
    }
    // --- END ---

    override fun onDestroyView() {
        chartViewController = null
        _binding = null
        super.onDestroyView()
    }
}