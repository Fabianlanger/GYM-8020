package nl.ibs.gym8020.core.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A reminder the daily check can show (D-65). */
sealed interface Reminder {
    /** A schema is planned for today in the week plan. */
    data class TrainingDay(val schemaId: Long) : Reminder

    /** The week target gets tight: [remaining] trainings left in [daysLeft] days (today included). */
    data class Behind(val remaining: Int, val daysLeft: Int) : Reminder

    /** Body weight has not been logged for a while, and bodyweight exercises are used. */
    data object Bodyweight : Reminder
}

/** Which daily reminders are switched on (all off by default). */
data class ReminderFlags(val trainingDay: Boolean = false, val behind: Boolean = false, val bodyweight: Boolean = false) {
    val any: Boolean get() = trainingDay || behind || bodyweight
}

/**
 * The daily check at the chosen time. Pure logic: the app feeds it the week plan, history and
 * body weight, and shows what comes out. Nothing if you already trained today.
 */
object ReminderPlanner {

    fun daily(
        today: LocalDate,
        slots: List<PlanSlot>,
        targetPerWeek: Int?,
        history: List<DoneTraining>,
        lastBodyweight: LocalDate?,
        usesBodyweight: Boolean,
        flags: ReminderFlags,
    ): List<Reminder> {
        val out = mutableListOf<Reminder>()
        if (targetPerWeek != null) {
            val advice = HomeAdvisor.advise(today, slots, targetPerWeek, history)
            if (advice !is HomeAdvice.DoneForToday) {
                if (flags.trainingDay && advice is HomeAdvice.Today) {
                    out += Reminder.TrainingDay(advice.schemaId)
                } else if (flags.behind) {
                    val remaining = targetPerWeek - advice.doneThisWeek
                    val daysLeft = 8 - today.dayOfWeek.value // Monday 7 … Sunday 1
                    // One spare day: nudge when the rest of the week is (almost) full.
                    if (remaining > 0 && daysLeft - remaining <= 1) out += Reminder.Behind(remaining, daysLeft)
                }
            }
        }
        if (flags.bodyweight && usesBodyweight && bodyweightDue(today, lastBodyweight)) out += Reminder.Bodyweight
        return out
    }

    /** Weekly, not daily: on day 7, 14, … after the last measurement; never measured → on Mondays. */
    fun bodyweightDue(today: LocalDate, last: LocalDate?): Boolean {
        if (last == null) return today.dayOfWeek.value == 1
        val days = ChronoUnit.DAYS.between(last, today)
        return days >= 7 && days % 7 == 0L
    }
}
