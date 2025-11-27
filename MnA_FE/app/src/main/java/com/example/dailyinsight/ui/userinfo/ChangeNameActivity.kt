package com.example.dailyinsight.ui.userinfo

import android.content.Context
import android.content.Intent
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
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.SignUpResponse
import com.example.dailyinsight.data.dto.UserProfileResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.sign.SetPortfolioActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call

class ChangeNameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_name)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val IDText = findViewById<TextInputLayout>(R.id.IDText)
        val IDTextField = findViewById<TextInputEditText>(R.id.IDTextField)
        val changeButton = findViewById<MaterialButton>(R.id.changeButton)

        IDTextField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                changeButton.performClick()
                true
            } else {
                false
            }
        }

        IDText.error = null

        IDTextField.addTextChangedListener {
            IDText.error = null
        }

        changeButton.setOnClickListener {
            val id = IDTextField.text.toString().trim()
            if(id.isEmpty()) {
//                Toast.makeText(this, R.string.id_required, Toast.LENGTH_SHORT).show()
                IDText.error = getString(R.string.id_required)
                return@setOnClickListener
            }

            // TODO - check id format here
            if(id.length > 20) {
                IDText.error = "20자를 넘을 수 없습니다"
                return@setOnClickListener
            }
            else {
                IDText.error = null
            }

            lifecycleScope.launch {
                changeName(id)
            }
        }
    }

    suspend fun changeName(id: String) {
        val request = ChangeNameRequest(id)
        try {
            val response = RetrofitInstance.api.changeName(request)

            if (response.isSuccessful) {
                Toast.makeText(this@ChangeNameActivity, R.string.on_change_successful, Toast.LENGTH_SHORT).show()
            } else {
                val result = response.errorBody()?.string()
                val message = Gson().fromJson(result, LogInResponse::class.java).message
                Log.e("change name", "response with ${response.code()}: $message")
                Toast.makeText(this@ChangeNameActivity, R.string.id_already_in_use, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("change name", "exception on api call")
            e.printStackTrace()
            Toast.makeText(this@ChangeNameActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
        }
    }
}