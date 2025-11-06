package com.example.dailyinsight.data

import android.content.Context
import android.content.Intent
import com.example.dailyinsight.data.dto.StockItem
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.data.dto.CompanyListResponse
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.PortfolioRequest
import com.example.dailyinsight.data.network.ApiService
import com.example.dailyinsight.data.network.RetrofitInstance
import com.google.gson.Gson
import okhttp3.Callback
import retrofit2.Call
import retrofit2.Response

class RemoteStockRepository(
    private val api: ApiService
) : StockRepository {
    private lateinit var stockList : List<StockItem>
    override suspend fun fetchStocks(): List<StockItem> {
        try {
            val response = api.getCompanyList(100, 0)
            if(response.isSuccessful) {
                stockList = response.body()!!.items
            } else {
                Log.i("remote stock repo", "fetch status ${response.code()}")
                stockList = emptyList()
            }
        } catch (e: Exception) {
            Log.e("remote stock repo", "fetch failed")
            e.printStackTrace()
            stockList = emptyList()
        }
        return stockList
    }

    override suspend fun submitSelectedStocks(selected: Set<String>): Boolean {
        try {
            val response = api.setPortfolio(PortfolioRequest(selected))
            if(response.isSuccessful) {
                return true
            } else {
                Log.i("remote stock repo", "submit status ${response.code()}")
                return false
            }
        } catch (e: Exception) {
            Log.e("remote stock repo", "submit failed")
            e.printStackTrace()
            return false
        }
    }
}
