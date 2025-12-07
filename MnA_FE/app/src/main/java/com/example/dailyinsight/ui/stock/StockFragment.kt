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
import com.example.dailyinsight.ui.start.StartActivity
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.datastore.CookieKeys
import kotlinx.coroutines.flow.first
import android.content.Intent
import android.widget.Toast

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
                // 1. 비동기로 로그인 상태 확인
                viewLifecycleOwner.lifecycleScope.launch {
                    // DataStore에서 액세스 토큰 가져오기
                    val prefs = requireContext().cookieDataStore.data.first()
                    val accessToken = prefs[CookieKeys.ACCESS_TOKEN]
                    val isLoggedIn = !accessToken.isNullOrEmpty()

                    if (isLoggedIn) {
                        // 로그인 유저: 정상적으로 즐겨찾기 토글
                        viewModel.toggleFavorite(item, isActive)
                        // (옵션) 토스트 메시지
                        val msg = if (isActive) "관심 종목에 추가되었습니다." else "관심 종목에서 해제되었습니다."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        // 비로그인 유저: 로그인 화면으로 납치
                        Toast.makeText(requireContext(), "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()

                        // 로그인 화면(StartActivity)으로 이동
                        val intent = Intent(requireContext(), StartActivity::class.java)
                        startActivity(intent)
                        // 데이터 변경 없이 UI만 리프레시해서 체크박스를 원래대로 돌림
                        adapter.notifyDataSetChanged()
                    }
                }
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
                    //viewModel.refresh() // 현재 설정된 필터(대/중/소) 유지하며 새로고침
                }
                else -> {}
            }
        }
        // 3. 스크롤 리스너 (무한 스크롤 핵심)
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy) // 스크롤이 아래로 내려갔을 때만 체크 (dy > 0)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition() // 바닥에 거의 다다랐을 때 (여유분 2개 정도 남기고)
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
                binding.tvEmptyMessage2.visibility = View.VISIBLE
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
                } else { // 시간이 아직 안 왔으면 현재 시간 표시 (임시)
                    val now = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(Date())
                    binding.tvTime.text = "$now 기준"
                }
            }
        }
    }

    private fun setupChipListeners() { // 1. [관심 종목] 버튼 리스너 (독립적으로 동작)
        binding.chipInterest.setOnClickListener {
            val isChecked = binding.chipInterest.isChecked
            // 뷰모델에 "관심 모드 켜기/끄기" 요청
            viewModel.setFavoriteMode(isChecked)
        }
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId != R.id.chipSize) binding.chipSize.text = "규모 ▼"
            if (checkedId != R.id.chipIndustry) binding.chipIndustry.text = "산업 ▼"
            when (checkedId) {
                R.id.chipSize -> {
                    val currentSize = viewModel.getCurrentFilterState().size
                    viewModel.setSizeFilter(currentSize)
                }
                R.id.chipIndustry -> { /* 바텀시트에서 처리 */ }
                else -> viewModel.setSort("market_cap") // 선택 해제 시 기본
            }
            restoreFilterUI()
        }
        // 3. 팝업 및 바텀시트
        binding.chipSize.setOnClickListener { showSizePopupMenu(binding.chipSize) }
        binding.chipIndustry.setOnClickListener { showIndustryBottomSheet() }
    }

    private fun showSizePopupMenu(anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_stock_size, popup.menu) // 메뉴 리소스 필요
        popup.setOnMenuItemClickListener { item ->
            binding.chipSize.text = "${item.title} ▼"
            if (!binding.chipSize.isChecked) binding.chipSize.isChecked = true
            when (item.itemId) {
                R.id.option_all -> viewModel.setSizeFilter(StockViewModel.SizeFilter.ALL)
                R.id.option_large -> viewModel.setSizeFilter(StockViewModel.SizeFilter.LARGE)
                R.id.option_mid -> viewModel.setSizeFilter(StockViewModel.SizeFilter.MID)
                R.id.option_small -> viewModel.setSizeFilter(StockViewModel.SizeFilter.SMALL)
            }
            true
        }
        popup.show()
    }

    // 바텀 시트 구현
    private fun showIndustryBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_industry_filter, null)
        dialog.setContentView(view)
        val container = view.findViewById<LinearLayout>(R.id.containerCheckBoxes)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val tempSelected = HashSet(selectedIndustries) // 임시 선택 저장소 (취소하면 반영 안 되게)
        Tag.values().forEach { tag -> // Enum 돌면서 체크박스 동적 생성
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
            //  ViewModel에 산업 필터 변경 요청 (주석 해제 및 연결)
            val industryStrings = selectedIndustries.map { it.korean }.toSet()
            viewModel.setIndustryFilter(industryStrings) // Set<Tag> 전달
            Toast.makeText(context, "${selectedIndustries.size}개 산업 필터 적용", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
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
            StockViewModel.SizeFilter.ALL -> "전체 ▼"
            else -> " "
        }
        binding.chipSize.text = text
        //  산업 버튼 복구
        val industries = viewModel.getCurrentIndustries()
        if (industries.isEmpty()) {
            binding.chipIndustry.text = "산업 ▼"
            binding.chipIndustry.isChecked = false
        } else {
            binding.chipIndustry.text = "산업 (${industries.size}) ▼"
            binding.chipIndustry.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}