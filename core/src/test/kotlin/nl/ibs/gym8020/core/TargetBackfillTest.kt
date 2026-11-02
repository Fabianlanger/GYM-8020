package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.BackfillEntry
import nl.ibs.gym8020.core.logic.FromCurrentTarget
import nl.ibs.gym8020.core.logic.FromNextSession
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.TargetBackfill
import nl.ibs.gym8020.core.model.SetRole
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Schema v3, owner decision Q3: the target for next time of an older training is derived from the session target
 * of the next training with that exercise. Nothing is invented: without a session target there is no target.
 */
class TargetBackfillTest {
    private fun entry(
        seId: Long,
        startedAt: Long,
        target: List<PlannedSet>,
        exerciseId: Long = 1,
        sessionId: Long = seId,
        finished: Boolean = true,
        skipped: Boolean = false,
    ) = BackfillEntry(seId, exerciseId, sessionId, startedAt, finished, skipped, target)

    private val goal80 = listOf(PlannedSet(SetRole.HEAVY, 80.0), PlannedSet(SetRole.LIGHT, 60.0))
    private val goal85 = listOf(PlannedSet(SetRole.HEAVY, 85.0), PlannedSet(SetRole.LIGHT, 62.5))

    @Test
    fun `the target of a training is the session target of the next training with that exercise`() {
        val result = TargetBackfill.derive(listOf(entry(1, 1_000, goal80), entry(2, 2_000, goal85)), emptySet())

        assertEquals(FromNextSession(1, goal85), result.single { it.sessionExerciseId == 1L })
    }

    @Test
    fun `the next training is found by date, not by the order of entry`() {
        val entries = listOf(entry(5, 3_000, goal85), entry(6, 1_000, emptyList()), entry(7, 2_000, goal80))

        val result = TargetBackfill.derive(entries, emptySet()).associateBy { it.sessionExerciseId }

        assertEquals(FromNextSession(6, goal80), result[6])
        assertEquals(FromNextSession(7, goal85), result[7])
    }

    @Test
    fun `other exercises in between do not count`() {
        val entries = listOf(
            entry(1, 1_000, goal80, exerciseId = 1),
            entry(2, 2_000, goal85, exerciseId = 2),
            entry(3, 3_000, goal85, exerciseId = 1),
        )

        assertEquals(FromNextSession(1, goal85), TargetBackfill.derive(entries, emptySet()).single { it.sessionExerciseId == 1L })
    }

    @Test
    fun `a training that already has a target keeps it`() {
        val result = TargetBackfill.derive(listOf(entry(1, 1_000, goal80), entry(2, 2_000, goal85)), setOf(1L, 2L))

        assertEquals(emptyList(), result)
    }

    @Test
    fun `without a session target in the next training nothing is derived`() {
        val result = TargetBackfill.derive(listOf(entry(1, 1_000, goal80), entry(2, 2_000, emptyList())), setOf(2L))

        assertEquals(emptyList(), result)
    }

    @Test
    fun `reps targets come along, warm-ups and sets without a session target do not`() {
        val next = listOf(
            PlannedSet(SetRole.WARMUP, 20.0),
            PlannedSet(SetRole.HEAVY, null, 22),
            PlannedSet(SetRole.LIGHT, null, null),
            PlannedSet(SetRole.HEAVY, 0.0, 30),
        )

        val result = TargetBackfill.derive(listOf(entry(1, 1_000, emptyList()), entry(2, 2_000, next)), setOf(2L))

        assertEquals(
            FromNextSession(1, listOf(PlannedSet(SetRole.HEAVY, null, 22), PlannedSet(SetRole.HEAVY, 0.0, 30))),
            result.single(),
        )
    }

    @Test
    fun `the latest finished training gets the current target`() {
        val result = TargetBackfill.derive(listOf(entry(1, 1_000, goal80), entry(2, 2_000, goal85)), emptySet())

        assertEquals(FromCurrentTarget(2, exerciseId = 1), result.single { it.sessionExerciseId == 2L })
    }

    @Test
    fun `a skipped or unfinished latest training gets no target`() {
        val skipped = TargetBackfill.derive(listOf(entry(1, 1_000, goal80, skipped = true)), emptySet())
        val open = TargetBackfill.derive(listOf(entry(1, 1_000, goal80, finished = false)), emptySet())

        assertEquals(emptyList(), skipped)
        assertEquals(emptyList(), open)
    }

    @Test
    fun `a training in progress still tells the target of the training before it`() {
        val result = TargetBackfill.derive(listOf(entry(1, 1_000, goal80), entry(2, 2_000, goal85, finished = false)), emptySet())

        assertEquals(listOf(FromNextSession(1, goal85)), result)
    }
}
