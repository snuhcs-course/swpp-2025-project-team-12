package com.example.dailyinsight.ui.common

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class LoadResultTest {

    // ===== Loading Tests =====

    @Test
    fun loading_isSingleton() {
        val loading1 = LoadResult.Loading
        val loading2 = LoadResult.Loading
        assertSame(loading1, loading2)
    }

    @Test
    fun loading_isLoadResult() {
        val loading: LoadResult<String> = LoadResult.Loading
        assertTrue(loading is LoadResult.Loading)
    }

    @Test
    fun loading_toString() {
        val result = LoadResult.Loading.toString()
        assertTrue(result.contains("Loading"))
    }

    @Test
    fun loading_hashCode_consistent() {
        assertEquals(LoadResult.Loading.hashCode(), LoadResult.Loading.hashCode())
    }

    // ===== Success Tests =====

    @Test
    fun success_withString() {
        val result = LoadResult.Success("test")
        assertEquals("test", result.data)
    }

    @Test
    fun success_withInt() {
        val result = LoadResult.Success(42)
        assertEquals(42, result.data)
    }

    @Test
    fun success_withList() {
        val list = listOf(1, 2, 3)
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
    fun success_hashCode() {
        val result1 = LoadResult.Success("test")
        val result2 = LoadResult.Success("test")
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun success_copy() {
        val original = LoadResult.Success("original")
        val copied = original.copy(data = "copied")
        assertEquals("copied", copied.data)
    }

    @Test
    fun success_toString() {
        val result = LoadResult.Success("test")
        assertTrue(result.toString().contains("Success"))
        assertTrue(result.toString().contains("test"))
    }

    @Test
    fun success_isLoadResult() {
        val success: LoadResult<String> = LoadResult.Success("test")
        assertTrue(success is LoadResult.Success)
    }

    @Test
    fun success_destructuring() {
        val result = LoadResult.Success("test")
        val (data) = result
        assertEquals("test", data)
    }

    @Test
    fun success_withDataClass() {
        data class TestData(val id: Int, val name: String)
        val testData = TestData(1, "Test")
        val result = LoadResult.Success(testData)
        assertEquals(1, result.data.id)
        assertEquals("Test", result.data.name)
    }

    // ===== Empty Tests =====

    @Test
    fun empty_isSingleton() {
        val empty1 = LoadResult.Empty
        val empty2 = LoadResult.Empty
        assertSame(empty1, empty2)
    }

    @Test
    fun empty_isLoadResult() {
        val empty: LoadResult<String> = LoadResult.Empty
        assertTrue(empty is LoadResult.Empty)
    }

    @Test
    fun empty_toString() {
        val result = LoadResult.Empty.toString()
        assertTrue(result.contains("Empty"))
    }

    @Test
    fun empty_hashCode_consistent() {
        assertEquals(LoadResult.Empty.hashCode(), LoadResult.Empty.hashCode())
    }

    // ===== Error Tests =====

    @Test
    fun error_withException() {
        val exception = Exception("test")
        val result = LoadResult.Error(exception)
        assertEquals(exception, result.throwable)
    }

    @Test
    fun error_withIOException() {
        val exception = IOException("Network error")
        val result = LoadResult.Error(exception)
        assertTrue(result.throwable is IOException)
        assertEquals("Network error", result.throwable.message)
    }

    @Test
    fun error_withRuntimeException() {
        val exception = RuntimeException("Runtime error")
        val result = LoadResult.Error(exception)
        assertTrue(result.throwable is RuntimeException)
    }

    @Test
    fun error_equality() {
        val exception = Exception("test")
        val result1 = LoadResult.Error(exception)
        val result2 = LoadResult.Error(exception)
        assertEquals(result1, result2)
    }

    @Test
    fun error_inequality_differentExceptions() {
        val result1 = LoadResult.Error(Exception("error1"))
        val result2 = LoadResult.Error(Exception("error2"))
        assertNotEquals(result1, result2)
    }

    @Test
    fun error_hashCode() {
        val exception = Exception("test")
        val result1 = LoadResult.Error(exception)
        val result2 = LoadResult.Error(exception)
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun error_copy() {
        val original = LoadResult.Error(Exception("original"))
        val newException = Exception("copied")
        val copied = original.copy(throwable = newException)
        assertEquals("copied", copied.throwable.message)
    }

    @Test
    fun error_toString() {
        val result = LoadResult.Error(Exception("test"))
        assertTrue(result.toString().contains("Error"))
    }

    @Test
    fun error_isLoadResult() {
        val error: LoadResult<String> = LoadResult.Error(Exception("test"))
        assertTrue(error is LoadResult.Error)
    }

    @Test
    fun error_destructuring() {
        val exception = Exception("test")
        val result = LoadResult.Error(exception)
        val (throwable) = result
        assertEquals(exception, throwable)
    }

    @Test
    fun error_withCause() {
        val cause = Exception("cause")
        val exception = Exception("test", cause)
        val result = LoadResult.Error(exception)
        assertEquals(cause, result.throwable.cause)
    }

    // ===== When Expression Tests =====

    @Test
    fun when_handlesAllCases() {
        val loading: LoadResult<String> = LoadResult.Loading
        val success: LoadResult<String> = LoadResult.Success("data")
        val empty: LoadResult<String> = LoadResult.Empty
        val error: LoadResult<String> = LoadResult.Error(Exception("error"))

        fun getState(result: LoadResult<String>): String = when (result) {
            is LoadResult.Loading -> "loading"
            is LoadResult.Success -> "success: ${result.data}"
            is LoadResult.Empty -> "empty"
            is LoadResult.Error -> "error: ${result.throwable.message}"
        }

        assertEquals("loading", getState(loading))
        assertEquals("success: data", getState(success))
        assertEquals("empty", getState(empty))
        assertEquals("error: error", getState(error))
    }

    // ===== Type Safety Tests =====

    @Test
    fun loadResult_maintainsTypeParameter() {
        val intResult: LoadResult<Int> = LoadResult.Success(42)
        val stringResult: LoadResult<String> = LoadResult.Success("test")

        assertTrue(intResult is LoadResult.Success<Int>)
        assertTrue(stringResult is LoadResult.Success<String>)

        assertEquals(42, (intResult as LoadResult.Success).data)
        assertEquals("test", (stringResult as LoadResult.Success).data)
    }

    @Test
    fun loadResult_canBeUsedWithGenerics() {
        fun <T> wrapInSuccess(data: T): LoadResult<T> = LoadResult.Success(data)

        val intResult = wrapInSuccess(42)
        val stringResult = wrapInSuccess("test")

        assertEquals(42, (intResult as LoadResult.Success).data)
        assertEquals("test", (stringResult as LoadResult.Success).data)
    }
}
