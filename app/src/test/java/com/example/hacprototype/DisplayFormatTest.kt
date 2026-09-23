// ========================================
// START OF CODE
// ========================================

package com.example.hacprototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home and score screens format ISO dates and averages for display only. */
class DisplayFormatTest {

    @Test
    fun formatDisplayDateTurnsIsoIntoDayMonthYear() {
        assertEquals("12 September 2026", formatDisplayDate("2026-09-12"))
    }

    @Test
    fun formatDisplayDateLeavesUnparseableValuesAlone() {
        assertEquals("soon", formatDisplayDate("soon"))
        assertEquals("2026-13-01", formatDisplayDate("2026-13-01"))
    }

    @Test
    fun formatAverageUsesTwoDecimals() {
        assertTrue(formatAverage(8.733).matches(Regex("8[.,]73")))
    }

    @Test
    fun formatImprovementKeepsTheSign() {
        assertTrue(formatImprovement(12.5).matches(Regex("\\+12[.,]5%")))
        assertTrue(formatImprovement(-3.0).matches(Regex("-3[.,]0%")))
    }
}

// ========================================
// END OF CODE
// ========================================
