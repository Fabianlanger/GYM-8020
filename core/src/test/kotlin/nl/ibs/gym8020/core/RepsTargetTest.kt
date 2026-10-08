package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.RepsTarget
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals

class RepsTargetTest {

    @Test
    fun `each set aims at the reps of its role last time`() {
        val last = listOf(PerformedSet(HEAVY, null, 20), PerformedSet(LIGHT, null, 35))
        assertEquals(listOf(35, 20), RepsTarget.forSets(listOf(LIGHT, HEAVY), last))
    }

    @Test
    fun `without a last time there is no number`() {
        assertEquals(listOf(null, null), RepsTarget.forSets(listOf(HEAVY, LIGHT), null))
    }

    @Test
    fun `a warm-up has no target`() {
        val last = listOf(PerformedSet(WARMUP, null, 10), PerformedSet(HEAVY, null, 20))
        assertEquals(listOf(null, 20), RepsTarget.forSets(listOf(WARMUP, HEAVY), last))
    }

    @Test
    fun `the second heavy set aims at the second heavy set, sets not done do not count`() {
        val last = listOf(PerformedSet(HEAVY, null, 20), PerformedSet(LIGHT, null, null), PerformedSet(HEAVY, null, 17))
        assertEquals(listOf(20, null, 17, 17), RepsTarget.forSets(listOf(HEAVY, LIGHT, HEAVY, HEAVY), last))
    }
}
