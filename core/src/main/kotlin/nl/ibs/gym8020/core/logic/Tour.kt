package nl.ibs.gym8020.core.logic

/**
 * The steps of the tour in the practice workout (#97, variant A of #83), in this order: each lights up one part of
 * the training screen with a short explanation.
 */
enum class TourStep {
    /** The target card before GO: weight and plates per set. */
    GOAL,

    /** Stacking (GLOSSARY "Stapelen"): from heavy to light you only take plates off. */
    STACKING,

    /** The warm-up button with the default warm-up. */
    WARMUP,

    /** GO: countdown, then the exercise timer. */
    GO,

    /** Entering reps: the app says at once whether it was too light, good or too heavy. Only after GO. */
    REPS,

    /** The next target with the app's advice. */
    NEXT_GOAL,
}

/**
 * Where the tour is: [done] steps were shown and passed with "Next", or the whole tour was [skipped]. A step is only
 * shown once its part is on screen (#97); until then the tour waits and the screen works as usual.
 */
data class Tour(val done: Int = 0, val skipped: Boolean = false) {

    val finished: Boolean get() = skipped || done >= TourStep.entries.size

    /** The step the tour waits for or shows; null when finished. */
    val current: TourStep? get() = if (finished) null else TourStep.entries[done]

    /** "2 / 6" in the balloon. */
    val number: Int get() = done + 1

    val total: Int get() = TourStep.entries.size

    /** On to the next step ("Next"). */
    fun next(): Tour = if (finished) this else copy(done = done + 1)

    /** "Skip": no more balloons. */
    fun skip(): Tour = copy(skipped = true)

    /**
     * When the part of the current step is gone from the screen (not shown at all) while a later step's part is there,
     * the tour moves on to that later step, so it never waits for something that will not come (for instance no
     * plates to take off). [present] are the parts on the screen, in view or not.
     */
    fun catchUp(present: Set<TourStep>): Tour {
        val step = current ?: return this
        if (step in present) return this
        val later = TourStep.entries.drop(step.ordinal + 1).firstOrNull { it in present } ?: return this
        return copy(done = later.ordinal)
    }

    /** The step to show now: the current one (after [catchUp]), only when its part is in view. */
    fun showing(present: Set<TourStep>, inView: Set<TourStep>): TourStep? = catchUp(present).current?.takeIf { it in inView }
}

/** When a part counts as "in view" for the tour. */
object TourView {
    /** At least this share of a part must be visible on the screen. */
    const val MIN_VISIBLE = 0.8f

    /** A part of [fullHeight] of which [visibleHeight] is on the screen. */
    fun inView(fullHeight: Float, visibleHeight: Float): Boolean = fullHeight > 0f && visibleHeight >= fullHeight * MIN_VISIBLE
}

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
