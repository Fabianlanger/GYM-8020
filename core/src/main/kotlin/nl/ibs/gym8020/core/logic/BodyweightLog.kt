package nl.ibs.gym8020.core.logic

/**
 * One body weight measurement (V-14, A-14): the local day it belongs to ([epochDay]) and the moment it was taken
 * ([measuredAt], epoch millis), which orders several measurements on one day.
 */
data class Measurement(val epochDay: Long, val measuredAt: Long, val kg: Double)

/** The rule for several measurements per day: the last measurement on or before a day counts. */
object BodyweightLog {
    private val order = compareBy<Measurement>({ it.epochDay }, { it.measuredAt })

    /** The body weight of [epochDay]: its last measurement, or else the last one before it. */
    fun lastOnOrBefore(measurements: List<Measurement>, epochDay: Long): Measurement? =
        measurements.filter { it.epochDay <= epochDay }.maxWithOrNull(order)

    /** Per day the measurement that counts, oldest day first (charts and trends). */
    fun lastPerDay(measurements: List<Measurement>): List<Measurement> =
        measurements.groupBy { it.epochDay }.values.map { day -> day.maxWith(order) }.sortedBy { it.epochDay }

    /** All measurements of [epochDay], in the order they were taken. */
    fun ofDay(measurements: List<Measurement>, epochDay: Long): List<Measurement> =
        measurements.filter { it.epochDay == epochDay }.sortedWith(order)

    /**
     * The moment given to measurements from before schema v3, which only had a day: noon (UTC) of that day. Matches
     * the database migration 2 → 3 and older backups.
     */
    fun legacyMeasuredAt(epochDay: Long): Long = epochDay * DAY_MILLIS + DAY_MILLIS / 2

    private const val DAY_MILLIS = 86_400_000L
}
