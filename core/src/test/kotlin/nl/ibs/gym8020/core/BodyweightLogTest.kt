package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.BodyweightDay
import nl.ibs.gym8020.core.logic.BodyweightLog
import nl.ibs.gym8020.core.logic.Measurement
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** V-14, A-14, #94: several measurements per day; the last one counts, for a training the last one before its start. */
class BodyweightLogTest {
    private val hour = 3_600_000L

    private fun m(day: Long, hourOfDay: Int, kg: Double) = Measurement(day, day * 24 * hour + hourOfDay * hour, kg)

    /** Moment [hourOfDay]:00 of [day]. */
    private fun at(day: Long, hourOfDay: Int) = day * 24 * hour + hourOfDay * hour

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

    // ---- #94: which measurement counts for a training ----

    @Test
    fun `#94 a training counts the last measurement before its start`() {
        val list = listOf(m(99, 20, 79.0), m(100, 7, 80.0), m(100, 9, 80.4))

        assertEquals(80.4, BodyweightLog.countsFor(list, startedAt = at(100, 18), startDay = 100)?.kg)
    }

    @Test
    fun `#94 a measurement later that day does not change a training that started before it`() {
        val list = listOf(m(100, 7, 80.0), m(100, 21, 82.0))

        assertEquals(80.0, BodyweightLog.countsFor(list, startedAt = at(100, 18), startDay = 100)?.kg)
    }

    @Test
    fun `#94 a morning training without a measurement that day takes the one of the day before`() {
        val list = listOf(m(99, 20, 79.0), m(100, 13, 80.0))

        assertEquals(79.0, BodyweightLog.countsFor(list, startedAt = at(100, 8), startDay = 100)?.kg)
    }

    @Test
    fun `#94 nothing counts before the first measurement`() {
        assertNull(BodyweightLog.countsFor(listOf(m(100, 13, 80.0)), startedAt = at(100, 8), startDay = 100))
    }

    @Test
    fun `#94 a measurement from before schema v3 counts for the whole of its day`() {
        val old = Measurement(100, BodyweightLog.legacyMeasuredAt(100), 80.0)

        assertEquals(80.0, BodyweightLog.countsFor(listOf(m(99, 20, 79.0), old), startedAt = at(100, 8), startDay = 100)?.kg)
        assertNull(BodyweightLog.countsFor(listOf(old), startedAt = at(99, 23), startDay = 99))
    }

    @Test
    fun `#94 rule - the measurement that counts is taken at or before the start, and none later before the start is skipped`() {
        val random = Random(94)
        repeat(2_000) {
            val list = randomLog(random)
            val start = at(100, 0) + random.nextLong(-2 * 24 * hour, 4 * 24 * hour)
            val startDay = Math.floorDiv(start, 24 * hour)
            val counts = BodyweightLog.countsFor(list, start, startDay)
            val before = list.filter { it.measuredAt <= start }
            if (before.isEmpty()) {
                assertNull(counts)
            } else {
                assertTrue(counts!!.measuredAt <= start)
                assertTrue(before.none { it.measuredAt > counts.measuredAt })
            }
        }
    }

    @Test
    fun `#94 rule - adding a measurement after the start never changes what counts`() {
        val random = Random(8094)
        repeat(2_000) {
            val list = randomLog(random)
            val start = at(100, 0) + random.nextLong(0, 4 * 24 * hour)
            val startDay = Math.floorDiv(start, 24 * hour)
            val later = start + 1 + random.nextLong(0, 2 * 24 * hour)
            val added = list + Measurement(Math.floorDiv(later, 24 * hour), later, 60 + random.nextDouble() * 40)

            assertEquals(BodyweightLog.countsFor(list, start, startDay), BodyweightLog.countsFor(added, start, startDay))
        }
    }

    // ---- #94: per day low, high and last ----

    @Test
    fun `#94 per day the low, the high and the last measurement`() {
        val list = listOf(m(100, 21, 81.0), m(100, 7, 80.2), m(100, 12, 81.4), m(101, 8, 80.0))

        val days = BodyweightLog.days(list)

        assertEquals(listOf(100L, 101L), days.map { it.epochDay })
        with(days[0]) {
            assertEquals(80.2, lowKg)
            assertEquals(81.4, highKg)
            assertEquals(81.0, last.kg)
            assertEquals(listOf(80.2, 81.4), others.map { it.kg })
            assertEquals(3, count)
        }
        with(days[1]) {
            assertEquals(80.0, lowKg)
            assertEquals(80.0, highKg)
            assertEquals(emptyList(), others)
            assertEquals(1, count)
        }
    }

    @Test
    fun `#94 a day has at least one measurement`() {
        assertFailsWith<IllegalArgumentException> { BodyweightDay(100, emptyList()) }
    }

    @Test
    fun `#94 rule - per day every measurement is between low and high, the last is the latest, nothing gets lost`() {
        val random = Random(81)
        repeat(1_000) {
            val list = randomLog(random)
            val days = BodyweightLog.days(list)

            assertEquals(list.size, days.sumOf { it.count })
            assertEquals(days.map { it.epochDay }.sorted().distinct(), days.map { it.epochDay })
            for (day in days) {
                assertTrue(day.measurements.all { it.epochDay == day.epochDay && it.kg in day.lowKg..day.highKg })
                assertTrue(day.measurements.all { it.measuredAt <= day.last.measuredAt })
                assertEquals(day.last, BodyweightLog.lastOnOrBefore(list, day.epochDay))
            }
        }
    }

    /** Up to 12 measurements over days 98 to 103, each at a random moment of its day. */
    private fun randomLog(random: Random): List<Measurement> = List(random.nextInt(0, 13)) {
        val day = 98L + random.nextInt(0, 6)
        Measurement(day, day * 24 * hour + random.nextLong(0, 24 * hour), 60 + random.nextDouble() * 40)
    }
}
