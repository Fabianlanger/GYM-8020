package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.TourOffer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** When the practice workout is offered (#97). The demo itself: [PracticeDemoTest]. */
class TourTest {

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
