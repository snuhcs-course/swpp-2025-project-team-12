package com.example.dailyinsight.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
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

    fun logout() {
        RetrofitInstance.api.logOut()
            .enqueue(object : retrofit2.Callback<UserProfileResponse> {
                override fun onResponse(
                    request: Call<UserProfileResponse?>,
                    response: Response<UserProfileResponse?>
                ) {
                    if(response.isSuccessful) {
                        Toast.makeText(context, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show()
                        viewModelScope.launch {
                            context.cookieDataStore.edit { it.clear() }
                        }
                        val intent = Intent(context, StartActivity::class.java)
                        context.startActivity(intent)
                        if(context is Activity) {
                            context.finishAffinity()
                        }
                    } else {
                        val result = response.errorBody()?.string()
                        val message = Gson().fromJson(result, LogInResponse::class.java).message
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UserProfileResponse>, t: Throwable) {
                    Toast.makeText(context, "Please check network connection", Toast.LENGTH_SHORT)
                        .show()
                }
            })
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