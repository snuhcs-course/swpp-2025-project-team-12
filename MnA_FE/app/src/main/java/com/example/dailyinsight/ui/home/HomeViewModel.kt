package com.example.dailyinsight.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {

    private val _state =
        MutableStateFlow<LoadResult<List<RecommendationDto>>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<List<RecommendationDto>>> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _state.value = runCatching { repo.getTodayRecommendations() }
                .fold(
                    onSuccess = { LoadResult.Success(it) },
                    onFailure = { LoadResult.Error(it) }
                )
        }
    }

    init {
        refresh()
    }
}