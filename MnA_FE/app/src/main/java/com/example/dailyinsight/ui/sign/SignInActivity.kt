package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.LogInRequest
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.start.StartActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Call
import com.google.gson.Gson

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val IDTextField = findViewById<TextInputEditText>(R.id.IDTextField)
        val PWTextField = findViewById<TextInputEditText>(R.id.PWTextField)

        val findPWButton = findViewById<MaterialButton>(R.id.findPWButton)
        findPWButton.setOnClickListener {
            // TODO - find PW (is implemented on server?)
            Toast.makeText(this, "find PW feature not implemented yet", Toast.LENGTH_SHORT).show()
        }

        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        loginButton.setOnClickListener {
            val id = IDTextField.text.toString().trim()
            val password = PWTextField.text.toString().trim()
            // check if all fields are provided
            if(id.isEmpty()) {
                Toast.makeText(this, "please enter id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password.isEmpty()) {
                Toast.makeText(this, "please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // send login request to the server
            sendLoginRequest(id, password)
        }

        // move to sign up activity
        val signUpButton = findViewById<MaterialButton>(R.id.signUpButton)
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    fun sendLoginRequest(id: String, password: String) {
        /**
         * send login request to the server with given id and password
         * @id : id
         * @password : password
         **/
        val request = LogInRequest(id = id, password = password)

        RetrofitInstance.api.logIn(request)
            .enqueue(object : retrofit2.Callback<LogInResponse> {
                override fun onResponse(
                    call: Call<LogInResponse>,
                    response: retrofit2.Response<LogInResponse>
                ) {
                    if (response.isSuccessful) {
                        val intent = Intent(this@SignInActivity, MainActivity::class.java)
                        finishAffinity()
                        startActivity(intent)
                    } else {
                        val result = response.errorBody()?.string()
                        val message = Gson().fromJson(result, LogInResponse::class.java).message
                        Toast.makeText(this@SignInActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LogInResponse>, t: Throwable) {
                    Toast.makeText(this@SignInActivity, "failed to login", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }
}