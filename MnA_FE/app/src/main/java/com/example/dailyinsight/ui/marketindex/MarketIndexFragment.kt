package com.example.dailyinsight.ui.marketindex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentMarketIndexBinding

class MarketIndexFragment : Fragment() {

    private var _binding: FragmentMarketIndexBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val marketIndexViewModel =
            ViewModelProvider(this).get(MarketIndexViewModel::class.java)

        _binding = FragmentMarketIndexBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        marketIndexViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonToKospi.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_market_index_to_stockIndexDetailFragment)
        }

        binding.buttonToKosdaq.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_market_index_to_stockIndexDetailFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}