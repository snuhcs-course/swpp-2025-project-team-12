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
}