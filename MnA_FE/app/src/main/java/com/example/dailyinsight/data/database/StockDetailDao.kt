package com.example.dailyinsight.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StockDetailDao {
    @Query("SELECT * FROM stock_detail_cache WHERE ticker = :ticker")
    suspend fun getDetail(ticker: String): StockDetailCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(data: StockDetailCache)

    @Query("DELETE FROM stock_detail_cache")
    suspend fun clearAll()
}