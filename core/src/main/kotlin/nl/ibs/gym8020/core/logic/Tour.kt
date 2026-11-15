package nl.ibs.gym8020.core.logic

/**
 * When the practice workout with the tour is offered (#97): once, after the introduction on the first start. Existing
 * users do not get it unasked; for them it counts as offered already. Everyone can start it again from the settings.
 */
object TourOffer {
    /**
     * Whether the tour was offered, from the stored flag. Installs from before the tour have no flag: whoever finished
     * the introduction then is an existing user, for whom it counts as offered.
     */
    fun offered(stored: Boolean?, onboarded: Boolean): Boolean = stored ?: onboarded

    /** After finishing (or skipping) the introduction: offer the practice workout when it was not offered yet. */
    fun offerAfterIntro(offered: Boolean): Boolean = !offered
}
