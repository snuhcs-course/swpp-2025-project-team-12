package com.example.dailyinsight.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean
import androidx.lifecycle.asLiveData

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

    // DB 데이터 + 관심 필터 결합
    val briefingList = combine(
        repo.getBriefingFlow(),_filterState
    ) { list, state ->
        // 🚨 화면에 보여줄 때의 최종 필터링 (AND 조건)
        var result = list
        // [관심 모드] 켜져있으면 '별표 친 것'만 남김
        if (state.isFavMode) {
            result = result.filter { it.isFavorite }
        }
        // (참고: 산업/규모 필터링은 이미 API 호출 시점에 적용되어 DB에 들어옴.
        //  하지만 '살아남은 다른 찜 목록'을 가리고 싶다면 여기서 추가 필터링 가능.
        //  현재는 DB에 industry 정보가 없으므로 로컬 필터링 불가능 -> API 결과 신뢰)
        result
    }.map { entities -> entities.map { it.toDto() } }
        .asLiveData()

    init {
        loadData(reset = true) // 초기 데이터 로드 -
        //  로그인 상태라면 서버 관심 목록 동기화 (비로그인이면 401 에러 나거나 무시됨 -> 안전)
        viewModelScope.launch(Dispatchers.IO) { repo.syncFavorites() }
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

    fun refresh(filter: SizeFilter = sizeFilterMode, sort: String? = currentSort) {
        // 관심 모드 켜져있으면 -> 서버 호출 안 함 (로컬 DB에 있는 것만 보여줌)
        if (isLoading.getAndSet(true)) return
        viewModelScope.launch {
            // 상태 업데이트
            sizeFilterMode = filter
            currentSort = sort
            currentOffset = 0 // 필터링 시 offset은 항상 0부터 시작 (페이징은 서버가 함)
            // 1. 산업 파라미터 변환 ("IT|건설|화학")
            val industryParam = if (selectedIndustries.isEmpty()) null
            else selectedIndustries.joinToString("|")
            // 2. 규모 파라미터 변환 (Enum -> Int)
            val minParam = filter.minRank
            val maxParam = filter.maxRank
            // 3. API 호출 / 첫 페이지 로드 (DB 클리어
            val asOf = repo.fetchAndSaveBriefing(
                offset = currentOffset,
                clear = true,
                industry = industryParam,
                min = minParam,
                max = maxParam
            )
            if (asOf != null) { _asOfTime.value = asOf }
            isLoading.set(false)
        }
    }

    private fun loadData(reset: Boolean) {
        if (isLoading.getAndSet(true)) return

        viewModelScope.launch {
            val state = _filterState.value
            if (reset) { currentOffset = 0 }

            // 산업 파라미터 변환
            val industryParam = if (state.industries.isEmpty()) null
            else state.industries.joinToString("|")

            // API 호출 (DB 갱신)
            val asOf = repo.fetchAndSaveBriefing(
                offset = currentOffset,
                clear = reset,
                industry = industryParam,
                min = state.size.minRank,
                max = state.size.maxRank
            )

            if (asOf != null) _asOfTime.value = asOf
            isLoading.set(false)
        }
    }

    // 무한 스크롤
    fun loadNextPage() {
        if (isLoading.get()) return

        val state = _filterState.value
        val limit = state.size.maxRank

        // 제한선 체크
        if (limit != null && currentOffset + 10 >= limit) return

        currentOffset += 10
        loadData(reset = false) // 추가 로드
    }

    fun getCurrentFilterState() = _filterState.value

    // 기존 호환용 (Fragment에서 호출)
    fun refreshSortOnly(sort: String) = setSort(sort)
    fun refresh() = loadData(reset = true)
    fun getCurrentFilterMode() = _filterState.value.size
    fun updateIndustryFilter(industries: Set<String>) {
        // _filterState.value = _filterState.value.copy(industries = industries)
        // loadData(reset = true)
        // 위 코드가 주석 처리되어 있고 아래 setIndustryFilter를 호출하는지 확인 필요
        setIndustryFilter(industries)
    }
    fun getCurrentIndustries(): Set<String> = _filterState.value.industries

    enum class SizeFilter(val minRank: Int?, val maxRank: Int?) {
        ALL(null, null), LARGE(0, 100), MID(100, 300), SMALL(300, null)
    }
}