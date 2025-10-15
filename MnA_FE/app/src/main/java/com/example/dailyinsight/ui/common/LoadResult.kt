package com.example.dailyinsight.ui.common

sealed class LoadResult<out T> {
    data object Loading : LoadResult<Nothing>()
    data class Success<T>(val data: T) : LoadResult<T>()
    data object Empty : LoadResult<Nothing>()
    data class Error(val throwable: Throwable) : LoadResult<Nothing>()
}