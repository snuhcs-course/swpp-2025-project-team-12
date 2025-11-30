package com.example.dailyinsight.ui.sign

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import com.example.dailyinsight.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.junit.Before
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignInToastTest {

    @Before
    fun enableVectorSupport() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    @Test
    fun clickLoginButton_withEmptyIdAndPassword_showErrorOnIdField() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val idLayout = activity.findViewById<TextInputLayout>(R.id.IDText)

        // when
        button.performClick()

        // then
        assertEquals(activity.getString(R.string.id_required), idLayout.error)
    }

    @Test
    fun clickLoginButton_withEmptyId_showErrorOnIdField() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val idLayout = activity.findViewById<TextInputLayout>(R.id.IDText)
        val pwField = activity.findViewById<TextInputEditText>(R.id.PWTextField)

        // when
        pwField.setText("password123")
        button.performClick()

        // then
        assertEquals(activity.getString(R.string.id_required), idLayout.error)
    }

    @Test
    fun clickLoginButton_withEmptyPassword_showErrorOnPasswordField() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val idField = activity.findViewById<TextInputEditText>(R.id.IDTextField)
        val pwLayout = activity.findViewById<TextInputLayout>(R.id.PWText)

        // when
        idField.setText("Alice")
        button.performClick()

        // then
        assertEquals(activity.getString(R.string.password_required), pwLayout.error)
    }

    @Test
    fun textChange_clearsIdError() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val idField = activity.findViewById<TextInputEditText>(R.id.IDTextField)
        val idLayout = activity.findViewById<TextInputLayout>(R.id.IDText)

        // when - 먼저 에러 발생시킴
        button.performClick()
        assertEquals(activity.getString(R.string.id_required), idLayout.error)

        // then - 텍스트 입력하면 에러 사라짐
        idField.setText("newId")
        assertNull(idLayout.error)
    }

    @Test
    fun textChange_clearsPasswordError() {
        // given
        val controller = Robolectric.buildActivity(SignInActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<MaterialButton>(R.id.loginButton)
        val idField = activity.findViewById<TextInputEditText>(R.id.IDTextField)
        val pwField = activity.findViewById<TextInputEditText>(R.id.PWTextField)
        val pwLayout = activity.findViewById<TextInputLayout>(R.id.PWText)

        // when - 먼저 에러 발생시킴
        idField.setText("Alice")
        button.performClick()
        assertEquals(activity.getString(R.string.password_required), pwLayout.error)

        // then - 텍스트 입력하면 에러 사라짐
        pwField.setText("newPassword")
        assertNull(pwLayout.error)
    }
}