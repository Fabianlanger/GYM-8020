package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.ProgressionSwitch
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgressionSwitchTest {

    @Test
    fun `reps exercises can go to weight, bodyweight exercises on weight can go to reps`() {
        assertEquals(Progression.WEIGHT, ProgressionSwitch.offered(Progression.REPS, LoadType.BODYWEIGHT))
        assertEquals(Progression.REPS, ProgressionSwitch.offered(Progression.WEIGHT, LoadType.BODYWEIGHT))
        assertNull(ProgressionSwitch.offered(Progression.WEIGHT, LoadType.EXTERNAL))
    }

    @Test
    fun `to weight on a bodyweight exercise starts at body weight only`() {
        val target = ProgressionSwitch.target(
            listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, null), PlannedSet(LIGHT, null)),
            listOf(PerformedSet(HEAVY, 5.0, 20), PerformedSet(LIGHT, null, 35)),
            Progression.WEIGHT, LoadType.BODYWEIGHT,
        )
        assertEquals(listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 0.0), PlannedSet(LIGHT, 0.0)), target)
    }

    @Test
    fun `to reps the target carries no weight`() {
        val target = ProgressionSwitch.target(
            listOf(PlannedSet(HEAVY, -20.0), PlannedSet(LIGHT, -30.0)),
            listOf(PerformedSet(HEAVY, -20.0, 8), PerformedSet(LIGHT, -30.0, 14)),
            Progression.REPS, LoadType.BODYWEIGHT,
        )
        assertEquals(listOf(PlannedSet(HEAVY, null), PlannedSet(LIGHT, null)), target)
    }
}
