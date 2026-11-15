package nl.ibs.gym8020.core.logic

/**
 * The practice workout plays itself (#99): a finger moves to a part of the training screen, taps it and types numbers,
 * with a balloon that explains each step. The viewer does nothing. This file is the script of that demo (which action,
 * on which part, with which value, how long) and where the playback is; the app only performs it.
 */

/** A part of the training screen the demo points at, taps or types into. */
enum class DemoTarget {
    /** The target card before GO. */
    GOAL,

    /** The line with what comes off for the light set (GLOSSARY "Stapelen"). */
    STACKING,

    /** The warm-up button with the default warm-up. */
    WARMUP,

    /** GO: the short countdown, then the exercise timer. */
    GO,

    /** The reps of the heavy set (its field, or its line when another set is open). */
    HEAVY_REPS,

    /** The reps of the light set. */
    LIGHT_REPS,

    /** "Done" under the open set. */
    SET_DONE,

    /** The next target with the app's advice. */
    NEXT_GOAL,

    /** "Finish" at the bottom: in the demo straight to the short summary, no confirmation. */
    FINISH,
}

/** The steps of the demo, in this order, each with its own balloon. */
enum class DemoStep { GOAL, STACKING, WARMUP, GO, HEAVY, LIGHT, NEXT_GOAL, FINISH }

/** One thing the demo does, and how long it takes. */
sealed interface DemoAction {
    val millis: Long

    /** The finger moves to the part (scrolled into view first); the part lights up. */
    data class MoveTo(val target: DemoTarget) : DemoAction {
        override val millis: Long get() = PracticeDemo.MOVE_MS
    }

    /** The finger taps: a ring around it, and the part does what a tap does. */
    data class Tap(val target: DemoTarget) : DemoAction {
        override val millis: Long get() = PracticeDemo.TAP_MS
    }

    /** The finger types [text] into the part, one character at a time. */
    data class Type(val target: DemoTarget, val text: String) : DemoAction {
        override val millis: Long get() = PracticeDemo.KEY_MS * text.length
    }

    /** Time to read the balloon. */
    data class Wait(override val millis: Long) : DemoAction
}

/** One step of the demo: its balloon shows while its actions play. */
data class DemoScene(val step: DemoStep, val actions: List<DemoAction>) {
    val millis: Long get() = actions.sumOf { it.millis }
}

/**
 * What the demo has done before a scene, as data: the app sets up the practice workout like this to start (again) at
 * that scene, for "‹ back" and for "Watch again".
 */
data class DemoProgress(
    val warmup: Boolean = false,
    val go: Boolean = false,
    val heavyReps: Int? = null,
    val lightReps: Int? = null,
)

/** The whole demo: its scenes in order, and the reps it types. */
data class DemoScript(val scenes: List<DemoScene>, val heavyReps: Int, val lightReps: Int) {
    val millis: Long get() = scenes.sumOf { it.millis }

    /** Where scene [index] starts on the timeline. */
    fun startOf(index: Int): Long = scenes.take(index).sumOf { it.millis }

    /** What the scenes before [index] did: a tap on the warm-up or GO, reps typed into a set. */
    fun progressBefore(index: Int): DemoProgress =
        scenes.take(index).flatMap { it.actions }.fold(DemoProgress()) { p, a ->
            when {
                a is DemoAction.Tap && a.target == DemoTarget.WARMUP -> p.copy(warmup = true)
                a is DemoAction.Tap && a.target == DemoTarget.GO -> p.copy(go = true)
                a is DemoAction.Type && a.target == DemoTarget.HEAVY_REPS -> p.copy(heavyReps = a.text.toInt())
                a is DemoAction.Type && a.target == DemoTarget.LIGHT_REPS -> p.copy(lightReps = a.text.toInt())
                else -> p
            }
        }
}

object PracticeDemo {
    /** The finger moves to a part. */
    const val MOVE_MS = 700L

    /** A tap with its ring. */
    const val TAP_MS = 500L

    /** One typed character. */
    const val KEY_MS = 220L

    /** The countdown before the exercise timer in the practice workout: short, no real rest (#97). */
    const val COUNTDOWN_SECONDS = 2

    /** The longest the demo waits for a part to appear (e.g. the reps after the countdown); then it goes on. */
    const val TARGET_WAIT_MS = 5_000L

    /**
     * The heavy set gets more reps than the target allows (too light: the advice goes up), the light set its target
     * (good), so the demo shows a signal and an advice.
     */
    fun heavyReps(heavyTarget: Int, goodMargin: Int): Int = heavyTarget + goodMargin + 1

    fun lightReps(lightTarget: Int): Int = lightTarget

    /** The demo for the method's rep targets ([heavyTarget], [lightTarget], ±[goodMargin]). */
    fun script(heavyTarget: Int, lightTarget: Int, goodMargin: Int): DemoScript {
        val heavy = heavyReps(heavyTarget, goodMargin)
        val light = lightReps(lightTarget)
        fun scene(step: DemoStep, vararg actions: DemoAction) = DemoScene(step, actions.toList())
        fun reps(target: DemoTarget, reps: Int) = arrayOf(
            DemoAction.MoveTo(target), DemoAction.Tap(target), DemoAction.Type(target, reps.toString()),
            DemoAction.Wait(2_500),
            DemoAction.MoveTo(DemoTarget.SET_DONE), DemoAction.Tap(DemoTarget.SET_DONE), DemoAction.Wait(600),
        )
        val scenes = listOf(
            scene(DemoStep.GOAL, DemoAction.MoveTo(DemoTarget.GOAL), DemoAction.Wait(3_600)),
            scene(DemoStep.STACKING, DemoAction.MoveTo(DemoTarget.STACKING), DemoAction.Wait(3_600)),
            scene(DemoStep.WARMUP, DemoAction.MoveTo(DemoTarget.WARMUP), DemoAction.Tap(DemoTarget.WARMUP), DemoAction.Wait(3_000)),
            // The countdown runs during the wait, then the exercise timer.
            scene(DemoStep.GO, DemoAction.MoveTo(DemoTarget.GO), DemoAction.Tap(DemoTarget.GO), DemoAction.Wait(COUNTDOWN_SECONDS * 1_000L + 1_500)),
            scene(DemoStep.HEAVY, *reps(DemoTarget.HEAVY_REPS, heavy)),
            scene(DemoStep.LIGHT, *reps(DemoTarget.LIGHT_REPS, light)),
            scene(DemoStep.NEXT_GOAL, DemoAction.MoveTo(DemoTarget.NEXT_GOAL), DemoAction.Wait(4_000)),
            scene(
                DemoStep.FINISH, DemoAction.MoveTo(DemoTarget.FINISH), DemoAction.Wait(1_200), DemoAction.Tap(DemoTarget.FINISH),
                DemoAction.Wait(1_000),
            ),
        )
        return DemoScript(scenes, heavy, light)
    }
}

/** How the demo ended: played to the end (the summary follows) or skipped (the practice closes). */
enum class DemoEnd { FINISHED, SKIPPED }

/**
 * Where the playback is: the [scene] that plays, whether it is [paused], and how it [end]ed. [take] counts the starts,
 * so a scene that starts over (‹ back, "Watch again") is a new take even at the same index.
 */
data class DemoPlayback(val scene: Int = 0, val paused: Boolean = false, val end: DemoEnd? = null, val take: Int = 0) {
    val ended: Boolean get() = end != null

    /** Pause / Resume. */
    fun togglePause(): DemoPlayback = if (ended) this else copy(paused = !paused)

    /** The scene played to its end: on to the next, or the end after the last of [sceneCount]. */
    fun sceneDone(sceneCount: Int): DemoPlayback = when {
        ended -> this
        scene + 1 >= sceneCount -> copy(end = DemoEnd.FINISHED)
        else -> copy(scene = scene + 1)
    }

    /**
     * "‹": early in a scene ([elapsedMs] under [BACK_RESTARTS_AFTER_MS]) back to the previous one, later this scene
     * again from its start. It plays at once, also when paused.
     */
    fun back(elapsedMs: Long): DemoPlayback {
        if (ended) return this
        val to = if (elapsedMs >= BACK_RESTARTS_AFTER_MS) scene else (scene - 1).coerceAtLeast(0)
        return copy(scene = to, paused = false, take = take + 1)
    }

    /** This scene again from its start, paused or not (e.g. after a rotation in the middle of it). */
    fun restart(): DemoPlayback = if (ended) this else copy(take = take + 1)

    /** "Skip": the demo stops. */
    fun skip(): DemoPlayback = if (ended) this else copy(end = DemoEnd.SKIPPED)

    /** "Watch again" after the end: from the first scene. */
    fun again(): DemoPlayback = DemoPlayback(take = take + 1)

    companion object {
        /** After this long in a scene, "‹" starts the same scene again instead of going to the previous one. */
        const val BACK_RESTARTS_AFTER_MS = 1_500L
    }
}
