package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Advice
import nl.ibs.gym8020.core.logic.DefaultTarget
import nl.ibs.gym8020.core.logic.OwnSets
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.SetByYou
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import nl.ibs.gym8020.core.model.TargetOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * #92 (decision #90): "zelf gezet" per set. A set you changed or whose advice you took over stays; the other sets
 * follow your performance with their own advice. Synthetic data; rep targets 6 and 16, margin 1, step 2.5.
 */
class SetByYouTest {
    private val method = MethodSettings(heavyTarget = 6, lightTarget = 16, goodMargin = 1, weightStepKg = 2.5)
    private val h70l40 = listOf(PlannedSet(HEAVY, 70.0), PlannedSet(LIGHT, 40.0))

    private fun default(vararg today: PerformedSet) =
        DefaultTarget.of(h70l40, listOf(HEAVY, LIGHT), today.toList(), Progression.WEIGHT, LoadType.EXTERNAL)

    private fun advice(target: List<PlannedSet>, vararg today: PerformedSet) =
        Advice.perSet(target, today.toList(), Progression.WEIGHT, LoadType.EXTERNAL, method::targetFor, method.goodMargin, method.weightStepKg)

    @Test
    fun `prototype situation 1 - after taking the heavy advice the light set still follows your performance`() {
        // The training starts with H 70 · L 40. Heavy set 75 × 9: too light, advice 77,5.
        val afterHeavy = arrayOf(PerformedSet(HEAVY, 75.0, 9), PerformedSet(LIGHT, null, null))
        val first = SetByYou.follow(null, default(*afterHeavy))
        assertEquals(listOf(PlannedSet(HEAVY, 75.0), PlannedSet(LIGHT, 40.0)), first.sets)
        assertEquals(listOf(77.5, null), advice(first.sets, *afterHeavy))

        // You take over the advice of the heavy set: only that set is yours.
        val taken = SetByYou.change(first, 0, PlannedSet(HEAVY, 77.5))
        assertEquals(listOf(true, false), taken.yours)

        // Light set 30 × 20: the light set follows (30), with its own advice 32,5; the heavy set stays 77,5.
        val afterLight = arrayOf(PerformedSet(HEAVY, 75.0, 9), PerformedSet(LIGHT, 30.0, 20))
        val next = SetByYou.follow(taken, default(*afterLight))
        assertEquals(OwnSets(listOf(PlannedSet(HEAVY, 77.5), PlannedSet(LIGHT, 30.0)), listOf(true, false)), next)
        assertEquals(listOf(null, 32.5), advice(next.sets, *afterLight))
    }

    @Test
    fun `undo puts back only that set, it follows your performance again`() {
        val today = arrayOf(PerformedSet(HEAVY, 75.0, 9), PerformedSet(LIGHT, 30.0, 20))
        val own = OwnSets(listOf(PlannedSet(HEAVY, 77.5), PlannedSet(LIGHT, 32.5)), listOf(true, true))

        val undone = SetByYou.release(own, 0, default(*today))

        assertEquals(OwnSets(listOf(PlannedSet(HEAVY, 75.0), PlannedSet(LIGHT, 32.5)), listOf(false, true)), undone)
    }

    @Test
    fun `undoing the only set you set gives the default target back`() {
        val today = arrayOf(PerformedSet(HEAVY, 75.0, 9), PerformedSet(LIGHT, 30.0, 20))
        val own = OwnSets(listOf(PlannedSet(HEAVY, 77.5), PlannedSet(LIGHT, 30.0)), listOf(true, false))

        assertEquals(OwnSets.none(default(*today)), SetByYou.release(own, 0, default(*today)))
    }

    @Test
    fun `a target you set as a whole does not follow your performance`() {
        val own = OwnSets.all(listOf(PlannedSet(HEAVY, 80.0), PlannedSet(HEAVY, 70.0)))

        assertEquals(own, SetByYou.follow(own, default(PerformedSet(HEAVY, 75.0, 6), PerformedSet(LIGHT, 30.0, 16))))
    }

    @Test
    fun `an extra set you do after setting one yourself joins the target`() {
        val own = OwnSets(listOf(PlannedSet(HEAVY, 77.5), PlannedSet(LIGHT, 40.0)), listOf(true, false))
        val today = default(PerformedSet(HEAVY, 75.0, 9), PerformedSet(HEAVY, 60.0, 8), PerformedSet(LIGHT, 30.0, 20))

        val next = SetByYou.follow(own, today)

        assertEquals(listOf(PlannedSet(HEAVY, 77.5), PlannedSet(HEAVY, 60.0), PlannedSet(LIGHT, 30.0)), next.sets)
        assertEquals(listOf(true, false, false), next.yours)
    }

    @Test
    fun `the k-th heavy set you set stays the k-th heavy set`() {
        val own = OwnSets(listOf(PlannedSet(HEAVY, 60.0), PlannedSet(HEAVY, 47.5), PlannedSet(LIGHT, 20.0)), listOf(false, true, false))
        val today = DefaultTarget.of(
            own.sets, listOf(HEAVY, LIGHT),
            listOf(PerformedSet(HEAVY, 62.5, 6), PerformedSet(HEAVY, 50.0, 3), PerformedSet(LIGHT, 22.5, 16)),
            Progression.WEIGHT, LoadType.EXTERNAL,
        )

        val next = SetByYou.follow(own, today)

        assertEquals(listOf(PlannedSet(HEAVY, 62.5), PlannedSet(HEAVY, 47.5), PlannedSet(LIGHT, 22.5)), next.sets)
    }

    @Test
    fun `a set of yours without a place in the default target is kept at the end`() {
        val own = OwnSets(listOf(PlannedSet(HEAVY, 70.0), PlannedSet(HEAVY, 65.0), PlannedSet(LIGHT, 40.0)), listOf(false, true, false))
        val today = listOf(PlannedSet(HEAVY, 72.5), PlannedSet(LIGHT, 40.0))

        val next = SetByYou.follow(own, today)

        assertEquals(listOf(PlannedSet(HEAVY, 72.5), PlannedSet(LIGHT, 40.0), PlannedSet(HEAVY, 65.0)), next.sets)
        assertEquals(listOf(false, false, true), next.yours)
    }

    @Test
    fun `warm-ups are never yours by following and following twice changes nothing`() {
        val own = OwnSets(listOf(PlannedSet(HEAVY, 77.5), PlannedSet(LIGHT, 40.0)), listOf(true, false))
        val today = listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 75.0), PlannedSet(LIGHT, 30.0))

        val once = SetByYou.follow(own, today)

        assertEquals(listOf(false, true, false), once.yours)
        assertEquals(once, SetByYou.follow(once, today))
    }

    @Test
    fun `the target as a whole is yours when at least one set is`() {
        assertEquals(TargetOrigin.YOURS, SetByYou.originOf(listOf(TargetOrigin.DEFAULT, TargetOrigin.YOURS)))
        assertEquals(TargetOrigin.DEFAULT, SetByYou.originOf(listOf(TargetOrigin.DEFAULT, TargetOrigin.DEFAULT)))
        assertEquals(TargetOrigin.DEFAULT, SetByYou.originOf(emptyList()))
        assertEquals(TargetOrigin.UNKNOWN, SetByYou.originOf(listOf(TargetOrigin.UNKNOWN)))
    }
}
