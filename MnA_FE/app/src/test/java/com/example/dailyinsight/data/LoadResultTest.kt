package com.example.dailyinsight.data

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
    fun empty_isSingleton() {
        val empty1 = LoadResult.Empty
        val empty2 = LoadResult.Empty
        assertSame(empty1, empty2)
    }

    @Test
    fun success_holdsData() {
        val data = "test data"
        val result = LoadResult.Success(data)
        assertEquals(data, result.data)
    }

    @Test
    fun success_withInt() {
        val result = LoadResult.Success(42)
        assertEquals(42, result.data)
    }

    @Test
    fun success_withList() {
        val list = listOf("a", "b", "c")
        val result = LoadResult.Success(list)
        assertEquals(list, result.data)
        assertEquals(3, result.data.size)
    }

    @Test
    fun success_withNull() {
        val result = LoadResult.Success<String?>(null)
        assertNull(result.data)
    }

    @Test
    fun error_holdsThrowable() {
        val exception = RuntimeException("test error")
        val result = LoadResult.Error(exception)
        assertEquals(exception, result.throwable)
        assertEquals("test error", result.throwable.message)
    }

    @Test
    fun error_withDifferentExceptionTypes() {
        val ioException = java.io.IOException("network error")
        val result = LoadResult.Error(ioException)
        assertTrue(result.throwable is java.io.IOException)
    }

    @Test
    fun success_equality() {
        val result1 = LoadResult.Success("test")
        val result2 = LoadResult.Success("test")
        assertEquals(result1, result2)
    }

    @Test
    fun success_inequality() {
        val result1 = LoadResult.Success("test1")
        val result2 = LoadResult.Success("test2")
        assertNotEquals(result1, result2)
    }

    @Test
    fun error_equality() {
        val exception = RuntimeException("error")
        val result1 = LoadResult.Error(exception)
        val result2 = LoadResult.Error(exception)
        assertEquals(result1, result2)
    }

    @Test
    fun differentTypes_areNotEqual() {
        val success = LoadResult.Success("data")
        val loading = LoadResult.Loading
        val empty = LoadResult.Empty
        val error = LoadResult.Error(RuntimeException())

        assertNotEquals(success, loading)
        assertNotEquals(success, empty)
        assertNotEquals(success, error)
        assertNotEquals(loading, empty)
        assertNotEquals(loading, error)
        assertNotEquals(empty, error)
    }

    @Test
    fun success_hashCode_consistency() {
        val result1 = LoadResult.Success("test")
        val result2 = LoadResult.Success("test")
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun success_toString_containsData() {
        val result = LoadResult.Success("myData")
        assertTrue(result.toString().contains("myData"))
    }

    @Test
    fun error_toString_containsThrowable() {
        val result = LoadResult.Error(RuntimeException("myError"))
        assertTrue(result.toString().contains("RuntimeException"))
    }
}
