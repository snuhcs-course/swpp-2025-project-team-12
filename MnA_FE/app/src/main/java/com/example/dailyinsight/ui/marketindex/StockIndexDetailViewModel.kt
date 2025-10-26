package com.example.dailyinsight.ui.marketindex

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.dto.StockIndexHistoryItem // New import
import com.example.dailyinsight.data.repository.MarketIndexRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Data class to hold a single point for the chart
data class ChartDataPoint(val timestamp: Long, val closePrice: Float)

class StockIndexDetailViewModel(application: Application, private val stockIndexType: String) : AndroidViewModel(application) {

    private val repository = MarketIndexRepository()

    private val _stockIndexData = MutableLiveData<StockIndexData>()
    val stockIndexData: LiveData<StockIndexData> = _stockIndexData

    private val _historicalData = MutableLiveData<List<ChartDataPoint>>()
    val historicalData: LiveData<List<ChartDataPoint>> = _historicalData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // --- ADD LIVE DATA FOR 1-YEAR HIGH/LOW ---
    private val _yearHigh = MutableLiveData<Double>()
    val yearHigh: LiveData<Double> = _yearHigh

    private val _yearLow = MutableLiveData<Double>()
    val yearLow: LiveData<Double> = _yearLow
    // --- END ---

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Fetch latest data for the header (no changes here)
                val latestDataMap = repository.getMarketData()
                latestDataMap[stockIndexType]?.let {
                    _stockIndexData.postValue(it)
                }

                // Fetch 1 year (365 days) of historical data
                val historicalDataList = repository.getHistoricalData(stockIndexType, 365) // This now returns a List
                val chartPoints = parseHistoryListToChartPoints(historicalDataList) // Pass the list to the parser
                _historicalData.postValue(chartPoints)

                // --- ADD LOGIC TO CALCULATE 1-YEAR HIGH/LOW ---
                if (historicalDataList.isNotEmpty()) {
                    val yearHigh = historicalDataList.maxOfOrNull { it.close }
                    val yearLow = historicalDataList.minOfOrNull { it.close }
                    _yearHigh.postValue(yearHigh)
                    _yearLow.postValue(yearLow)
                }
                // --- END ---

            } catch (e: Exception) {
                _error.postValue("Failed to fetch data: ${e.message}")
                Log.e("StockIndexDetailVM", "API Call Failed", e)
            }
        }
    }

    // Renamed and updated to parse a List instead of a Map
    private fun parseHistoryListToChartPoints(data: List<StockIndexHistoryItem>): List<ChartDataPoint> {
        val points = mutableListOf<ChartDataPoint>()
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        data.forEach { item ->
            val date = dateParser.parse(item.date)
            date?.let {
                points.add(
                    ChartDataPoint(
                        timestamp = it.time,
                        closePrice = item.close.toFloat()
                    )
                )
            }
        }
        // Sorting is still a good practice to ensure the chart line connects points chronologically.
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