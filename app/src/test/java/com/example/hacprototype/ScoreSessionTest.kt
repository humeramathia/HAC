// ========================================
// START OF CODE
// ========================================

package com.example.hacprototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Derived totals on [ScoreSession] must match the Python scoring rules:
 * max is arrows × 10, X counts as 10, and empty sessions stay at zero.
 */
class ScoreSessionTest {

    private fun session(vararg ends: List<ArrowScore>): ScoreSession {
        return ScoreSession(
            sessionId = "S1",
            memberId = "M001",
            type = SessionType.PRACTICE,
            title = "Test",
            distanceMeters = 18,
            arrowsPerEnd = ends.firstOrNull()?.size ?: 6,
            numberOfEnds = ends.size.coerceAtLeast(1),
            date = "2026-09-12",
            ends = ends.mapIndexed { index, arrows ->
                ScoreEnd(index + 1, arrows.toMutableList())
            }.toMutableList()
        )
    }

    @Test
    fun totalsCountXAsTenAndTrackXSeparately() {
        val scored = session(
            listOf(ArrowScore(10, true), ArrowScore(9), ArrowScore(8)),
            listOf(ArrowScore(7), ArrowScore(6), ArrowScore(5))
        )
        assertEquals(45, scored.totalScore)
        assertEquals(60, scored.maxScore)
        assertEquals(1, scored.tensCount)
        assertEquals(1, scored.xCount)
        assertEquals(27, scored.highestEnd)
        assertEquals(18, scored.lowestEnd)
        assertEquals("X", scored.ends[0].arrows[0].label)
    }

    @Test
    fun emptySessionHasZeroAverages() {
        val empty = session()
        empty.ends.clear()
        assertEquals(0.0, empty.averageArrow, 0.0)
        assertEquals(0.0, empty.averageEnd, 0.0)
        assertEquals(0, empty.highestEnd)
        assertEquals("0 / 60", empty.scoreLabel)
    }

    @Test
    fun leagueStandardIsSixtyArrows() {
        assertEquals(60, LeagueStandard.TOTAL_ARROWS)
        assertEquals(6, LeagueStandard.ARROWS_PER_END)
        assertEquals(10, LeagueStandard.numberOfEnds)
    }

    @Test
    fun toScoreFlattensTheSessionForOlderLists() {
        val scored = session(listOf(ArrowScore(10), ArrowScore(10), ArrowScore(10)))
        val flat = scored.toScore()
        assertEquals(30, flat.scoreValue)
        assertEquals(scored.date, flat.scoreDate)
        assertEquals(scored.title, flat.event)
    }

    @Test
    fun memberFullNameJoinsFirstAndLast() {
        val member = Member("M1", "Aaliyah", "Smit", "a@b.c", "Member", true, "2024-01-10")
        assertEquals("Aaliyah Smit", member.fullName)
        assertTrue(member.emailVerified)
    }
}

// ========================================
// END OF CODE
// ========================================
