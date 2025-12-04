package com.example.dailyinsight.data

import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import com.example.dailyinsight.data.database.BriefingCardCache
import kotlinx.coroutines.flow.Flow
interface Repository {

    //  DB에 저장된 브리핑 목록을 실시간으로 관찰
    fun getBriefingFlow(): Flow<List<BriefingCardCache>>
    // 네트워크에서 데이터를 받아와 DB에 저장 (offset: 페이징, clear: 새로고침 여부)
    suspend fun fetchAndSaveBriefing(
        offset: Int,
        clear: Boolean,
        industry: String? = null,
        min: Int? = null,
        max: Int? = null
    ): String?
    //suspend fun getStockList(offset: Int, sort: String?): Pair<List<RecommendationDto>, String?>
    suspend fun getStockReport(ticker: String): StockDetailDto
    suspend fun getStockOverview(ticker: String): StockOverviewDto
    suspend fun toggleFavorite(ticker: String, isActive: Boolean): Boolean

    suspend fun syncFavorites()

    suspend fun clearUserData()

    fun getFavoriteFlow(): Flow<List<BriefingCardCache>>
}