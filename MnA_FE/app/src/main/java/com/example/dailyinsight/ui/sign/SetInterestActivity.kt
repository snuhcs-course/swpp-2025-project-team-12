package com.example.dailyinsight.ui.sign

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.example.dailyinsight.R
import com.example.dailyinsight.data.model.Tag
import com.example.dailyinsight.utils.dp
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

class SetInterestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_interest)

        // app bar
        // go back on press back arrow button
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // interests selection
        val toNextButton = findViewById<MaterialButton>(R.id.toNextButton)
        val buttonGroup = findViewById<LinearLayout>(R.id.buttonGroup)
        val none = findViewById<MaterialCheckBox>(R.id.selectNone)

        var checkCount = 0
        for (tag in Tag.entries) {
            // make a button for each interest
            val button = LayoutInflater.from(this).inflate(
                R.layout.outlined_button,
                buttonGroup,
                false
            ) as MaterialButton
            // apply attributes on the button : text, width, height, checkable
            button.apply {
                text = tag.korean
                layoutParams = LinearLayout.LayoutParams(
                    344.dp,
                    LayoutParams.WRAP_CONTENT
                )
                isCheckable = true
            }
            // set listener on the button : enable toNextButton if any selected else disable
            button.setOnClickListener {
                if(button.isChecked) {
                    checkCount++
                    toNextButton.isEnabled = true
                }
                else checkCount--
                if(checkCount <= 0) toNextButton.isEnabled = false
            }
            // add the button on the scroll view
            buttonGroup.addView(button)
        }

        // check box of "i don't know"
        // if selected, enable toNextButton, disable any selection
        // else, restore the enable values
        none.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                toNextButton.isEnabled = true
                buttonGroup.children.forEach { it.isEnabled = false }
            }
            else {
                if(checkCount <= 0) toNextButton.isEnabled = false
                buttonGroup.children.forEach { it.isEnabled = true }
            }
        }

        // to the next button
        // send selected interests to the server
        // and go to the next activity : style selection
        toNextButton.setOnClickListener {
            // TODO - send interests to server
            buttonGroup.children.forEach {  }

            // and then
            val intent = Intent(this, SetStyleActivity::class.java)
            startActivity(intent)
        }
    }
}