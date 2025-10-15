package com.example.dailyinsight.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyinsight.R
import com.example.dailyinsight.data.LoadResult
import com.example.dailyinsight.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.dailyinsight.ui.common.showSnack

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private val adapter by lazy { HistoryAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DashboardFragment.adapter
        }

        binding.swipe.setOnRefreshListener { viewModel.load(force = true) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                when (st) {
                    is LoadResult.Loading -> binding.swipe.isRefreshing = true
                    is LoadResult.Success -> {
                        binding.swipe.isRefreshing = false
                        adapter.submitList(st.data.toRows())
                    }
                    is LoadResult.Empty -> {
                        binding.swipe.isRefreshing = false
                        adapter.submitList(emptyList())
                        showSnack("기록이 없습니다.")
                    }
                    is LoadResult.Error -> {
                        binding.swipe.isRefreshing = false
                        showSnack("불러오기 실패: ${st.throwable.message ?: "알 수 없는 오류"}")
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