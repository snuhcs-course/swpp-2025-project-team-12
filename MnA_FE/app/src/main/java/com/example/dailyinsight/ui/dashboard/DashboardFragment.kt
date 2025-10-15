package com.example.dailyinsight.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.R
import com.example.dailyinsight.ui.common.LoadResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyinsight.data.dto.RecommendationDto
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.recycler)
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe)

        adapter = HistoryAdapter()

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)

        rv.adapter = adapter
        // ✅ 임시로 화면에 그려지는지 체크
        adapter.submitList(
            listOf(
                HistoryRow.Header("오늘"),
                HistoryRow.Item(
                    RecommendationDto(
                        code = "005930", name = "삼성전자",
                        price = 71400, change = -1200, changeRate = -1.65, time = "09:30",
                        headline = "반도체 업황 기대"
                    )
                )
            )
        )
        swipe.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                when (st) {
                    is LoadResult.Loading -> swipe.isRefreshing = true
                    is LoadResult.Success -> {
                        swipe.isRefreshing = false
                        adapter.submitList(st.data)
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

    private fun snackbar(msg: String) {
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
    }
}