package com.example.dailyinsight.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.databinding.ItemRecommendationBinding
import java.text.DecimalFormat
import kotlin.math.abs

class RecommendationAdapter(
    private val onItemClick: (RecommendationDto) -> Unit = {}
) : ListAdapter<RecommendationDto, RecommendationAdapter.VH>(DIFF) {

    inner class VH(private val binding: ItemRecommendationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dfPrice = DecimalFormat("#,##0")
        private val dfChange = DecimalFormat("#,##0")
        private val dfRate = DecimalFormat("#0.##")

        fun bind(d: RecommendationDto) = with(binding) {
            // 이름
            tvName.text = d.name

            // 현재가
            tvPrice.text = dfPrice.format(d.price)

            // 헤드라인 (nullable → 없으면 GONE)
            if (d.headline.isNullOrBlank()) {
                tvHeadline.visibility = View.GONE
            } else {
                tvHeadline.visibility = View.VISIBLE
                tvHeadline.text = d.headline
            }

            // 등락(부호 포함) + 색상
            val sign = if (d.change >= 0) "+" else "-"
            val body = "${sign}${dfChange.format(abs(d.change))} (${sign}${dfRate.format(abs(d.changeRate))}%)"
            tvChange.text = body

            // 상승=빨강, 하락=파랑
            val colorRes =
                if (d.change >= 0) com.google.android.material.R.color.m3_ref_palette_error40
                else com.google.android.material.R.color.m3_ref_palette_primary40
            tvChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

            root.setOnClickListener { onItemClick(d) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRecommendationBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RecommendationDto>() {
            override fun areItemsTheSame(oldItem: RecommendationDto, newItem: RecommendationDto): Boolean =
                oldItem.ticker == newItem.ticker

            override fun areContentsTheSame(oldItem: RecommendationDto, newItem: RecommendationDto): Boolean =
                oldItem == newItem
        }
    }
}