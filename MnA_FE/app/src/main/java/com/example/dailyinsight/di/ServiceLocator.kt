package com.example.dailyinsight.di

import android.content.Context
import com.example.dailyinsight.data.RemoteRepository
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.database.AppDatabase
import com.example.dailyinsight.data.database.BriefingDao
import com.example.dailyinsight.data.database.HistoryCacheDao
import com.example.dailyinsight.data.database.StockDetailDao
import com.example.dailyinsight.data.network.ApiService
import com.example.dailyinsight.data.network.RetrofitInstance

/**
 * Simple Service Locator for dependency injection
 * Provides singleton instances of repositories, DAOs, and API service
 */
object ServiceLocator {

    private lateinit var appContext: Context
    private var isInitialized = false

    // Lazy initialization of database
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }

    // DAOs
    val historyCacheDao: HistoryCacheDao by lazy {
        database.historyCacheDao()
    }

    val briefingDao: BriefingDao by lazy {
        database.briefingDao()
    }

    val stockDetailDao: StockDetailDao by lazy {
        database.stockDetailDao()
    }

    // API Service
    val api: ApiService
        get() = RetrofitInstance.api

    // Repository
    val repository: Repository by lazy {
        RemoteRepository(
            api = api,
            briefingDao = briefingDao,
            stockDetailDao = stockDetailDao,
            context = appContext
        )
    }

    /**
     * Initialize ServiceLocator with application context
     * Must be called in Application.onCreate() or MainActivity.onCreate()
     */
    fun init(context: Context) {
        if (!isInitialized) {
            appContext = context.applicationContext
            RetrofitInstance.init(context)
            isInitialized = true
        }
    }
}
