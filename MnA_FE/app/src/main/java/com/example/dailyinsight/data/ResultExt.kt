package com.example.dailyinsight.data

sealed class LoadResult<out T> {
    data object Loading : LoadResult<Nothing>()
    data class Success<T>(val data: T): LoadResult<T>()
    data class Error(val throwable: Throwable): LoadResult<Nothing>()
    data object Empty: LoadResult<Nothing>()
}