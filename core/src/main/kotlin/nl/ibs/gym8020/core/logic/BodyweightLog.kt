package nl.ibs.gym8020.core.logic

/**
 * One body weight measurement (V-14, A-14): the local day it belongs to ([epochDay]) and the moment it was taken
 * ([measuredAt], epoch millis), which orders several measurements on one day.
 */
data class Measurement(val epochDay: Long, val measuredAt: Long, val kg: Double)

/**
 * The measurements of one day (#94), in the order they were taken: never empty. The chart draws a thin bar from
 * [lowKg] to [highKg], a dot for [last] and small dots for the [others]; the list shows [last] and [count].
 */
data class BodyweightDay(val epochDay: Long, val measurements: List<Measurement>) {
    init {
        require(measurements.isNotEmpty()) { "a day has at least one measurement" }
    }

    /** The measurement that counts for the day: the last one taken. */
    val last: Measurement get() = measurements.last()

    /** The measurements before [last]. */
    val others: List<Measurement> get() = measurements.dropLast(1)

    val lowKg: Double get() = measurements.minOf { it.kg }
    val highKg: Double get() = measurements.maxOf { it.kg }
    val count: Int get() = measurements.size
}

/**
 * The rules for several measurements per day (#94, decided in #81): adding a measurement never replaces one; for a
 * day the last measurement counts; for a training the last measurement before its start.
 */
object BodyweightLog {
    private val order = compareBy<Measurement>({ it.epochDay }, { it.measuredAt })

    /** The body weight of [epochDay]: its last measurement, or else the last one before it. */
    fun lastOnOrBefore(measurements: List<Measurement>, epochDay: Long): Measurement? =
        measurements.filter { it.epochDay <= epochDay }.maxWithOrNull(order)

    /**
     * The measurement that counts for a training that started at [startedAt] (epoch millis) on local day [startDay]
     * (#94): the last one taken at or before the start. A measurement later that day, or entered later with an
     * earlier date, never changes a training that already started after it, and a later one never counts for it.
     * A measurement from before schema v3 only knows its day ([isDayOnly]): it counts for the whole day, as before.
     */
    fun countsFor(measurements: List<Measurement>, startedAt: Long, startDay: Long): Measurement? =
        measurements.filter { it.measuredAt <= startedAt || (isDayOnly(it) && it.epochDay <= startDay) }.maxWithOrNull(order)

    /** Per day its measurements, oldest day first (chart and list). */
    fun days(measurements: List<Measurement>): List<BodyweightDay> =
        measurements.groupBy { it.epochDay }.map { (day, list) -> BodyweightDay(day, list.sortedWith(order)) }.sortedBy { it.epochDay }

    /** Per day the measurement that counts, oldest day first (charts and trends). */
    fun lastPerDay(measurements: List<Measurement>): List<Measurement> = days(measurements).map { it.last }

    /** All measurements of [epochDay], in the order they were taken. */
    fun ofDay(measurements: List<Measurement>, epochDay: Long): List<Measurement> =
        measurements.filter { it.epochDay == epochDay }.sortedWith(order)

    /**
     * The moment given to measurements from before schema v3, which only had a day: noon (UTC) of that day. Matches
     * the database migration 2 → 3 and older backups.
     */
    fun legacyMeasuredAt(epochDay: Long): Long = epochDay * DAY_MILLIS + DAY_MILLIS / 2

    /** Whether [m] comes from before schema v3 and so only knows its day, not its moment ([legacyMeasuredAt]). */
    fun isDayOnly(m: Measurement): Boolean = m.measuredAt == legacyMeasuredAt(m.epochDay)

    private const val DAY_MILLIS = 86_400_000L
}
