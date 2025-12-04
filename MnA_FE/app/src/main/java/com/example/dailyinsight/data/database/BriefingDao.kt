package com.example.dailyinsight.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

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

    // 삭제 시 내 것만 삭제
    @Query("DELETE FROM favorite_tickers WHERE ticker = :ticker AND username = :username")
    suspend fun deleteFavorite(ticker: String, username: String)

    // 내 목록만 전체 삭제 (서버 동기화용)
    @Query("DELETE FROM favorite_tickers WHERE username = :username")
    suspend fun clearFavoritesForUser(username: String)

    @Query("SELECT ticker FROM favorite_tickers")
    suspend fun getAllFavoriteTickers(): List<String>

    // 내 찜 목록만 동기화 (내 거면 1, 남의 거거나 없으면 0)
    @Query("""
        UPDATE briefing_cards 
        SET isFavorite = (ticker IN (SELECT ticker FROM favorite_tickers WHERE username = :username))
    """)
    suspend fun syncFavorites(username: String)

    //  즐겨찾기 상태만 업데이트 (전체 덮어쓰기 X)
    @Query("UPDATE briefing_cards SET isFavorite = :isFavorite WHERE ticker = :ticker")
    suspend fun updateFavorite(ticker: String, isFavorite: Boolean)

    //  내 아이디에 해당하는 찜 목록만 가져오기
    @Query("SELECT ticker FROM favorite_tickers WHERE username = :username")
    suspend fun getFavoriteTickers(username: String): List<String>

    // 즐겨찾기 테이블 싹 비우기 (로그아웃 하거나 서버랑 맞출 때 사용)
    @Query("DELETE FROM favorite_tickers")
    suspend fun clearAllFavorites()

    // 리스트 통째로 넣기
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(items: List<FavoriteTicker>)

    //  특정 종목이 이미 DB에 있는지 확인용
    @Query("SELECT * FROM briefing_cards WHERE ticker = :ticker")
    suspend fun getCard(ticker: String): BriefingCardCache?

    // 트랜잭션 함수 추가
    // 지우고 다시 쓰는 동안에는 아무도 DB를 건드리지 못하게 잠급니다.
    @Transaction
    suspend fun replaceFavorites(items: List<FavoriteTicker>) {
        clearAllFavorites()
        insertFavorites(items)
    }

    // 모든 종목의 별표 해제 (로그아웃용)
    @Query("UPDATE briefing_cards SET isFavorite = 0")
    suspend fun uncheckAllFavorites()

    @Query("DELETE FROM briefing_cards WHERE ticker NOT IN (SELECT ticker FROM favorite_tickers)")
    suspend fun deleteNonFavorites()

    // [일반 목록용] rank가 있는 것만 가져옴 (필터링된 결과)
    @Query("SELECT * FROM briefing_cards WHERE rank IS NOT NULL ORDER BY rank ASC")
    fun getNormalListFlow(): Flow<List<BriefingCardCache>>

    // 관심 목록 정렬: 시가총액(marketCap) 내림차순 (NULL은 맨 뒤로)
    @Query("SELECT * FROM briefing_cards WHERE isFavorite = 1 ORDER BY marketCap DESC")
    fun getFavoriteListFlow(): Flow<List<BriefingCardCache>>

    // [초기화] 데이터를 지우지 않고 '순서(rank)'만 떼버림 (화면에서 숨김)
    @Query("UPDATE briefing_cards SET rank = NULL")
    suspend fun resetRanks()

    // [청소] 화면에도 안 나오고 찜도 안 한 데이터 삭제
    @Query("DELETE FROM briefing_cards WHERE rank IS NULL AND isFavorite = 0")
    suspend fun deleteGarbage()
}