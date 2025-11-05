package com.example.dailyinsight.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dailyinsight.data.dto.StockItem

class FakeStockRepository {

    // 실제로는 네트워크 / DB에서 가져올 데이터
    private val stockList = listOf(
        StockItem(5930, "SAMSUNG", "KOSPI"),
        StockItem(373220, "LG에너지솔루션", "KOSPI"),
        StockItem(660, "SK하이닉스", "KOSPI"),
        StockItem(35720, "KAKAO", "KOSPI"),
        StockItem(207940, "NAVER", "KOSPI"),
        StockItem(105560, "KB금융", "KOSPI"),
        StockItem(181710, "NH투자증권", "KOSPI"),
        StockItem(251270, "넷마블", "KOSDAQ"),
        StockItem(123456, "NVIDIA", "KOSPI")
    )

    private val _stocks = MutableLiveData<List<StockItem>>(stockList)
    val stocks: LiveData<List<StockItem>> get() = _stocks

    fun fetchStocks() {
        // TODO - get stock infos from the server

    }

    // 🔍 검색 기능 (LiveData 업데이트)
    fun searchStocks(query: String) {
        val filtered = if (query.isBlank()) stockList
        else stockList.filter { it.name.contains(query, ignoreCase = true) }
        _stocks.value = filtered
    }
}
