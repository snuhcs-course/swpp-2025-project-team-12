package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class StyleTest {

    @Test
    fun style_allValuesAccessible() {
        // Test all enum values exist
        val allStyles = Style.values()
        assertEquals(4, allStyles.size)
    }

    @Test
    fun style_containsExpectedValues() {
        // Test specific values exist
        val styles = Style.values().toList()
        assertTrue(styles.contains(Style.STABLE))
        assertTrue(styles.contains(Style.AGGRESSIVE))
        assertTrue(styles.contains(Style.NEUTRAL))
        assertTrue(styles.contains(Style.NONE))
    }

    @Test
    fun style_valueOfWorks() {
        // Test valueOf works correctly
        assertEquals(Style.STABLE, Style.valueOf("STABLE"))
        assertEquals(Style.AGGRESSIVE, Style.valueOf("AGGRESSIVE"))
        assertEquals(Style.NEUTRAL, Style.valueOf("NEUTRAL"))
        assertEquals(Style.NONE, Style.valueOf("NONE"))
    }

    @Test
    fun style_ordinalValues() {
        // Test ordinal positions
        assertEquals(0, Style.STABLE.ordinal)
        assertEquals(1, Style.AGGRESSIVE.ordinal)
        assertEquals(2, Style.NEUTRAL.ordinal)
        assertEquals(3, Style.NONE.ordinal)
    }
}