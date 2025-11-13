package com.example.dailyinsight.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BriefingDao {
    @Query("SELECT * FROM briefing_card_cache WHERE filterKey = :filterKey LIMIT 1")
    fun observe(filterKey: String): Flow<BriefingCardCache?>

    @Query("SELECT * FROM briefing_card_cache WHERE filterKey = :filterKey LIMIT 1")
    suspend fun getOnce(filterKey: String): BriefingCardCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: BriefingCardCache)
}