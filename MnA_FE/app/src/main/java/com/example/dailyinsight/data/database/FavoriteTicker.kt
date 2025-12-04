package com.example.dailyinsight.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tickers")
data class FavoriteTicker(
    @PrimaryKey val ticker: String,
    val username: String,
    val timestamp: Long = System.currentTimeMillis() // 정렬용
)