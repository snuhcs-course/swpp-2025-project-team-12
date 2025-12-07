package com.example.dailyinsight.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.RemoteRepository
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.data.dto.RecommendationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

class StockViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {

    private val _asOfTime = MutableStateFlow<String?>(null)
    val asOfTime: StateFlow<String?> = _asOfTime
    // 페이징 상태 관리
    private var currentOffset = 0
    private var currentSort: String? = "market_cap" // 기본 정렬
    private var sizeFilterMode: SizeFilter = SizeFilter.ALL
    private val isLoading = AtomicBoolean(false) // 중복 호출 방지

    private val _isFavoriteMode = MutableStateFlow(false)

    // 선택된 산업들 (Set)
    private var selectedIndustries: Set<String> = emptySet()

    // 1. 필터 상태를 한 곳에서 관리하는 데이터 클래스
    data class FilterState(
        val size: SizeFilter = SizeFilter.ALL,
        val industries: Set<String> = emptySet(),
        val sort: String = "market_cap",
        val isFavMode: Boolean = false
    )

    // 2. 상태 관리 Flow
    private val _filterState = MutableStateFlow(FilterState())

    // 리스트 끝 도달 여부 (무한 오토 페이징 방지)
    private var isEndOfList = false

    val briefingList = _filterState.flatMapLatest { state ->
        val isFav = state.isFavMode
        // 필터가 하나라도 걸려있는지 확인 (규모가 전체가 아니거나, 산업이 선택되었거나)
        val hasFilter = state.size != SizeFilter.ALL || state.industries.isNotEmpty()

        if (isFav && !hasFilter) {
            // A. [관심 모드 + 필터 없음 (전체)]:
            // DB에 있는 '모든' 찜 목록을 가져옴 (시총순 정렬됨)
            (repo as RemoteRepository).getFavoriteFlow().map { list ->
                list.map { it.toDto() }
            }
        } else {
            // B. [관심 모드 + 필터 있음] OR [일반 모드]:
            // API가 필터링해서 준 '현재 화면 목록'을 사용
            repo.getBriefingFlow().map { list ->
                val dtos = list.map { it.toDto() }
                val filteredList = if (isFav) {
                    // API 결과(10개) 중에서 '내 찜'만 남김 (교집합)
                    dtos.filter { it.isFavorite }
                } else {
                    dtos
                }
                val minCount = 10
                // 관심 모드이고, 필터도 걸려있는데, 결과가 비었다? -> 다음 페이지 검색!
                // (list.isNotEmpty() 체크: DB가 비어있으면 로딩 전이므로 스킵)
                if (isFav && hasFilter && filteredList.size < minCount && list.isNotEmpty()) {
                    if (!isLoading.get() && !isEndOfList) {
                        loadNextPage() // "여기 없네? 더 가져와!"
                    }
                }

                filteredList
            }
        }
    }.asLiveData()

    init {
        refresh()
        //  로그인 상태라면 서버 관심 목록 동기화 (비로그인이면 401 에러 나거나 무시됨 -> 안전)
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearUserData()
            repo.syncFavorites()
        }
    }
    //  별표 클릭 시 호출
    fun toggleFavorite(item: RecommendationDto, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.toggleFavorite(item.ticker, isActive) // DB 업데이트 & 서버 전송
        }
    }

    //  "관심" 칩을 눌렀을 때 호출
    fun setFavoriteMode(enabled: Boolean) {
        _filterState.value = _filterState.value.copy(isFavMode = enabled)
        // 모드 변경 시에도 데이터 갱신 (필요 시)
        loadData(reset = true)
    }

    fun setSizeFilter(size: SizeFilter) {
        _filterState.value = _filterState.value.copy(size = size)
        loadData(reset = true)
    }

    fun setIndustryFilter(industries: Set<String>) {
        _filterState.value = _filterState.value.copy(industries = industries)
        loadData(reset = true)
    }

    fun setSort(sort: String) {
        _filterState.value = _filterState.value.copy(sort = sort)
        loadData(reset = true)
    }

    private fun loadData(reset: Boolean) {
        val state = _filterState.value
        val hasFilter = state.size != SizeFilter.ALL || state.industries.isNotEmpty()

        // A. [관심 모드 + 필터 없음] -> API 호출 안 함 (이미 syncFavorites로 다 가져왔으니까)
        if (state.isFavMode && !hasFilter) {
            isLoading.set(false)
            return
        }

        // B. [그 외] -> API 호출 (필터링된 데이터나 일반 목록 가져오기 위해)
        if (isLoading.getAndSet(true)) return

        viewModelScope.launch {
            if (reset) currentOffset = 0

            // 산업 파라미터
            val industryParam = if (state.industries.isEmpty()) null
            else state.industries.joinToString("|")

            // API 호출 (DB 갱신 - deleteNonFavorites 작동)
            val asOf = repo.fetchAndSaveBriefing(
                offset = currentOffset,
                clear = reset,
                industry = industryParam,
                min = state.size.minRank,
                max = state.size.maxRank
            )

            if (asOf != null) {
                _asOfTime.value = asOf
            } else {
                if (!reset) isEndOfList = true // 더 이상 데이터 없음
            }
            isLoading.set(false)
        }
    }

    // 무한 스크롤
    fun loadNextPage() {
        if (isLoading.get() || isEndOfList) return
        val state = _filterState.value

        // A. [관심 + 전체] -> 스크롤 안 함 (이미 다 있음)
        val hasFilter = state.size != SizeFilter.ALL || state.industries.isNotEmpty()
        if (state.isFavMode && !hasFilter) return

        val limit = state.size.maxRank
        if (limit != null && currentOffset + 10 >= limit) {
            isEndOfList = true
            return
        }
        currentOffset += 10
        loadData(reset = false) // 추가 로드
    }

    fun getCurrentFilterState() = _filterState.value
    fun refresh() = loadData(reset = true)
    fun getCurrentFilterMode() = _filterState.value.size
    fun getCurrentIndustries(): Set<String> = _filterState.value.industries
    enum class SizeFilter(val minRank: Int?, val maxRank: Int?) {
        ALL(null, null), LARGE(0, 100), MID(100, 300), SMALL(300, null)
    }
}