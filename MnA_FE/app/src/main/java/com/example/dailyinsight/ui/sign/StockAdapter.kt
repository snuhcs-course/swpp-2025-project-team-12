package com.example.dailyinsight.ui.sign


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.StockItem

class StockAdapter(
    private var stockList: List<StockItem> = emptyList(),
    private var selectedTickers: Set<Int> = emptySet(),
    private val onCheckedChange: (StockItem, Boolean) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {
    var isEnabled: Boolean = true
    inner class StockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkBox: MaterialCheckBox = view.findViewById(R.id.stockItem)

        fun bind(stock: StockItem, isSelected: Boolean) {
            checkBox.setOnCheckedChangeListener(null)
            checkBox.text = stock.name
            checkBox.isChecked = isSelected
            checkBox.isEnabled = isEnabled

            checkBox.setOnCheckedChangeListener { _, checked ->
                onCheckedChange(stock, checked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stock_item, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stockList[position]
        val isSelected = selectedTickers.contains(stock.ticker)
        holder.bind(stock, isSelected)
    }

    override fun getItemCount(): Int = stockList.size

    fun submitList(newList: List<StockItem>, selected: Set<Int>) {
        stockList = newList
        selectedTickers = selected
        notifyDataSetChanged()
    }
}
