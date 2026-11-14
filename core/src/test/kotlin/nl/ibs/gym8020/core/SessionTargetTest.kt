package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.SessionTarget
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals

/** #92 (decision #90): the session target follows the role, not the place. Synthetic data. */
class SessionTargetTest {
    private val h70l40 = listOf(PlannedSet(HEAVY, 70.0), PlannedSet(LIGHT, 40.0))

    private fun aims(vararg roles: SetRole) = SessionTarget.byRole(h70l40, roles.toList()).map { it?.weightKg }

    @Test
    fun `prototype situation 2 - add a set and switch roles, the session target follows the role`() {
        // The training starts with H 70 · L 40.
        assertEquals(listOf(70.0, 40.0), aims(HEAVY, LIGHT))
        // ＋ Set: a heavy set at the end, for which there was no target.
        assertEquals(listOf(70.0, 40.0, null), aims(HEAVY, LIGHT, HEAVY))
        // The second set becomes heavy, the third light: heavy · heavy · light.
        assertEquals(listOf(70.0, null, null), aims(HEAVY, HEAVY, HEAVY))
        val sessionTargets = aims(HEAVY, HEAVY, LIGHT)
        assertEquals(listOf(70.0, null, 40.0), sessionTargets)

        // You do 70 × 6, 50 × 7 and 40 × 16: the extra set has no session target and does not count.
        val lifted = listOf(70.0, 50.0, 40.0)
        val reached = sessionTargets.zip(lifted) { goal, kg -> GoalAdvisor.goalReached(Progression.WEIGHT, goal, kg, null, null) }
        assertEquals(listOf(true, null, true), reached)
        assertEquals(2, reached.count { it != null })
        assertEquals(2, reached.count { it == true })
    }

    @Test
    fun `switching a role back gives the set its target back`() {
        assertEquals(listOf(40.0, null), aims(LIGHT, LIGHT))
        assertEquals(listOf(70.0, 40.0), aims(HEAVY, LIGHT))
    }

    @Test
    fun `warm-ups have no session target and do not shift the work sets`() {
        val start = listOf(PlannedSet(WARMUP, null)) + h70l40

        assertEquals(listOf(null, null, 70.0, 40.0), SessionTarget.byRole(start, listOf(WARMUP, WARMUP, HEAVY, LIGHT)).map { it?.weightKg })
    }

    @Test
    fun `with two heavy sets the k-th heavy set aims at the k-th heavy target`() {
        val start = listOf(PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 50.0), PlannedSet(LIGHT, 20.0))

        assertEquals(listOf(60.0, 20.0, 50.0), SessionTarget.byRole(start, listOf(HEAVY, LIGHT, HEAVY)).map { it?.weightKg })
    }

    @Test
    fun `with reps progression the reps target follows the role too`() {
        val start = listOf(PlannedSet(HEAVY, null, 12), PlannedSet(LIGHT, null, 30))

        assertEquals(listOf(30, 12), SessionTarget.byRole(start, listOf(LIGHT, HEAVY)).map { it?.reps })
    }
}
