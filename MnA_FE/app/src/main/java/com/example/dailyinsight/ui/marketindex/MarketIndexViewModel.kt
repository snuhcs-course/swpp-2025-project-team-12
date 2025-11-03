package com.example.dailyinsight.ui.marketindex

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.repository.MarketIndexRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream

class MarketIndexViewModel : ViewModel() {

    private val repository = MarketIndexRepository()

    // LiveData to hold the fetched data
    private val _marketData = MutableLiveData<Map<String, StockIndexData>>()
    val marketData: LiveData<Map<String, StockIndexData>> = _marketData

    // LiveData for error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Load the data when the ViewModel is created
    init {
        fetchMarketData()
    }

    private fun fetchMarketData() {
        // Use viewModelScope to launch a coroutine

        viewModelScope.launch {
            try {
                // The repository now returns the map directly
                val dataMap = repository.getMarketData()

                // Manually add the name (key) to each StockIndexData object
                dataMap.forEach { (key, value) ->
                    value.name = key
                }

                _marketData.postValue(dataMap)
            } catch (e: Exception) {
                _error.postValue("Failed to fetch data: ${e.message}")
                Log.e("MarketIndexViewModel", "API Call Failed", e)
            }
        }
    }
}