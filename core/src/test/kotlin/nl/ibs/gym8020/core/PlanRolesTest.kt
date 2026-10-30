package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.PlanRoles
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanRolesTest {
    @Test
    fun `B-3 an added set can be switched from heavy to light, warm-up and back`() {
        // Plan: heavy 85, light 60, and a new set (added as heavy 85).
        val plan = listOf(PlannedSet(HEAVY, 85.0), PlannedSet(LIGHT, 60.0), PlannedSet(HEAVY, 85.0))
        val light = PlanRoles.cycle(plan, 2)
        assertEquals(PlannedSet(LIGHT, 60.0), light[2], "takes the weight of the other light set")
        val warmup = PlanRoles.cycle(light, 2)
        assertEquals(PlannedSet(WARMUP, null), warmup[2], "warm-ups have no planned weight")
        val heavy = PlanRoles.cycle(warmup, 2)
        assertEquals(PlannedSet(HEAVY, 85.0), heavy[2], "back to heavy with the heavy weight")
        // The other sets are untouched.
        assertEquals(plan.take(2), heavy.take(2))
    }

    @Test
    fun `rule - cycling three times returns the same role`() {
        val plan = listOf(PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 55.0))
        for (i in plan.indices) {
            val back = PlanRoles.cycle(PlanRoles.cycle(PlanRoles.cycle(plan, i), i), i)
            assertEquals(plan[i].role, back[i].role)
        }
    }

    @Test
    fun `#69 with reps targets a switched set takes the reps of the other set with that role`() {
        val plan = listOf(PlannedSet(HEAVY, null, 18), PlannedSet(LIGHT, null, 30), PlannedSet(HEAVY, null, 18))
        val light = PlanRoles.cycle(plan, 2)
        assertEquals(PlannedSet(LIGHT, null, 30), light[2])
        val warmup = PlanRoles.cycle(light, 2)
        assertEquals(PlannedSet(WARMUP, null), warmup[2], "warm-ups have no target")
        assertEquals(PlannedSet(HEAVY, null, 18), PlanRoles.cycle(warmup, 2)[2])
    }
}
