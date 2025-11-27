package com.example.dailyinsight.ui.userinfo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.ChangeNameRequest
import com.example.dailyinsight.data.dto.ChangePasswordRequest
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.UserProfileResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.model.PasswordState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.launch
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

        val PWText = findViewById<TextInputLayout>(R.id.PWText)
        val verifyPW = findViewById<TextInputLayout>(R.id.verifyPW)
        val PWTextField = findViewById<TextInputEditText>(R.id.PWTextField)
        val verifyPWField = findViewById<TextInputEditText>(R.id.verifyPWField)
        val changeButton = findViewById<MaterialButton>(R.id.changeButton)

        PWText.error = null
        verifyPW.error = null

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

        PWTextField.addTextChangedListener {
            PWText.error = null
        }
        verifyPWField.addTextChangedListener {
            verifyPW.error = null
        }

        changeButton.setOnClickListener {
            val password = PWTextField.text.toString().trim()
            val verify = verifyPWField.text.toString().trim()
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

            // TODO - check pw format here
            when(isPasswordValid(password)) {
                PasswordState.TOO_SHORT -> PWText.error = "최소 8자 이상이어야 합니다"
                PasswordState.TOO_LONG -> PWText.error = "최대 20자를 넘을 수 없습니다"
                PasswordState.NO_LOWERCASE -> PWText.error = "소문자를 하나 이상 포함해야 합니다"
                PasswordState.NO_UPPERCASE -> PWText.error = "대문자를 하나 이상 포함해야 합니다"
                PasswordState.NO_DIGIT -> PWText.error = "숫자를 하나 이상 포함해야 합니다"
                PasswordState.VALID -> PWText.error = null
            }
            if(PWText.error != null) return@setOnClickListener


            lifecycleScope.launch {
                changePassword(password)
            }
        }
    }

    suspend fun changePassword(password: String) {
        val request = ChangePasswordRequest(password)
        try {
            val response = RetrofitInstance.api.changePassword(request)
            if (response.isSuccessful) {
                Toast.makeText(this@ChangePasswordActivity, R.string.on_change_successful, Toast.LENGTH_SHORT).show()
            } else {
                val result = response.errorBody()?.string()
                val message = Gson().fromJson(result, LogInResponse::class.java).message
                Log.e("change password", "response with ${response.code()}: $message")
                Toast.makeText(this@ChangePasswordActivity, R.string.on_change_password_unsuccessful, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("change password", "exception on api call")
            e.printStackTrace()
            Toast.makeText(this@ChangePasswordActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
        }
    }

    fun isPasswordValid(pw: String) : PasswordState {
        return if(pw.length < 8) return PasswordState.TOO_SHORT
        else if(pw.length > 20) return PasswordState.TOO_LONG
        else if(!pw.contains(Regex("[A-X]"))) return PasswordState.NO_UPPERCASE
        else if(!pw.contains(Regex("[a-x]"))) return PasswordState.NO_LOWERCASE
        else if(!pw.contains(Regex("\\d"))) return PasswordState.NO_DIGIT
        else PasswordState.VALID
    }
}