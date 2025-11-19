package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class StyleEnumTest {
    @Test
    fun style_allValuesExist() {
        assertEquals(4, Style.values().size)
    }

    @Test
    fun style_STABLE() {
        assertEquals("STABLE", Style.STABLE.name)
    }
}
