package com.example.dailyinsight.utils

import android.content.res.Resources

/**
 * change dimension unit
 * px -> dp
 *
 * Usage:
 *   val margin = 16.dp
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()