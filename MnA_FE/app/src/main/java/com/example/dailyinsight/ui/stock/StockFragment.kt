package com.example.dailyinsight.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.FragmentStockBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

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

        val layoutManager = LinearLayoutManager(context)
        binding.recycler.layoutManager = layoutManager
        binding.recycler.adapter = adapter

        // 2. 스크롤 리스너 (무한 스크롤 핵심)
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 스크롤이 아래로 내려갔을 때만 체크 (dy > 0)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    // 바닥에 거의 다다랐을 때 (여유분 2개 정도 남기고)
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                        && firstVisibleItemPosition >= 0
                    ) {
                        viewModel.loadNextPage() // 다음 페이지 요청
                    }
                }
            }
        })

        // 3. SwipeRefreshLayout (당겨서 새로고침)
        binding.swipe.setOnRefreshListener {
            viewModel.refresh()
            binding.swipe.isRefreshing = false
        }

        // 4. 데이터 관찰 (DB -> UI 자동 반영)
        viewModel.briefingList.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.tvEmptyMessage2.visibility = View.VISIBLE
                binding.tvEmptyMessage2.text = getString(R.string.placeholder_loading)
                binding.recycler.visibility = View.GONE
            } else {
                binding.tvEmptyMessage2.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
                adapter.submitList(list)
            }
        }

        // 시간 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.asOfTime.collect { timeStr ->
                if (!timeStr.isNullOrBlank()) {
                    binding.tvTime.text = "${formatDate(timeStr)}"
                } else {
                    binding.tvTime.text = ""
                }
            }
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREA)
            val date = parser.parse(dateStr)
            date?.let { formatter.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}