package com.example.dailyinsight.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.HistoryItem
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.dailyinsight.ui.common.chart.ChartUi
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.Locale

data class PriceChartUi(val chart: ChartUi)

class StockDetailViewModel(
    private val repo: Repository = ServiceLocator.repository
) : ViewModel() {

    // 1) 종목 상세 원본 상태
    private val _state = MutableStateFlow<LoadResult<StockDetailDto>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<StockDetailDto>> = _state

    // 2) 주가 그래프 전용 상태
    private val _priceState = MutableStateFlow<LoadResult<PriceChartUi>>(LoadResult.Empty)
    val priceState: StateFlow<LoadResult<PriceChartUi>> = _priceState

    private val historyDateParser = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    fun load(ticker: String) {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _priceState.value = LoadResult.Loading

            runCatching { repo.getStockDetail(ticker) }
                .onSuccess { detail ->
                    _state.value = LoadResult.Success(detail)
                    _priceState.value = runCatching { buildPriceChart(detail.history.orEmpty()) }
                        .fold(
                            onSuccess = { LoadResult.Success(it) },
                            onFailure = { LoadResult.Error(it) }
                        )
                }
                .onFailure { e ->
                    _state.value = LoadResult.Error(e)
                    _priceState.value = LoadResult.Error(e)
                }
        }
    }

    /** StockDetailDto → 차트 UI 모델 생성 */
    private fun buildPriceChart(history: List<HistoryItem>): PriceChartUi {
        require(history.isNotEmpty()) { "주가 데이터(history)가 비어 있습니다." }

        val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.KOREA)
        val xLabels = mutableListOf<String>()

        val entries = history.mapIndexed { i, item ->
            val timestamp = try {
                historyDateParser.parse(item.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
            xLabels.add(sdf.format(java.util.Date(timestamp))) // 라벨 추가
            Entry(i.toFloat(), item.close.toFloat(), timestamp) // ✅ [수정] data에 타임스탬프 저장
        }

        val dataSet = LineDataSet(entries, "Close Price").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }
        return PriceChartUi(ChartUi(xLabels, LineData(dataSet)))
    }
}