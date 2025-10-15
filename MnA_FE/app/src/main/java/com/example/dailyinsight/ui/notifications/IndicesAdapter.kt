package com.example.dailyinsight.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.IndexDto
import java.text.DecimalFormat
import kotlin.math.abs

class IndicesAdapter : ListAdapter<IndexDto, IndicesAdapter.VH>(DIFF) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName   = itemView.findViewById<TextView>(R.id.tvName)
        private val tvTime   = itemView.findViewById<TextView>(R.id.tvTime)
        private val tvValue  = itemView.findViewById<TextView>(R.id.tvValue)
        private val tvChange = itemView.findViewById<TextView>(R.id.tvChange)

        // named argument 빼고 간단히 사용
        private val dfNumber = DecimalFormat("#,##0.##")
        private val dfRate   = DecimalFormat("#0.##")

        fun bind(d: IndexDto) {
            tvName.text  = d.name
            tvTime.text  = d.time
            tvValue.text = dfNumber.format(d.price)

            val sign = if (d.change >= 0) "+" else "-"
            // Material Color 리소스는 앱 R이 아니라 material R에서 가져오거나, ContextCompat로 색상 값 얻기
            val colorRes = if (d.change >= 0)
                com.google.android.material.R.color.m3_ref_palette_error40
            else
                com.google.android.material.R.color.m3_ref_palette_primary40

            val body = "${sign}${dfNumber.format(abs(d.change))} (${sign}${dfRate.format(abs(d.changeRate))}%)"
            tvChange.text = body
            tvChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_index_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<IndexDto>() {
            override fun areItemsTheSame(old: IndexDto, new: IndexDto) = old.code == new.code
            override fun areContentsTheSame(old: IndexDto, new: IndexDto) = old == new
        }
    }
}