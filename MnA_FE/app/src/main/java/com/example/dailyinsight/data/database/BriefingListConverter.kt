package com.example.dailyinsight.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BriefingListConverter {
    private val gson = Gson()
    @TypeConverter
    fun toJson(items: List<BriefingCardItem>): String = gson.toJson(items)
    @TypeConverter
    fun fromJson(json: String): List<BriefingCardItem> =
        gson.fromJson(json, object: TypeToken<List<BriefingCardItem>>(){}.type)
}