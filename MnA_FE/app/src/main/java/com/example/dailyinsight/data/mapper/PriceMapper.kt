package com.example.dailyinsight.data.mapper

import com.example.dailyinsight.data.dto.StockDetailDto
import com.example.dailyinsight.data.model.PricePoint

object PriceMapper {

    /**
     * pandas.Series가 JSON 맵({ "YYYY-MM-DD": 70100, ... })으로 내려온 것을
     */
    fun extractPricePoints(detail: StockDetailDto): List<PricePoint> {
        // DTO 정의 기준: priceFinancialInfo?.price 만 존재
        val map: Map<String, Double>? = detail.priceFinancialInfo?.price

        return map
            ?.entries
            ?.sortedBy { it.key }                 // 날짜 오름차순
            ?.map { (date, value) ->
                PricePoint(date = date, close = value)
            }
            ?: emptyList()
    }
}
