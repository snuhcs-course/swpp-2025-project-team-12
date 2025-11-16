package com.example.dailyinsight.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OverviewDao {
    @Query("SELECT * FROM stock_overview_cache WHERE symbol = :symbol LIMIT 1")
    fun observe(symbol: String): Flow<OverviewCache?>

    @Query("SELECT * FROM stock_overview_cache WHERE symbol = :symbol LIMIT 1")
    suspend fun getOnce(symbol: String): OverviewCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: OverviewCache)
}