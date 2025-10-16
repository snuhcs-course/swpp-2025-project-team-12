package com.example.dailyinsight.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.ui.common.setChange // TextView 확장함수(있다면)

class TodayAdapter(
    private val onClick: (RecommendationDto) -> Unit = {}
) : ListAdapter<RecommendationDto, TodayAdapter.VH>(
    object : DiffUtil.ItemCallback<RecommendationDto>() {
        override fun areItemsTheSame(o: RecommendationDto, n: RecommendationDto) = o.ticker == n.ticker
        override fun areContentsTheSame(o: RecommendationDto, n: RecommendationDto) = o == n
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(view: View, private val onClick: (RecommendationDto) -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvName)
        private val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        private val tvChange = view.findViewById<TextView>(R.id.tvChange)
        private val tvHeadline = view.findViewById<TextView?>(R.id.tvHeadline)

        private var current: RecommendationDto? = null

        init {
            itemView.setOnClickListener { current?.let(onClick) }
        }

        fun bind(d: RecommendationDto) {
            current = d
            tvName.text = d.name
            tvPrice.text = "%,d".format(d.price)
            // setChange 확장함수 있으면 사용, 없으면 문자열로 표시
            try {
                tvChange.setChange(d.change.toDouble(), d.changeRate)
            } catch (_: Throwable) {
                tvChange.text = "${if (d.change >= 0) "+" else ""}${d.change} (%.2f%%)".format(d.changeRate)
            }
            tvHeadline?.text = d.headline ?: ""
        }
    }
}