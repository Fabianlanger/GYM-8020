package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.DoneTraining
import nl.ibs.gym8020.core.logic.HomeAdvice
import nl.ibs.gym8020.core.logic.HomeAdvisor
import nl.ibs.gym8020.core.logic.PlanSlot
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeAdvisorTest {
    private val monday = LocalDate.of(2026, 9, 28)
    private val ppl = listOf(PlanSlot(1, MONDAY), PlanSlot(2, WEDNESDAY), PlanSlot(3, FRIDAY))

    @Test
    fun `scheduled today`() {
        assertEquals(HomeAdvice.Today(1, 0, 3), HomeAdvisor.advise(monday, ppl, 3, emptyList()))
    }

    @Test
    fun `done for today`() {
        val advice = HomeAdvisor.advise(monday, ppl, 3, listOf(DoneTraining(monday, 1)))
        assertEquals(HomeAdvice.DoneForToday(1, 3), advice)
    }

    @Test
    fun `rest day suggests first open schema of the week`() {
        val tuesday = monday.plusDays(1)
        val advice = HomeAdvisor.advise(tuesday, ppl, 3, listOf(DoneTraining(monday, 1)))
        assertEquals(HomeAdvice.RestDay(2, 1, 3), advice)
    }

    @Test
    fun `last week does not count`() {
        val advice = HomeAdvisor.advise(monday.plusDays(1), ppl, 3, listOf(DoneTraining(monday.minusDays(7), 1)))
        assertEquals(HomeAdvice.RestDay(1, 0, 3), advice)
    }

    @Test
    fun `same schema planned three times needs three trainings`() {
        val fullBody = listOf(PlanSlot(9, MONDAY), PlanSlot(9, WEDNESDAY), PlanSlot(9, FRIDAY))
        val advice = HomeAdvisor.advise(monday.plusDays(1), fullBody, 3, listOf(DoneTraining(monday, 9)))
        assertEquals(HomeAdvice.RestDay(9, 1, 3), advice)
    }

    @Test
    fun `rotation without days`() {
        val slots = listOf(PlanSlot(1, null), PlanSlot(2, null))
        assertEquals(HomeAdvice.Next(1, 0, 2), HomeAdvisor.advise(monday, slots, 2, emptyList()))
        assertEquals(HomeAdvice.Next(2, 0, 2), HomeAdvisor.advise(monday, slots, 2, listOf(DoneTraining(monday.minusDays(3), 1))))
        assertEquals(HomeAdvice.Next(1, 0, 2), HomeAdvisor.advise(monday, slots, 2, listOf(DoneTraining(monday.minusDays(3), 2))))
    }

    @Test
    fun `ask bodyweight after seven days`() {
        assertTrue(HomeAdvisor.shouldAskBodyweight(monday, null))
        assertFalse(HomeAdvisor.shouldAskBodyweight(monday, monday.minusDays(7)))
        assertTrue(HomeAdvisor.shouldAskBodyweight(monday, monday.minusDays(8)))
    }
}
