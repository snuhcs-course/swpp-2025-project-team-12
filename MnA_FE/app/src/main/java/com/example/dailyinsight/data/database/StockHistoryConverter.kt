package com.example.dailyinsight.data.database

import androidx.room.TypeConverter
import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StockHistoryConverter {

    private val gson = Gson()
    private val type = object : TypeToken<List<StockIndexHistoryItem>>() {}.type

    /**
     * List -> String (DB에 저장할 때)
     */
    @TypeConverter
    fun fromStockHistoryItemList(list: List<StockIndexHistoryItem>): String {
        return gson.toJson(list, type)
    }

    /**
     * String -> List (DB에서 읽어올 때)
     */
    @TypeConverter
    fun toStockHistoryItemList(json: String): List<StockIndexHistoryItem> {
        return gson.fromJson(json, type)
    }
}