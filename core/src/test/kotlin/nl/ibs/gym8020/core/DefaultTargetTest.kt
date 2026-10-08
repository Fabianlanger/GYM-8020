package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.DefaultTarget
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultTargetTest {

    private fun squat(stored: List<PlannedSet>?, vararg today: PerformedSet, workout: List<SetRole> = listOf(HEAVY, LIGHT)) =
        DefaultTarget.of(stored, workout, today.toList(), Progression.WEIGHT, LoadType.EXTERNAL)

    @Test
    fun `each set gets the weight you lifted today, not the advice`() {
        val target = squat(
            listOf(PlannedSet(HEAVY, 100.0), PlannedSet(LIGHT, 70.0)),
            PerformedSet(HEAVY, 102.5, 12), PerformedSet(LIGHT, 70.0, 16),
        )
        assertEquals(listOf(PlannedSet(HEAVY, 102.5), PlannedSet(LIGHT, 70.0)), target)
    }

    @Test
    fun `a set you did not do today keeps its previous target`() {
        val target = squat(
            listOf(PlannedSet(HEAVY, 100.0), PlannedSet(LIGHT, 70.0)),
            PerformedSet(HEAVY, 102.5, 6), PerformedSet(LIGHT, null, null),
        )
        assertEquals(listOf(PlannedSet(HEAVY, 102.5), PlannedSet(LIGHT, 70.0)), target)
    }

    @Test
    fun `the sets of your stored target stay, filled per role from today`() {
        val target = squat(
            listOf(PlannedSet(HEAVY, 100.0), PlannedSet(LIGHT, 70.0)),
            PerformedSet(WARMUP, null, 10), PerformedSet(HEAVY, 105.0, 6), PerformedSet(LIGHT, 72.5, 15), PerformedSet(HEAVY, 100.0, 5),
        )
        assertEquals(listOf(PlannedSet(HEAVY, 105.0), PlannedSet(LIGHT, 72.5)), target)
    }

    @Test
    fun `without a stored target the sets come from the workout`() {
        val target = squat(
            null,
            PerformedSet(WARMUP, null, 10), PerformedSet(HEAVY, 80.0, 7), PerformedSet(LIGHT, 60.0, 14), PerformedSet(HEAVY, 77.5, 6),
            workout = listOf(HEAVY, LIGHT, HEAVY),
        )
        assertEquals(listOf(PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 60.0), PlannedSet(HEAVY, 77.5)), target)
    }

    @Test
    fun `an empty stored target counts as none`() {
        val target = squat(emptyList(), PerformedSet(HEAVY, 80.0, 7), PerformedSet(LIGHT, 60.0, 14))
        assertEquals(listOf(PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 60.0)), target)
    }

    @Test
    fun `with reps progression the target carries no weight`() {
        val target = DefaultTarget.of(
            listOf(PlannedSet(HEAVY, null), PlannedSet(LIGHT, null)), listOf(HEAVY, LIGHT),
            listOf(PerformedSet(HEAVY, 10.0, 20), PerformedSet(LIGHT, null, 35)),
            Progression.REPS, LoadType.BODYWEIGHT,
        )
        assertEquals(listOf(PlannedSet(HEAVY, null), PlannedSet(LIGHT, null)), target)
    }

    @Test
    fun `an empty field on a bodyweight exercise is body weight only`() {
        val target = DefaultTarget.of(
            listOf(PlannedSet(HEAVY, -20.0), PlannedSet(LIGHT, -30.0)), listOf(HEAVY, LIGHT),
            listOf(PerformedSet(HEAVY, null, 8), PerformedSet(LIGHT, null, null)),
            Progression.WEIGHT, LoadType.BODYWEIGHT,
        )
        assertEquals(listOf(PlannedSet(HEAVY, 0.0), PlannedSet(LIGHT, -30.0)), target)
    }

    @Test
    fun `a warm-up in the target has no weight`() {
        val target = squat(
            listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 100.0)),
            PerformedSet(WARMUP, 40.0, 10), PerformedSet(HEAVY, 100.0, 6),
        )
        assertEquals(listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 100.0)), target)
    }
}
