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

    // DB 데이터 + 관심 필터 결합
    val briefingList = kotlinx.coroutines.flow.combine(
        repo.getBriefingFlow(),
        _isFavoriteMode
    ) { list, isFavMode ->
        list.filter { item ->
            val matchFavorite = !isFavMode || item.isFavorite
            matchFavorite
        }
    }.map { entities -> entities.map { it.toDto() } }
        .asLiveData()

    init {
        refresh(SizeFilter.ALL, "market_cap") // 초기 데이터 로드 (전체 보기, 시총순)
    }
    //  새로고침 (offset = 0, DB 초기화)
    //  별표 클릭 시 호출
    fun toggleFavorite(item: RecommendationDto, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // DB 업데이트 & 서버 전송
            repo.toggleFavorite(item.ticker, isActive)
        }
    }

    //  "관심" 칩을 눌렀을 때 호출
    fun setFavoriteMode(enabled: Boolean) {
        _isFavoriteMode.value = enabled
        // 관심 모드일 때는 서버 페이징을 할 필요가 없으므로(로컬 DB 필터링)
        // 추가적인 fetch는 하지 않아도 됩니다.
    }

    // 산업 필터 업데이트
    fun updateIndustryFilter(industry: String?) {
        //_currentIndustry.value = industry
        // 산업 변경 시에는 서버 데이터를 다시 긁어와야 할 수도 있음
        //refresh(industry = industry)
    }
    fun refresh(filter: SizeFilter = sizeFilterMode, sort: String? = currentSort) {
        /*
        if (_isFavoriteMode.value) {
            isLoading.set(false)
            return
        }*/
        if (isLoading.getAndSet(true)) return
        viewModelScope.launch {
            // 상태 업데이트
            sizeFilterMode = filter
            currentSort = sort
            currentOffset = filter.startOffset
            //_currentIndustry.value = industry // 상태 동기화
            // 첫 페이지 로드 (DB 클리어)
            val asOf = repo.fetchAndSaveBriefing(offset = currentOffset, clear = true)
            if (asOf != null) { _asOfTime.value = asOf }
            isLoading.set(false)
        }
    }

    //  무한 스크롤 (다음 페이지 로드)
    fun loadNextPage() {
        //if (_isFavoriteMode.value) return
        if (isLoading.getAndSet(true)) return // 이미 로딩 중이면 무시
        // 필터 제한선 체크 (예: 대형주는 100개까지만)
        val limit = sizeFilterMode.endOffset
        if (limit != null && currentOffset + 10 >= limit) {
            isLoading.set(false)
            return
        }
        viewModelScope.launch {
            // 다음 페이지 (offset 증가)
            currentOffset += 10
            val asOf = repo.fetchAndSaveBriefing(offset = currentOffset, clear = false)
            // 다음 페이지 로드 시에도 시간이 오면 업데이트, 안 오면 유지
            if (asOf != null) { _asOfTime.value = asOf }
            isLoading.set(false)
        }
    }
    fun refreshSortOnly(sort: String) {
        refresh(sizeFilterMode, sort)
    }
    // 외부에서 현재 필터 상태 확인용
    fun getCurrentFilterMode(): SizeFilter = sizeFilterMode

    enum class SizeFilter(val startOffset: Int, val endOffset: Int?) {
        ALL(0, null), LARGE(0, 100), MID(100, 300), SMALL(300, null)
    }
}