package com.example.dailyinsight.ui.start

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.dailyinsight.MainActivity
import com.example.dailyinsight.R
import com.example.dailyinsight.ui.sign.SignInActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class StartTest {

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
    fun clickPassThroughButton_moveToMainActivity() {
        // GIVEN - launch app
        ActivityScenario.launch(StartActivity::class.java)

        // WHEN - click pass through button
        onView(withId(R.id.passThroughButton)).perform(click())

        // THEN - move to main activity
        Intents.intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun clickSignInButton_moveToSignInActivity() {
        // GIVEN - launch app
        ActivityScenario.launch(StartActivity::class.java)

        // WHEN - click sign in button
        onView(withId(R.id.signInButton)).perform(click())

        // THEN - move to sign in activity
        Intents.intended(hasComponent(SignInActivity::class.java.name))
    }
}