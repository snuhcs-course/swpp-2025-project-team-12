package com.example.dailyinsight.ui.common

import org.junit.Assert.*
import org.junit.Test

class LoadResultTest {

    @Test
    fun loading_isSingleton() {
        val loading1 = LoadResult.Loading
        val loading2 = LoadResult.Loading
        assertSame(loading1, loading2)
    }

    @Test
    fun success_withString() {
        val result = LoadResult.Success("test")
        assertEquals("test", result.data)
    }

    @Test
    fun empty_isSingleton() {
        val empty1 = LoadResult.Empty
        val empty2 = LoadResult.Empty
        assertSame(empty1, empty2)
    }

    @Test
    fun error_withException() {
        val exception = Exception("test")
        val result = LoadResult.Error(exception)
        assertEquals(exception, result.throwable)
    }
}
