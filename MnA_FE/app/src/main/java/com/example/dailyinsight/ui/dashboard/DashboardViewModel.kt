package com.example.dailyinsight.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.LoadResult
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.RecommendationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val repo = Repository()
    private val _state = MutableStateFlow<LoadResult<Map<String, List<RecommendationDto>>>>(LoadResult.Loading)
    val state: StateFlow<LoadResult<Map<String, List<RecommendationDto>>>> = _state

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            if (force) _state.value = LoadResult.Loading
            repo.history()
                .onSuccess { map ->
                    _state.value = if (map.isEmpty()) LoadResult.Empty else LoadResult.Success(map)
                }
                .onFailure { e -> _state.value = LoadResult.Error(e) }
        }
    }

    init { load() }
}