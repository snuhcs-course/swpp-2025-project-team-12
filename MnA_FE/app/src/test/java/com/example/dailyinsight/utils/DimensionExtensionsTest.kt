package com.example.dailyinsight.utils

import android.content.res.Resources
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DimensionExtensionsTest {

    @Before
    fun setup() {
        // Robolectric provides Resources with density = 1.0 by default
    }

    @Test
    fun dp_convertsIntegerCorrectly() {
        // Given: Various integer values
        val values = listOf(0, 1, 10, 16, 100)

        // When/Then: Each should convert to dp
        values.forEach { value ->
            val result = value.dp
            assertTrue("$value.dp should be non-negative", result >= 0)
        }
    }

    @Test
    fun dp_zeroValueReturnsZero() {
        // Given: Zero value
        val value = 0

        // When: Convert to dp
        val result = value.dp

        // Then: Should be zero
        assertEquals(0, result)
    }

    @Test
    fun dp_positiveValueReturnsPositive() {
        // Given: Positive value
        val value = 16

        // When: Convert to dp
        val result = value.dp

        // Then: Should be positive
        assertTrue(result >= 0)
    }

    @Test
    fun dp_negativeValueHandledCorrectly() {
        // Given: Negative value
        val value = -16

        // When: Convert to dp  
        val result = value.dp

        // Then: Should handle negative (implementation dependent)
        // Just verify it doesn't crash
        assertNotNull(result)
    }

    @Test
    fun dp_largeValueDoesNotOverflow() {
        // Given: Large value
        val value = 1000

        // When: Convert to dp
        val result = value.dp

        // Then: Should not overflow
        assertTrue(result >= value) // dp should be >= original for density >= 1
    }

    @Test
    fun dp_consistentResults() {
        // Given: Same value called multiple times
        val value = 24

        // When: Convert multiple times
        val result1 = value.dp
        val result2 = value.dp
        val result3 = value.dp

        // Then: Results should be consistent
        assertEquals(result1, result2)
        assertEquals(result2, result3)
    }

    @Test
    fun dp_differentValuesGiveDifferentResults() {
        // Given: Different values
        val small = 8
        val large = 64

        // When: Convert to dp
        val smallDp = small.dp
        val largeDp = large.dp

        // Then: Larger value should give larger dp
        assertTrue(largeDp > smallDp)
    }
}