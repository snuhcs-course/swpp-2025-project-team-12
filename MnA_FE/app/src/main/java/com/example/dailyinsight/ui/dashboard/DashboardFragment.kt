package com.example.dailyinsight.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.dailyinsight.R
import com.example.dailyinsight.ui.common.LoadResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.dailyinsight.ui.dashboard.HistoryAdapter
import com.example.dailyinsight.ui.dashboard.HistoryRow

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.recycler)
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe)

        adapter = HistoryAdapter { item ->
            val bundle = Bundle().apply { putParcelable("item", item) }
            findNavController().navigate(R.id.action_dashboard_to_stock_detail, bundle)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)

        rv.adapter = adapter

        swipe.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                when (st) {
                    is LoadResult.Loading -> swipe.isRefreshing = true
                    is LoadResult.Success -> {
                        swipe.isRefreshing = false
                        adapter.submitList(st.data)   // <- HistoryRow 리스트
                    }
                    is LoadResult.Empty -> {
                        swipe.isRefreshing = false
                        adapter.submitList(emptyList())
                        snackbar("기록이 없습니다.")
                    }
                    is LoadResult.Error -> {
                        swipe.isRefreshing = false
                        snackbar("불러오기 실패: ${st.throwable.message ?: "알 수 없는 오류"}")
                    }
                }
            }
        }

        viewModel.refresh()
    }

    private fun snackbar(msg: String) =
        com.google.android.material.snackbar.Snackbar
            .make(requireView(), msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
}