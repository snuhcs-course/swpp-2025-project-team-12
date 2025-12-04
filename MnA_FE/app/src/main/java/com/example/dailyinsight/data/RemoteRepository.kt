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
import kotlinx.coroutines.flow.first
import android.content.Context
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.datastore.CookieKeys
class RemoteRepository(
    private val api: ApiService,
    private val briefingDao: BriefingDao,
    private val stockDetailDao: StockDetailDao,
    private val context: Context
) : Repository {
    private val gson = Gson()

    // 💡 로그인한 유저네임 가져오기 (Helper)
    private suspend fun getCurrentUsername(): String {
        val prefs = context.cookieDataStore.data.first()
        return prefs[CookieKeys.USERNAME] ?: "guest" // 없으면 게스트
    }

    /* 1. DB 데이터를 관찰하는 Flow (화면 표시용)
    override fun getBriefingFlow(): Flow<List<BriefingCardCache>> {
        return briefingDao.getAllCards()
    }*/

    // Flow 분리 (ViewModel에서 골라 씀)
    override fun getBriefingFlow(): Flow<List<BriefingCardCache>> {
        return briefingDao.getNormalListFlow()
    }

    // 인터페이스에 추가 필요 (없으면 캐스팅해서 사용)
    override fun getFavoriteFlow(): Flow<List<BriefingCardCache>> {
        return briefingDao.getFavoriteListFlow()
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
            val username = getCurrentUsername() // 🚨 유저 확인
            val savedFavorites = briefingDao.getFavoriteTickers(username).toSet()
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
            if (clear) { //  새로고침이면: 모든 데이터의 순서표(rank)를 떼버림 (화면에서 안 보임, 데이터는 유지)
                briefingDao.resetRanks()
            }
            val entities = items.mapIndexed { index, item ->

                //  3. String -> Number 안전 변환
                val price = item.close?.toLongOrNull() ?: 0L
                val change = item.change?.toLongOrNull() ?: 0L
                val changeRate = item.changeRate?.toDoubleOrNull() ?: 0.0
                val isFav = savedFavorites.contains(item.ticker)

                // 🚨 [수정] 기존 데이터의 marketCap 보존 (API가 안 줄 경우 대비)
                val existing = briefingDao.getCard(item.ticker)
                val apiCap = item.marketCap ?: 0L
                val finalCap = if (apiCap > 0) apiCap else (existing?.marketCap ?: 0L)
                val savedIndustry = existing?.industry

                BriefingCardCache(
                    ticker = item.ticker,
                    name = item.name,
                    price = price,
                    change = change,
                    changeRate = changeRate,
                    headline = item.summary, // 요약 텍스트는 item.summary에서 가져옴
                    label = null, confidence = null, // (company-list JSON에 label, confidence는 없으므로 null 처리)
                    rank = offset + index,
                    fetchedAt = baseTime + offset + index, // 이번에 받아온 데이터에만 순서표(rank) 부여
                    marketCap = finalCap,
                    industry = savedIndustry,
                    isFavorite = isFav
                )
            }
            // (3) DB 트랜잭션
            if (clear) briefingDao.deleteGarbage()
            if (entities.isNotEmpty()) briefingDao.insertCards(entities)
            briefingDao.syncFavorites(username) // 동기화 (내 걸로만 색칠)
            return@coroutineScope response.asOf
        } catch (e: Exception) {
            e.printStackTrace()
            return@coroutineScope null
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
        // 상세 정보 가져올 때 시가총액도 같이 업데이트 (다음에 목록 볼 때 정렬이 잘되기 위해)
        val existing = briefingDao.getCard(ticker)
        val json = gson.toJson(detail)
        stockDetailDao.insertDetail(StockDetailCache(ticker, json, System.currentTimeMillis()))
        return detail
    }

    override suspend fun getStockOverview(ticker: String): StockOverviewDto {
        return api.getStockOverview(ticker)
    }

    //  즐겨찾기 토글 (Optimistic Update: DB 먼저 -> 서버 나중)
    override suspend fun toggleFavorite(ticker: String, isActive: Boolean): Boolean {
        val username = getCurrentUsername() // 유저 확인
        /// A. 로컬 DB 즉시 반영 (UX 우선)
        if (isActive) { briefingDao.insertFavorite(FavoriteTicker(ticker, username))
        } else { briefingDao.deleteFavorite(ticker, username) }
        briefingDao.syncFavorites(username) // 화면 갱신용
        // B. 서버 동기화 (로그인 상태라면 전송)
        // (여기서는 예외 처리를 꼼꼼히 해서 앱이 죽지 않게 함)
        if (username != "guest") {
            try {
                // 현재 찜 목록 전체를 가져옴
                val currentList = briefingDao.getFavoriteTickers(username)
                // 서버 전송 (Set<String> 형태)
                api.setPortfolio(PortfolioRequest(currentList))
            } catch (e: Exception) { e.printStackTrace() }
        }
        return true
    }
    override suspend fun clearUserData() {
        // 1. 찜 목록 테이블 비우기 (영구 저장소 초기화)
        briefingDao.clearAllFavorites()
        // 2. 현재 화면의 별표 모두 끄기
        briefingDao.uncheckAllFavorites()
    }

    // 서버의 관심 목록을 가져와서 로컬 DB와 동기화 (누락된 종목 살려내기)
    override suspend fun syncFavorites() {
        try {
            val username = getCurrentUsername() // 유저 확인
            if (username == "guest") return // 게스트는 서버 동기화 X
            // 1. 서버에 요청
            val response = api.getPortfolio()
            if (response.isSuccessful) {
                val serverList = response.body()?.portfolio ?: emptyList()
                android.util.Log.d("RemoteRepo", "Server Portfolio: $serverList") // 1. 서버 목록 확인
                // 1. 로컬 찜 목록(FavoriteTicker)을 서버 데이터로 덮어쓰기
                briefingDao.clearFavoritesForUser(username)
                if (serverList.isNotEmpty()) {
                    val entities = serverList.map { FavoriteTicker(it, username) }
                    briefingDao.insertFavorites(entities)
                }
                val needUpdateTickers = serverList.filter { ticker ->
                    val card = briefingDao.getCard(ticker)
                    // 카드가 없거나, 있어도 시총이 0이면 업데이트 대상
                    card == null || card.marketCap == 0L
                }
                // 3. DB에 없는 관심 종목 데이터 채워넣기 (Missing Data Fetching)
                // (이게 없으면 소형주 관심종목이 화면에 안 뜹니다!)
                val baseTime = System.currentTimeMillis()
                if (needUpdateTickers.isNotEmpty()) {
                    // 없는 놈들만 API 찔러서 정보 가져옴 (병렬 처리)
                    val missingCards = coroutineScope {
                        needUpdateTickers.map { ticker ->
                            async {
                                try {
                                    //  가격 정보 + 요약 정보 둘 다 가져오기
                                    val detailDeferred = async { api.getStockReport(ticker) }
                                    val overviewDeferred = async {
                                        try { api.getStockOverview(ticker) } catch (e: Exception) { null }
                                    }
                                    val detail = detailDeferred.await()
                                    val overview = overviewDeferred.await() // 요약 정보
                                    if (detail.name == null) {
                                        android.util.Log.e("RemoteRepo", "Name is NULL for $ticker")
                                    }
                                    BriefingCardCache(
                                        ticker = ticker,
                                        name = detail.name ?: "",
                                        price = detail.current?.price ?: 0L,
                                        change = detail.current?.change ?: 0L,
                                        changeRate = detail.current?.changeRate ?: 0.0,
                                        headline = overview?.summary, // 요약은 없을 수 있음
                                        label = null,
                                        confidence = null,
                                        fetchedAt = baseTime, // 정렬 순서는 뒤쪽으로
                                        marketCap = detail.current?.marketCap ?: 0L,
                                        industry = detail.industry,
                                        isFavorite = true, // 당연히 찜한 상태
                                        rank = null
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("RemoteRepo", "Failed to fetch $ticker", e)
                                    null // 실패 시 건너뜀
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    // DB에 추가
                    if (missingCards.isNotEmpty()) {
                        briefingDao.insertCards(missingCards)
                        android.util.Log.d("RemoteRepo", "Inserted ${missingCards.size} missing cards")
                    }
                }
                briefingDao.syncFavorites(username) // 4. 마지막으로 화면 갱신 (별표 색칠)
            }
        } catch (e: Exception) {
            e.printStackTrace() // 실패하면 로컬 데이터 유지 (건드리지 않음)
            android.util.Log.e("RemoteRepo", "Sync failed", e)
        }
    }
}