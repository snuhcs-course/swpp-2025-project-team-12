package com.example.dailyinsight.data.dto

import com.google.gson.annotations.SerializedName

// Root response wrapper
data class LLMSummaryResponse(
    @SerializedName("llm_output")
    val llmOutput: String
)

// Parsed LLM output structure
data class LLMSummaryData(
    @SerializedName("asof_date")
    val asofDate: String,
    val regime: String,
    val overview: List<String>,
    val kospi: MarketSummary,
    val kosdaq: MarketSummary,
    @SerializedName("news_used")
    val newsUsed: List<String>
)

data class MarketSummary(
    val market: String,
    @SerializedName("asof_date")
    val asofDate: String,
    val label: String,
    val confidence: Double,
    val summary: String,
    val signals: List<String>,
    val drivers: List<String>,
    val risks: List<String>
)