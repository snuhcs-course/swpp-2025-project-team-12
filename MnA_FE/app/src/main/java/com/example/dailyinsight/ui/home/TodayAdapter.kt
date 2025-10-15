package com.example.dailyinsight.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.databinding.ItemRecommendationBinding
import com.example.dailyinsight.ui.common.setChange
import com.example.dailyinsight.ui.common.setPriceWon

class TodayAdapter : ListAdapter<RecommendationDto, TodayAdapter.VH>(Diff) {

    private object Diff : DiffUtil.ItemCallback<RecommendationDto>() {
        override fun areItemsTheSame(o: RecommendationDto, n: RecommendationDto) =
            o.code == n.code && o.time == n.time
        override fun areContentsTheSame(o: RecommendationDto, n: RecommendationDto) = o == n
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemRecommendationBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val b: ItemRecommendationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: RecommendationDto) = with(b) {
            // ⚠️ 레이아웃의 id가 아래와 다르면 해당 이름으로 바꿔 주세요.
            tvName.text = r.name
            tvPrice.setPriceWon(r.price)
            tvChange.setChange(r.change, r.changeRate)
            tvTime.text = r.time
            // 필요 시 요약/헤드라인: tvDesc?.text = r.headline ?: ""
        }
    }
}