package com.example.dailyinsight.data

import android.content.Context
import com.example.dailyinsight.data.dto.ApiResponse
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
class MockRepository(private val context: Context) : Repository {

    private val gson = Gson()

    override suspend fun getTodayRecommendations(): List<RecommendationDto> =
        readWrapped<List<RecommendationDto>>("api_today.json")

    override suspend fun getStockRecommendations(): Map<String, List<RecommendationDto>> =
        readWrapped<Map<String, List<RecommendationDto>>>("api_history.json")

    override suspend fun getStockReport(ticker: String): StockDetailDto =
        readJson<StockDetailDto>("detail/stock_${ticker}.json") // assets/detail/stock_005930.json

    override suspend fun getStockOverview(ticker: String): StockOverviewDto {
        // (예: ticker가 "005930"이면 "assets/detail/overview_005930.json" 파일을 읽음)
        return readJson<StockOverviewDto>("detail/overview_${ticker}.json")
    }

    // --- helpers ---
    private inline fun <reified T> readWrapped(file: String): T {
        val text = readAsset(file)
        // Gson을 사용하여 ApiResponse<T> 타입으로 디코딩
        val type = object : TypeToken<ApiResponse<T>>() {}.type
        val wrapper = gson.fromJson<ApiResponse<T>>(text, type)
        return wrapper.data
    }

    private inline fun <reified T> readJson(file: String): T {
        val text = readAsset(file)
        // Gson을 사용하여 T 타입으로 바로 디코딩
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(text, type)
    }

    private fun readAsset(file: String): String =
        context.assets.open(file).bufferedReader().use { it.readText() }
}