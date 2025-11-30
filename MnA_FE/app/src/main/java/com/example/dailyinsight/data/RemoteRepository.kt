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
import com.example.dailyinsight.data.dto.PortfolioResponse
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
    override suspend fun fetchAndSaveBriefing(
        offset: Int,
        clear: Boolean,
        industry: String?,
        min: Int?,
        max: Int?
    ): String? = coroutineScope {
        try {
            // 1. [백업] 로컬 즐겨찾기 상태 백업
            val savedFavorites = briefingDao.getFavoriteTickers().toSet()
            // 2. API 호출
            val response = api.getBriefingList(
                limit = 10,
                offset = offset,
                sort = null, // (기본 정렬 사용)
                industry = industry,
                min = min,
                max = max
            )
            val items = response.items
            //  저장 시점의 기준 시간 (순서 보장용)
            val baseTime = System.currentTimeMillis()
            //  getStockReport 호출 없이, 받은 데이터로 바로 Entity 생성
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
        /// A. 로컬 DB 즉시 반영 (UX 우선)
        if (isActive) { briefingDao.insertFavorite(FavoriteTicker(ticker))
        } else { briefingDao.deleteFavorite(ticker) }
        briefingDao.syncFavorites() // 화면 갱신용
        // B. 서버 동기화 (로그인 상태라면 전송)
        // (여기서는 예외 처리를 꼼꼼히 해서 앱이 죽지 않게 함)
        try {
            // 현재 찜 목록 전체를 가져옴
            val currentList = briefingDao.getFavoriteTickers().toSet()
            // 서버 전송 (Set<String> 형태)
            val response = api.setPortfolio(PortfolioRequest(currentList))
            if (!response.isSuccessful) {
                // Log.e("Repo", "서버 저장 실패: ${response.code()}")
                // 실패해도 로컬은 유지 (다음 동기화 때 맞춰짐)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }
    override suspend fun clearUserData() {
        // 1. 찜 목록 테이블 비우기 (영구 저장소 초기화)
        briefingDao.clearAllFavorites()
        // 2. 현재 화면의 별표 모두 끄기
        briefingDao.uncheckAllFavorites()
    }

    // 서버의 관심 목록을 가져와서 로컬 DB와 동기화
    override suspend fun syncFavorites() {
        try {
            // 1. 서버에 요청
            val response = api.getPortfolio()
            if (response.isSuccessful) {
                val serverList = response.body()?.portfolio ?: emptyList()
                // 2. 로컬 DB 업데이트 (Transaction 느낌으로)
                val entities = serverList.map { FavoriteTicker(it) }
                briefingDao.replaceFavorites(entities) // 👈 clear + insert를 안전하게 수행
                // 3. DB에 없는 관심 종목 데이터 채워넣기 (Missing Data Fetching)
                // (이게 없으면 소형주 관심종목이 화면에 안 뜹니다!)
                val baseTime = System.currentTimeMillis()

                // 서버 목록 중 로컬 DB(briefing_cards)에 없는 놈들 찾기
                val missingTickers = serverList.filter { ticker ->
                    briefingDao.getCard(ticker) == null
                }

                if (missingTickers.isNotEmpty()) {
                    // 없는 놈들만 API 찔러서 정보 가져옴 (병렬 처리)
                    val missingCards = coroutineScope {
                        missingTickers.map { ticker ->
                            async {
                                try {
                                    // 상세 정보 API 활용 (가격 등 정보 획득)
                                    val detail = api.getStockReport(ticker)
                                    BriefingCardCache(
                                        ticker = ticker,
                                        name = detail.name ?: "",
                                        price = detail.current?.price ?: 0L,
                                        change = detail.current?.change ?: 0L,
                                        changeRate = detail.current?.changeRate ?: 0.0,
                                        headline = null, // 요약은 없을 수 있음
                                        label = null,
                                        confidence = null,
                                        fetchedAt = baseTime, // 정렬 순서는 뒤쪽으로
                                        isFavorite = true // 당연히 찜한 상태
                                    )
                                } catch (e: Exception) {
                                    null // 실패 시 건너뜀
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }

                    // DB에 추가
                    if (missingCards.isNotEmpty()) {
                        briefingDao.insertCards(missingCards)
                    }
                }

                // 4. 마지막으로 화면 갱신 (별표 색칠)
                briefingDao.syncFavorites()
            }
        } catch (e: Exception) {
            e.printStackTrace() // 실패하면 로컬 데이터 유지 (건드리지 않음)
        }
    }
}