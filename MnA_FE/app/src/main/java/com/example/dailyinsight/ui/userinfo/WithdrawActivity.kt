package com.example.dailyinsight.ui.userinfo

import android.content.Context
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
import com.example.dailyinsight.R
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.dto.LogInResponse
import com.example.dailyinsight.data.dto.UserProfileResponse
import com.example.dailyinsight.data.network.RetrofitInstance
import com.example.dailyinsight.ui.start.StartActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import android.view.inputmethod.InputMethodManager

class WithdrawActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_withdraw)
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
        val changeButton = findViewById<MaterialButton>(R.id.changeButton)

        PWTextField.setOnEditorActionListener { v, actionId, _ ->
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
            if(password.isEmpty()) {
                Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.withdraw_dialog_title)
                .setMessage(R.string.withdraw_dialog_content)
                .setNeutralButton(R.string.neutral_button) { dialog, which ->
                    // Respond to neutral button press
                    return@setNeutralButton
                }
                .setPositiveButton(R.string.yes_button) { dialog, which ->
                    // Respond to positive button press
                    lifecycleScope.launch {
                        withdraw()
                    }
                }
                .show()
        }
    }

    suspend fun withdraw() {
        try {
            val response = RetrofitInstance.api.withdraw()
            if (response.isSuccessful) {
                Toast.makeText(this@WithdrawActivity, R.string.on_withdraw_successful, Toast.LENGTH_SHORT).show()
                // if successful
                RetrofitInstance.cookieJar.clear()
                val intent = Intent(this@WithdrawActivity, StartActivity::class.java)
                finishAffinity()
                startActivity(intent)
            } else {
                val result = response.errorBody()?.string()
                val message = Gson().fromJson(result, LogInResponse::class.java).message
                Log.e("withdraw", "response with ${response.code()}: $message")
                Toast.makeText(this@WithdrawActivity, R.string.on_withdraw_unsuccessful, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("withdraw", "exception on api call")
            e.printStackTrace()
            Toast.makeText(this@WithdrawActivity, R.string.on_api_failure, Toast.LENGTH_SHORT).show()
        }
    }
}