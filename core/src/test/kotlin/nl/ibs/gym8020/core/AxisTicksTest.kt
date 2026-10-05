package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.stats.AxisTicks
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** V-13: chart axes with readable, "nice" values. */
class AxisTicksTest {
    @Test
    fun `body weight range gives readable ticks`() {
        val t = AxisTicks.nice(79.3, 82.6, 4)
        assertEquals(listOf(79.0, 80.0, 81.0, 82.0, 83.0), t.values)
        assertEquals(0, t.decimals)
    }

    @Test
    fun `small ranges get decimals`() {
        val t = AxisTicks.nice(80.1, 80.7, 4)
        assertTrue(t.step < 1.0)
        assertEquals(1, t.decimals)
    }

    @Test
    fun `rule - ticks cover the data, ascend, and use a nice step`() {
        val r = Random(13)
        repeat(2_000) {
            val a = r.nextDouble(-50.0, 500.0)
            val lo = a
            val hi = a + r.nextDouble(0.0, 300.0)
            val t = AxisTicks.nice(lo, hi, 4)
            assertTrue(t.values.first() <= lo + 1e-9 && t.values.last() >= hi - 1e-9, "$lo..$hi -> ${t.values}")
            assertTrue(t.values.zipWithNext().all { (x, y) -> y > x }, "ascending: ${t.values}")
            assertTrue(t.values.size in 2..9, "count: ${t.values}")
            val mant = t.step / 10.0.pow(kotlin.math.floor(log10(t.step)))
            assertTrue(listOf(1.0, 2.0, 2.5, 5.0, 10.0).any { abs(it - mant) < 1e-6 }, "nice step: ${t.step}")
        }
    }
}
