package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton

import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.SetStyleRequest
import com.example.dailyinsight.data.dto.SetStyleResponse
import com.example.dailyinsight.data.dto.InterestsList
import com.example.dailyinsight.data.dto.Strategy
import com.example.dailyinsight.data.network.RetrofitInstance
import com.google.android.material.appbar.MaterialToolbar
import com.example.dailyinsight.model.Style
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.gson.Gson
import retrofit2.Call

class SetStyleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_style)

        val interests = intent.getStringArrayListExtra("interests") as ArrayList<String>
//        val t = interests.joinToString()
//        Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val stable = findViewById<MaterialButton>(R.id.styleStable)
        val aggressive = findViewById<MaterialButton>(R.id.styleAggressive)
        val neutral = findViewById<MaterialButton>(R.id.styleNeutral)
        val none = findViewById<MaterialCheckBox>(R.id.selectNone)
        val toNextButton = findViewById<MaterialButton>(R.id.toNextButton)
        stable.setOnClickListener {
            if(aggressive.isChecked) aggressive.toggle()
            if(neutral.isChecked) neutral.toggle()
            toNextButton.isEnabled = stable.isChecked
        }
        aggressive.setOnClickListener {
            if(stable.isChecked) stable.toggle()
            if(neutral.isChecked) neutral.toggle()
            toNextButton.isEnabled = aggressive.isChecked
        }
        neutral.setOnClickListener {
            if(stable.isChecked) stable.toggle()
            if(aggressive.isChecked) aggressive.toggle()
            toNextButton.isEnabled = neutral.isChecked
        }
        none.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                toNextButton.isEnabled = true
                stable.isEnabled = false
                aggressive.isEnabled = false
                neutral.isEnabled = false
            }
            else {
                toNextButton.isEnabled = stable.isChecked || aggressive.isChecked || neutral.isChecked
                stable.isEnabled = true
                aggressive.isEnabled = true
                neutral.isEnabled = true
            }
        }

        toNextButton.setOnClickListener {
            val strategy = when {
                none.isChecked -> Style.NONE.name
                stable.isChecked -> Style.STABLE.name
                aggressive.isChecked -> Style.AGGRESSIVE.name
                neutral.isChecked -> Style.NEUTRAL.name
                else -> Style.NONE.name
            }

            val request = SetStyleRequest(
                interests = InterestsList(interests),
                strategy = Strategy(strategy)
            )

            RetrofitInstance.api.setStyle(request)
                .enqueue(object : retrofit2.Callback<SetStyleResponse> {
                    override fun onResponse(
                        call: Call<SetStyleResponse>,
                        response: retrofit2.Response<SetStyleResponse>
                    ) {
                        if (response.isSuccessful) {
                            val intent = Intent(this@SetStyleActivity, MainActivity::class.java)
                            finishAffinity()
                            startActivity(intent)
                        } else {
                            val result = response.errorBody()?.string()
                            val message = Gson().fromJson(result, SetStyleResponse::class.java).message
                            Toast.makeText(this@SetStyleActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<SetStyleResponse>, t: Throwable) {
                        Toast.makeText(this@SetStyleActivity, "failed to sign up", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}