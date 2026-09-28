package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.Signal
import kotlin.test.Test
import kotlin.test.assertEquals

class GoalsTest {
    private val settings = MethodSettings()

    @Test
    fun `signal follows the method table`() {
        // heavy target 6: ≥8 too light, 5..7 good, ≤4 too heavy
        assertEquals(Signal.TOO_LIGHT, GoalAdvisor.signal(8, 6))
        assertEquals(Signal.GOOD, GoalAdvisor.signal(7, 6))
        assertEquals(Signal.GOOD, GoalAdvisor.signal(5, 6))
        assertEquals(Signal.TOO_HEAVY, GoalAdvisor.signal(4, 6))
        // light target 16
        assertEquals(Signal.TOO_LIGHT, GoalAdvisor.signal(18, 16))
        assertEquals(Signal.GOOD, GoalAdvisor.signal(16, 16))
        assertEquals(Signal.TOO_HEAVY, GoalAdvisor.signal(14, 16))
    }

    @Test
    fun `margin is adjustable`() {
        assertEquals(Signal.GOOD, GoalAdvisor.signal(8, 6, goodMargin = 2))
        assertEquals(Signal.TOO_LIGHT, GoalAdvisor.signal(9, 6, goodMargin = 2))
    }

    @Test
    fun `plan keeps sets and roles and suggests weight per set`() {
        val performed = listOf(
            PerformedSet(SetRole.WARMUP, null, 12),
            PerformedSet(SetRole.HEAVY, 60.0, 9),
            PerformedSet(SetRole.LIGHT, 40.0, 16),
            PerformedSet(SetRole.HEAVY, 50.0, 3),
        )
        val plan = GoalAdvisor.planNext(performed, LoadType.EXTERNAL, Progression.WEIGHT, settings)
        assertEquals(
            listOf(
                PlannedSet(SetRole.WARMUP, null),
                PlannedSet(SetRole.HEAVY, 62.5),
                PlannedSet(SetRole.LIGHT, 40.0),
                PlannedSet(SetRole.HEAVY, 47.5),
            ),
            plan,
        )
    }

    @Test
    fun `assisted bodyweight gets less assistance when too light`() {
        val plan = GoalAdvisor.planNext(listOf(PerformedSet(SetRole.HEAVY, -20.0, 10)), LoadType.BODYWEIGHT, Progression.WEIGHT, settings)
        assertEquals(-17.5, plan.single().weightKg)
    }

    @Test
    fun `external weight never goes below zero`() {
        assertEquals(0.0, GoalAdvisor.suggestWeight(1.0, Signal.TOO_HEAVY, 2.5, LoadType.EXTERNAL))
    }

    @Test
    fun `reps progression has no weight goal`() {
        val plan = GoalAdvisor.planNext(listOf(PerformedSet(SetRole.HEAVY, 0.0, 25)), LoadType.BODYWEIGHT, Progression.REPS, settings)
        assertEquals(0.0, plan.single().weightKg)
    }

    @Test
    fun `goal reached`() {
        assertEquals(true, GoalAdvisor.goalReached(Progression.WEIGHT, 60.0, 60.0, null, 5))
        assertEquals(false, GoalAdvisor.goalReached(Progression.WEIGHT, 62.5, 60.0, null, 5))
        assertEquals(true, GoalAdvisor.goalReached(Progression.WEIGHT, -20.0, -17.5, null, 6))
        assertEquals(true, GoalAdvisor.goalReached(Progression.REPS, null, 0.0, 22, 22))
        assertEquals(false, GoalAdvisor.goalReached(Progression.REPS, null, 0.0, 22, 21))
        assertEquals(null, GoalAdvisor.goalReached(Progression.REPS, null, 0.0, null, 21))
    }
}
