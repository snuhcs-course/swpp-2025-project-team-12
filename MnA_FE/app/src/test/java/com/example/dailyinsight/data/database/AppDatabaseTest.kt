package com.example.dailyinsight.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AppDatabase using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppDatabaseTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset singleton before each test
        resetDatabaseInstance()
    }

    @After
    fun tearDown() {
        // Close and reset after each test
        try {
            val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
            field.isAccessible = true
            val instance = field.get(null) as? AppDatabase
            instance?.close()
        } catch (_: Exception) {}
        resetDatabaseInstance()
    }

    private fun resetDatabaseInstance() {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    }

    // ===== getDatabase Tests =====

    @Test
    fun getDatabase_returnsNonNull() {
        val db = AppDatabase.getDatabase(context)
        assertNotNull(db)
    }

    @Test
    fun getDatabase_returnsSameInstance() {
        val db1 = AppDatabase.getDatabase(context)
        val db2 = AppDatabase.getDatabase(context)
        assertSame(db1, db2)
    }

    @Test
    fun getDatabase_returnsAppDatabase() {
        val db = AppDatabase.getDatabase(context)
        assertTrue(db is AppDatabase)
    }

    // ===== DAO Accessor Tests =====

    @Test
    fun historyCacheDao_returnsNonNull() {
        val db = AppDatabase.getDatabase(context)
        val dao = db.historyCacheDao()
        assertNotNull(dao)
    }

    @Test
    fun briefingDao_returnsNonNull() {
        val db = AppDatabase.getDatabase(context)
        val dao = db.briefingDao()
        assertNotNull(dao)
    }

    @Test
    fun stockDetailDao_returnsNonNull() {
        val db = AppDatabase.getDatabase(context)
        val dao = db.stockDetailDao()
        assertNotNull(dao)
    }

}
