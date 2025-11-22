package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.dto.LogInRequest
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.SignUpRequest
import com.example.dailyinsight.data.dto.SignUpResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Call
import kotlin.math.sign
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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

        verifyPWField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                signUpButton.performClick()
                true
            } else {
                false
            }
        }

        signUpButton.setOnClickListener {
            val id = IDTextField.text.toString().trim()
            val password = PWTextField.text.toString().trim()
            val verifyPW = verifyPWField.text.toString().trim()
            // check if all fields are provided and password matches
            if(id.isEmpty()) {
                Toast.makeText(this, R.string.id_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password.isEmpty()) {
                Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password != verifyPW) {
                Toast.makeText(this, R.string.password_not_same, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO - check formats of id and pw here (maybe need to define new functions to check formats)

            // send signup request to the server
            sendSignupRequest(id, password)
        }
    }

    fun sendSignupRequest(id: String, password: String) {
        /**
         * send signup request to the server with given id and password
         * @id : id
         * @password : password
         **/
        val request = SignUpRequest(id = id, password = password)

        RetrofitInstance.api.signUp(request)
            .enqueue(object : retrofit2.Callback<SignUpResponse> {
                override fun onResponse(
                    call: Call<SignUpResponse>,
                    response: retrofit2.Response<SignUpResponse>
                ) {
                    if (response.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            applicationContext.cookieDataStore.edit { prefs ->
                                prefs[CookieKeys.USERNAME] = id
                            }
                        }
                        val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                        finishAffinity()
                        startActivity(intent)
                    } else {
                        val result = response.errorBody()?.string()
                        val message = Gson().fromJson(result, LogInResponse::class.java).message
                        Log.e("Sign Up", "response with ${response.code()}: $message")
                        // must show 'id already used' -> check id, pw formats before the api call
                        Toast.makeText(this@SignUpActivity, R.string.id_already_in_use, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                    Toast.makeText(this@SignUpActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
                }
            })

    }
}