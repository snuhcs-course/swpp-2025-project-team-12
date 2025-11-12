package com.example.dailyinsight.ui.stock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.setChange
import java.text.DecimalFormat

class StockAdapter(
    private val onClick: (RecommendationDto) -> Unit = {}
) : ListAdapter<StockRow, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<StockRow>() {
        override fun areItemsTheSame(o: StockRow, n: StockRow): Boolean = o == n
        override fun areContentsTheSame(o: StockRow, n: StockRow): Boolean = o == n
    }
) {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is StockRow.Header -> TYPE_HEADER
        is StockRow.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_header, parent, false)
            HeaderVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_row, parent, false)
            ItemVH(v, onClick)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is StockRow.Header -> (holder as HeaderVH).bind(row)
            is StockRow.Item -> (holder as ItemVH).bind(row.data)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate = view.findViewById<TextView>(R.id.tvDate)
        fun bind(h: StockRow.Header) {
            tvDate.text = h.label
        }
    }

    class ItemVH(view: View, private val onClick: (RecommendationDto) -> Unit) :
        RecyclerView.ViewHolder(view) {

        private val tvName = view.findViewById<TextView>(R.id.tvName)
        private val tvScore = view.findViewById<TextView>(R.id.tvScore)   // 가격 등
        private val tvDesc  = view.findViewById<TextView>(R.id.tvDesc)    // 헤드라인
        private val tvChange = view.findViewById<TextView>(R.id.tvChange)

        private val dfPrice = DecimalFormat("#,##0")
        private var current: RecommendationDto? = null

        init {
            itemView.setOnClickListener { current?.let(onClick) }
        }

        fun bind(d: RecommendationDto) {
            current = d

            tvName.text = d.name
            tvScore.text = dfPrice.format(d.price)

            if (d.headline.isNullOrBlank()) {
                tvDesc.visibility = View.GONE
            } else {
                tvDesc.visibility = View.VISIBLE
                tvDesc.text = d.headline
            }

            // 상승/하락 텍스트 & 색상은 setChange에서 처리
            tvChange.setChange(d.change.toDouble(), d.changeRate)
        }
    }
}