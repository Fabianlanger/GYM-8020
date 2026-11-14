package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Advice
import nl.ibs.gym8020.core.logic.DefaultTarget
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * #72: with more than one heavy or light set the default target and the advice refer to the right set (the k-th
 * heavy set with the k-th heavy set). The owner's examples: weight progression, rep targets 6 and 16, margin 1,
 * step 2.5.
 */
class MultipleSetsTest {
    private val method = MethodSettings(heavyTarget = 6, lightTarget = 16, goodMargin = 1, weightStepKg = 2.5)

    private fun nextTarget(stored: List<PlannedSet>, vararg today: PerformedSet) =
        DefaultTarget.of(stored, listOf(HEAVY, LIGHT), today.toList(), Progression.WEIGHT, LoadType.EXTERNAL)

    private fun advice(target: List<PlannedSet>, vararg today: PerformedSet) =
        Advice.perSet(target, today.toList(), Progression.WEIGHT, LoadType.EXTERNAL, method::targetFor, method.goodMargin, method.weightStepKg)

    private val h70l40 = listOf(PlannedSet(HEAVY, 70.0), PlannedSet(LIGHT, 40.0))

    @Test
    fun `issue 72 example 1 - both sets good, the next target is what you lifted and there is no advice`() {
        val today = arrayOf(PerformedSet(HEAVY, 80.0, 6), PerformedSet(LIGHT, 40.0, 16))

        val target = nextTarget(h70l40, *today)

        assertEquals(listOf(PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 40.0)), target)
        assertEquals(listOf(null, null), advice(target, *today))
    }

    @Test
    fun `issue 72 example 2 - an extra heavy set gets its own target and its own advice`() {
        val today = arrayOf(PerformedSet(HEAVY, 60.0, 7), PerformedSet(HEAVY, 50.0, 3), PerformedSet(LIGHT, 20.0, 18))

        val target = nextTarget(h70l40, *today)

        assertEquals(listOf(PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 50.0), PlannedSet(LIGHT, 20.0)), target)
        // H 60 is good (no chip), H 50 × 3 too heavy, L 20 × 18 too light.
        assertEquals(listOf(null, 47.5, 22.5), advice(target, *today))
    }

    @Test
    fun `issue 72 example 2 - the advice line names every set at its own place`() {
        // Collapsed, "Advies Z 47,5 · L 22,5" next to "Z 60 · Z 50 · L 20" reads as advice for the first heavy set.
        // The line follows the target set by set; a good set shows its own weight.
        val today = arrayOf(PerformedSet(HEAVY, 60.0, 7), PerformedSet(HEAVY, 50.0, 3), PerformedSet(LIGHT, 20.0, 18))
        val target = nextTarget(h70l40, *today)

        val line = Advice.line(target, advice(target, *today))

        assertEquals(listOf(PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 47.5), PlannedSet(LIGHT, 22.5)), line)
    }

    @Test
    fun `issue 72 example 1 - no advice line when every set is good`() {
        val today = arrayOf(PerformedSet(HEAVY, 80.0, 6), PerformedSet(LIGHT, 40.0, 16))
        val target = nextTarget(h70l40, *today)

        assertEquals(null, Advice.line(target, advice(target, *today)))
    }

    @Test
    fun `issue 72 - the advice line leaves out warm-ups`() {
        val target = listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 50.0), PlannedSet(LIGHT, 20.0))

        val line = Advice.line(target, listOf(null, null, 47.5, 22.5))

        assertEquals(listOf(PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 47.5), PlannedSet(LIGHT, 22.5)), line)
    }

    @Test
    fun `issue 72 example 2 with the extra heavy set done last`() {
        val today = arrayOf(PerformedSet(HEAVY, 60.0, 7), PerformedSet(LIGHT, 20.0, 18), PerformedSet(HEAVY, 50.0, 3))

        val target = nextTarget(h70l40, *today)

        assertEquals(listOf(PlannedSet(HEAVY, 60.0), PlannedSet(LIGHT, 20.0), PlannedSet(HEAVY, 50.0)), target)
        assertEquals(listOf(null, 22.5, 47.5), advice(target, *today))
    }

    @Test
    fun `issue 72 example 2 with warm-ups in the target and today`() {
        val stored = listOf(PlannedSet(WARMUP, null)) + h70l40
        val today = arrayOf(
            PerformedSet(WARMUP, 40.0, 10), PerformedSet(WARMUP, 50.0, 5),
            PerformedSet(HEAVY, 60.0, 7), PerformedSet(HEAVY, 50.0, 3), PerformedSet(LIGHT, 20.0, 18),
        )

        val target = nextTarget(stored, *today)

        assertEquals(
            listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 50.0), PlannedSet(LIGHT, 20.0)),
            target,
        )
        // The advice is per set of the target, so the chip under set i is the advice of set i.
        assertEquals(listOf(null, null, 47.5, 22.5), advice(target, *today))
    }

    @Test
    fun `issue 72 - the default target stays the same when it is computed again from itself`() {
        // Moving on stores the default target; leaving the screen or correcting the training computes it again,
        // then from the stored default target. It must not shift sets.
        val today = arrayOf(PerformedSet(HEAVY, 60.0, 7), PerformedSet(HEAVY, 50.0, 3), PerformedSet(LIGHT, 20.0, 18))
        val first = nextTarget(h70l40, *today)

        assertEquals(first, nextTarget(first, *today))
    }
}
