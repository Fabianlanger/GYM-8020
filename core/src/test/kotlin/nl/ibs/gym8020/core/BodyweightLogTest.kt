package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.BodyweightLog
import nl.ibs.gym8020.core.logic.Measurement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** V-14, A-14: several measurements per day; the last measurement on or before a day counts. */
class BodyweightLogTest {
    private val hour = 3_600_000L

    private fun m(day: Long, hourOfDay: Int, kg: Double) = Measurement(day, day * 24 * hour + hourOfDay * hour, kg)

    @Test
    fun `the last measurement of the day counts`() {
        val list = listOf(m(100, 7, 80.0), m(100, 21, 81.2), m(100, 12, 80.6))

        assertEquals(81.2, BodyweightLog.lastOnOrBefore(list, 100)?.kg)
    }

    @Test
    fun `a day without a measurement takes the last one before it`() {
        val list = listOf(m(98, 8, 79.0), m(98, 20, 79.4), m(101, 8, 80.0))

        assertEquals(79.4, BodyweightLog.lastOnOrBefore(list, 100)?.kg)
    }

    @Test
    fun `a later measurement never counts for an earlier day`() {
        assertNull(BodyweightLog.lastOnOrBefore(listOf(m(101, 8, 80.0)), 100))
    }

    @Test
    fun `per day the last measurement, in date order`() {
        val list = listOf(m(101, 8, 80.0), m(100, 21, 81.2), m(100, 7, 80.0), m(102, 9, 79.8))

        assertEquals(listOf(81.2, 80.0, 79.8), BodyweightLog.lastPerDay(list).map { it.kg })
        assertEquals(listOf(100L, 101L, 102L), BodyweightLog.lastPerDay(list).map { it.epochDay })
    }

    @Test
    fun `adding a measurement keeps the older ones of the same day`() {
        val morning = m(100, 7, 80.0)
        val evening = m(100, 21, 81.0)

        assertEquals(listOf(morning, evening), BodyweightLog.ofDay(listOf(evening, morning), 100))
    }

    @Test
    fun `measurements from before schema v3 count at noon of their day`() {
        assertEquals(100L * 24 * hour + 12 * hour, BodyweightLog.legacyMeasuredAt(100))
    }
}
