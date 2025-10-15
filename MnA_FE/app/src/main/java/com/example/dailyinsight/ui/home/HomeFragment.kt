package com.example.dailyinsight.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyinsight.R
import com.example.dailyinsight.data.LoadResult
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.dailyinsight.ui.common.showSnack
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val adapter by lazy { TodayAdapter() } // 아래 2) 어댑터 참고

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // RecyclerView 기본 설정
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
            adapter = this@HomeFragment.adapter
        }

        // 당겨서 새로고침
        binding.swipe.setOnRefreshListener { viewModel.load(force = true) }

        // 상태 구독
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                when (st) {
                    is LoadResult.Loading -> {
                        binding.swipe.isRefreshing = true
                    }
                    is LoadResult.Success -> {
                        binding.swipe.isRefreshing = false
                        adapter.submitList(st.data)
                    }
                    is LoadResult.Empty -> {
                        binding.swipe.isRefreshing = false
                        adapter.submitList(emptyList())
                        showSnack("표시할 추천이 없습니다.")
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