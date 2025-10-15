package com.example.dailyinsight.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentNotificationsBinding
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    // ✅ ViewModel 주입 (ServiceLocator.repository 사용)
    private val viewModel: NotificationsViewModel by viewModels {
        NotificationsVMFactory(ServiceLocator.repository)
    }

    private lateinit var indicesAdapter: IndicesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        // RecyclerView 세팅
        indicesAdapter = IndicesAdapter()
        binding.rvIndices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = indicesAdapter
            setHasFixedSize(true)
        }

        // 당겨서 새로고침
        binding.swipe.setOnRefreshListener { viewModel.refresh() }

        // 상태 수집 → UI 업데이트
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { st ->
                    when (st) {
                        is LoadResult.Loading -> {
                            binding.swipe.isRefreshing = true
                        }
                        is LoadResult.Success -> {
                            binding.swipe.isRefreshing = false
                            indicesAdapter.submitList(st.data)
                        }
                        is LoadResult.Empty -> {
                            binding.swipe.isRefreshing = false
                            indicesAdapter.submitList(emptyList())
                        }
                        is LoadResult.Error -> {
                            binding.swipe.isRefreshing = false
                            // TODO: 에러 스낵바/플레이스홀더 표시
                        }
                    }
                }
            }
        }

        // 첫 로딩
        viewModel.refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}