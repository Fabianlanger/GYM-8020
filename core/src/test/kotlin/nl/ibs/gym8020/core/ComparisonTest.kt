package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Change
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.Comparison
import nl.ibs.gym8020.core.stats.ComparisonFormat
import nl.ibs.gym8020.core.stats.Measure
import nl.ibs.gym8020.core.stats.SetRecord
import nl.ibs.gym8020.core.stats.Trend
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Comparison in the summary (#66, decision #8 points 1 and 4). */
class ComparisonTest {
    private val today = LocalDate.of(2026, 10, 9)
    private val earlier = LocalDate.of(2026, 10, 1)

    private fun s(
        session: Long, ex: Long, role: SetRole, w: Double?, reps: Int?,
        prog: Progression = Progression.WEIGHT, eff: Double? = w,
    ) = SetRecord(session, if (session == 2L) today else earlier, ex, role, w, reps, eff, null, prog)

    // ---- per exercise ----

    @Test
    fun `strength today against the last time, absolute and relative`() {
        // 100 × 4 → e1RM 113,33; 90 × 6 → 108
        val c = assertNotNull(Comparison.exercise(listOf(s(2, 1, SetRole.HEAVY, 100.0, 4)), listOf(s(1, 1, SetRole.HEAVY, 90.0, 6))))
        assertEquals(Measure.E1RM, c.measure)
        assertEquals(113.333, c.now!!, 1e-3)
        assertEquals(5.333, c.change!!.absolute, 1e-3)
        assertEquals(0.0494, c.change!!.relative!!, 1e-4)
        assertEquals(Trend.UP, c.trend)
    }

    @Test
    fun `a difference within 1 percent counts as equal (owner's 100x4 vs 90x8)`() {
        // 113,33 vs 114,0 = −0,6 %: noise, not worse
        val c = assertNotNull(Comparison.exercise(listOf(s(2, 1, SetRole.HEAVY, 100.0, 4)), listOf(s(1, 1, SetRole.HEAVY, 90.0, 8))))
        assertEquals(-0.667, c.change!!.absolute, 1e-3)
        assertEquals(Trend.SAME, c.trend)
    }

    @Test
    fun `more than 1 percent lower is worse`() {
        // 100 × 4 = 113,33 vs 100 × 6 = 120: −5,6 %
        val c = assertNotNull(Comparison.exercise(listOf(s(2, 1, SetRole.HEAVY, 100.0, 4)), listOf(s(1, 1, SetRole.HEAVY, 100.0, 6))))
        assertEquals(Trend.DOWN, c.trend)
    }

    @Test
    fun `light sets do not count for strength but get their own line when changed`() {
        val now = listOf(s(2, 1, SetRole.HEAVY, 100.0, 4), s(2, 1, SetRole.LIGHT, 40.0, 20))
        val before = listOf(s(1, 1, SetRole.HEAVY, 90.0, 8), s(1, 1, SetRole.LIGHT, 40.0, 17))
        val c = assertNotNull(Comparison.exercise(now, before))
        assertEquals(113.333, c.now!!, 1e-3)
        val light = assertNotNull(c.light)
        assertEquals(40.0, light.weightKg)
        assertEquals(20, light.reps)
        assertEquals(0.0, light.weightChangeKg)
        assertEquals(3, light.repsChange)
    }

    @Test
    fun `an unchanged light set gets no line`() {
        val now = listOf(s(2, 1, SetRole.HEAVY, 100.0, 5), s(2, 1, SetRole.LIGHT, 40.0, 17))
        val before = listOf(s(1, 1, SetRole.HEAVY, 100.0, 4), s(1, 1, SetRole.LIGHT, 40.0, 17))
        assertNull(assertNotNull(Comparison.exercise(now, before)).light)
    }

    @Test
    fun `reps progression compares the most reps in one work set`() {
        val now = listOf(s(2, 3, SetRole.HEAVY, 0.0, 16, Progression.REPS), s(2, 3, SetRole.LIGHT, 0.0, 12, Progression.REPS))
        val before = listOf(s(1, 3, SetRole.HEAVY, 0.0, 14, Progression.REPS), s(1, 3, SetRole.WARMUP, 0.0, 30, Progression.REPS))
        val c = assertNotNull(Comparison.exercise(now, before))
        assertEquals(Measure.REPS, c.measure)
        assertEquals(16.0, c.now)
        assertEquals(2.0, c.change!!.absolute)
        assertEquals(Trend.UP, c.trend)
        assertNull(c.light)
    }

    @Test
    fun `one rep less in reps progression is worse, the same reps equal`() {
        val now = listOf(s(2, 3, SetRole.HEAVY, 0.0, 99, Progression.REPS))
        assertEquals(Trend.DOWN, Comparison.exercise(now, listOf(s(1, 3, SetRole.HEAVY, 0.0, 100, Progression.REPS)))!!.trend)
        assertEquals(Trend.SAME, Comparison.exercise(now, listOf(s(1, 3, SetRole.HEAVY, 0.0, 99, Progression.REPS)))!!.trend)
    }

    @Test
    fun `no last time gives today's value without a change`() {
        val c = assertNotNull(Comparison.exercise(listOf(s(2, 1, SetRole.HEAVY, 60.0, 1)), null))
        assertEquals(60.0, c.now)
        assertNull(c.change)
        assertNull(c.trend)
        assertTrue(c.firstTime)
    }

    @Test
    fun `switching between reps and weight progression cannot be compared`() {
        val now = listOf(s(2, 3, SetRole.HEAVY, 10.0, 10, Progression.REPS))
        val before = listOf(s(1, 3, SetRole.HEAVY, 0.0, 14, Progression.REPS))
        val c = assertNotNull(Comparison.exercise(now, before))
        assertEquals(Measure.E1RM, c.measure)
        assertNull(c.change)
        assertFalse(c.firstTime)
    }

    @Test
    fun `no work sets with reps today means nothing to compare`() {
        assertNull(Comparison.exercise(listOf(s(2, 1, SetRole.WARMUP, 20.0, 10), s(2, 1, SetRole.HEAVY, 60.0, null)), null))
    }

    @Test
    fun `last time is the newest earlier training with a work set, from any workout`() {
        val newest = listOf(s(5, 1, SetRole.WARMUP, 20.0, 10), s(5, 2, SetRole.HEAVY, 50.0, 5))
        val middle = listOf(s(4, 1, SetRole.HEAVY, 80.0, 5))
        val oldest = listOf(s(3, 1, SetRole.HEAVY, 70.0, 5))
        assertEquals(middle, Comparison.lastTime(1, listOf(newest, middle, oldest)))
        assertNull(Comparison.lastTime(9, listOf(newest, middle, oldest)))
    }

    // ---- per workout ----

    @Test
    fun `workout against the previous session of the same workout`() {
        // today: 100×5 + 40×20 + 60×10 = 500 + 800 + 600 = 1900 kg in 3 sets, warm-up not counted
        val now = listOf(
            s(2, 1, SetRole.WARMUP, 40.0, 10), s(2, 1, SetRole.HEAVY, 100.0, 5), s(2, 1, SetRole.LIGHT, 40.0, 20),
            s(2, 2, SetRole.HEAVY, 60.0, 10),
        )
        // previous: 100×5 + 40×20 + 60×10 + 20×10 + 20×10 = 2300 kg in 5 sets (exercise 4 skipped today: counts as 0)
        val before = listOf(
            s(1, 1, SetRole.HEAVY, 100.0, 5), s(1, 1, SetRole.LIGHT, 40.0, 20),
            s(1, 2, SetRole.HEAVY, 60.0, 10), s(1, 4, SetRole.HEAVY, 20.0, 10), s(1, 4, SetRole.LIGHT, 20.0, 10),
        )
        val w = Comparison.workout(now, before, earlier, skipped = 1)
        assertEquals(earlier, w.previousDate)
        assertEquals(1900.0, w.volume)
        assertEquals(Change(-400.0, -400.0 / 2300.0), w.volumeChange)
        assertEquals(Trend.DOWN, w.volumeTrend)
        assertEquals(3, w.sets)
        assertEquals(-2, w.setsChange)
        assertEquals(1, w.skipped)
    }

    @Test
    fun `workout volume within 1 percent is equal`() {
        val w = Comparison.workout(listOf(s(2, 1, SetRole.HEAVY, 100.0, 10)), listOf(s(1, 1, SetRole.HEAVY, 101.0, 10)), earlier, 0)
        assertEquals(Trend.SAME, w.volumeTrend)
        assertEquals(0, w.setsChange)
    }

    // ---- formatting ----

    @Test
    fun `changes are formatted with sign, decimal comma and the weight unit`() {
        assertEquals("+2,5 kg", ComparisonFormat.weight(2.5, WeightUnit.KG))
        assertEquals("−0,7 kg", ComparisonFormat.weight(-0.667, WeightUnit.KG))
        assertEquals("+5,5 lbs", ComparisonFormat.weight(2.5, WeightUnit.LBS))
        assertEquals("0 kg", ComparisonFormat.weight(0.0, WeightUnit.KG))
        assertEquals("+2,4%", ComparisonFormat.percent(0.024))
        assertEquals("−0,6%", ComparisonFormat.percent(-0.00588))
        assertEquals("+3%", ComparisonFormat.percent(0.0301))
        assertEquals("0%", ComparisonFormat.percent(0.0))
        assertEquals("+2", ComparisonFormat.count(2))
        assertEquals("−2", ComparisonFormat.count(-2))
        assertEquals("±0", ComparisonFormat.count(0))
    }

    @Test
    fun `volume is shown whole with a thousands separator`() {
        assertEquals("12.400", ComparisonFormat.volume(12400.4, WeightUnit.KG))
        assertEquals("950", ComparisonFormat.volume(950.0, WeightUnit.KG))
        assertEquals("+350", ComparisonFormat.volume(350.0, WeightUnit.KG, signed = true))
        assertEquals("−1.200", ComparisonFormat.volume(-1200.0, WeightUnit.KG, signed = true))
    }
}
