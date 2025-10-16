package com.example.dailyinsight.ui.marketindex

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentStockIndexDetailBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockIndexDetailFragment : Fragment() {

    private lateinit var viewModel: StockIndexDetailViewModel
    private val args: StockIndexDetailFragmentArgs by navArgs()
    private var _binding: FragmentStockIndexDetailBinding? = null
    private val binding get() = _binding!!

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

        // The Fragment now only observes the data. It no longer calls load methods.
        viewModel.stockIndexData.observe(viewLifecycleOwner) { data ->
            data?.let {
                updateIndexUI(it)
            }
        }

        viewModel.historicalData.observe(viewLifecycleOwner) { dataPoints ->
            if (dataPoints.isNotEmpty()) {
                setupChart(dataPoints)
            }
        }
    }

    private fun updateIndexUI(data: StockIndexData) {
        binding.stockIndexItem.name.text = data.name
        binding.stockIndexItem.price.text = String.format(Locale.getDefault(), "%.2f", data.close)

        val sign = if (data.changeAmount >= 0) "+" else ""
        val changeText = String.format(
            Locale.getDefault(),
            "%s%.2f (%.2f%%)",
            sign,
            data.changeAmount,
            data.changePercent
        )
        binding.stockIndexItem.priceChange.text = changeText

        val colorRes = if (data.changeAmount >= 0) R.color.positive_red else R.color.negative_blue
        binding.stockIndexItem.priceChange.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun setupChart(dataPoints: List<ChartDataPoint>) {
        val entries = dataPoints.map { Entry(it.timestamp.toFloat(), it.closePrice) }

        val dataSet = LineDataSet(entries, "Stock Index Price").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_200)
            valueTextColor = Color.BLACK
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fade_blue)
        }

        val lineData = LineData(dataSet)

        binding.stockChart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = DateAxisValueFormatter()
                setDrawGridLines(false)
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DateAxisValueFormatter : ValueFormatter() {
    private val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
    override fun getFormattedValue(value: Float): String {
        return dateFormat.format(Date(value.toLong()))
    }
}