package com.example.dailyinsight.ui.sign

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.dailyinsight.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SignInTest {

    @Before
    fun setup() {
        // initialize Espresso-Intents
        Intents.init()
    }

    @After
    fun release() {
        // release Espresso-Intents
        Intents.release()
    }

    @Test
    fun clickSignUpButton_moveToSignUpActivity() {
        // GIVEN - at sign in activity
        ActivityScenario.launch(SignInActivity::class.java)

        // WHEN - click sign up button
        onView(withId(R.id.signUpButton)).perform(click())

        // THEN - move to sign up activity
        Intents.intended(hasComponent(SignUpActivity::class.java.name))
    }
}