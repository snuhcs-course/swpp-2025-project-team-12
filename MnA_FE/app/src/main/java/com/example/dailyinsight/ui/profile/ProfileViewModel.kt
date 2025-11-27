package com.example.dailyinsight.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.example.dailyinsight.R
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.UserProfileResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.start.StartActivity
import com.google.gson.Gson
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.Callback
import retrofit2.Call
import retrofit2.Response


class ProfileViewModel(private val context: Context) : ViewModel() {

    // 로그인 상태를 실시간으로 관찰
    val isLoggedIn: LiveData<Boolean> = context.cookieDataStore.data
        .map { prefs -> !prefs[CookieKeys.ACCESS_TOKEN].isNullOrEmpty() }
        .asLiveData()

    val username: LiveData<String> = context.cookieDataStore.data
        .map { prefs -> prefs[CookieKeys.USERNAME] ?: "Guest" }
        .asLiveData()

    suspend fun logout() {
        try {
            val response = RetrofitInstance.api.logOut()
            if(response.isSuccessful) {
                Toast.makeText(context, R.string.on_logout_successful, Toast.LENGTH_SHORT).show()
                RetrofitInstance.cookieJar.clear()
                val intent = Intent(context, StartActivity::class.java)
                context.startActivity(intent)
                if(context is Activity) {
                    context.finishAffinity()
                }
            } else {
                val result = response.errorBody()?.string()
                val message = Gson().fromJson(result, LogInResponse::class.java).message
                Log.e("logout", "response with ${response.code()}: $message")
                Toast.makeText(context, R.string.on_logout_unsuccessful, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("logout", "exception on api call")
            e.printStackTrace()
            Toast.makeText(context, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
        }
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}