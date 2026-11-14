package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.RepsTarget
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import kotlin.test.Test
import kotlin.test.assertEquals

/** #91: a set skipped last time must not shift the reps target of the sets after it. */
class RepsTargetSkippedSetTest {

    @Test
    fun `#91 the first heavy set does not take the reps of the second when the first was skipped`() {
        val last = listOf(PerformedSet(HEAVY, null, null), PerformedSet(HEAVY, null, 17), PerformedSet(LIGHT, null, 30))
        assertEquals(listOf(null, 17, 30), RepsTarget.forSets(listOf(HEAVY, HEAVY, LIGHT), last))
    }

    @Test
    fun `#91 a skipped set in the middle keeps the later sets in place`() {
        val last = listOf(PerformedSet(HEAVY, null, 20), PerformedSet(HEAVY, null, null), PerformedSet(HEAVY, null, 15))
        assertEquals(listOf(20, null, 15), RepsTarget.forSets(listOf(HEAVY, HEAVY, HEAVY), last))
    }

    @Test
    fun `#91 an extra set today aims at the last heavy set you really did`() {
        val last = listOf(PerformedSet(HEAVY, null, 20), PerformedSet(HEAVY, null, null))
        assertEquals(listOf(20, null, 20), RepsTarget.forSets(listOf(HEAVY, HEAVY, HEAVY), last))
    }
}
