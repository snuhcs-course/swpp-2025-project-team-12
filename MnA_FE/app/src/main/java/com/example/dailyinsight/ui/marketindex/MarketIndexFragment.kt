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
        descriptionView.text = "hahaha"

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