package com.example.dailyinsight.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.RemoteRepository
import com.example.dailyinsight.data.dto.HistoryItem
import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.dto.StockOverviewDto
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
    private val repo: Repository = RemoteRepository(ServiceLocator.api)
) : ViewModel() {

    // 1) 종목 상세 원본 상태
    private val _state = MutableStateFlow<LoadResult<StockDetailDto>>(LoadResult.Empty)
    val state: StateFlow<LoadResult<StockDetailDto>> = _state

    // 2) 주가 그래프 전용 상태
    private val _priceState = MutableStateFlow<LoadResult<PriceChartUi>>(LoadResult.Empty)
    val priceState: StateFlow<LoadResult<PriceChartUi>> = _priceState

    // 3) 요약/분석 텍스트용 상태
    private val _overviewState = MutableStateFlow<LoadResult<StockOverviewDto>>(LoadResult.Empty)
    val overviewState: StateFlow<LoadResult<StockOverviewDto>> = _overviewState

    private val historyDateParser = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    fun load(ticker: String) {
        // --- 1. 차트/테이블/기업개요 로드 (기존 로직 수정) ---
        viewModelScope.launch {
            _state.value = LoadResult.Loading
            _priceState.value = LoadResult.Loading

            runCatching { repo.getStockReport(ticker) } // getStockReport 호출
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

        // --- 2. 요약/분석 텍스트 로드 (별도 호출) ---
        viewModelScope.launch {
            _overviewState.value = LoadResult.Loading
            runCatching { repo.getStockOverview(ticker) } // getStockOverview 호출
                .onSuccess { overview ->
                    _overviewState.value = LoadResult.Success(overview)
                }
                .onFailure { e ->
                    _overviewState.value = LoadResult.Error(e)
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
            Entry(i.toFloat(), item.close.toFloat(), timestamp) // data에 타임스탬프 저장
        }

        val dataSet = LineDataSet(entries, "Close Price").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }
        return PriceChartUi(ChartUi(xLabels, LineData(dataSet)))
    }
}