package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.dto.LogInRequest
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.start.StartActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.Call
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val IDText = findViewById<TextInputLayout>(R.id.IDText)
        val PWText = findViewById<TextInputLayout>(R.id.PWText)
        val IDTextField = findViewById<TextInputEditText>(R.id.IDTextField)
        val PWTextField = findViewById<TextInputEditText>(R.id.PWTextField)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)

        IDText.error = null
        PWText.error = null

        PWTextField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginButton.performClick()
                true
            } else {
                false
            }
        }

        IDTextField.addTextChangedListener {
            IDText.error = null
        }
        PWTextField.addTextChangedListener {
            PWText.error = null
        }

//        val findPWButton = findViewById<MaterialButton>(R.id.findPWButton)
//        findPWButton.setOnClickListener {
//            // TODO - find PW (is implemented on server?)
//            Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show()
//        }

        loginButton.setOnClickListener {
            val id = IDTextField.text.toString().trim()
            val password = PWTextField.text.toString().trim()
            // check if all fields are provided
            if(id.isEmpty()) {
//                Toast.makeText(this, R.string.id_required, Toast.LENGTH_SHORT).show()
                IDText.error = getString(R.string.id_required)
                return@setOnClickListener
            }
            if(password.isEmpty()) {
//                Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show()
                PWText.error = getString(R.string.password_required)
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
                        CoroutineScope(Dispatchers.IO).launch {
                            applicationContext.cookieDataStore.edit { prefs ->
                                prefs[CookieKeys.USERNAME] = id
                            }
                        }
                        val intent = Intent(this@SignInActivity, MainActivity::class.java)
                        finishAffinity()
                        startActivity(intent)
                    } else {
                        val result = response.errorBody()?.string()
                        val message = Gson().fromJson(result, LogInResponse::class.java).message
                        Log.e("Sign In", "response with ${response.code()}: $message")
                        Toast.makeText(this@SignInActivity, R.string.on_login_unsuccessful, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LogInResponse>, t: Throwable) {
                    Toast.makeText(this@SignInActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
                }
            })
    }
}