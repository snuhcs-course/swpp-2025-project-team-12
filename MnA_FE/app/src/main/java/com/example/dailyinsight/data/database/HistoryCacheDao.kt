package com.example.dailyinsight.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryCacheDao {

    /** 특정 지수(KOSPI/KOSDAQ)의 캐시 데이터를 Flow로 관찰 */
    @Query("SELECT * FROM stock_history_cache WHERE indexType = :indexType")
    fun getHistoryCacheFlow(indexType: String): Flow<CachedHistory?>

    /** 새 캐시 데이터를 삽입/덮어쓰기 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(cachedHistory: CachedHistory)

    /** 거래일/시간 스킵 판단용 1회 조회 (Cold Start 포함) */
    @Query("SELECT * FROM stock_history_cache WHERE indexType = :indexType LIMIT 1")
    suspend fun getHistoryCacheOnce(indexType: String): CachedHistory?
}