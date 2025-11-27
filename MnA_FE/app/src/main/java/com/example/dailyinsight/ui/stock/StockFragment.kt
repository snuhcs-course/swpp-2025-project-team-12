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
import com.example.dailyinsight.ui.stock.StockViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.model.Tag
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Button

class StockFragment : Fragment(R.layout.fragment_stock) {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockViewModel by viewModels()
    private lateinit var adapter: StockAdapter

    private val selectedIndustries = mutableSetOf<Tag>() // 선택된 산업들을 저장할 Set (다중 선택)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터 연결 (별표 클릭 시 ViewModel 호출)
        adapter = StockAdapter(
            onClick = { item ->
                val action = StockFragmentDirections.actionStockToStockDetail(item)
                findNavController().navigate(action)
            },
            onFavoriteClick = { item, isActive ->
                //  뷰모델에 토글 요청
                viewModel.toggleFavorite(item, isActive)
                // (옵션) 토스트 메시지
                val msg = if (isActive) "관심 종목에 추가되었습니다." else "관심 종목에서 해제되었습니다."
                //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )

        val layoutManager = LinearLayoutManager(context)
        binding.recycler.layoutManager = layoutManager
        binding.recycler.adapter = adapter
        restoreFilterUI() // 화면 복귀 시, 현재 필터 상태에 맞춰 버튼 글씨 복구
        // 칩(필터) 리스너 설정
        setupChipListeners()
        // 1. '규모' 칩 클릭 리스너 (팝업 메뉴 전용), 이게 있어야 이미 선택된 상태에서도 또 누르면 메뉴가 뜸.
        binding.chipSize.setOnClickListener {
            showSizePopupMenu(binding.chipSize)
        }
        // 2. 칩 그룹 리스너 (데이터 필터링 전용)
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            when (checkedId) {
                R.id.chipSize -> {
                    viewModel.refresh() // 현재 설정된 필터(대/중/소) 유지하며 새로고침
                }
                //R.id.chipInterest -> viewModel.refreshSortOnly("favorites")
                else -> {}
            }
        }
        // 3. 스크롤 리스너 (무한 스크롤 핵심)
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

        // 4. SwipeRefreshLayout (당겨서 새로고침)
        binding.swipe.setOnRefreshListener {
            viewModel.refresh()
            binding.swipe.isRefreshing = false
        }

        // 5. 데이터 관찰 (DB -> UI 자동 반영)
        viewModel.briefingList.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.recycler.visibility = View.GONE
                binding.tvEmptyMessage2.text = "조건에 맞는 종목이 없습니다."
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
                    // 시간이 아직 안 왔으면 현재 시간 표시 (임시)
                    val now = SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREA).format(Date())
                    binding.tvTime.text = "$now 기준"
                }
            }
        }
    }

    private fun setupChipListeners() {
        // 1. [관심 종목] 버튼 리스너 (독립적으로 동작)
        binding.chipInterest.setOnClickListener {
            val isChecked = binding.chipInterest.isChecked
            // 뷰모델에 "관심 모드 켜기/끄기" 요청
            viewModel.setFavoriteMode(isChecked)
        }

        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            // 관심 버튼은 이 그룹 로직에서 제외 (위에서 따로 처리함)
            if (checkedId == R.id.chipInterest) return@setOnCheckedStateChangeListener

            // 규모 칩 텍스트 복구
            if (checkedId != R.id.chipSize) {
                restoreFilterUI() // "규모 ▼" or "대형주 ▼" 복구
            }
            when (checkedId) {
                // 1. 관심 버튼 -> 로컬 필터링 모드 ON
                R.id.chipInterest -> {
                    viewModel.setFavoriteMode(true)
                }
                // 나머지 버튼들은 서버 API 호출
                else -> {
                    viewModel.setFavoriteMode(false) // 관심 모드 끄기
                    when (checkedId) {
                        R.id.chipSize -> {
                            // 팝업 메뉴는 OnClickListener에서 처리하므로 여기선 패스하거나 refresh()
                            // viewModel.refresh()
                        }
                    }
                }
            }
            restoreFilterUI()
        }
        binding.chipIndustry.setOnClickListener {
            showIndustryBottomSheet()
        }
        // 규모 버튼 클릭 시 팝업
        binding.chipSize.setOnClickListener {
            showSizePopupMenu(binding.chipSize)
        }
    }

    private fun showSizePopupMenu(anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_stock_size, popup.menu) // 메뉴 리소스 필요

        popup.setOnMenuItemClickListener { item ->
            // 메뉴 선택 시 '규모' 칩을 체크 상태로 변경
            if (!binding.chipSize.isChecked) {
                binding.chipSize.isChecked = true
            }
            when (item.itemId) {
                R.id.option_all -> {
                    viewModel.refresh(StockViewModel.SizeFilter.ALL)
                    binding.chipSize.text = "전체 ▼"
                }
                R.id.option_large -> {
                    viewModel.refresh(StockViewModel.SizeFilter.LARGE)
                    binding.chipSize.text = "대형주 ▼"
                }
                R.id.option_mid -> {
                    viewModel.refresh(StockViewModel.SizeFilter.MID)
                    binding.chipSize.text = "중형주 ▼"
                }
                R.id.option_small -> {
                    viewModel.refresh(StockViewModel.SizeFilter.SMALL)
                    binding.chipSize.text = "소형주 ▼"
                }
            }
            true
        }
        popup.show()
    }

    /* 산업 분류 팝업 메뉴 (Enum 활용)
    private fun showIndustryPopupMenu(anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)

        // 1. '전체' 옵션 수동 추가
        popup.menu.add(0, 0, 0, "전체 산업")

        // 2. Enum을 돌면서 메뉴 항목 자동 생성
        Tag.values().forEachIndexed { index, tag ->
            // itemId를 index + 1로 설정 (0은 전체)
            popup.menu.add(0, index + 1, index + 1, tag.korean)
        }

        popup.setOnMenuItemClickListener { item ->
            // 버튼 체크 상태 강제 적용
            if (!binding.chipIndustry.isChecked) {
                binding.chipIndustry.isChecked = true
            }

            if (item.itemId == 0) {
                // 전체 선택 시
                binding.chipIndustry.text = "산업 ▼"
                // viewModel.refresh(industry = null) -> 2단계 구현
                android.widget.Toast.makeText(context, "전체 산업 선택됨", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                // 특정 산업 선택 시
                val selectedTag = Tag.values()[item.itemId - 1]
                binding.chipIndustry.text = "${selectedTag.korean} ▼"

                // viewModel.refresh(industry = selectedTag) -> 2단계 구현
                android.widget.Toast.makeText(context, "${selectedTag.korean} 선택됨 (2단계 구현 예정)", android.widget.Toast.LENGTH_SHORT).show()
            }
            true
        }
        popup.show()
    } */

    // 바텀 시트 구현
    private fun showIndustryBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_industry_filter, null)
        dialog.setContentView(view)

        val container = view.findViewById<LinearLayout>(R.id.containerCheckBoxes)
        val btnApply = view.findViewById<Button>(R.id.btnApply)

        // 임시 선택 저장소 (취소하면 반영 안 되게)
        val tempSelected = HashSet(selectedIndustries)

        // Enum 돌면서 체크박스 동적 생성
        Tag.values().forEach { tag ->
            val checkBox = CheckBox(requireContext())
            checkBox.text = tag.korean
            checkBox.textSize = 16f
            checkBox.isChecked = tempSelected.contains(tag)

            // 체크 상태 변경 시 임시 저장소 업데이트
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) tempSelected.add(tag)
                else tempSelected.remove(tag)
            }
            container.addView(checkBox)
        }

        // [적용] 버튼 클릭
        btnApply.setOnClickListener {
            // 1. 실제 선택 변수에 반영
            selectedIndustries.clear()
            selectedIndustries.addAll(tempSelected)

            // 2. 칩 텍스트 업데이트
            if (selectedIndustries.isEmpty()) {
                binding.chipIndustry.text = "산업 ▼"
                binding.chipIndustry.isChecked = false
            } else {
                binding.chipIndustry.text = "산업 (${selectedIndustries.size}) ▼"
                binding.chipIndustry.isChecked = true
            }

            // 3. 데이터 갱신 요청 (2단계에서 구현할 API 호출 부분)
            // viewModel.refresh(industryFilter = selectedIndustries.toList())
            android.widget.Toast.makeText(context, "${selectedIndustries.size}개 산업 필터 적용", android.widget.Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }

        dialog.show()
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

    // UI 복구 함수
    private fun restoreFilterUI() {
        val currentMode = viewModel.getCurrentFilterMode()
        val text = when (currentMode) {
            StockViewModel.SizeFilter.LARGE -> "대형주 ▼"
            StockViewModel.SizeFilter.MID -> "중형주 ▼"
            StockViewModel.SizeFilter.SMALL -> "소형주 ▼"
            else -> "전체 ▼"
        }
        binding.chipSize.text = text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}