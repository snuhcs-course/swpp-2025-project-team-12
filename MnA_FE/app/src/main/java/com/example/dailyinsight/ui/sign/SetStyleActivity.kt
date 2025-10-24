package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.android.material.button.MaterialButton

import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.google.android.material.appbar.MaterialToolbar

class SetStyleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_style)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // TODO - handle selection

        val toNextButton = findViewById<MaterialButton>(R.id.toNextButton)
        toNextButton.setOnClickListener {
            // TODO - send style to server
            // and then
            val intent = Intent(this, MainActivity::class.java)
            finishAffinity()
            startActivity(intent)
        }
    }
}