package com.example.dailyinsight.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
fun Fragment.showSnack(message: String, length: Int = Snackbar.LENGTH_SHORT) {
    val root: View = requireActivity().findViewById(android.R.id.content)
    Snackbar.make(root, message, length).show()
}