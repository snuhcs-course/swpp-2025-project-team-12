package com.example.dailyinsight.ui.common

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.dailyinsight.R
import java.text.NumberFormat
import java.util.Locale

private val wonFormat = NumberFormat.getInstance(Locale.KOREA)

fun TextView.setChange(change: Number, rate: Double) {
    val sign = when {
        rate > 0 -> "+"
        rate < 0 -> "-"
        else -> ""
    }
    val changeStr = wonFormat.format(change.toLong())
    val rateStr   = String.format(Locale.KOREA, "%.2f", rate)

    text = context.getString(R.string.change_text, sign, changeStr, rateStr)

    val colorRes = when {
        rate > 0 -> R.color.price_up
        rate < 0 -> R.color.price_down
        else -> R.color.price_flat
    }
    setTextColor(ContextCompat.getColor(context, colorRes))
}

fun TextView.setPriceWon(value: Long) {
    val priceStr = wonFormat.format(value)
    text = context.getString(R.string.price_won, priceStr)
}