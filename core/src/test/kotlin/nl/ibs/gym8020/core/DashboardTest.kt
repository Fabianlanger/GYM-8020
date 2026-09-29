package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleGroup
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.Dashboard
import nl.ibs.gym8020.core.stats.ExerciseTiming
import nl.ibs.gym8020.core.stats.Point
import nl.ibs.gym8020.core.stats.SessionRecord
import nl.ibs.gym8020.core.stats.SetRecord
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DashboardTest {
    private val mon = LocalDate.of(2026, 9, 28)
    private val bench = 1L
    private val squat = 2L
    private val crunch = 3L
    private val muscles = mapOf(
        bench to listOf(MuscleShare(Muscle.CHEST, 100), MuscleShare(Muscle.TRICEPS, 50)),
        squat to listOf(MuscleShare(Muscle.QUADS, 100)),
        crunch to listOf(MuscleShare(Muscle.ABS, 100)),
    )

    private fun set(session: Long, date: LocalDate, ex: Long, role: SetRole, w: Double?, reps: Int?, goal: Double? = null, prog: Progression = Progression.WEIGHT) =
        SetRecord(session, date, ex, role, w, reps, w, goal, prog)

    private val sets = listOf(
        set(1, mon.minusWeeks(1), bench, SetRole.WARMUP, null, 12),
        set(1, mon.minusWeeks(1), bench, SetRole.HEAVY, 60.0, 6, goal = 60.0),
        set(1, mon.minusWeeks(1), bench, SetRole.LIGHT, 40.0, 16, goal = 42.5),
        set(1, mon.minusWeeks(1), crunch, SetRole.HEAVY, 0.0, 20, prog = Progression.REPS),
        set(2, mon, bench, SetRole.HEAVY, 62.5, 7, goal = 62.5),
        set(2, mon, squat, SetRole.HEAVY, 100.0, 5),
        set(2, mon, crunch, SetRole.HEAVY, 0.0, 19, prog = Progression.REPS),
    )

    @Test
    fun `trainings per week`() {
        val weeks = Dashboard.trainingsPerWeek(listOf(SessionRecord(1, mon.minusWeeks(1), 1), SessionRecord(2, mon, 1), SessionRecord(3, mon.plusDays(2), 1)), mon.plusDays(3), 3)
        assertEquals(listOf(0.0, 1.0, 2.0), weeks.map { it.value })
        assertEquals(mon, weeks.last().weekStart)
    }

    @Test
    fun `exercise progress uses heavy sets only`() {
        val p = Dashboard.exerciseProgress(sets, bench)
        assertEquals(listOf(72.0, 62.5 * (1 + 7 / 30.0)), p.e1rm.map { it.value })
        assertEquals(listOf(60.0, 62.5), p.heavyWeight.map { it.value })
    }

    @Test
    fun `records per exercise`() {
        val r = Dashboard.records(sets)
        assertEquals(62.5, r.getValue(bench).weightKg)
        assertEquals(mon, r.getValue(bench).date)
    }

    @Test
    fun `sets per muscle skip warm-ups and weight secondary muscles`() {
        val perMuscle = Dashboard.setsPerMuscle(sets, muscles, mon, 2)
        assertEquals(listOf(2.0, 1.0), perMuscle.getValue(Muscle.CHEST).map { it.value })
        assertEquals(listOf(1.0, 0.5), perMuscle.getValue(Muscle.TRICEPS).map { it.value })
        val groups = Dashboard.byGroup(perMuscle)
        assertEquals(listOf(1.0, 0.5), groups.getValue(MuscleGroup.ARMS).map { it.value })
        assertEquals(listOf(0.0, 1.0), groups.getValue(MuscleGroup.LEGS).map { it.value })
        val vol = Dashboard.volumePerMuscle(sets, muscles, mon, 2)
        assertEquals(60.0 * 6 + 40.0 * 16, vol.getValue(Muscle.CHEST)[0].value, 1e-9)
    }

    @Test
    fun `balance`() {
        val perMuscle = Dashboard.setsPerMuscle(sets, muscles, mon, 2)
        assertEquals(1.0, Dashboard.pushPull(perMuscle).ratio)
        assertEquals(4.5 / 5.5, Dashboard.upperLower(perMuscle).ratio!!, 1e-9)
        assertNull(Dashboard.pushPull(emptyMap()).ratio)
    }

    @Test
    fun `goal results per set`() {
        val results = Dashboard.goalResults(sets)
        // bench heavy 60≥60 ✓, bench light 40<42.5 ✗, bench heavy 62.5 ✓, crunch 19 < 20 ✗ (first crunch has no reference)
        assertEquals(listOf(true, false, true, false), results.map { it.second })
        val rate = Dashboard.goalRatePerWeek(results, mon, 2)
        assertEquals(listOf(0.5, 0.5), rate.map { it.value })
    }

    @Test
    fun `time per position and moving average`() {
        val t = Dashboard.timePerPosition(listOf(ExerciseTiming(1, 1, 60_000), ExerciseTiming(2, 1, 80_000), ExerciseTiming(1, 2, null)))
        assertEquals(listOf(1 to 70_000.0), t)
        val pts = listOf(Point(mon, 80.0), Point(mon.plusDays(1), 82.0), Point(mon.plusDays(2), 84.0))
        assertEquals(listOf(80.0, 81.0, 83.0), Dashboard.movingAverage(pts, 2).map { it.value })
        assertEquals(0.05, Dashboard.change(pts)!!, 1e-9)
    }
}
