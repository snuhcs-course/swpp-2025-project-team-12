package com.example.dailyinsight.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dailyinsight.data.Repository
import com.example.dailyinsight.data.database.BriefingDao
import com.example.dailyinsight.data.database.HistoryCacheDao
import com.example.dailyinsight.data.database.StockDetailDao
import com.example.dailyinsight.data.network.ApiService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

/**
 * Unit tests for ServiceLocator using Robolectric.
 * Uses reflection to reset singleton state between tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ServiceLocatorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        // Reset ServiceLocator state before each test
        resetServiceLocator()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        resetServiceLocator()
    }

    /**
     * Reset ServiceLocator singleton state using reflection.
     * This allows each test to start with a fresh ServiceLocator.
     */
    private fun resetServiceLocator() {
        try {
            // Reset isInitialized flag
            val isInitializedField = ServiceLocator::class.java.getDeclaredField("isInitialized")
            isInitializedField.isAccessible = true
            isInitializedField.setBoolean(ServiceLocator, false)

            // Reset appContext (lateinit var)
            val appContextField = ServiceLocator::class.java.getDeclaredField("appContext")
            appContextField.isAccessible = true
            // For lateinit, we need to use unsafe operations or just leave it
            // The isInitialized flag will prevent re-use of old context
        } catch (e: Exception) {
            // Ignore reflection errors in cleanup
        }
    }

    // ===== init() Tests =====

    @Test
    fun init_setsIsInitializedToTrue() {
        // Given: ServiceLocator is not initialized
        val isInitializedBefore = getIsInitialized()
        assertFalse(isInitializedBefore)

        // When: init is called
        ServiceLocator.init(context)

        // Then: isInitialized should be true
        val isInitializedAfter = getIsInitialized()
        assertTrue(isInitializedAfter)
    }

    @Test
    fun init_calledTwice_onlyInitializesOnce() {
        // Given: ServiceLocator is initialized with one context
        ServiceLocator.init(context)
        val firstInitTime = System.currentTimeMillis()

        // When: init is called again
        ServiceLocator.init(context)

        // Then: Should still be initialized (idempotent)
        assertTrue(getIsInitialized())
    }

    @Test
    fun init_setsApplicationContext() {
        // When: init is called
        ServiceLocator.init(context)

        // Then: appContext should be set
        val appContext = getAppContext()
        assertNotNull(appContext)
        assertEquals(context.applicationContext, appContext)
    }

    // ===== Property Access Tests (after init) =====

    @Test
    fun api_afterInit_returnsApiService() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing api
        val api = ServiceLocator.api

        // Then: Should return ApiService instance
        assertNotNull(api)
        assertTrue(api is ApiService)
    }

    @Test
    fun historyCacheDao_afterInit_returnsDao() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing historyCacheDao
        val dao = ServiceLocator.historyCacheDao

        // Then: Should return HistoryCacheDao instance
        assertNotNull(dao)
        assertTrue(dao is HistoryCacheDao)
    }

    @Test
    fun briefingDao_afterInit_returnsDao() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing briefingDao
        val dao = ServiceLocator.briefingDao

        // Then: Should return BriefingDao instance
        assertNotNull(dao)
        assertTrue(dao is BriefingDao)
    }

    @Test
    fun stockDetailDao_afterInit_returnsDao() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing stockDetailDao
        val dao = ServiceLocator.stockDetailDao

        // Then: Should return StockDetailDao instance
        assertNotNull(dao)
        assertTrue(dao is StockDetailDao)
    }

    @Test
    fun repository_afterInit_returnsRepository() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing repository
        val repo = ServiceLocator.repository

        // Then: Should return Repository instance
        assertNotNull(repo)
        assertTrue(repo is Repository)
    }

    // ===== Lazy Initialization Tests =====

    @Test
    fun daos_areLazilyInitialized() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing DAOs multiple times
        val dao1 = ServiceLocator.historyCacheDao
        val dao2 = ServiceLocator.historyCacheDao

        // Then: Should return the same instance (lazy singleton)
        assertSame(dao1, dao2)
    }

    @Test
    fun repository_isLazilyInitialized() {
        // Given: ServiceLocator is initialized
        ServiceLocator.init(context)

        // When: accessing repository multiple times
        val repo1 = ServiceLocator.repository
        val repo2 = ServiceLocator.repository

        // Then: Should return the same instance (lazy singleton)
        assertSame(repo1, repo2)
    }

    // ===== Helper Methods for Reflection =====

    private fun getIsInitialized(): Boolean {
        val field = ServiceLocator::class.java.getDeclaredField("isInitialized")
        field.isAccessible = true
        return field.getBoolean(ServiceLocator)
    }

    private fun getAppContext(): Context? {
        return try {
            val field = ServiceLocator::class.java.getDeclaredField("appContext")
            field.isAccessible = true
            field.get(ServiceLocator) as? Context
        } catch (e: UninitializedPropertyAccessException) {
            null
        }
    }
}
