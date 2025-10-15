package com.example.dailyinsight.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dailyinsight.data.Repository

class NotificationsVMFactory(
    private val repo: Repository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            return NotificationsViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}