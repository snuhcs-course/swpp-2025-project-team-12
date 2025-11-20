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
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.view.ViewTreeObserver
import android.util.Log

class StockAdapter(
    private val onClick: (RecommendationDto) -> Unit = {}
) : ListAdapter<RecommendationDto, StockAdapter.ItemVH>(
    object : DiffUtil.ItemCallback<RecommendationDto>() {
        override fun areItemsTheSame(o: RecommendationDto, n: RecommendationDto): Boolean =
            o.ticker == n.ticker
        override fun areContentsTheSame(o: RecommendationDto, n: RecommendationDto): Boolean =
            o == n
    }
) {
    private val expandedTickers = mutableSetOf<String>() // 확장 상태를 저장할 Set (어떤 종목이 펼쳐져 있는지 Ticker로 기억)
    // Adapter의 토글 함수 (상태를 바꾸고 RecyclerView에 갱신 알림)
    private val onExpandToggle: (String) -> Unit = { ticker ->
        if (expandedTickers.contains(ticker)) {
            expandedTickers.remove(ticker)
        } else {
            expandedTickers.add(ticker)
        }
        // 해당 아이템만 갱신하여 UI를 전환합니다.
        notifyItemChanged(currentList.indexOfFirst { it.ticker == ticker })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_row, parent, false)
        return ItemVH(v, onClick, onExpandToggle) //  ItemVH에 토글 함수를 전달
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        val item = getItem(position)
        val isExpanded = expandedTickers.contains(item.ticker)
        holder.bind(item, isExpanded)
    }

    class ItemVH(view: View,
                 private val onClick: (RecommendationDto) -> Unit,
                 private val onExpandToggle: (String) -> Unit) :
        RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvName)
        private val tvScore = view.findViewById<TextView>(R.id.tvScore)
        private val tvDesc  = view.findViewById<TextView>(R.id.tvDesc)
        private val tvChange = view.findViewById<TextView>(R.id.tvChange)
        private val tvMore = view.findViewById<TextView>(R.id.tvMore)
        private val dfPrice = DecimalFormat("#,##0")
        private var current: RecommendationDto? = null

        init {
            itemView.setOnClickListener { current?.let(onClick) }
            tvMore.setOnClickListener { // 더보기 버튼 클릭 리스너
                current?.ticker?.let(onExpandToggle)
            }
        }

        fun bind(d: RecommendationDto, isExpanded: Boolean) {
            current = d
            tvName.text = d.name

            // 1. 가격/색상 설정
            tvScore.text = dfPrice.format(d.price)
            val colorRes = when {
                d.change > 0 -> R.color.positive_red
                d.change < 0 -> R.color.negative_blue
                else -> R.color.black
            }
            val color = ContextCompat.getColor(itemView.context, colorRes)
            tvScore.setTextColor(ContextCompat.getColor(itemView.context, R.color.black)) // 가격은 검정 고정
            tvChange.setChange(d.change, d.changeRate)
            tvChange.setTextColor(color) // 등락률에만 색상 적용

            // 2. 요약 텍스트 및 확장 상태 적용
            tvDesc.text = d.headline

            // 3. 확장 상태에 따라 maxLines와 ellipsize 설정
            tvDesc.maxLines = if (isExpanded) Int.MAX_VALUE else 6
            tvDesc.ellipsize = if (isExpanded) null else TextUtils.TruncateAt.END
            tvMore.text = if (isExpanded) "접기" else "더보기"

            if (d.headline.isNullOrBlank()) {
                tvDesc.visibility = View.GONE
                tvMore.visibility = View.GONE // 텍스트 없으면 버튼도 숨김
                return // 여기서 함수 종료
            }

            tvDesc.visibility = View.VISIBLE
            // 💡 5. 오버플로우 체크
            // onPreDrawListener를 사용하여 텍스트가 그려진 후 정확히 상태를 확인합니다.
            tvDesc.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    tvDesc.viewTreeObserver.removeOnPreDrawListener(this)
                    val layout = tvDesc.layout
                    // 텍스트가 줄임표(ellipsize)로 잘렸는지 확인
                    // layout이 null이 아니고 (뷰가 준비됨),
                    // 마지막 줄이 잘렸는지 (getEllipsisCount > 0) 확인
                    val isTruncated = layout != null &&
                            layout.lineCount > 0 &&
                            layout.getEllipsisCount(layout.lineCount - 1) > 0

                    // 현재 펼쳐진 상태이거나 (접기 버튼 필요) 또는 텍스트가 잘렸다면 (더보기 버튼 필요) 버튼을 보여줍니다.
                    if (isExpanded || isTruncated) {
                        tvMore.visibility = View.VISIBLE
                    } else {
                        tvMore.visibility = View.GONE
                    }
                    return true
                }
            })

        }
    }
}