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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * - ServiceLocator를 사용하도록 생성자 복구 (ViewModel과 호환)
 * - "거래일 + 평일 20:00 KST 이후"에만 갱신
 * - 평일 20:00 이전이라도 캐시가 없으면 1회 fetch
 */
class MarketIndexRepository {

    // ServiceLocator에서 의존성 주입
    private val apiService: ApiService = ServiceLocator.api
    private val historyCacheDao: HistoryCacheDao = ServiceLocator.historyCacheDao

    private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private val REFRESH_READY_TIME: LocalTime = LocalTime.of(20, 0) // 평일 20:00 이후 갱신

    // ----- 외부 노출 API -----

    suspend fun getMarketData(): Map<String, StockIndexData> {
        val responseMap = apiService.getStockIndex().data
        responseMap.forEach { (key, stockIndexData) -> stockIndexData.name = key }
        return responseMap
    }

    suspend fun getLLMSummary(): LLMSummaryData {
        val response = apiService.getLLMSummary()
        return Gson().fromJson(response.llmOutput, LLMSummaryData::class.java)
    }

    fun getHistoryCacheFlow(indexType: String): Flow<CachedHistory?> {
        return historyCacheDao.getHistoryCacheFlow(indexType)
    }

    /**
     * "거래일 + 평일 20:00 이후" 기준으로만 새 데이터 요청.
     * Cold Start: 평일 20:00 이전이라도 캐시가 없으면 1회 fetch
     * - 주말: 기대 거래일 = 직전 금요일, 해당 날짜로 이미 갱신되어 있으면 스킵
     */
    suspend fun refreshHistoricalData(indexType: String) {
        try {
            val cached = historyCacheDao.getHistoryCacheOnce(indexType)
            val nowKst = ZonedDateTime.now(KST)

            // 평일 20:00 이전: 당일 데이터 미확정 가정 → 캐시가 없으면 1회 fetch, 있으면 스킵
            if (isWeekday(nowKst) && nowKst.toLocalTime().isBefore(REFRESH_READY_TIME)) {
                if (cached == null) { // 👈 Cold Start 보완 (주석 해제됨)
                    fetchAndStore(indexType)
                }
                return
            }

            // 주말이면 기대 거래일을 직전 금요일로 보정, 평일이면 당일
            val expectedTradingDate = expectedTradingDateKst(nowKst)

            // 캐시가 있고, 마지막 갱신일이 기대 거래일과 같다면 네트워크 생략
            if (cached != null && toKstDate(cached.lastFetched) == expectedTradingDate) {
                return
            }

            // 여기까지 왔으면 갱신 대상 → 네트워크 호출
            fetchAndStore(indexType)

        } catch (e: Exception) {
            Log.e("MarketIndexRepo", "refreshHistoricalData failed", e)
            // (폴백 로직 없음)
        }
    }

    // ----- 내부 유틸 -----

    private suspend fun fetchAndStore(indexType: String) {
        // (API 호출 시 365일 고정)
        val newData = apiService.getHistoricalData(indexType, 365).data
        if (newData.isNotEmpty()) {
            val yearHigh = newData.maxOfOrNull { it.close } ?: 0.0
            val yearLow  = newData.minOfOrNull { it.close } ?: 0.0
            val cache = CachedHistory(
                indexType = indexType,
                data = newData,
                yearHigh = yearHigh,
                yearLow = yearLow,
                lastFetched = System.currentTimeMillis() // 현재 시간으로 저장
            )
            historyCacheDao.insertHistory(cache)
        }
    }

    private fun isWeekday(zdt: ZonedDateTime): Boolean {
        return when (zdt.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> false
            else -> true
        }
    }

    /** KST 기준 기대 거래일: 토/일이면 직전 금요일, 평일이면 그날 */
    private fun expectedTradingDateKst(now: ZonedDateTime): LocalDate {
        var d = now.toLocalDate()
        when (d.dayOfWeek) {
            DayOfWeek.SATURDAY -> d = d.minusDays(1)
            DayOfWeek.SUNDAY   -> d = d.minusDays(2)
            else -> { /* 평일은 그대로 */ }
        }
        return d
    }

    /** lastFetched(epoch millis) → KST LocalDate */
    private fun toKstDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(KST).toLocalDate()
}