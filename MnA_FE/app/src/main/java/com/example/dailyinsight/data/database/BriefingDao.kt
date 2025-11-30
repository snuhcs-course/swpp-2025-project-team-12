package com.example.dailyinsight.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BriefingDao {
    // 저장된 순서대로(fetchedAt) 가져오기
    @Query("SELECT * FROM briefing_cards ORDER BY fetchedAt ASC")
    fun getAllCards(): Flow<List<BriefingCardCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<BriefingCardCache>)

    @Query("DELETE FROM briefing_cards")
    suspend fun clearAll()

    // 즐겨찾기 전용 테이블 조작 함수
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(item: FavoriteTicker)

    @Query("DELETE FROM favorite_tickers WHERE ticker = :ticker")
    suspend fun deleteFavorite(ticker: String)

    @Query("SELECT ticker FROM favorite_tickers")
    suspend fun getAllFavoriteTickers(): List<String>

    // 브리핑 카드 + 즐겨찾기 조인 업데이트
    // (화면에 보이는 리스트의 별표 상태를 '즐겨찾기 테이블' 보고 동기화)
    @Query("""
        UPDATE briefing_cards 
        SET isFavorite = (ticker IN (SELECT ticker FROM favorite_tickers))
    """)
    suspend fun syncFavorites()

    //  즐겨찾기 상태만 업데이트 (전체 덮어쓰기 X)
    @Query("UPDATE briefing_cards SET isFavorite = :isFavorite WHERE ticker = :ticker")
    suspend fun updateFavorite(ticker: String, isFavorite: Boolean)

    //  현재 즐겨찾기된 Ticker 목록 (서버 전송용)
    @Query("SELECT ticker FROM briefing_cards WHERE isFavorite = 1")
    suspend fun getFavoriteTickers(): List<String>

    // 즐겨찾기 테이블 싹 비우기 (로그아웃 하거나 서버랑 맞출 때 사용)
    @Query("DELETE FROM favorite_tickers")
    suspend fun clearAllFavorites()

    // 리스트 통째로 넣기
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(items: List<FavoriteTicker>)

    //  특정 종목이 이미 DB에 있는지 확인용
    @Query("SELECT * FROM briefing_cards WHERE ticker = :ticker")
    suspend fun getCard(ticker: String): BriefingCardCache?
}