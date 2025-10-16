package com.example.dailyinsight.ui.marketindex

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dailyinsight.R
import org.json.JSONObject
import java.io.InputStream

class MarketIndexViewModel(application: Application) : AndroidViewModel(application) {

    // LiveData to hold the parsed data for KOSPI and KOSDAQ
    private val _marketData = MutableLiveData<Map<String, StockIndexData>>()
    val marketData: LiveData<Map<String, StockIndexData>> = _marketData

    // Load the data when the ViewModel is created
    init {
        loadMarketData()
    }

    private fun loadMarketData() {
        val jsonData = readJsonFromRaw(R.raw.market_data)
        val parsedData = parseMarketData(jsonData)
        _marketData.value = parsedData
    }

    private fun readJsonFromRaw(resourceId: Int): String {
        val inputStream: InputStream = getApplication<Application>().resources.openRawResource(resourceId)
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseMarketData(jsonData: String): Map<String, StockIndexData> {
        val dataMap = mutableMapOf<String, StockIndexData>()
        val rootObject = JSONObject(jsonData)
        val dataObject = rootObject.getJSONObject("data")

        // Iterate through keys like "KOSPI", "KOSDAQ"
        dataObject.keys().forEach { key ->
            val indexObject = dataObject.getJSONObject(key)
            val stockIndex = StockIndexData(
                name = key,
                close = indexObject.getDouble("close"),
                changeAmount = indexObject.getDouble("change_amount"),
                changePercent = indexObject.getDouble("change_percent"),
                description = indexObject.getString("description")
            )
            dataMap[key] = stockIndex
        }
        return dataMap
    }
}