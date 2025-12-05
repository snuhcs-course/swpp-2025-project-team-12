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

    // ===== Additional Tests =====

    @Test
    fun style_name_returnsCorrectString() {
        assertEquals("STABLE", Style.STABLE.name)
        assertEquals("AGGRESSIVE", Style.AGGRESSIVE.name)
        assertEquals("NEUTRAL", Style.NEUTRAL.name)
        assertEquals("NONE", Style.NONE.name)
    }

    @Test
    fun style_entries_returnsAllValues() {
        val entries = Style.entries
        assertEquals(4, entries.size)
        assertTrue(entries.contains(Style.STABLE))
        assertTrue(entries.contains(Style.AGGRESSIVE))
        assertTrue(entries.contains(Style.NEUTRAL))
        assertTrue(entries.contains(Style.NONE))
    }

    @Test
    fun style_toString_returnsName() {
        assertEquals("STABLE", Style.STABLE.toString())
        assertEquals("AGGRESSIVE", Style.AGGRESSIVE.toString())
        assertEquals("NEUTRAL", Style.NEUTRAL.toString())
        assertEquals("NONE", Style.NONE.toString())
    }

    @Test
    fun style_equality() {
        val style1 = Style.STABLE
        val style2 = Style.STABLE
        assertEquals(style1, style2)
    }

    @Test
    fun style_inequality() {
        assertNotEquals(Style.STABLE, Style.AGGRESSIVE)
        assertNotEquals(Style.NEUTRAL, Style.NONE)
    }

    @Test
    fun style_compareTo() {
        assertTrue(Style.STABLE.compareTo(Style.AGGRESSIVE) < 0)
        assertTrue(Style.AGGRESSIVE.compareTo(Style.STABLE) > 0)
        assertEquals(0, Style.NEUTRAL.compareTo(Style.NEUTRAL))
    }

    @Test
    fun style_hashCode() {
        assertEquals(Style.STABLE.hashCode(), Style.STABLE.hashCode())
        assertNotEquals(Style.STABLE.hashCode(), Style.AGGRESSIVE.hashCode())
    }

    @Test
    fun style_javaClass() {
        assertEquals(Style::class.java, Style.STABLE.javaClass)
        assertEquals(Style::class.java, Style.AGGRESSIVE.javaClass)
    }

    @Test(expected = IllegalArgumentException::class)
    fun style_valueOf_invalidValue_throws() {
        Style.valueOf("INVALID")
    }

    @Test
    fun style_stableIsFirst() {
        assertEquals(Style.STABLE, Style.values().first())
    }

    @Test
    fun style_noneIsLast() {
        assertEquals(Style.NONE, Style.values().last())
    }
}