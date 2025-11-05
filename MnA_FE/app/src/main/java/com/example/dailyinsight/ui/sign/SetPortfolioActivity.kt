package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.FakeStockRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText

class SetPortfolioActivity : AppCompatActivity() {
    private lateinit var adapter: StockAdapter
    private val repository = FakeStockRepository()
    private val viewModel: PortfolioViewModel by viewModels {
        PortfolioViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_portfolio)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.fetchStocks()

        // go back when back button clicked on top app bar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // search bar
        val searchBar = findViewById<TextInputEditText>(R.id.searchEditText)
        // list of stock items
        val recyclerView = findViewById<RecyclerView>(R.id.stockItemList)
        // 보유한 주식이 없어요
        val selectNone = findViewById<MaterialCheckBox>(R.id.selectNone)
        // to the next (MainActivity)
        val toNextButton = findViewById<MaterialButton>(R.id.toNextButton)

        // 🔹 어댑터 초기화 (체크 이벤트 → ViewModel로 전달)
        adapter = StockAdapter(onCheckedChange = { stock, checked ->
            viewModel.toggleSelection(stock.ticker, checked)
        })

        // 🔹 RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchBar.doAfterTextChanged { text ->
            viewModel.searchStocks(text.toString())
        }

        viewModel.filteredStocks.observe(this) { stocks ->
            val selected = viewModel.selectedTickers.value ?: emptySet()
            adapter.submitList(stocks, selected)
        }

        // 🔹 선택 상태 관찰 (체크박스 상태 변경 시)
        viewModel.selectedTickers.observe(this, Observer { selected ->
            val stocks = viewModel.stockItems.value ?: emptyList()
            recyclerView.post {
                adapter.submitList(stocks, selected)
            }
        })

        viewModel.selectNone.observe(this) { isChecked ->
            selectNone.isChecked = isChecked
            recyclerView.isEnabled = !isChecked
            adapter.isEnabled = !isChecked
            toNextButton.isEnabled = isChecked || (viewModel.selectedTickers.value?.isNotEmpty() == true)
        }

        viewModel.selectedTickers.observe(this) { selected ->
            toNextButton.isEnabled = selected.isNotEmpty() || (viewModel.selectNone.value == true)
        }

        selectNone.addOnCheckedStateChangedListener { _, state ->
            val isChecked = (state == MaterialCheckBox.STATE_CHECKED)
            viewModel.toggleSelectNone(isChecked)
        }

        toNextButton.setOnClickListener {
            viewModel.submitSelectedStocks()
            val intent = Intent(this, MainActivity::class.java)
            finishAffinity()
            startActivity(intent)
        }
    }
}