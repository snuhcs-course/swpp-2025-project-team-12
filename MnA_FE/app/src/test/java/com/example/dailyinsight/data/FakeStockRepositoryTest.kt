package com.example.dailyinsight.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FakeStockRepositoryTest {

    private lateinit var repository: FakeStockRepository

    @Before
    fun setup() {
        repository = FakeStockRepository()
    }

    // ===== fetchStocks Tests =====

    @Test
    fun fetchStocks_returnsPresetStockList() = runTest {
        val result = repository.fetchStocks()

        assertTrue(result.isNotEmpty())
        assertEquals(9, result.size)
    }

    @Test
    fun fetchStocks_containsSamsung() = runTest {
        val result = repository.fetchStocks()

        val samsung = result.find { it.name == "SAMSUNG" }
        assertNotNull(samsung)
        assertEquals("5930", samsung?.ticker)
    }

    @Test
    fun fetchStocks_containsKakao() = runTest {
        val result = repository.fetchStocks()

        val kakao = result.find { it.name == "KAKAO" }
        assertNotNull(kakao)
        assertEquals("35720", kakao?.ticker)
    }

    @Test
    fun fetchStocks_containsNaver() = runTest {
        val result = repository.fetchStocks()

        val naver = result.find { it.name == "NAVER" }
        assertNotNull(naver)
        assertEquals("207940", naver?.ticker)
    }

    @Test
    fun fetchStocks_multipleCalls_returnsSameList() = runTest {
        val result1 = repository.fetchStocks()
        val result2 = repository.fetchStocks()

        assertEquals(result1, result2)
    }

    // ===== submitSelectedStocks Tests =====

    @Test
    fun submitSelectedStocks_alwaysReturnsTrue() = runTest {
        val selected = setOf("5930", "35720")

        val result = repository.submitSelectedStocks(selected)

        assertTrue(result)
    }

    @Test
    fun submitSelectedStocks_emptySet_returnsTrue() = runTest {
        val selected = emptySet<String>()

        val result = repository.submitSelectedStocks(selected)

        assertTrue(result)
    }

    @Test
    fun submitSelectedStocks_singleItem_returnsTrue() = runTest {
        val selected = setOf("5930")

        val result = repository.submitSelectedStocks(selected)

        assertTrue(result)
    }

    @Test
    fun submitSelectedStocks_allItems_returnsTrue() = runTest {
        val allTickers = repository.fetchStocks().map { it.ticker }.toSet()

        val result = repository.submitSelectedStocks(allTickers)

        assertTrue(result)
    }

}