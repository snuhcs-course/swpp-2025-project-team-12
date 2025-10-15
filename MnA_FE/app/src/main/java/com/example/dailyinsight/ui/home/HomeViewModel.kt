package com.example.dailyinsight.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.LoadResult
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.RecommendationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo = Repository()

    private val _state = MutableStateFlow<LoadResult<List<RecommendationDto>>>(LoadResult.Loading)
    val state: StateFlow<LoadResult<List<RecommendationDto>>> = _state

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            if (force) _state.value = LoadResult.Loading
            repo.today()
                .onSuccess { list ->
                    _state.value = if (list.isEmpty()) LoadResult.Empty else LoadResult.Success(list)
                }
                .onFailure { e ->
                    _state.value = LoadResult.Error(e)
                }
        }
    }

    init { load() }
}