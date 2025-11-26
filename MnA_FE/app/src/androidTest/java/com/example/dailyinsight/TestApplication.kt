package com.example.dailyinsight

import android.app.Application
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.di.ServiceLocator

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.init(this)
        ServiceLocator.init(applicationContext)
    }
}