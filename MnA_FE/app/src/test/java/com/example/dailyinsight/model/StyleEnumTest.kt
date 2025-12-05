package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class StyleEnumTest {

    @Test
    fun style_allValuesExist() {
        assertEquals(4, Style.entries.size)
    }

    @Test
    fun style_STABLE_exists() {
        assertEquals("STABLE", Style.STABLE.name)
    }

    @Test
    fun style_AGGRESSIVE_exists() {
        assertEquals("AGGRESSIVE", Style.AGGRESSIVE.name)
    }

    @Test
    fun style_NEUTRAL_exists() {
        assertEquals("NEUTRAL", Style.NEUTRAL.name)
    }

    @Test
    fun style_NONE_exists() {
        assertEquals("NONE", Style.NONE.name)
    }

    @Test
    fun style_valueOf_returnsCorrectEnum() {
        assertEquals(Style.STABLE, Style.valueOf("STABLE"))
        assertEquals(Style.AGGRESSIVE, Style.valueOf("AGGRESSIVE"))
        assertEquals(Style.NEUTRAL, Style.valueOf("NEUTRAL"))
        assertEquals(Style.NONE, Style.valueOf("NONE"))
    }

    @Test
    fun style_ordinal_isConsistent() {
        val entries = Style.entries
        for (i in entries.indices) {
            assertEquals(i, entries[i].ordinal)
        }
    }

    @Test
    fun style_containsExpectedValues() {
        val styleNames = Style.entries.map { it.name }
        assertTrue(styleNames.contains("STABLE"))
        assertTrue(styleNames.contains("AGGRESSIVE"))
        assertTrue(styleNames.contains("NEUTRAL"))
        assertTrue(styleNames.contains("NONE"))
    }

    @Test
    fun style_entriesAreUnique() {
        val entries = Style.entries
        assertEquals(entries.size, entries.distinct().size)
    }
}
