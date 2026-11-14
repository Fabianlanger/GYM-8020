package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.RepsRecord
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import nl.ibs.gym8020.core.stats.SetRecord
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * #73: with reps progression a record is the most reps in one work set, at a signed load at least as heavy as the
 * previous record's (more reps with more assistance does not count).
 */
class RepsRecordTest {

    private var session = 0L

    /** One training of pull-ups at body weight [bodyweightKg]: sets as (role, signed load, reps). */
    private fun training(vararg sets: Triple<SetRole, Double?, Int>, bodyweightKg: Double = 80.0): List<SetRecord> {
        session++
        return sets.map { (role, load, reps) ->
            SetRecord(
                sessionId = session, date = LocalDate.of(2026, 10, 1).plusDays(session), exerciseId = 1, role = role,
                weightKg = load, reps = reps, effectiveKg = bodyweightKg + (load ?: 0.0), goalWeightKg = null,
                progression = Progression.REPS,
            )
        }
    }

    private fun set(role: SetRole, load: Double?, reps: Int) = Triple(role, load, reps)

    @Test
    fun `more reps than the record at the same load is a record, with the kg you moved and the improvement`() {
        val earlier = listOf(training(set(HEAVY, null, 19), set(LIGHT, null, 15)))
        val today = training(set(HEAVY, null, 22), set(LIGHT, null, 16), bodyweightKg = 76.0)

        assertEquals(RepsRecord(reps = 22, loadKg = 0.0, effectiveKg = 76.0, improvement = 3), RepsRecord.of(today, earlier))
    }

    @Test
    fun `more reps with more assistance does not count`() {
        val earlier = listOf(training(set(HEAVY, 0.0, 10)))

        assertNull(RepsRecord.of(training(set(HEAVY, -20.0, 15)), earlier))
    }

    @Test
    fun `more reps with less assistance does count`() {
        val earlier = listOf(training(set(HEAVY, -20.0, 10)))

        assertEquals(RepsRecord(12, -10.0, 70.0, 2), RepsRecord.of(training(set(HEAVY, -10.0, 12)), earlier))
    }

    @Test
    fun `the first time is no record`() {
        assertNull(RepsRecord.of(training(set(HEAVY, 0.0, 12)), emptyList()))
    }

    @Test
    fun `the record of all earlier trainings counts, not only the last time`() {
        val earlier = listOf(training(set(HEAVY, 0.0, 15)), training(set(HEAVY, 0.0, 12)))

        assertNull(RepsRecord.of(training(set(HEAVY, 0.0, 14)), earlier))
        assertEquals(1, RepsRecord.of(training(set(HEAVY, 0.0, 16)), earlier)?.improvement)
    }

    @Test
    fun `many reps with more assistance in between do not lower the bar`() {
        val earlier = listOf(training(set(HEAVY, 0.0, 15)), training(set(HEAVY, -20.0, 25)))

        assertNull(RepsRecord.of(training(set(HEAVY, -10.0, 18)), earlier))
        assertEquals(RepsRecord(17, 0.0, 80.0, 2), RepsRecord.of(training(set(HEAVY, 0.0, 17)), earlier))
    }

    @Test
    fun `today the most reps at a load that counts, never a set with more assistance`() {
        val earlier = listOf(training(set(HEAVY, 0.0, 19)))
        val today = training(set(HEAVY, 0.0, 22), set(LIGHT, -20.0, 30))

        assertEquals(RepsRecord(22, 0.0, 80.0, 3), RepsRecord.of(today, earlier))
    }

    @Test
    fun `warm-ups never count`() {
        val earlier = listOf(training(set(WARMUP, 0.0, 30), set(HEAVY, 0.0, 10)))

        assertEquals(2, RepsRecord.of(training(set(WARMUP, 0.0, 40), set(HEAVY, 0.0, 12)), earlier)?.improvement)
        assertNull(RepsRecord.of(training(set(WARMUP, 0.0, 40), set(HEAVY, 0.0, 9)), earlier))
    }
}
