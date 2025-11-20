package com.example.dailyinsight.ui.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.UserNameResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)

        RetrofitInstance.init(this)
        lifecycleScope.launch {
            validateCookies()
        }
    }

    private suspend fun validateCookies() {
        try {
            val response = RetrofitInstance.api.getName()

            if (response.isSuccessful) {
                navigateToMain()
            } else {
                navigateToLogin()
            }

        } catch (e: Exception) {
            Log.e("splash", "getName")
            e.printStackTrace()
            Toast.makeText(this, "failed to reach server", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }
}