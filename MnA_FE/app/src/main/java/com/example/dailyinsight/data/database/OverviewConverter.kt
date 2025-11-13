package com.example.dailyinsight.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
class OverviewConverter {
    private val gson = Gson()
    @TypeConverter
    fun toJson(payload: OverviewPayload): String = gson.toJson(payload)
    @TypeConverter
    fun fromJson(json: String): OverviewPayload =
        gson.fromJson(json, object: TypeToken<OverviewPayload>(){}.type)
}