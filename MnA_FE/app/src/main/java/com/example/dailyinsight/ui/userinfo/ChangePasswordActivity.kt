package com.example.dailyinsight.ui.userinfo

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.ChangeNameRequest
import com.example.dailyinsight.data.dto.ChangePasswordRequest
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.UserProfileResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import retrofit2.Call

class ChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val PWTextField = findViewById<TextInputEditText>(R.id.PWTextField)
        val verifyPWField = findViewById<TextInputEditText>(R.id.verifyPWField)
        val changeButton = findViewById<MaterialButton>(R.id.changeButton)

        verifyPWField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                changeButton.performClick()
                true
            } else {
                false
            }
        }

        changeButton.setOnClickListener {
            val password = PWTextField.text.toString().trim()
            val verifyPW = verifyPWField.text.toString().trim()
            if(password.isEmpty()) {
                Toast.makeText(this, "please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password != verifyPW) {
                Toast.makeText(this, "passwords entered are not same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO - to server change PW
            val request = ChangePasswordRequest(password)
            RetrofitInstance.api.changePassword(request)
                .enqueue(object : retrofit2.Callback<UserProfileResponse> {
                    override fun onResponse(
                        call: Call<UserProfileResponse>,
                        response: retrofit2.Response<UserProfileResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ChangePasswordActivity, "successfully changed!", Toast.LENGTH_SHORT).show()
                        } else {
                            val result = response.errorBody()?.string()
                            val message = Gson().fromJson(result, LogInResponse::class.java).message
                            Toast.makeText(this@ChangePasswordActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UserProfileResponse>, t: Throwable) {
                        Toast.makeText(this@ChangePasswordActivity, "Please check network connection", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }
}