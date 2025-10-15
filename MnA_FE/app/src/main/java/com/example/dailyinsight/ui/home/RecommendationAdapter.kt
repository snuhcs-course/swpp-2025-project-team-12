package com.example.dailyinsight.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.R
import com.example.dailyinsight.databinding.ItemRecommendationBinding
import com.example.dailyinsight.model.Recommendation
import java.text.NumberFormat
import java.util.Locale

class RecommendationAdapter(
    private val onClick: (Recommendation) -> Unit = {}
) : ListAdapter<Recommendation, RecommendationAdapter.VH>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<Recommendation>() {
        override fun areItemsTheSame(o: Recommendation, n: Recommendation) = o.name == n.name
        override fun areContentsTheSame(o: Recommendation, n: Recommendation) = o == n
    }

    inner class VH(private val b: ItemRecommendationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Recommendation) {
            val nf = NumberFormat.getNumberInstance(Locale.KOREA)
            b.tvName.text = item.name
            b.tvPrice.text = nf.format(item.price)
            val sign = if (item.diff >= 0) "+" else ""
            b.tvChange.text = "$sign${nf.format(item.diff)} (${String.format(Locale.KOREA,"%.1f", item.pct)}%)"
            b.tvChange.setTextColor(
                ContextCompat.getColor(
                    b.root.context,
                    if (item.diff >= 0) R.color.index_up else R.color.index_down
                )
            )
            b.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        val b = ItemRecommendationBinding.inflate(inf, parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}