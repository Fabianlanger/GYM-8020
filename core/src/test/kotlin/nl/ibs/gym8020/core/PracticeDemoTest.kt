package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.DemoAction.MoveTo
import nl.ibs.gym8020.core.logic.DemoAction.Tap
import nl.ibs.gym8020.core.logic.DemoAction.Type
import nl.ibs.gym8020.core.logic.DemoAction.Wait
import nl.ibs.gym8020.core.logic.DemoEnd
import nl.ibs.gym8020.core.logic.DemoPlayback
import nl.ibs.gym8020.core.logic.DemoProgress
import nl.ibs.gym8020.core.logic.DemoStep
import nl.ibs.gym8020.core.logic.DemoTarget
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PracticeDemo
import nl.ibs.gym8020.core.model.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The practice workout plays itself (#99): the script of the demo and its playback. */
class PracticeDemoTest {

    private val script = PracticeDemo.script(heavyTarget = 6, lightTarget = 16, goodMargin = 1)

    private fun scene(step: DemoStep) = script.scenes.single { it.step == step }

    @Test
    fun `the scenes come in the order of the training`() {
        assertEquals(DemoStep.entries.toList(), script.scenes.map { it.step })
    }

    @Test
    fun `the demo taps the warm-up and GO, types both sets and finishes`() {
        assertEquals(listOf(MoveTo(DemoTarget.GOAL)), scene(DemoStep.GOAL).actions.filter { it !is Wait })
        assertEquals(listOf(MoveTo(DemoTarget.STACKING)), scene(DemoStep.STACKING).actions.filter { it !is Wait })
        assertEquals(listOf(MoveTo(DemoTarget.WARMUP), Tap(DemoTarget.WARMUP)), scene(DemoStep.WARMUP).actions.filter { it !is Wait })
        assertEquals(listOf(MoveTo(DemoTarget.GO), Tap(DemoTarget.GO)), scene(DemoStep.GO).actions.filter { it !is Wait })
        assertEquals(
            listOf(
                MoveTo(DemoTarget.HEAVY_REPS), Tap(DemoTarget.HEAVY_REPS), Type(DemoTarget.HEAVY_REPS, "8"),
                MoveTo(DemoTarget.SET_DONE), Tap(DemoTarget.SET_DONE),
            ),
            scene(DemoStep.HEAVY).actions.filter { it !is Wait },
        )
        assertEquals(
            listOf(
                MoveTo(DemoTarget.LIGHT_REPS), Tap(DemoTarget.LIGHT_REPS), Type(DemoTarget.LIGHT_REPS, "16"),
                MoveTo(DemoTarget.SET_DONE), Tap(DemoTarget.SET_DONE),
            ),
            scene(DemoStep.LIGHT).actions.filter { it !is Wait },
        )
        assertEquals(listOf(MoveTo(DemoTarget.NEXT_GOAL)), scene(DemoStep.NEXT_GOAL).actions.filter { it !is Wait })
        assertEquals(listOf(MoveTo(DemoTarget.FINISH), Tap(DemoTarget.FINISH)), scene(DemoStep.FINISH).actions.filter { it !is Wait })
    }

    @Test
    fun `the finger always moves to a part before it taps or types there`() {
        script.scenes.forEach { s ->
            var at: DemoTarget? = null
            s.actions.forEach { a ->
                when (a) {
                    is MoveTo -> at = a.target
                    is Tap -> assertEquals(at, a.target, "${s.step}: tap where the finger is")
                    is Type -> assertEquals(at, a.target, "${s.step}: type where the finger is")
                    is Wait -> {}
                }
            }
            assertTrue(s.actions.first() is MoveTo, "${s.step} starts by pointing at its part")
        }
    }

    @Test
    fun `the heavy set comes out too light and the light set good, so there is a signal and an advice`() {
        listOf(Triple(6, 16, 1), Triple(5, 15, 2), Triple(8, 20, 0)).forEach { (h, l, m) ->
            val s = PracticeDemo.script(h, l, m)
            assertEquals(Signal.TOO_LIGHT, GoalAdvisor.signal(s.heavyReps, h, m))
            assertEquals(Signal.GOOD, GoalAdvisor.signal(s.lightReps, l, m))
        }
    }

    @Test
    fun `about 3 to 6 seconds per step, well under a minute and a half in all`() {
        script.scenes.forEach { s -> assertTrue(s.millis in 3_000L..6_500L, "${s.step}: ${s.millis} ms") }
        assertTrue(script.millis in 30_000L..70_000L, "whole demo: ${script.millis} ms")
        assertEquals(script.millis, script.startOf(script.scenes.size))
        assertEquals(0L, script.startOf(0))
        assertEquals(scene(DemoStep.GOAL).millis, script.startOf(1))
    }

    @Test
    fun `timings of the actions`() {
        assertEquals(PracticeDemo.MOVE_MS, MoveTo(DemoTarget.GO).millis)
        assertEquals(PracticeDemo.TAP_MS, Tap(DemoTarget.GO).millis)
        assertEquals(2 * PracticeDemo.KEY_MS, Type(DemoTarget.LIGHT_REPS, "16").millis, "one key per digit")
        // The GO scene lasts longer than the countdown, so the reps are there when the next scene points at them.
        assertTrue(scene(DemoStep.GO).actions.filterIsInstance<Wait>().sumOf { it.millis } > PracticeDemo.COUNTDOWN_SECONDS * 1_000L)
    }

    @Test
    fun `a scene starts from what the scenes before it did`() {
        val i = { step: DemoStep -> script.scenes.indexOfFirst { it.step == step } }
        assertEquals(DemoProgress(), script.progressBefore(0))
        assertEquals(DemoProgress(), script.progressBefore(i(DemoStep.WARMUP)))
        assertEquals(DemoProgress(warmup = true), script.progressBefore(i(DemoStep.GO)))
        assertEquals(DemoProgress(warmup = true, go = true), script.progressBefore(i(DemoStep.HEAVY)))
        assertEquals(DemoProgress(warmup = true, go = true, heavyReps = 8), script.progressBefore(i(DemoStep.LIGHT)))
        val all = DemoProgress(warmup = true, go = true, heavyReps = 8, lightReps = 16)
        assertEquals(all, script.progressBefore(i(DemoStep.NEXT_GOAL)))
        assertEquals(all, script.progressBefore(i(DemoStep.FINISH)))
    }

    @Test
    fun `the playback goes scene by scene to the end`() {
        var p = DemoPlayback()
        val seen = mutableListOf<Int>()
        while (!p.ended) {
            seen += p.scene
            p = p.sceneDone(script.scenes.size)
        }
        assertEquals(script.scenes.indices.toList(), seen)
        assertEquals(DemoEnd.FINISHED, p.end)
        assertEquals(p, p.sceneDone(script.scenes.size), "nothing after the end")
    }

    @Test
    fun `pause holds the scene, resume goes on`() {
        val p = DemoPlayback(scene = 3).togglePause()
        assertTrue(p.paused)
        assertEquals(3, p.scene)
        val resumed = p.togglePause()
        assertFalse(resumed.paused)
        assertEquals(3, resumed.scene)
    }

    @Test
    fun `back goes to the previous scene early on, else starts this scene again`() {
        val p = DemoPlayback(scene = 3, take = 2)
        assertEquals(2, p.back(elapsedMs = 400).scene)
        assertEquals(3, p.back(elapsedMs = DemoPlayback.BACK_RESTARTS_AFTER_MS).scene)
        // Each back is a new take, so the same scene starts over too.
        assertEquals(3, p.back(400).take)
        assertEquals(0, DemoPlayback().back(0).scene, "the first scene starts over")
        assertFalse(p.togglePause().back(400).paused, "back plays at once")
        // Starting a scene over keeps the scene and the pause, as a new take.
        assertEquals(DemoPlayback(scene = 3, paused = true, take = 3), p.togglePause().restart())
    }

    @Test
    fun `skip ends the demo at any scene, watch again starts at the first`() {
        script.scenes.indices.forEach { i ->
            val skipped = DemoPlayback(scene = i).skip()
            assertEquals(DemoEnd.SKIPPED, skipped.end)
            assertEquals(skipped, skipped.togglePause().back(0).sceneDone(script.scenes.size), "nothing plays after skip")
        }
        val again = DemoPlayback(scene = 7, end = DemoEnd.FINISHED, take = 4).again()
        assertEquals(DemoPlayback(scene = 0, take = 5), again)
    }
}
