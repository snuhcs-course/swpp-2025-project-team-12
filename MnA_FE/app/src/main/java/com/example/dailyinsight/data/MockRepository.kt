package com.example.dailyinsight.data

import android.content.Context
import com.example.dailyinsight.data.dto.ApiResponse
import com.example.dailyinsight.data.dto.IndexDto
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.readJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
class MockRepository(private val context: Context) : Repository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getTodayRecommendations(): List<RecommendationDto> =
        readWrapped<List<RecommendationDto>>("api_today.json")

    override suspend fun getHistoryRecommendations(): Map<String, List<RecommendationDto>> =
        readWrapped<Map<String, List<RecommendationDto>>>("api_history.json")

    override suspend fun getMainIndices(): List<IndexDto> =
        readWrapped<List<IndexDto>>("api_indices.json")

    override suspend fun getStockDetail(ticker: String): StockDetailDto =
        readWrapped<StockDetailDto>("detail/stock_${ticker}.json") // assets/detail/stock_005930.jso

    // --- helpers ---
    private inline fun <reified T> readWrapped(file: String): T {
        val text = readAsset(file)
        // JSON은 { "data": ... } 형태라고 가정
        val wrapper = json.decodeFromString<ApiResponse<T>>(text)
        return wrapper.data
    }

    private fun readAsset(file: String): String =
        context.assets.open(file).bufferedReader().use { it.readText() }
}