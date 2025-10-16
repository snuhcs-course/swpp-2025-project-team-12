package com.example.dailyinsight.ui.marketindex

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentStockIndexDetailBinding

class StockIndexDetailFragment : Fragment() {
    // Use the navArgs delegate to safely retrieve the argument
    private val args: StockIndexDetailFragmentArgs by navArgs()

    // It's good practice to use ViewBinding here as well
    private var _binding: FragmentStockIndexDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stock_index_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the stock index type from the arguments
        val stockIndexType = args.stockIndexType

        // Now you can use the value, for example, to set a TextView
        // Assuming you have a TextView with id 'textView_index_title' in your detail fragment layout
        // binding.textViewIndexTitle.text = stockIndexType

        // You can also use it to fetch the correct data from a ViewModel
        // e.g., viewModel.loadIndexData(stockIndexType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}