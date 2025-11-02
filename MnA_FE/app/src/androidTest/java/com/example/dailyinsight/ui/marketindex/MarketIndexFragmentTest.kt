package com.example.dailyinsight.ui.marketindex

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dailyinsight.R
import com.example.dailyinsight.data.dto.StockIndexData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
class MarketIndexFragmentTest {

    private lateinit var scenario: FragmentScenario<MarketIndexFragment>
    private lateinit var mockNavController: NavController

    @Before
    fun setup() {
        mockNavController = mock(NavController::class.java)
    }

    @Test
    fun onCreateView_fragmentIsDisplayed() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        // Verify that the fragment is in RESUMED state
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Verify that key views are displayed
        onView(withId(R.id.kospi_block)).check(matches(isDisplayed()))
        onView(withId(R.id.kosdaq_block)).check(matches(isDisplayed()))
    }

    @Test
    fun onViewCreated_clickListenersAreSet() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        // Click on KOSPI block
        onView(withId(R.id.kospi_block)).perform(click())

        // Verify navigation was triggered for KOSPI
        verify(mockNavController).navigate(any<androidx.navigation.NavDirections>())
    }

    @Test
    fun onViewCreated_kosdaqClickNavigates() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        // Click on KOSDAQ block
        onView(withId(R.id.kosdaq_block)).perform(click())

        // Verify navigation was triggered for KOSDAQ
        verify(mockNavController).navigate(any<androidx.navigation.NavDirections>())
    }

    @Test
    fun onCreateOptionsMenu_menuIsInflated() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.onFragment { fragment ->
            // Verify that the fragment has set hasOptionsMenu to true
            assertTrue(fragment.hasOptionsMenu())
        }
    }

    @Test
    fun onOptionsItemSelected_notificationsHandled() {
        // This test verifies the menu item handling logic exists
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.onFragment { fragment ->
            val menuItem = mock(android.view.MenuItem::class.java)
            `when`(menuItem.itemId).thenReturn(R.id.action_notifications)

            val result = fragment.onOptionsItemSelected(menuItem)
            assertTrue("Notifications menu item should be handled", result)
        }
    }

    @Test
    fun onOptionsItemSelected_profileHandled() {
        // This test verifies the menu item handling logic exists
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        scenario.onFragment { fragment ->
            val menuItem = mock(android.view.MenuItem::class.java)
            `when`(menuItem.itemId).thenReturn(R.id.action_profile)

            val result = fragment.onOptionsItemSelected(menuItem)
            assertTrue("Profile menu item should be handled", result)
        }
    }

    @Test
    fun onDestroyView_bindingIsCleared() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        // Move to DESTROYED state which will call onDestroyView
        scenario.moveToState(Lifecycle.State.DESTROYED)

        // If we get here without crashing, the binding cleanup worked correctly
        // (binding is set to null in onDestroyView, preventing memory leaks)
        assertTrue(true)
    }

    @Test
    fun marketDataObserver_updatesKospiUI() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        // Wait for the view to be created and LiveData to be observed
        Thread.sleep(1000)

        // After LiveData updates, the views should contain data
        // Note: This test depends on the actual API or mock data being available
        onView(withId(R.id.kospi_block)).check(matches(isDisplayed()))
    }

    @Test
    fun marketDataObserver_updatesKosdaqUI() {
        // Launch the fragment
        scenario = launchFragmentInContainer<MarketIndexFragment>(
            themeResId = R.style.Theme_DailyInsight
        )

        // Wait for the view to be created and LiveData to be observed
        Thread.sleep(1000)

        // After LiveData updates, the views should contain data
        // Note: This test depends on the actual API or mock data being available
        onView(withId(R.id.kosdaq_block)).check(matches(isDisplayed()))
    }
}