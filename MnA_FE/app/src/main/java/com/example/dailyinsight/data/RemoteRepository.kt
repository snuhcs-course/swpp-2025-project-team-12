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
import com.example.dailyinsight.data.dto.PortfolioRequest
import com.example.dailyinsight.data.database.FavoriteTicker

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
            // [백업] DB를 지우기 전에, 현재 내가 찜한 종목들의 Ticker를 미리 가져옵니다.
            // (BriefingDao에 getFavoriteTickers() 함수가 있어야 합니다)
            val savedFavorites = briefingDao.getFavoriteTickers().toSet()
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
                val isFav = savedFavorites.contains(item.ticker)

                BriefingCardCache(
                    ticker = item.ticker,
                    name = item.name,
                    price = price,
                    change = change,
                    changeRate = changeRate,
                    headline = item.summary, // 요약 텍스트는 item.summary에서 가져옴
                    label = null, confidence = null, // (company-list JSON에 label, confidence는 없으므로 null 처리)
                    fetchedAt = baseTime + offset + index,
                    isFavorite = isFav
                )
            }
            // (3) DB 트랜잭션
            if (clear) briefingDao.clearAll()
            if (entities.isNotEmpty()) briefingDao.insertCards(entities)
            briefingDao.syncFavorites()
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

    //  즐겨찾기 토글 (Optimistic Update: DB 먼저 -> 서버 나중)
    override suspend fun toggleFavorite(ticker: String, isActive: Boolean): Boolean {
        //  로컬 DB 즉시 반영 (화면이 바로 바뀜)
        //briefingDao.updateFavorite(ticker, isActive)
        /*
        try {
            // 2. 현재 나의 모든 즐겨찾기 목록을 가져옴
            val currentFavorites = briefingDao.getFavoriteTickers().toMutableSet()

            // (혹시 모를 DB 갱신 시차 고려, 확실하게 처리)
            if (isActive) currentFavorites.add(ticker)
            else currentFavorites.remove(ticker)

            // 3. 서버에 통째로 전송 (SetPortfolio)
            // (주의: PortfolioRequest가 { "portfolio": ["005930", "000660"] } 형태여야 함)
            api.setPortfolio(PortfolioRequest(currentFavorites))
        } catch (e: Exception) {
            e.printStackTrace()
            // 실패하면? 나중에 다시 동기화하거나 조용히 넘어감 (UX 방해 X)
        }*/
        if (isActive) {
            briefingDao.insertFavorite(FavoriteTicker(ticker))
        } else {
            briefingDao.deleteFavorite(ticker)
        }
        // 화면 갱신을 위해 동기화
        briefingDao.syncFavorites()
        return true
    }
}