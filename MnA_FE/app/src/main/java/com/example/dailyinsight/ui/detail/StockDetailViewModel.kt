package com.example.dailyinsight.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StockDetailViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {

    private val _state = MutableStateFlow<LoadResult<StockDetailDto>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<StockDetailDto>> = _state

    fun load(code: String) {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _state.value = runCatching { repo.getStockDetail(code) }
                .fold(
                    onSuccess = { LoadResult.Success(it) },
                    onFailure = { LoadResult.Error(it) }
                )
        }
    }
}