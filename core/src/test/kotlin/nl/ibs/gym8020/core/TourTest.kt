package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Tour
import nl.ibs.gym8020.core.logic.TourOffer
import nl.ibs.gym8020.core.logic.TourStep
import nl.ibs.gym8020.core.logic.TourStep.GO
import nl.ibs.gym8020.core.logic.TourStep.GOAL
import nl.ibs.gym8020.core.logic.TourStep.NEXT_GOAL
import nl.ibs.gym8020.core.logic.TourStep.REPS
import nl.ibs.gym8020.core.logic.TourStep.STACKING
import nl.ibs.gym8020.core.logic.TourStep.WARMUP
import nl.ibs.gym8020.core.logic.TourView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The tour in the practice workout (#97): order of the steps, when a step shows, and when it is offered. */
class TourTest {

    private val beforeGo = setOf(GOAL, STACKING, WARMUP, GO)

    @Test
    fun `the steps come in the order of the prototype`() {
        assertEquals(listOf(GOAL, STACKING, WARMUP, GO, REPS, NEXT_GOAL), TourStep.entries.toList())
        var tour = Tour()
        val seen = mutableListOf<TourStep>()
        while (!tour.finished) {
            seen += tour.current!!
            tour = tour.next()
        }
        assertEquals(TourStep.entries.toList(), seen)
        assertNull(tour.current)
        assertEquals(tour, tour.next())
    }

    @Test
    fun `a step only shows once its part is in view`() {
        val tour = Tour()
        assertNull(tour.showing(present = emptySet(), inView = emptySet()), "nothing on screen yet")
        assertNull(tour.showing(present = setOf(GOAL), inView = emptySet()), "on the screen but scrolled away")
        assertEquals(GOAL, tour.showing(present = setOf(GOAL), inView = setOf(GOAL)))
    }

    @Test
    fun `reps wait for GO, the next target waits until it is in view`() {
        val atReps = Tour(done = REPS.ordinal)
        // Before GO the reps part is not there (the screen only marks it after GO): the tour waits, no balloon.
        assertNull(atReps.showing(present = beforeGo, inView = beforeGo))
        assertEquals(REPS, atReps.catchUp(beforeGo).current, "earlier parts never move the tour back or on")
        assertEquals(REPS, atReps.showing(present = setOf(REPS), inView = setOf(REPS)))

        val atNext = atReps.next()
        assertNull(atNext.showing(present = setOf(REPS), inView = setOf(REPS)), "no target before reps are in")
        assertNull(atNext.showing(present = setOf(REPS, NEXT_GOAL), inView = setOf(REPS)), "below the fold")
        assertEquals(NEXT_GOAL, atNext.showing(present = setOf(NEXT_GOAL), inView = setOf(NEXT_GOAL)))
    }

    @Test
    fun `a step whose part does not come is passed when a later part is there`() {
        // No plates to take off: the stacking line is not there, the warm-up is.
        val atStacking = Tour(done = STACKING.ordinal)
        val present = setOf(GOAL, WARMUP, GO)
        assertEquals(WARMUP, atStacking.showing(present, present))
        assertEquals(GO, atStacking.catchUp(present).next().current)
        // Nothing later there either: wait.
        assertEquals(atStacking, atStacking.catchUp(setOf(GOAL)))
    }

    @Test
    fun `skip ends the tour at any step`() {
        TourStep.entries.forEach { step ->
            val skipped = Tour(done = step.ordinal).skip()
            assertTrue(skipped.finished)
            assertNull(skipped.showing(TourStep.entries.toSet(), TourStep.entries.toSet()))
        }
    }

    @Test
    fun `the balloon counts the steps`() {
        assertEquals(1, Tour().number)
        assertEquals(6, Tour().total)
        assertEquals(6, Tour(done = NEXT_GOAL.ordinal).number)
    }

    @Test
    fun `in view means most of the part is on the screen`() {
        assertTrue(TourView.inView(100f, 100f))
        assertTrue(TourView.inView(100f, 80f))
        assertFalse(TourView.inView(100f, 79f))
        assertFalse(TourView.inView(0f, 0f), "a part without size is not in view")
    }

    @Test
    fun `offered once after the introduction to new users only`() {
        // A new install: no flag, introduction not done yet.
        val fresh = TourOffer.offered(stored = null, onboarded = false)
        assertTrue(TourOffer.offerAfterIntro(fresh))
        // Once offered (start or skip), the flag is stored and the offer does not come again.
        assertFalse(TourOffer.offerAfterIntro(TourOffer.offered(stored = true, onboarded = true)))
        // An existing user from before the tour: introduction done, no flag. Counts as offered.
        assertFalse(TourOffer.offerAfterIntro(TourOffer.offered(stored = null, onboarded = true)))
        // A stored flag wins.
        assertTrue(TourOffer.offerAfterIntro(TourOffer.offered(stored = false, onboarded = true)))
    }
}
