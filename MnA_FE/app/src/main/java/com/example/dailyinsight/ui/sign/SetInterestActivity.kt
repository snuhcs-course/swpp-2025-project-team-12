package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dailyinsight.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SetInterestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_interest)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // TODO - handle selection

        val toNextButton = findViewById<MaterialButton>(R.id.toNextButton)
        toNextButton.setOnClickListener {
            // TODO - send interests to server
            // and then
            val intent = Intent(this, SetStyleActivity::class.java)
            startActivity(intent)
        }
    }
}