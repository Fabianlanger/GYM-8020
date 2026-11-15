package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.Comparison
import nl.ibs.gym8020.core.stats.SetRecord
import nl.ibs.gym8020.core.stats.SummaryHistory
import nl.ibs.gym8020.core.stats.TrainingRef
import nl.ibs.gym8020.core.stats.Trend
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The history of one training's summary (#100): an older training opened from the log compares with the training
 * before it and keeps the records as they held then; a later, better training never counts.
 */
class SummaryHistoryTest {
    private val day = 86_400_000L
    private val squat = 1L
    private val pullUp = 2L

    private fun t(id: Long, dayNr: Int, workout: String = "Full body") = TrainingRef(id, dayNr * day, workout)

    private fun s(
        session: Long, ex: Long, w: Double?, reps: Int, role: SetRole = SetRole.HEAVY,
        prog: Progression = Progression.WEIGHT,
    ) = SetRecord(session, LocalDate.of(2026, 10, session.toInt()), ex, role, w, reps, w, null, prog)

    // Day 1: squat 90 × 6 (108). Day 2: 100 × 4 (113,3), the training we open. Day 3: 120 × 5 (140), later and better.
    private val trainings = listOf(t(1, 1), t(2, 2), t(3, 3))
    private val sets = listOf(s(1, squat, 90.0, 6), s(2, squat, 100.0, 4), s(3, squat, 120.0, 5))

    @Test
    fun `an older training compares with the training before it, not with the latest`() {
        val h = assertNotNull(SummaryHistory.of(2, trainings, sets))
        assertEquals(listOf(1L), h.earlier.map { it.id })
        val last = assertNotNull(h.lastTime(squat))
        assertEquals(90.0, last.single().weightKg)
        val c = assertNotNull(Comparison.exercise(h.today(squat), last))
        assertEquals(Trend.UP, c.trend)
        assertEquals(113.333 - 108.0, c.change!!.absolute, 1e-3)
    }

    @Test
    fun `a record then stays a record when a later training was better`() {
        val h = assertNotNull(SummaryHistory.of(2, trainings, sets))
        val record = assertNotNull(h.weightRecord(squat))
        assertEquals(100.0, record.weightKg)
        assertEquals(4, record.reps)
    }

    @Test
    fun `no record when an earlier training was better, whatever came later`() {
        val h = assertNotNull(SummaryHistory.of(2, trainings, sets + s(1, squat, 110.0, 5)))
        assertNull(h.weightRecord(squat))
    }

    @Test
    fun `the first training of an exercise has no last time and no record`() {
        val h = assertNotNull(SummaryHistory.of(1, trainings, sets))
        assertTrue(h.earlier.isEmpty())
        assertNull(h.lastTime(squat))
        assertNull(h.weightRecord(squat))
        assertNull(h.previousOfWorkout)
    }

    @Test
    fun `right after finishing the newest training sees all earlier ones`() {
        val h = assertNotNull(SummaryHistory.of(3, trainings, sets))
        assertEquals(listOf(2L, 1L), h.earlier.map { it.id })
        // 120 × 5 = 140 against 100 × 4 = 113,3
        assertEquals(140.0 - 113.333, Comparison.exercise(h.today(squat), h.lastTime(squat))!!.change!!.absolute, 1e-3)
        assertNotNull(h.weightRecord(squat))
    }

    @Test
    fun `the previous session of the same workout is the one before, not a later one`() {
        val all = listOf(t(1, 1, "Upper"), t(2, 2, "Full body"), t(3, 3, "Upper"), t(4, 4, "Upper"))
        val h = assertNotNull(SummaryHistory.of(3, all, emptyList()))
        assertEquals(1L, h.previousOfWorkout?.id)
    }

    @Test
    fun `a training entered afterwards with an older date only sees what came before that date`() {
        // Id 4 is entered last but happened on day 2, between trainings 1 (day 1) and 3 (day 3).
        val all = trainings.filter { it.id != 2L } + t(4, 2)
        val h = assertNotNull(SummaryHistory.of(4, all, listOf(s(1, squat, 90.0, 6), s(3, squat, 120.0, 5), s(4, squat, 100.0, 4))))
        assertEquals(listOf(1L), h.earlier.map { it.id })
        assertNotNull(h.weightRecord(squat))
    }

    @Test
    fun `on the same start time the lower id counts as earlier`() {
        val all = listOf(t(5, 1), t(6, 1))
        assertEquals(listOf(5L), SummaryHistory.of(6, all, emptyList())!!.earlier.map { it.id })
        assertTrue(SummaryHistory.of(5, all, emptyList())!!.earlier.isEmpty())
    }

    @Test
    fun `a reps record counts only the trainings before it`() {
        fun p(session: Long, reps: Int) = s(session, pullUp, null, reps, prog = Progression.REPS)
        val repsSets = listOf(p(1, 8), p(2, 10), p(3, 15))
        val h = assertNotNull(SummaryHistory.of(2, trainings, repsSets))
        val record = assertNotNull(h.repsRecord(pullUp))
        assertEquals(10, record.reps)
        assertEquals(2, record.improvement)
    }

    @Test
    fun `the last sets for the older goal rule come from before the training`() {
        val h = assertNotNull(SummaryHistory.of(2, trainings, sets))
        assertEquals(1L, h.lastSets(squat)!!.single().sessionId)
    }

    @Test
    fun `an unknown training has no history`() {
        assertNull(SummaryHistory.of(99, trainings, sets))
    }
}
