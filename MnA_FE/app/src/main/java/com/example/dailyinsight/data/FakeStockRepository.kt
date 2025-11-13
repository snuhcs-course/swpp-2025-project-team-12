package com.example.dailyinsight.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dailyinsight.data.dto.StockItem

class FakeStockRepository : StockRepository{

    // 실제로는 네트워크 / DB에서 가져올 데이터
    private val stockList = listOf(
        StockItem("5930", "SAMSUNG"),
        StockItem("373220", "LG에너지솔루션"),
        StockItem("660", "SK하이닉스"),
        StockItem("35720", "KAKAO"),
        StockItem("207940", "NAVER"),
        StockItem("105560", "KB금융"),
        StockItem("181710", "NH투자증권"),
        StockItem("251270", "넷마블"),
        StockItem("123456", "NVIDIA")
    )

    private val _stocks = MutableLiveData<List<StockItem>>(stockList)
    val stocks: LiveData<List<StockItem>> get() = _stocks

    override suspend fun fetchStocks(): List<StockItem> {
        return stockList
    }

    override suspend fun submitSelectedStocks(selected: Set<String>): Boolean {
        Log.d("Fake repo", "submitted: $selected")
        return true
    }
}
