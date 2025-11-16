package com.example.dailyinsight.ui.marketindex

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.database.CachedHistory
import com.example.dailyinsight.data.dto.StockIndexData
import com.example.dailyinsight.data.dto.StockIndexHistoryItem
import com.example.dailyinsight.data.repository.MarketIndexRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Data class to hold a single point for the chart
data class ChartDataPoint(val timestamp: Long, val closePrice: Float)

class StockIndexDetailViewModel(application: Application, private val stockIndexType: String) : AndroidViewModel(application) {

    private val repository = MarketIndexRepository()

    private val _stockIndexData = MutableLiveData<StockIndexData>()
    val stockIndexData: LiveData<StockIndexData> = _stockIndexData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // --- DB Flow를 관찰하는 LiveData (수정) ---

    // 1. DB Flow를 ViewModel에서 구독 (이것이 "단일 진실 공급원")
    private val cacheFlow: Flow<CachedHistory?> = repository.getHistoryCacheFlow(stockIndexType)

    // 2. 차트 데이터용 LiveData (cacheFlow에서 'data' 필드만 추출)
    val historicalData: LiveData<List<ChartDataPoint>> = cacheFlow
        .map { it?.data ?: emptyList() } // 캐시에서 'data' 리스트만 추출
        .map { parseHistoryListToChartPoints(it) } // 차트용으로 변환
        .asLiveData() // LiveData로 만듦

    // 3. 52주 최고가용 LiveData (cacheFlow에서 'yearHigh' 필드만 추출)
    val yearHigh: LiveData<Double?> = cacheFlow
        .map { it?.yearHigh } // 캐시에서 'yearHigh' 값만 추출
        .asLiveData()

    // 4. 52주 최저가용 LiveData (cacheFlow에서 'yearLow' 필드만 추출)
    val yearLow: LiveData<Double?> = cacheFlow
        .map { it?.yearLow } // 캐시에서 'yearLow' 값만 추출
        .asLiveData()

    init {
        // ViewModel이 생성되면 2가지 작업을 병렬로 실행
        loadHeaderData()   // 1. 헤더 (빠른 API)
        refreshChartData() // 2. 차트/52주 (느린 API -> DB 업데이트)
    }

    // 1. 헤더 데이터 로드 (빠른 API 호출)
    private fun loadHeaderData() {
        viewModelScope.launch {
            try {
                // (Repository가 name을 채워주므로 VM은 받기만 함)
                val latestDataMap = repository.getMarketData()
                latestDataMap[stockIndexType]?.let {
                    _stockIndexData.postValue(it)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to fetch header data: ${e.message}")
                Log.e("StockIndexDetailVM", "API Call Failed", e)
            }
        }
    }

    // 2. 차트 데이터를 백그라운드에서 새로고침 (DB 업데이트 트리거)
    private fun refreshChartData() {
        viewModelScope.launch {
            // (이 함수는 UI를 직접 업데이트하지 않고, DB를 업데이트함)
            // (DB가 업데이트되면 위의 'cacheFlow'가 자동으로 반응)
            repository.refreshHistoricalData(stockIndexType)
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