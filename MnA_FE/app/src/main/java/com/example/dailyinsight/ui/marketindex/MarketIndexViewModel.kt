package com.example.dailyinsight.ui.marketindex

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.dto.LLMSummaryData
import com.example.dailyinsight.data.repository.MarketIndexRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream

class MarketIndexViewModel : ViewModel() {

    private val repository = MarketIndexRepository()

    // LiveData to hold the fetched data
    private val _marketData = MutableLiveData<Map<String, StockIndexData>>()
    val marketData: LiveData<Map<String, StockIndexData>> = _marketData

    // LiveData for LLM summary data
    private val _llmSummary = MutableLiveData<LLMSummaryData>()
    val llmSummary: LiveData<LLMSummaryData> = _llmSummary

    private val _llmOverviewText = MutableLiveData<String>()
    val llmOverviewText: LiveData<String> = _llmOverviewText

    // LiveData for error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Load the data when the ViewModel is created
    init {
        fetchMarketData()
        fetchLLMSummary()
        preCacheChartData()
    }
    private fun fetchMarketData() {
        viewModelScope.launch {
            try {
                // The repository now returns the map directly
                val dataMap = repository.getMarketData()
                // Manually add the name (key) to each StockIndexData
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

    private fun fetchLLMSummary() {
        viewModelScope.launch {
            try {
                val summaryData = repository.getLLMSummaryLatest()
                _llmSummary.postValue(summaryData)
                val combined = buildString {
                    if (!summaryData.basicOverview.isNullOrBlank()) append(summaryData.basicOverview)
                    if (!summaryData.newsOverview.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(summaryData.newsOverview)
                    }
                }
                _llmSummary.postValue(summaryData)   // 필요하면 그대로도 보관
                _llmOverviewText.postValue(combined) // 화면에 뿌릴 텍스트
            } catch (e: Exception) {
                // Log error but don't show to user since this is supplementary data
                Log.e("MarketIndexViewModel", "Failed to fetch LLM summary: ${e.message}", e)
                // Optionally, you could post a default/fallback value
                _llmOverviewText.postValue("") // 실패 시 빈 값
            }
        }
    }

    // 상세 차트 화면을 위한 1년치 데이터를 미리 불러와 Room DB에 저장
    // 이 함수는 LiveData를 업데이트하지 않고, 조용히 DB만 채웁니다.
    private fun preCacheChartData() {
        viewModelScope.launch {
            try {
                // KOSPI 1년치 데이터를 불러와 DB에 저장
                repository.refreshHistoricalData("KOSPI")
                // KOSDAQ 1년치 데이터를 불러와 DB에 저장
                repository.refreshHistoricalData("KOSDAQ")
            } catch (e: Exception) {
                // 프리캐싱 실패는 사용자에게 알릴 필요 없음
                Log.w("MarketIndexViewModel", "Failed to pre-cache chart data: ${e.message}")
            }
        }
    }
}