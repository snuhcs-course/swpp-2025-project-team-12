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
import androidx.core.widget.addTextChangedListener
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
import com.example.dailyinsight.model.PasswordState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

        val IDText = findViewById<TextInputLayout>(R.id.IDText)
        val PWText = findViewById<TextInputLayout>(R.id.PWText)
        val verifyPW = findViewById<TextInputLayout>(R.id.verifyPW)
        IDText.error = null
        PWText.error = null
        verifyPW.error = null
        IDTextField.addTextChangedListener {
            IDText.error = null
        }
        PWTextField.addTextChangedListener {
            PWText.error = null
        }
        verifyPWField.addTextChangedListener {
            verifyPW.error = null
        }

        signUpButton.setOnClickListener {
            val id = IDTextField.text.toString().trim()
            val password = PWTextField.text.toString().trim()
            val verify = verifyPWField.text.toString().trim()
            // check if all fields are provided and password matches
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
            if(password != verify) {
//                Toast.makeText(this, R.string.password_not_same, Toast.LENGTH_SHORT).show()
                verifyPW.error = getString(R.string.password_not_same)
                return@setOnClickListener
            }
            // TODO - check formats of id and pw here (maybe need to define new functions to check formats)
            if(!isIDValid(id)) {
                IDText.error = "20자를 넘을 수 없습니다"
                return@setOnClickListener
            }
            else {
                IDText.error = null
            }

            when(isPasswordValid(password)) {
                PasswordState.TOO_SHORT -> PWText.error = "최소 8자 이상이어야 합니다"
                PasswordState.TOO_LONG -> PWText.error = "최대 20자를 넘을 수 없습니다"
                PasswordState.NO_LOWERCASE -> PWText.error = "소문자를 하나 이상 포함해야 합니다"
                PasswordState.NO_UPPERCASE -> PWText.error = "대문자를 하나 이상 포함해야 합니다"
                PasswordState.NO_DIGIT -> PWText.error = "숫자를 하나 이상 포함해야 합니다"
                PasswordState.VALID -> PWText.error = null
            }
            if(PWText.error != null) return@setOnClickListener

            // send signup request to the server
            lifecycleScope.launch {
                sendSignupRequest(id, password)
            }
        }
    }

    fun isIDValid(id: String) : Boolean {
        return if(id.length > 20) false
        else true
    }

    fun isPasswordValid(pw: String) : PasswordState {
        return if(pw.length < 8) return PasswordState.TOO_SHORT
        else if(pw.length > 20) return PasswordState.TOO_LONG
        else if(!pw.contains(Regex("[A-X]"))) return PasswordState.NO_UPPERCASE
        else if(!pw.contains(Regex("[a-x]"))) return PasswordState.NO_LOWERCASE
        else if(!pw.contains(Regex("\\d"))) return PasswordState.NO_DIGIT
        else PasswordState.VALID
    }

    suspend fun sendSignupRequest(id: String, password: String) {
        /**
         * send signup request to the server with given id and password
         * @id : id
         * @password : password
         **/
        val request = SignUpRequest(id = id, password = password)

        try {
            val response = RetrofitInstance.api.signUp(request)
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
        } catch (e: Exception) {
            Log.e("Sign up", "exception on api call")
            e.printStackTrace()
            Toast.makeText(this@SignUpActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
        }

//        RetrofitInstance.api.signUp(request)
//            .enqueue(object : retrofit2.Callback<SignUpResponse> {
//                override fun onResponse(
//                    call: Call<SignUpResponse>,
//                    response: retrofit2.Response<SignUpResponse>
//                ) {
//                    if (response.isSuccessful) {
//                        CoroutineScope(Dispatchers.IO).launch {
//                            applicationContext.cookieDataStore.edit { prefs ->
//                                prefs[CookieKeys.USERNAME] = id
//                            }
//                        }
//                        val intent = Intent(this@SignUpActivity, MainActivity::class.java)
//                        finishAffinity()
//                        startActivity(intent)
//                    } else {
//                        val result = response.errorBody()?.string()
//                        val message = Gson().fromJson(result, LogInResponse::class.java).message
//                        Log.e("Sign Up", "response with ${response.code()}: $message")
//                        // must show 'id already used' -> check id, pw formats before the api call
//                        Toast.makeText(this@SignUpActivity, R.string.id_already_in_use, Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
//                    Toast.makeText(this@SignUpActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
//                }
//            })

    }
}