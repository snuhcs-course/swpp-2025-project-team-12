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

class StockViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {

    private val _state =
        MutableStateFlow<LoadResult<List<StockRow>>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<List<StockRow>>> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _state.value = runCatching { repo.getStockRecommendations() }
                .map { mapToRows(it) }
                .fold(
                    onSuccess = { LoadResult.Success(it) },
                    onFailure = { LoadResult.Error(it) }
                )
        }
    }

    private fun mapToRows(map: Map<String, List<RecommendationDto>>): List<StockRow> {
        val rows = mutableListOf<StockRow>()
        // 날짜 최신순(원하는 정렬 로직 적용)
        map.toSortedMap(compareByDescending { it }).forEach { (date, list) ->
            rows += StockRow.Header(date)
            rows += list.map { StockRow.Item(it) }
        }
        return rows
    }

    init {
        refresh()
    }
}