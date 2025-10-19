package com.example.dailyinsight.ui.today

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyinsight.R
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.example.dailyinsight.databinding.FragmentTodayBinding

class TodayFragment : Fragment(R.layout.fragment_today) {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodayViewModel by viewModels()
    private val adapter = RecommendationAdapter { item ->
        val action = TodayFragmentDirections.actionTodayToStockDetail(item)
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTodayBinding.bind(view)

        binding.rvToday.layoutManager = LinearLayoutManager(requireContext())
        binding.rvToday.setHasFixedSize(true)
        binding.rvToday.adapter = adapter

        binding.swipe.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { st ->
                    when (st) {
                        is LoadResult.Loading -> binding.swipe.isRefreshing = true
                        is LoadResult.Success -> {
                            binding.swipe.isRefreshing = false
                            adapter.submitList(st.data)
                        }
                        is LoadResult.Error -> {
                            binding.swipe.isRefreshing = false
                            // TODO: error 표시
                        }
                        is LoadResult.Empty -> {
                            binding.swipe.isRefreshing = false
                            adapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}