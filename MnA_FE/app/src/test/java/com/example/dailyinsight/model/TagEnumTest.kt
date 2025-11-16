package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class TagEnumTest {
    @Test
    fun tag_allValuesExist() {
        assertEquals(26, Tag.values().size)
    }

    @Test
    fun tag_IT_SERVICES() {
        assertEquals("IT서비스", Tag.IT_SERVICES.korean)
    }
}
