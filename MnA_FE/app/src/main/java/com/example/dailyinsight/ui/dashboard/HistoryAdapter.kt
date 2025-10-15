package com.example.dailyinsight.ui.dashboard

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

class HistoryAdapter(
    private val onClick: (RecommendationDto) -> Unit = {}
) : ListAdapter<HistoryRow, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<HistoryRow>() {
        override fun areItemsTheSame(o: HistoryRow, n: HistoryRow): Boolean = o == n
        override fun areContentsTheSame(o: HistoryRow, n: HistoryRow): Boolean = o == n
    }
) {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryRow.Header -> TYPE_HEADER
        is HistoryRow.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_header, parent, false)
            HeaderVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_row, parent, false)
            ItemVH(v, onClick)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is HistoryRow.Header -> (holder as HeaderVH).bind(row)
            is HistoryRow.Item -> (holder as ItemVH).bind(row.data)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate = view.findViewById<TextView>(R.id.tvDate)
        fun bind(h: HistoryRow.Header) {
            tvDate.text = h.label
        }
    }

    class ItemVH(view: View, private val onClick: (RecommendationDto) -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvName)
        private val tvScore = view.findViewById<TextView>(R.id.tvScore)   // 점수/가격 등 표기용
        private val tvDesc = view.findViewById<TextView>(R.id.tvDesc)     // 설명/헤드라인
        private val tvChange = view.findViewById<TextView>(R.id.tvChange)

        private var current: RecommendationDto? = null

        init {
            itemView.setOnClickListener { current?.let(onClick) }
        }

        fun bind(d: RecommendationDto) {
            current = d
            tvName.text = d.name
            tvScore.text = "%,d".format(d.price)
            tvDesc.text = d.headline ?: ""
            try {
                tvChange.setChange(d.change.toDouble(), d.changeRate)
            } catch (_: Throwable) {
                tvChange.text = "${if (d.change >= 0) "+" else ""}${d.change} (%.2f%%)".format(d.changeRate)
            }
        }
    }
}