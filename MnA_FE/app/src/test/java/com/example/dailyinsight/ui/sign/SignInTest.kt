package com.example.dailyinsight.ui.sign

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.shadows.ShadowToast
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import com.example.dailyinsight.R
import com.google.android.material.textfield.TextInputEditText
import org.junit.Before
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])  // Changed from 36 to 28 (Java 17 compatible)
class SignInToastTest {

    @Before
    fun enableVectorSupport() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    @Test
    fun clickLoginButton_withEmptyIdAndPassword_showToastRequiringId() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)

        // when
        button.performClick()

        // then
        val latestToastText = ShadowToast.getTextOfLatestToast()
        assertEquals("please enter id", latestToastText)
    }

    @Test
    fun clickLoginButton_withEmptyId_showToastRequiringId() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val pwField = activity.findViewById<TextInputEditText>(R.id.PWTextField)

        // when
        button.performClick()
        pwField.setText("Bob")

        // then
        val latestToastText = ShadowToast.getTextOfLatestToast()
        assertEquals("please enter id", latestToastText)
    }

    @Test
    fun clickLoginButton_withEmptyPassword_showToastRequiringPassword() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val idField = activity.findViewById<TextInputEditText>(R.id.IDTextField)

        // when
        idField.setText("Alice")
        button.performClick()

        // then
        val latestToastText = ShadowToast.getTextOfLatestToast()
        assertEquals("please enter password", latestToastText)
    }
}