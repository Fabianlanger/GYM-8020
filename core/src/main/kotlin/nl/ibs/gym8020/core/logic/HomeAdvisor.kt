package nl.ibs.gym8020.core.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A schema slot in the active week plan, optionally bound to a weekday. */
data class PlanSlot(val schemaId: Long, val day: DayOfWeek?)

/** A finished training on a date. */
data class DoneTraining(val date: LocalDate, val schemaId: Long?)

sealed interface HomeAdvice {
    val doneThisWeek: Int
    val targetPerWeek: Int

    /** Already trained today; starting another training is still possible. */
    data class DoneForToday(override val doneThisWeek: Int, override val targetPerWeek: Int) : HomeAdvice

    /** A schema is planned for today. */
    data class Today(val schemaId: Long, override val doneThisWeek: Int, override val targetPerWeek: Int) : HomeAdvice

    /** Nothing planned today; [suggestion] is the first planned schema not yet done this week. */
    data class RestDay(val suggestion: Long?, override val doneThisWeek: Int, override val targetPerWeek: Int) : HomeAdvice

    /** Week plan without days: next schema in the rotation. */
    data class Next(val schemaId: Long?, override val doneThisWeek: Int, override val targetPerWeek: Int) : HomeAdvice
}

/**
 * Day recognition for the home screen (D-62). Flexible on purpose: a missed day does not need
 * to be made up, and the user can always pick another schema.
 */
object HomeAdvisor {

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun advise(today: LocalDate, slots: List<PlanSlot>, targetPerWeek: Int, history: List<DoneTraining>): HomeAdvice {
        val start = weekStart(today)
        val thisWeek = history.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
        val done = thisWeek.size
        if (thisWeek.any { it.date == today }) return HomeAdvice.DoneForToday(done, targetPerWeek)

        val hasDays = slots.any { it.day != null }
        if (hasDays) {
            slots.firstOrNull { it.day == today.dayOfWeek }?.let { return HomeAdvice.Today(it.schemaId, done, targetPerWeek) }
            val doneIds = thisWeek.mapNotNull { it.schemaId }.toMutableList()
            // Each slot consumes one matching training this week, so a schema planned twice needs two trainings.
            val open = slots.firstOrNull { slot -> !doneIds.remove(slot.schemaId) }
            return HomeAdvice.RestDay(open?.schemaId, done, targetPerWeek)
        }

        if (slots.isEmpty()) return HomeAdvice.Next(null, done, targetPerWeek)
        val last = history.maxByOrNull { it.date }?.schemaId
        val idx = slots.indexOfFirst { it.schemaId == last }
        val next = if (idx < 0) slots.first() else slots[(idx + 1) % slots.size]
        return HomeAdvice.Next(next.schemaId, done, targetPerWeek)
    }

    /** Whether to ask for today's body weight (D-16): no measurement in the last [days] days. */
    fun shouldAskBodyweight(today: LocalDate, lastMeasurement: LocalDate?, days: Long = 7): Boolean =
        lastMeasurement == null || lastMeasurement.isBefore(today.minusDays(days))
}
