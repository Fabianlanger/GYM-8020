package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.DoneTraining
import nl.ibs.gym8020.core.logic.PlanSlot
import nl.ibs.gym8020.core.logic.Reminder
import nl.ibs.gym8020.core.logic.ReminderFlags
import nl.ibs.gym8020.core.logic.ReminderPlanner
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemindersTest {
    private val monday = LocalDate.of(2026, 9, 28)
    private val thursday = monday.plusDays(3)
    private val ppl = listOf(PlanSlot(1, MONDAY), PlanSlot(2, WEDNESDAY), PlanSlot(3, FRIDAY))
    private val rotation = listOf(PlanSlot(1, null), PlanSlot(2, null))
    private val all = ReminderFlags(trainingDay = true, behind = true, bodyweight = true)

    private fun daily(today: LocalDate, slots: List<PlanSlot>, history: List<DoneTraining> = emptyList(), flags: ReminderFlags = all) =
        ReminderPlanner.daily(today, slots, 3, history, lastBodyweight = today, usesBodyweight = false, flags = flags)

    @Test
    fun `training day in the week plan`() {
        assertEquals(listOf(Reminder.TrainingDay(1)), daily(monday, ppl))
    }

    @Test
    fun `nothing after training today`() {
        assertEquals(emptyList(), daily(monday, ppl, listOf(DoneTraining(monday, 1))))
    }

    @Test
    fun `switched off means silent`() {
        assertEquals(emptyList(), daily(monday, ppl, flags = ReminderFlags()))
    }

    @Test
    fun `behind only when the week gets tight`() {
        // Monday 0/3: plenty of time.
        assertEquals(emptyList(), daily(monday, rotation))
        // Thursday 0/3: three trainings in four days.
        assertEquals(listOf(Reminder.Behind(3, 4)), daily(thursday, rotation))
        // Thursday 2/3: fine.
        val done = listOf(DoneTraining(monday, 1), DoneTraining(monday.plusDays(1), 2))
        assertEquals(emptyList(), daily(thursday, rotation, done))
    }

    @Test
    fun `training day wins over behind`() {
        val friday = monday.plusDays(4)
        assertEquals(listOf(Reminder.TrainingDay(3)), daily(friday, ppl))
    }

    @Test
    fun `no week plan, no training reminders`() {
        assertEquals(emptyList(), ReminderPlanner.daily(thursday, emptyList(), null, emptyList(), thursday, false, all))
    }

    @Test
    fun `body weight weekly, only with bodyweight exercises`() {
        assertTrue(ReminderPlanner.bodyweightDue(monday, monday.minusDays(7)))
        assertFalse(ReminderPlanner.bodyweightDue(monday, monday.minusDays(8)))
        assertTrue(ReminderPlanner.bodyweightDue(monday, monday.minusDays(14)))
        assertFalse(ReminderPlanner.bodyweightDue(monday, monday.minusDays(3)))
        assertTrue(ReminderPlanner.bodyweightDue(monday, null))
        assertFalse(ReminderPlanner.bodyweightDue(thursday, null))
        val flags = ReminderFlags(bodyweight = true)
        assertEquals(listOf(Reminder.Bodyweight), ReminderPlanner.daily(monday, emptyList(), null, emptyList(), monday.minusDays(7), true, flags))
        assertEquals(emptyList(), ReminderPlanner.daily(monday, emptyList(), null, emptyList(), monday.minusDays(7), false, flags))
    }
}
