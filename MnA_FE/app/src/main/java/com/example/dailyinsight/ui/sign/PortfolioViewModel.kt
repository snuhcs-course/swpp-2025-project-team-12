package com.example.dailyinsight.ui.sign

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dailyinsight.data.FakeStockRepository
import com.example.dailyinsight.data.dto.StockItem

class PortfolioViewModel(
    private val repository: FakeStockRepository
) : ViewModel() {

    val stockItems: LiveData<List<StockItem>> = repository.stocks

    // 🔹 선택된 종목 ticker 모음
    private val _selectedTickers = MutableLiveData<Set<Int>>(emptySet())
    val selectedTickers: LiveData<Set<Int>> get() = _selectedTickers

    private val _selectNone = MutableLiveData(false)
    val selectNone: LiveData<Boolean> get() = _selectNone

    private var previousSelection: Set<Int> = emptySet()

    // 🔹 검색어에 따라 리스트 필터링
    fun searchStocks(query: String) {
        repository.searchStocks(query)
    }

    // 🔹 체크박스 선택/해제 이벤트 처리
    fun toggleSelection(ticker: Int, isChecked: Boolean) {
        val current = _selectedTickers.value?.toMutableSet() ?: mutableSetOf()
        if (isChecked) current.add(ticker) else current.remove(ticker)
        _selectedTickers.value = current
    }

    fun toggleSelectNone(checked: Boolean) {
        if(checked) {
            previousSelection = _selectedTickers.value ?: emptySet()
            _selectedTickers.value = emptySet()
        } else {
            _selectedTickers.value = previousSelection
        }
        _selectNone.value = checked
    }

//    // (선택사항) 선택 초기화
//    fun clearSelections() {
//        _selectedTickers.value = emptySet()
//    }
}

class PortfolioViewModelFactory(
    private val repository: FakeStockRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            return PortfolioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}