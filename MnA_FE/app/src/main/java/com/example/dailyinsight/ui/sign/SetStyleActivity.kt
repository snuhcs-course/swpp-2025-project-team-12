package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton

import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.google.android.material.appbar.MaterialToolbar
import com.example.dailyinsight.model.Style
import com.google.android.material.checkbox.MaterialCheckBox

class SetStyleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_style)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // TODO - handle selection
        var style = Style.NONE
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
            style = when {
                none.isChecked -> Style.NONE
                stable.isChecked -> Style.STABLE
                aggressive.isChecked -> Style.AGGRESSIVE
                neutral.isChecked -> Style.NEUTRAL
                else -> Style.NONE
            }
            // TODO - send style to server
            // and then
            val intent = Intent(this, MainActivity::class.java)
            finishAffinity()
            startActivity(intent)
        }
    }
}