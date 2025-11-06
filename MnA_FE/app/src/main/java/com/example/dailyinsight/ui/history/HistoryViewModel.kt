package com.example.dailyinsight.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {

    private val _state =
        MutableStateFlow<LoadResult<List<HistoryRow>>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<List<HistoryRow>>> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _state.value = runCatching { repo.getHistoryRecommendations() }
                .map { mapToRows(it) }
                .fold(
                    onSuccess = { LoadResult.Success(it) },
                    onFailure = { LoadResult.Error(it) }
                )
        }
    }

    private fun mapToRows(map: Map<String, List<RecommendationDto>>): List<HistoryRow> {
        val rows = mutableListOf<HistoryRow>()
        // 날짜 최신순(원하는 정렬 로직 적용)
        map.toSortedMap(compareByDescending { it }).forEach { (date, list) ->
            rows += HistoryRow.Header(date)
            rows += list.map { HistoryRow.Item(it) }
        }
        return rows
    }

    init {
        refresh()
    }
}