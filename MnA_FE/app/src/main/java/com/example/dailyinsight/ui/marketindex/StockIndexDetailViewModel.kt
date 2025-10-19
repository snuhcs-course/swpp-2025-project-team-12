package com.example.dailyinsight.ui.marketindex

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dailyinsight.R
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

// Data class to hold a single point for the chart
data class ChartDataPoint(val timestamp: Long, val closePrice: Float)

class StockIndexDetailViewModel(application: Application, private val stockIndexType: String) : AndroidViewModel(application) {
    // Private MutableLiveData that can be updated within the ViewModel
    private val _stockIndexData = MutableLiveData<StockIndexData>()
    // Public LiveData that the Fragment can observe. This is read-only from the outside.
    val stockIndexData: LiveData<StockIndexData> = _stockIndexData

    // LiveData for the historical chart data
    private val _historicalData = MutableLiveData<List<ChartDataPoint>>()
    val historicalData: LiveData<List<ChartDataPoint>> = _historicalData

    // The init block is called when the ViewModel is created.
    // It immediately starts loading the data.
    init {
        loadData()
    }

    private fun loadData() {
        // Load the main index data (close, change, etc.)
        val marketJsonData = readJsonFromRaw(R.raw.market_data)
        val marketDataMap = parseMarketData(marketJsonData)
        _stockIndexData.value = marketDataMap[stockIndexType]

        // Load the historical data for the chart based on the index type
        val historicalResId = when (stockIndexType) {
            "KOSPI" -> R.raw.kospi_historical
            "KOSDAQ" -> R.raw.kosdaq_historical // Assuming you have a kosdaq_historical.json
            else -> R.raw.kospi_historical // Default case
        }
        val historicalJsonData = readJsonFromRaw(historicalResId)
        val historicalPoints = parseHistoricalData(historicalJsonData)
        _historicalData.value = historicalPoints
    }

    /**
     * Reads a raw resource file and returns its content as a String.
     */
    private fun readJsonFromRaw(resourceId: Int): String {
        val inputStream: InputStream = getApplication<Application>().resources.openRawResource(resourceId)
        return inputStream.bufferedReader().use { it.readText() }
    }

    // This function can be reused from your MarketIndexViewModel
    private fun parseMarketData(jsonData: String): Map<String, StockIndexData> {
        val dataMap = mutableMapOf<String, StockIndexData>()
        val rootObject = JSONObject(jsonData)
        val dataObject = rootObject.getJSONObject("data")

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

    private fun parseHistoricalData(jsonData: String): List<ChartDataPoint> {
        val points = mutableListOf<ChartDataPoint>()
        val jsonObject = JSONObject(jsonData)
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        jsonObject.keys().forEach { dateString ->
            val dayObject = jsonObject.getJSONObject(dateString)
            val closePrice = dayObject.getDouble("close").toFloat()
            val date = dateParser.parse(dateString)

            date?.let {
                points.add(ChartDataPoint(timestamp = it.time, closePrice = closePrice))
            }
        }
        return points.sortedBy { it.timestamp }
    }
}

// A ViewModelProvider Factory to create the ViewModel with the required parameter
class StockIndexDetailViewModelFactory(
    private val application: Application,
    private val stockIndexType: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockIndexDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockIndexDetailViewModel(application, stockIndexType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}