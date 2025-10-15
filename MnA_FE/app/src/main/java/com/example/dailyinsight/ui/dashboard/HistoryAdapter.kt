package com.example.dailyinsight.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.data.dto.RecommendationDto
import com.example.dailyinsight.databinding.ItemHistoryHeaderBinding
import com.example.dailyinsight.databinding.ItemHistoryRowBinding
import com.example.dailyinsight.ui.common.setChange
import com.example.dailyinsight.ui.common.setPriceWon

class HistoryAdapter :
    ListAdapter<HistoryRow, RecyclerView.ViewHolder>(Diff) {

    private object Diff : DiffUtil.ItemCallback<HistoryRow>() {
        override fun areItemsTheSame(oldItem: HistoryRow, newItem: HistoryRow): Boolean {
            return when {
                oldItem is HistoryRow.Header && newItem is HistoryRow.Header ->
                    oldItem.title == newItem.title
                oldItem is HistoryRow.Item && newItem is HistoryRow.Item ->
                    oldItem.rec.code == newItem.rec.code &&
                            oldItem.rec.time == newItem.rec.time
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryRow, newItem: HistoryRow): Boolean =
            oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryRow.Header -> VT_HEADER
        is HistoryRow.Item   -> VT_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == VT_HEADER) {
            HeaderVH(ItemHistoryHeaderBinding.inflate(inf, parent, false))
        } else {
            ItemVH(ItemHistoryRowBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is HistoryRow.Header -> (holder as HeaderVH).bind(row.title)
            is HistoryRow.Item   -> (holder as ItemVH).bind(row.rec)
        }
    }

    class HeaderVH(private val b: ItemHistoryHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(title: String) {
            b.tvDate.text = title
        }
    }

    class ItemVH(private val b: ItemHistoryRowBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: RecommendationDto) = with(b) {
            tvName.text = r.name

            // 우측 숫자: price가 있으면 원화 포맷으로, 없으면 빈값
            // (DTO에 score가 없으니 score 참조 제거)
            if (r.price != null) {
                tvScore.setPriceWon(r.price)
            } else {
                tvScore.text = ""
            }

            tvDesc.text = r.headline ?: ""

            // 등락 텍스트+색상 (확장함수 import 필수)
            tvChange.setChange(r.change, r.changeRate)
        }
    }

    companion object {
        private const val VT_HEADER = 0
        private const val VT_ITEM = 1
    }
}