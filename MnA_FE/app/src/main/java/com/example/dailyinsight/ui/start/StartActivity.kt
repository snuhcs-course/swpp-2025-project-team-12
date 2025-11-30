package com.example.dailyinsight.ui.start

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
import com.example.dailyinsight.data.dto.CsrfResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.sign.SignInActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.setCsrf()
                if (response.isSuccessful) {
//                        Toast.makeText(this@StartActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                } else {
                    val result = response.errorBody()?.string()
                    val message = Gson().fromJson(result, CsrfResponse::class.java).message
                    Log.e("set csrf", "response with ${response.code()}: $message")
                    Toast.makeText(this@StartActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("set csrf", "exception on api call")
                e.printStackTrace()
                Toast.makeText(this@StartActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
            }
        }

//        // checking csrf token
//        RetrofitInstance.api.setCsrf()
//            .enqueue(object : retrofit2.Callback<CsrfResponse> {
//                override fun onResponse(
//                    call: Call<CsrfResponse>,
//                    response: retrofit2.Response<CsrfResponse>
//                ) {
//                    if (response.isSuccessful) {
////                        Toast.makeText(this@StartActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
//                    } else {
//                        val result = response.errorBody()?.string()
//                        val message = Gson().fromJson(result, CsrfResponse::class.java).message
//                        Log.e("set csrf", "response with ${response.code()}: $message")
//                        Toast.makeText(this@StartActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<CsrfResponse>, t: Throwable) {
//                    Toast.makeText(this@StartActivity, R.string.on_api_failure, Toast.LENGTH_SHORT)
//                        .show()
//                }
//            })

        val signInButton = findViewById<MaterialButton>(R.id.signInButton)
        signInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val passThroughButton = findViewById<MaterialButton>(R.id.passThroughButton)
        passThroughButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}