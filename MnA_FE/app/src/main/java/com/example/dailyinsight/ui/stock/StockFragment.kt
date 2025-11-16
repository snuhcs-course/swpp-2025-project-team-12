package com.example.dailyinsight.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
// RecyclerView/SwipeRefreshLayout
import androidx.navigation.fragment.findNavController
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentStockBinding
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StockFragment : Fragment(R.layout.fragment_stock) {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockViewModel by viewModels()
    private lateinit var adapter: StockAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터 초기화 및 RecyclerView 설정 (binding 사용)
        adapter = StockAdapter { item ->
            val action = StockFragmentDirections.actionStockToStockDetail(item)
            findNavController().navigate(action)
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.setHasFixedSize(true)
        binding.recycler.adapter = adapter

        binding.swipe.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                binding.swipe.isRefreshing = st is LoadResult.Loading // 로딩 상태일 때만 true

                when (st) {
                    is LoadResult.Loading -> {
                        // 로딩 중일 때는 목록과 메시지 둘 다 숨길 수도 있음 (선택 사항)
                        binding.recycler.visibility = View.GONE
                        //binding.tvEmptyMessage.visibility = View.GONE
                    }
                    is LoadResult.Success -> {
                        // 성공 시: 메시지 숨기고 목록 보여줌
                        binding.tvEmptyMessage2.visibility = View.GONE
                        binding.recycler.visibility = View.VISIBLE
                        adapter.submitList(st.data)
                    }
                    is LoadResult.Empty -> {
                        // 비었을 때: 메시지 보여주고 목록 숨김
                        binding.tvEmptyMessage2.visibility = View.VISIBLE
                        binding.recycler.visibility = View.GONE
                        adapter.submitList(emptyList()) // 목록 비우기

                    }
                    is LoadResult.Error -> {
                        binding.recycler.visibility = View.GONE
                        binding.tvEmptyMessage2.text = "불러오기 실패"
                        snackbar("불러오기 실패: ${st.throwable.message ?: "알 수 없는 오류"}") // Snackbar 대신 TextView 사용 가능
                    }
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }

    private fun snackbar(msg: String) =
        com.google.android.material.snackbar.Snackbar
            .make(requireView(), msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()



}