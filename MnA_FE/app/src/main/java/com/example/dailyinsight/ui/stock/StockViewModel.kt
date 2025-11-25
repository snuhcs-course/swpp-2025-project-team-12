package com.example.dailyinsight.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean
import androidx.lifecycle.asLiveData

class StockViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {
    // 1. DB 데이터를 UI 데이터로 실시간 변환 (Source of Truth)
    // (DB가 업데이트되면 이 LiveData도 자동으로 업데이트됨)
    val briefingList = repo.getBriefingFlow()
        .map { entities -> entities.map { it.toDto() } }
        .asLiveData()
    // 2. 기준 시간
    private val _asOfTime = MutableStateFlow<String?>(null)
    val asOfTime: StateFlow<String?> = _asOfTime
    // 3. 페이징 상태 관리
    private var currentOffset = 0
    private val isLoading = AtomicBoolean(false) // 중복 호출 방지
    init {
        // 앱 켜자마자 초기 데이터(첫 페이지) 로드
        refresh()
    }
    //  새로고침 (offset = 0, DB 초기화)
    fun refresh() {
        if (isLoading.getAndSet(true)) return
        viewModelScope.launch {
            currentOffset = 0
            // 첫 페이지 로드 (DB 클리어)
            val asOf = repo.fetchAndSaveBriefing(offset = 0, clear = true)
            if (asOf != null) {
                _asOfTime.value = asOf
            }
            isLoading.set(false)
        }
    }

    //  무한 스크롤 (다음 페이지 로드)
    fun loadNextPage() {
        if (isLoading.getAndSet(true)) return // 이미 로딩 중이면 무시
        viewModelScope.launch {
            // 다음 페이지 (offset 증가)
            currentOffset += 10
            val asOf = repo.fetchAndSaveBriefing(offset = currentOffset, clear = false)
            // 다음 페이지 로드 시에도 시간이 오면 업데이트, 안 오면 유지
            if (asOf != null) {
                _asOfTime.value = asOf
            }
            isLoading.set(false)
        }
    }
}