package com.example.dailyinsight.data.repository

import android.util.Log
import com.example.dailyinsight.data.database.CachedHistory
import com.example.dailyinsight.data.database.HistoryCacheDao
import com.example.dailyinsight.data.dto.LLMSummaryData
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import com.example.dailyinsight.data.network.ApiService
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.di.ServiceLocator
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import com.google.gson.JsonElement
import com.google.gson.JsonParser

class MarketIndexRepository {

    // ServiceLocator에서 의존성 주입
    private val apiService: ApiService = ServiceLocator.api
    private val gson = Gson()
    private val historyCacheDao: HistoryCacheDao = ServiceLocator.historyCacheDao
    // ----- 외부 노출 API -----

    suspend fun getMarketData(): Map<String, StockIndexData> {
        val responseMap = apiService.getStockIndex().data
        responseMap.forEach { (key, stockIndexData) -> stockIndexData.name = key }
        return responseMap
    }

    // 1) 문자열로 받고 2) 한 번 디코딩한 뒤 3) 최종 DTO로 파싱
    suspend fun getLLMSummaryLatest(): LLMSummaryData {
        val raw = apiService.getLLMSummaryLatest().string()
        // 서버가 "{"asof_date":"...","basic_overview":"..."}" 를 문자열로 감싸 보내는 형태
        val innerJson = gson.fromJson(raw, String::class.java)   // 1차 디코딩(따옴표/이스케이프 제거)
        return gson.fromJson(innerJson, LLMSummaryData::class.java)
    }

    fun getHistoryCacheFlow(indexType: String): Flow<CachedHistory?> {
        return historyCacheDao.getHistoryCacheFlow(indexType)
    }

    suspend fun refreshHistoricalData(indexType: String) {
        try {
            val newData = apiService.getHistoricalData(indexType, 365).data
            if (newData.isNotEmpty()) {
                val yearHigh = newData.maxOfOrNull { it.close } ?: 0.0
                val yearLow = newData.minOfOrNull { it.close } ?: 0.0

                val cache = CachedHistory(
                    indexType = indexType,
                    data = newData,
                    yearHigh = yearHigh,
                    yearLow = yearLow,
                    lastFetched = System.currentTimeMillis() // 갱신 시간 기록
                )
                historyCacheDao.insertHistory(cache)
            }
        } catch (e: Exception) {
            Log.e("MarketIndexRepo", "refreshHistoricalData failed", e)
            // 실패해도 기존 캐시가 화면에 남아있음
        }
    }
}