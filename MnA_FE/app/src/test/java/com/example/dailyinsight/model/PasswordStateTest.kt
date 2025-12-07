package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class PasswordStateTest {

    @Test
    fun passwordState_allValuesExist() {
        assertEquals(6, PasswordState.entries.size)
    }

    @Test
    fun passwordState_VALID_exists() {
        assertEquals("VALID", PasswordState.VALID.name)
    }

    @Test
    fun passwordState_TOO_SHORT_exists() {
        assertEquals("TOO_SHORT", PasswordState.TOO_SHORT.name)
    }

    @Test
    fun passwordState_TOO_LONG_exists() {
        assertEquals("TOO_LONG", PasswordState.TOO_LONG.name)
    }

    @Test
    fun passwordState_NO_UPPERCASE_exists() {
        assertEquals("NO_UPPERCASE", PasswordState.NO_UPPERCASE.name)
    }

    @Test
    fun passwordState_NO_LOWERCASE_exists() {
        assertEquals("NO_LOWERCASE", PasswordState.NO_LOWERCASE.name)
    }

    @Test
    fun passwordState_NO_DIGIT_exists() {
        assertEquals("NO_DIGIT", PasswordState.NO_DIGIT.name)
    }

    @Test
    fun passwordState_valueOf_returnsCorrectEnum() {
        assertEquals(PasswordState.VALID, PasswordState.valueOf("VALID"))
        assertEquals(PasswordState.TOO_SHORT, PasswordState.valueOf("TOO_SHORT"))
        assertEquals(PasswordState.TOO_LONG, PasswordState.valueOf("TOO_LONG"))
        assertEquals(PasswordState.NO_UPPERCASE, PasswordState.valueOf("NO_UPPERCASE"))
        assertEquals(PasswordState.NO_LOWERCASE, PasswordState.valueOf("NO_LOWERCASE"))
        assertEquals(PasswordState.NO_DIGIT, PasswordState.valueOf("NO_DIGIT"))
    }

    @Test
    fun passwordState_ordinal_isConsistent() {
        val entries = PasswordState.entries
        for (i in entries.indices) {
            assertEquals(i, entries[i].ordinal)
        }
    }

    @Test
    fun passwordState_containsExpectedValues() {
        val stateNames = PasswordState.entries.map { it.name }
        assertTrue(stateNames.contains("VALID"))
        assertTrue(stateNames.contains("TOO_SHORT"))
        assertTrue(stateNames.contains("TOO_LONG"))
        assertTrue(stateNames.contains("NO_UPPERCASE"))
        assertTrue(stateNames.contains("NO_LOWERCASE"))
        assertTrue(stateNames.contains("NO_DIGIT"))
    }

    @Test
    fun passwordState_VALID_isFirst() {
        assertEquals(0, PasswordState.VALID.ordinal)
    }

    @Test
    fun passwordState_entriesAreUnique() {
        val entries = PasswordState.entries
        assertEquals(entries.size, entries.distinct().size)
    }

    @Test
    fun passwordState_canBeUsedInWhen() {
        val state = PasswordState.TOO_SHORT
        val message = when (state) {
            PasswordState.VALID -> "Valid password"
            PasswordState.TOO_SHORT -> "Password is too short"
            PasswordState.TOO_LONG -> "Password is too long"
            PasswordState.NO_UPPERCASE -> "Password needs uppercase"
            PasswordState.NO_LOWERCASE -> "Password needs lowercase"
            PasswordState.NO_DIGIT -> "Password needs digit"
        }
        assertEquals("Password is too short", message)
    }

    @Test
    fun passwordState_isValidCheck() {
        val validState = PasswordState.VALID
        val invalidState = PasswordState.TOO_SHORT

        assertTrue(validState == PasswordState.VALID)
        assertFalse(invalidState == PasswordState.VALID)
    }

    @Test
    fun passwordState_errorStates() {
        val errorStates = PasswordState.entries.filter { it != PasswordState.VALID }
        assertEquals(5, errorStates.size)
    }
}
