package com.example.dailyinsight.ui.profile

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import com.example.dailyinsight.data.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for ProfileViewModel using Robolectric.
 * Tests the LiveData flows for isLoggedIn and username.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        // Clear DataStore before each test
        runBlocking {
            context.cookieDataStore.edit { it.clear() }
        }

        // Initialize RetrofitInstance (needed for logout function)
        RetrofitInstance.init(context)
    }

    @After
    fun tearDown() {
        runBlocking {
            context.cookieDataStore.edit { it.clear() }
        }
        Dispatchers.resetMain()
    }

    // ===== isLoggedIn Tests =====

    @Test
    fun isLoggedIn_withNoToken_returnsFalse() {
        // Given: No access token in DataStore (default state after clear)
        val viewModel = ProfileViewModel(context)

        // When/Then: Observing isLoggedIn should be false
        val result = viewModel.isLoggedIn.getOrAwaitValue(2)
        assertFalse(result)
    }

    @Test
    fun isLoggedIn_withToken_returnsTrue() = runTest {
        // Given: Access token exists in DataStore
        context.cookieDataStore.edit { prefs ->
            prefs[CookieKeys.ACCESS_TOKEN] = "valid_token"
        }

        // When: Creating ViewModel after setting token
        val viewModel = ProfileViewModel(context)
        val result = viewModel.isLoggedIn.getOrAwaitValue(2)

        // Then: Should be true
        assertTrue(result)
    }

    @Test
    fun isLoggedIn_withEmptyToken_returnsFalse() = runTest {
        // Given: Empty access token
        context.cookieDataStore.edit { prefs ->
            prefs[CookieKeys.ACCESS_TOKEN] = ""
        }

        // When
        val viewModel = ProfileViewModel(context)
        val result = viewModel.isLoggedIn.getOrAwaitValue(2)

        // Then: Empty string should be considered not logged in
        assertFalse(result)
    }

    // ===== username Tests =====

    @Test
    fun username_withNoUsername_returnsGuest() {
        // Given: No username in DataStore (cleared in setup)
        val viewModel = ProfileViewModel(context)

        // When
        val result = viewModel.username.getOrAwaitValue(2)

        // Then: Should return "Guest"
        assertEquals("Guest", result)
    }

    @Test
    fun username_withUsername_returnsUsername() = runTest {
        // Given: Username exists in DataStore
        context.cookieDataStore.edit { prefs ->
            prefs[CookieKeys.USERNAME] = "testuser"
        }

        // When
        val viewModel = ProfileViewModel(context)
        val result = viewModel.username.getOrAwaitValue(2)

        // Then: Should return the username
        assertEquals("testuser", result)
    }

    // ===== ProfileViewModelFactory Tests =====

    @Test
    fun factory_createsProfileViewModel() {
        // Given
        val factory = ProfileViewModelFactory(context)

        // When
        val viewModel = factory.create(ProfileViewModel::class.java)

        // Then
        assertNotNull(viewModel)
        assertTrue(viewModel is ProfileViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun factory_throwsForUnknownClass() {
        // Given
        val factory = ProfileViewModelFactory(context)

        // When: Try to create unknown ViewModel class
        factory.create(UnknownViewModel::class.java)

        // Then: Should throw IllegalArgumentException
    }

    // ===== Combined State Tests =====

    @Test
    fun bothFieldsUpdate_whenDataStoreHasValues() = runTest {
        // Given: Set both token and username
        context.cookieDataStore.edit { prefs ->
            prefs[CookieKeys.ACCESS_TOKEN] = "token123"
            prefs[CookieKeys.USERNAME] = "johndoe"
        }

        // When
        val viewModel = ProfileViewModel(context)

        // Then: Both should reflect the DataStore values
        assertTrue(viewModel.isLoggedIn.getOrAwaitValue(2))
        assertEquals("johndoe", viewModel.username.getOrAwaitValue(2))
    }

    // Helper class for testing factory exception
    private class UnknownViewModel : ViewModel()

    // Helper extension to get LiveData value synchronously in tests with timeout
    private fun <T> androidx.lifecycle.LiveData<T>.getOrAwaitValue(timeoutSeconds: Long = 2): T {
        var data: T? = null
        val latch = CountDownLatch(1)
        val observer = Observer<T> { t ->
            data = t
            latch.countDown()
        }
        this.observeForever(observer)

        // Wait for data or timeout
        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            this.removeObserver(observer)
            throw AssertionError("LiveData value was never set within $timeoutSeconds seconds")
        }

        this.removeObserver(observer)
        @Suppress("UNCHECKED_CAST")
        return data as T
    }
}
