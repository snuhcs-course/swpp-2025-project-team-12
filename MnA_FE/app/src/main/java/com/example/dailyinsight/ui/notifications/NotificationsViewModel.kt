package com.example.dailyinsight.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.IndexDto
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repo: Repository
) : ViewModel() {

    private val _state =
        MutableStateFlow<LoadResult<List<IndexDto>>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<List<IndexDto>>> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            runCatching { repo.getMainIndices() }
                .onSuccess { _state.value = LoadResult.Success(it) }
                .onFailure { _state.value = LoadResult.Error(it) }
        }
    }
}