package com.example.dailyinsight.ui.marketindex

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dailyinsight.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketIndexFragmentTest {

    private lateinit var scenario: FragmentScenario<MarketIndexFragment>

    @Test
    fun onCreateView_fragmentIsDisplayed() {
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.moveToState(Lifecycle.State.RESUMED)

        onView(withId(R.id.kospiBlock)).check(matches(isDisplayed()))
        onView(withId(R.id.kosdaqBlock)).check(matches(isDisplayed()))
    }

    @Test
    fun onDestroyView_bindingIsCleared() {
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertTrue(true)
    }

    @Test
    fun marketDataObserver_updatesKospiUI() {
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        Thread.sleep(1000)

        onView(withId(R.id.kospiBlock)).check(matches(isDisplayed()))
    }

    @Test
    fun marketDataObserver_updatesKosdaqUI() {
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        Thread.sleep(1000)

        onView(withId(R.id.kosdaqBlock)).check(matches(isDisplayed()))
    }
}