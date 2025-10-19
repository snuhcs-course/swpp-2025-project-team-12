package com.example.dailyinsight.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.di.ServiceLocator
import com.example.dailyinsight.ui.common.LoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.dailyinsight.data.mapper.PriceMapper
import com.example.dailyinsight.ui.common.chart.ChartUi
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

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

    fun load(ticker: String) {
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _priceState.value = LoadResult.Loading

            runCatching { repo.getStockDetail(ticker) }
                .onSuccess { detail ->
                    _state.value = LoadResult.Success(detail)
                    _priceState.value = runCatching { buildPriceChart(detail) }
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
    private fun buildPriceChart(detail: StockDetailDto): PriceChartUi {
        val points = PriceMapper.extractPricePoints(detail)  // Map<String, Double> → List<PricePoint>(정렬)
        require(points.isNotEmpty()) { "주가 데이터가 비어 있습니다." }

        val xLabels = points.map { it.date }
        val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.close.toFloat()) }

        val dataSet = LineDataSet(entries, "Close Price").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }
        return PriceChartUi(ChartUi(xLabels, LineData(dataSet)))
    }
}