package com.example.dailyinsight.di

import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.RemoteRepository
import com.example.dailyinsight.data.network.ApiService
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.data.database.AppDatabase
import com.example.dailyinsight.data.database.HistoryCacheDao
import android.content.Context

object ServiceLocator {

    // 앱 컨텍스트 보관
    private lateinit var appContext: Context

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }

    val historyCacheDao: HistoryCacheDao by lazy {
        database.historyCacheDao()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 네트워크
    val api: ApiService by lazy { RetrofitInstance.api }

    // Repository 제공
    val repository: Repository by lazy {
        RemoteRepository(
            api,
            database.briefingDao(),
            database.stockDetailDao()
        )
    }
}