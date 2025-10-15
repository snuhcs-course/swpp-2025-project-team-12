package com.example.dailyinsight.ui.dashboard

import com.example.dailyinsight.data.dto.RecommendationDto

// UI에 쓰는 행 모델
sealed class HistoryRow {
    data class Header(val title: String) : HistoryRow()
    data class Item(val rec: RecommendationDto) : HistoryRow()
}

// 간단 변환: 한 섹션 헤더 + 아이템들
fun List<RecommendationDto>.toRows(title: String = "오늘"): List<HistoryRow> =
    buildList {
        if (isNotEmpty()) add(HistoryRow.Header(title))
        for (r in this@toRows) add(HistoryRow.Item(r))
    }

/** 서버에서 온 Map("오늘","어제","yyyy-MM-dd" -> List) 를 RecyclerView용 일렬 리스트로 변환 */
fun Map<String, List<RecommendationDto>>.toRows(): List<HistoryRow> {
    val rows = mutableListOf<HistoryRow>()

    // “오늘”, “어제”는 우선 보이게, 그 외는 날짜 역순
    val today = this["오늘"]
    val yesterday = this["어제"]
    if (!today.isNullOrEmpty()) {
        rows += HistoryRow.Header("오늘")
        rows += today.map { HistoryRow.Item(it) }
    }
    if (!yesterday.isNullOrEmpty()) {
        rows += HistoryRow.Header("어제")
        rows += yesterday.map { HistoryRow.Item(it) }
    }

    // 나머지 키(yyyy-MM-dd로 가정)는 역순 정렬
    this.keys
        .filter { it != "오늘" && it != "어제" }
        .sortedDescending()
        .forEach { key ->
            val items = this[key].orEmpty()
            if (items.isNotEmpty()) {
                rows += HistoryRow.Header(key)
                rows += items.map { HistoryRow.Item(it) }
            }
        }
    return rows
}