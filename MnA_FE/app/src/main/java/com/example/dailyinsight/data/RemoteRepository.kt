package com.example.dailyinsight.data

import com.example.dailyinsight.data.database.BriefingCardCache
import com.example.dailyinsight.data.database.BriefingDao
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
import com.example.dailyinsight.data.network.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.example.dailyinsight.data.database.StockDetailCache
import com.example.dailyinsight.data.database.StockDetailDao
import com.google.gson.Gson

class RemoteRepository(
    private val api: ApiService,
    private val briefingDao: BriefingDao,
    private val stockDetailDao: StockDetailDao
) : Repository {
    private val gson = Gson()

    // 1. DB 데이터를 관찰하는 Flow (화면 표시용)
    override fun getBriefingFlow(): Flow<List<BriefingCardCache>> {
        return briefingDao.getAllCards()
    }

    // 2. 네트워크 호출 -> DB 저장 (ViewModel이 호출)
    override suspend fun fetchAndSaveBriefing(offset: Int, clear: Boolean): String? = coroutineScope {
        try {
            // (1) 목록 가져오기 (10개씩)
            val response = api.getBriefingList(limit = 10, offset = offset, sort = null)
            val items = response.items
            //  저장 시점의 기준 시간 (순서 보장용)
            val baseTime = System.currentTimeMillis()
            //  2. getStockReport 호출 없이, 받은 데이터로 바로 Entity 생성
            val entities = items.mapIndexed { index, item ->

                //  3. String -> Number 안전 변환
                val price = item.close?.toLongOrNull() ?: 0L
                val change = item.change?.toLongOrNull() ?: 0L
                val changeRate = item.changeRate?.toDoubleOrNull() ?: 0.0

                BriefingCardCache(
                    ticker = item.ticker,
                    name = item.name,
                    price = price,
                    change = change,
                    changeRate = changeRate,
                    headline = item.summary, // 요약 텍스트는 item.summary에서 가져옴
                    label = null, confidence = null, // (company-list JSON에 label, confidence는 없으므로 null 처리)
                    fetchedAt = baseTime + offset + index
                )
            }
            // (3) DB 트랜잭션
            if (clear) briefingDao.clearAll()
            if (entities.isNotEmpty()) briefingDao.insertCards(entities)
            response.asOf
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getStockReport(ticker: String): StockDetailDto {
        // 1. DB 확인
        val cached = stockDetailDao.getDetail(ticker)
        if (cached != null) {
            // 캐시가 있으면 JSON -> DTO 변환해서 즉시 반환
            return gson.fromJson(cached.json, StockDetailDto::class.java)
        }
        // 2. 없으면 API 호출 (그리고 DB 저장)
        val detail = api.getStockReport(ticker)
        val json = gson.toJson(detail)
        stockDetailDao.insertDetail(StockDetailCache(ticker, json, System.currentTimeMillis()))
        return detail
    }

    override suspend fun getStockOverview(ticker: String): StockOverviewDto {
        return api.getStockOverview(ticker)
    }
}