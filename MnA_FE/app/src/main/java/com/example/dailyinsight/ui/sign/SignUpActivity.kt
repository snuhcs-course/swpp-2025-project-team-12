package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.network.LogInRequest
import com.example.dailyinsight.data.network.LogInResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.data.network.SignUpRequest
import com.example.dailyinsight.data.network.SignUpResponse
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Call
import kotlin.math.sign
import com.google.gson.Gson

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val IDTextField = findViewById<TextInputEditText>(R.id.IDTextField)
        val PWTextField = findViewById<TextInputEditText>(R.id.PWTextField)
        val verifyPWField = findViewById<TextInputEditText>(R.id.verifyPWField)

        val signUpButton = findViewById<MaterialButton>(R.id.signUpButton)
        signUpButton.setOnClickListener {
            // TODO - sign up implementation
            // 1. sign up success -> set tokens
            val id = IDTextField.text.toString().trim()
            val password = PWTextField.text.toString().trim()
            val verifyPW = verifyPWField.text.toString().trim()
            if(id.isEmpty()) {
                Toast.makeText(this, "please enter id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password.isEmpty()) {
                Toast.makeText(this, "please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password != verifyPW) {
                Toast.makeText(this, "passwords entered are not same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = SignUpRequest(id = id, password = password)

            RetrofitInstance.api.signUp(request)
                .enqueue(object : retrofit2.Callback<SignUpResponse> {
                    override fun onResponse(
                        call: Call<SignUpResponse>,
                        response: retrofit2.Response<SignUpResponse>
                    ) {
                        if (response.isSuccessful) {
                            val intent = Intent(this@SignUpActivity, SetInterestActivity::class.java)
                            startActivity(intent)
                        } else {
                            val result = response.errorBody()?.string()
                            val message = Gson().fromJson(result, LogInResponse::class.java).message
                            Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                        Toast.makeText(this@SignUpActivity, "failed to sign up", Toast.LENGTH_SHORT)
                            .show()
                    }
                })

        }
    }
}