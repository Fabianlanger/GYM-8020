package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.BodyShare
import nl.ibs.gym8020.core.logic.BodyweightTarget
import nl.ibs.gym8020.core.logic.DefaultTarget
import nl.ibs.gym8020.core.logic.Measurement
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** #87, ADR 0002: the target of a bodyweight exercise is an effective weight; what you hang follows from today. */
class BodyweightTargetTest {

    /** Assisted machine in steps of 5 kg, plates on a belt from the standard stock. */
    private val belt = WeightCalculator(MaterialProfile(MaterialKind.BODYWEIGHT, stepKg = 5.0), PlateStock.DEFAULT)

    @Test
    fun `5 kg lighter than last time - you hang 5 kg more today`() {
        val target = BodyShare(80.0, 1.0).effective(0.0)

        assertEquals(80.0, target)
        assertEquals(5.0, BodyShare(75.0, 1.0).today(target, belt::snap))
    }

    @Test
    fun `the bodyweight share counts - dips at 90 percent`() {
        val target = BodyShare(80.0, 0.9).effective(10.0)

        assertEquals(82.0, target)
        // 82 − 76 × 0,9 = 13,6 kg: on the belt 12,5 + 1,25.
        assertEquals(13.75, BodyShare(76.0, 0.9).today(target, belt::snap))
    }

    @Test
    fun `heavier than last time - more assistance, rounded to the steps of the machine`() {
        val target = BodyShare(80.0, 1.0).effective(-20.0)

        assertEquals(60.0, target)
        assertEquals(-25.0, BodyShare(85.0, 1.0).today(target, belt::snap))
        // −23: between −25 and −20, nearest is −25.
        assertEquals(-25.0, BodyShare(83.0, 1.0).today(target, belt::snap))
    }

    @Test
    fun `without material - rounded to the weight step, on a tie the lighter one`() {
        assertEquals(5.0, BodyShare(75.3, 1.0).today(81.0) { BodyweightTarget.roundToStep(it, 2.5) })
        assertEquals(5.0, BodyShare(75.0, 1.0).today(81.25) { BodyweightTarget.roundToStep(it, 2.5) })
        assertEquals(-12.5, BodyShare(80.0, 1.0).today(68.0) { BodyweightTarget.roundToStep(it, 2.5) })
        assertEquals(6.0, BodyweightTarget.roundToStep(6.0, 0.0))
    }

    @Test
    fun `converting is exact both ways - no rounding drift`() {
        val share = BodyShare(75.3, 0.95)
        val effective = share.effective(6.0)

        assertEquals(77.535, effective)
        assertEquals(6.0, share.signed(effective))
    }

    @Test
    fun `a target converts per set - warm-ups and sets without a weight stay as they are`() {
        val signed = listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 5.0), PlannedSet(LIGHT, -10.0), PlannedSet(HEAVY, null, 8))
        val effective = listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 85.0), PlannedSet(LIGHT, 70.0), PlannedSet(HEAVY, null, 8))

        assertEquals(effective, BodyweightTarget.toEffective(signed, BodyShare(80.0, 1.0)))
        assertEquals(signed, BodyweightTarget.toSigned(effective, BodyShare(80.0, 1.0)))
    }

    @Test
    fun `without a known body weight nothing is converted`() {
        val sets = listOf(PlannedSet(HEAVY, 5.0))

        assertEquals(sets, BodyweightTarget.toEffective(sets, null))
        assertEquals(sets, BodyweightTarget.toSigned(sets, null))
    }

    @Test
    fun `a set you did not do keeps its effective target when your body weight changed`() {
        // Your target: heavy 81 kg, light 70 kg effective. Today you weigh 75 kg; you only did the heavy set.
        val today = BodyShare(75.0, 1.0)
        val stored = BodyweightTarget.toSigned(listOf(PlannedSet(HEAVY, 81.0), PlannedSet(LIGHT, 70.0)), today)
        val performed = listOf(PerformedSet(HEAVY, 7.5, 6), PerformedSet(LIGHT, null, null))

        val next = DefaultTarget.of(stored, emptyList(), performed, Progression.WEIGHT, LoadType.BODYWEIGHT)

        assertEquals(listOf(PlannedSet(HEAVY, 82.5), PlannedSet(LIGHT, 70.0)), BodyweightTarget.toEffective(next, today))
    }

    @Test
    fun `the body weight of a training - its own, else the last measurement before its start, else the first one`() {
        val log = listOf(Measurement(epochDay = 10, measuredAt = at(10, 8), kg = 81.0), Measurement(epochDay = 20, measuredAt = at(20, 8), kg = 78.0))

        assertEquals(79.5, BodyweightTarget.bodyweightFor(79.5, log, startedAt = at(25, 18), startDay = 25))
        assertEquals(78.0, BodyweightTarget.bodyweightFor(null, log, startedAt = at(25, 18), startDay = 25))
        assertEquals(81.0, BodyweightTarget.bodyweightFor(null, log, startedAt = at(15, 18), startDay = 15))
        assertEquals(81.0, BodyweightTarget.bodyweightFor(null, log, startedAt = at(5, 18), startDay = 5))
        assertNull(BodyweightTarget.bodyweightFor(null, emptyList(), startedAt = at(5, 18), startDay = 5))
    }

    @Test
    fun `#94 the target in effective weight uses the measurement before the start, not a later one that day`() {
        val log = listOf(Measurement(20, at(20, 7), 80.0), Measurement(20, at(20, 21), 82.0))

        // Trained at 18:00: the evening measurement comes after the start.
        assertEquals(80.0, BodyweightTarget.bodyweightFor(null, log, startedAt = at(20, 18), startDay = 20))
    }

    /** Moment [hour]:00 (UTC) of [day], in epoch millis. */
    private fun at(day: Long, hour: Int): Long = day * 86_400_000L + hour * 3_600_000L
    @Test
    fun `the session target - each work set of the target as what you hang today`() {
        val target = listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 60.0))

        assertEquals(
            listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 5.0), PlannedSet(LIGHT, -15.0)),
            BodyweightTarget.forToday(target, BodyShare(75.0, 1.0), belt::snap),
        )
        assertEquals(target, BodyweightTarget.forToday(target, null, belt::snap))
    }
}
