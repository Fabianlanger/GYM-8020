package nl.ibs.gym8020.ui

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.MainActivity
import nl.ibs.gym8020.SPLASH_TAG
import nl.ibs.gym8020.data.AppLanguage
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.Seeder
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.manualMeasurement
import nl.ibs.gym8020.ui.tour.DEMO_BACK_TAG
import nl.ibs.gym8020.ui.tour.DEMO_BALLOON_TAG
import nl.ibs.gym8020.ui.tour.DEMO_FINGER_TAG
import nl.ibs.gym8020.ui.tour.DEMO_PAUSE_TAG
import nl.ibs.gym8020.ui.tour.DEMO_SKIP_TAG
import nl.ibs.gym8020.ui.tour.PRACTICE_AGAIN_TAG
import nl.ibs.gym8020.ui.tour.PRACTICE_BANNER_TAG
import nl.ibs.gym8020.ui.tour.PRACTICE_CLOSE_TAG
import nl.ibs.gym8020.ui.tour.PRACTICE_DONE_TAG
import nl.ibs.gym8020.ui.tour.TOUR_OFFER_START_TAG
import nl.ibs.gym8020.ui.tour.TOUR_OFFER_TAG
import nl.ibs.gym8020.ui.training.COUNTDOWN_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * F-11 (TESTING.md, #97, #99): the practice workout that plays itself. Offered once after the introduction; the demo
 * taps and types the whole practice workout without input up to the summary; Pause holds it, ‹ shows a step again,
 * Skip stops it; the practice leaves nothing behind in your own data, and it can be started again from the settings.
 * The demo runs on the test clock ([play]); clicks run the nodes' click actions (no windows/dialogs are opened, T-7).
 * Example data only (D-06a).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class PracticeFlowsTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val container get() = (context as GymApp).container

    /** A fresh install in English: default settings, standard content, body weight logged (no question window). */
    private fun freshInstall() = runBlocking {
        AppLanguage.set(context, AppLanguage.ENGLISH)
        val app = context as GymApp
        app.seeding.join()
        withContext(Dispatchers.IO) { container.db.clearAllTables() }
        container.settings.update { AppSettings() }
        Seeder(container.db, container.settings, dutch = false).seedIfNeeded()
        container.db.bodyweights().insert(manualMeasurement(80.0))
    }

    private fun ComposeTestRule.has(text: String) = onAllNodesWithText(text, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeTestRule.hasTag(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeTestRule.waitForText(text: String) = waitUntil(20_000) { has(text) }

    private fun ComposeTestRule.waitForTag(tag: String) = waitUntil(20_000) { hasTag(tag) }

    /** Click without a touch: runs the node's click action. */
    private fun ComposeTestRule.click(text: String) =
        onAllNodesWithText(text, ignoreCase = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    private fun ComposeTestRule.clickTag(tag: String) =
        onAllNodesWithTag(tag).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    /** The demo's balloon shows [title] (the step it plays). */
    private fun ComposeTestRule.balloon(title: String) = hasTag(DEMO_BALLOON_TAG) && has(title)

    private fun ComposeTestRule.waitForSplash() = waitUntil(20_000) { onAllNodesWithTag(SPLASH_TAG).fetchSemanticsNodes().isEmpty() }

    private fun ComposeTestRule.shot(name: String) {
        waitForIdle()
        onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** On the page on screen: the pager builds pages beside it too. */
    private val visible = SemanticsMatcher("on the page on screen") {
        it.positionInRoot.x >= 0f && it.positionInRoot.x < context.resources.displayMetrics.widthPixels
    }

    /**
     * Plays [ms] of the demo without any input: the compose clock (the demo's waits, the finger's animations) and the
     * main looper (the countdown's wall clock) go on together, with a moment for the practice database in between.
     */
    private fun play(ms: Long) {
        var left = ms
        while (left > 0) {
            val step = minOf(100L, left)
            rule.mainClock.advanceTimeBy(step)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            Thread.sleep(2)
            left -= step
        }
    }

    /** Plays the demo until [condition], at most [limitMs] of demo time. */
    private fun playUntil(what: String, limitMs: Long = 90_000, condition: () -> Boolean) {
        var played = 0L
        while (!condition()) {
            assertTrue("$what within $limitMs ms of demo time", played < limitMs)
            play(100)
            played += 100
        }
    }

    /** The practice workout's one exercise and its sets, in the practice database in memory. */
    private fun practiceExercise() = runBlocking { container.practice.db.sessions().allExercises().singleOrNull() }

    private fun practiceReps(role: SetRole) = runBlocking { container.practice.db.sessions().allSets().singleOrNull { it.role == role }?.reps }

    /** From the offer after the first introduction into the practice workout; the demo then runs on [play] only. */
    private fun startFromOffer() {
        rule.waitForText("Skip")
        rule.waitForSplash()
        rule.click("Skip")
        rule.waitForTag(TOUR_OFFER_TAG)
        rule.mainClock.autoAdvance = false
        rule.clickTag(TOUR_OFFER_START_TAG)
        playUntil("the practice workout") { rule.hasTag(PRACTICE_BANNER_TAG) && rule.hasTag(DEMO_BALLOON_TAG) }
    }

    /** Your own data stays untouched: no training, no target, no backup mark, the library exercise as it was. */
    private fun assertNothingSaved() = runBlocking {
        val db = container.db
        assertTrue("no training in the log", db.sessions().all().isEmpty())
        assertTrue("no sets", db.sessions().allSets().isEmpty())
        assertTrue("no target", db.plans().all().isEmpty() && db.targets().all().isEmpty())
        val squat = db.exercises().all().single { it.name == "Back squat" }
        assertNull("no default warm-up stored", squat.defaultWarmupKg)
        val settings = container.settings.current()
        assertNull("no automatic backup ran", settings.lastBackupAt)
        assertFalse(settings.backupFailed)
    }

    @Test
    fun `F-11 the practice workout plays itself to the summary, taps on the screen do nothing, nothing is saved`() {
        freshInstall()
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForText("Skip")
            rule.waitForSplash()
            rule.click("Skip")

            // Once, after the first introduction: the offer.
            rule.waitForTag(TOUR_OFFER_TAG)
            rule.shot("en_20_practice_offer")
            assertTrue("offered", runBlocking { container.settings.current().tourOffered })

            // From here on no input: only time passes.
            rule.mainClock.autoAdvance = false
            rule.clickTag(TOUR_OFFER_START_TAG)
            playUntil("the target") { rule.balloon("Your target for today.") }
            playUntil("the finger") { rule.hasTag(DEMO_FINGER_TAG) }
            play(800)
            rule.shot("en_21_demo_goal")

            // A real tap on GO does nothing: the demo plays by itself.
            rule.onAllNodesWithText("Go", ignoreCase = true).filter(visible).onFirst().performTouchInput { click() }
            play(200)
            assertFalse("no countdown from a tap", rule.hasTag(COUNTDOWN_TAG))
            assertNull("GO not tapped", practiceExercise()?.goAt)

            // The demo taps the warm-up and GO itself.
            playUntil("the warm-up") { rule.balloon("Warm-up (optional).") }
            playUntil("the warm-up tapped") { practiceExercise()?.warmupAt != null }
            playUntil("GO") { rule.balloon("GO starts the countdown and the timer.") }
            playUntil("the countdown") { rule.hasTag(COUNTDOWN_TAG) }
            playUntil("GO done") { practiceExercise()?.goAt != null }

            // It types the reps of the heavy set (too light) and of the light set (good).
            playUntil("the heavy set") { rule.balloon("Heavy set: 8 reps.") }
            playUntil("8 typed") { practiceReps(SetRole.HEAVY) == 8 }
            rule.shot("en_23_demo_heavy")
            playUntil("the light set") { rule.balloon("Light set: 16 reps.") }
            playUntil("16 typed") { practiceReps(SetRole.LIGHT) == 16 }

            // The next target with the advice, then finishing without a confirmation window.
            playUntil("the next target") { rule.balloon("You set your next target.") }
            play(1_500)
            rule.shot("en_24_demo_next_target")
            playUntil("the summary") { rule.hasTag(PRACTICE_DONE_TAG) }
            play(500)
            rule.shot("en_25_practice_summary")
            assertTrue(rule.has("Watch again"))
            assertNotNull("the practice training was finished", runBlocking { container.practice.db.sessions().all().single().endedAt })

            // The practice lives in its own database in memory; your own data is untouched.
            assertEquals(8, practiceReps(SetRole.HEAVY))
            assertEquals(16, practiceReps(SetRole.LIGHT))
            assertNothingSaved()

            // Watch again: the demo starts over on a new practice workout.
            rule.clickTag(PRACTICE_AGAIN_TAG)
            playUntil("the demo again") { rule.balloon("Your target for today.") }
            assertNull("a fresh practice workout", practiceReps(SetRole.HEAVY))

            // Skip: the demo stops and the practice workout closes; still nothing saved.
            rule.clickTag(DEMO_SKIP_TAG)
            rule.mainClock.autoAdvance = true
            rule.waitForText("Weight today")
            assertNothingSaved()
        }
    }

    @Test
    fun `F-11 pause holds the demo, back shows a step again, done closes the summary`() {
        freshInstall()
        ActivityScenario.launch(MainActivity::class.java).use {
            startFromOffer()

            // Pause during the warm-up step: time passes, the demo does not go on.
            playUntil("the warm-up") { rule.balloon("Warm-up (optional).") }
            rule.clickTag(DEMO_PAUSE_TAG)
            play(15_000)
            assertTrue("still the warm-up step", rule.balloon("Warm-up (optional)."))
            assertTrue(rule.has("Resume"))
            assertNull("GO not tapped while paused", practiceExercise()?.goAt)
            rule.shot("en_22_demo_paused")

            // Resume: on to GO.
            rule.clickTag(DEMO_PAUSE_TAG)
            playUntil("GO") { rule.balloon("GO starts the countdown and the timer.") }

            // ‹ right at the start of a step: the step before again, on a practice workout as it was then.
            rule.clickTag(DEMO_BACK_TAG)
            playUntil("the warm-up again") { rule.balloon("Warm-up (optional).") }
            playUntil("the warm-up tapped again") { practiceExercise()?.warmupAt != null }

            // It plays on by itself to the summary; Done closes it.
            playUntil("the summary") { rule.hasTag(PRACTICE_DONE_TAG) }
            assertEquals(8, practiceReps(SetRole.HEAVY))
            rule.clickTag(PRACTICE_CLOSE_TAG)
            rule.mainClock.autoAdvance = true
            rule.waitForText("Weight today")
            assertNothingSaved()
        }
    }

    @Test
    fun `F-11 offered once, again from the settings, skip stops the demo`() {
        freshInstall()
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForText("Skip")
            rule.waitForSplash()
            rule.click("Skip")
            rule.waitForTag(TOUR_OFFER_TAG)
            rule.clickTag(nl.ibs.gym8020.ui.tour.TOUR_OFFER_SKIP_TAG)
            rule.waitForText("Weight today")

            // The introduction again from the settings: no second offer.
            rule.click("Settings")
            rule.waitForText("Show introduction")
            rule.click("Show introduction")
            rule.waitForText("Skip")
            rule.click("Skip")
            rule.waitForText("Practice workout with tour")
            assertFalse("offered only once", rule.hasTag(TOUR_OFFER_TAG))

            // Started again from the settings; skip stops the demo and closes the practice workout.
            rule.click("Practice workout with tour")
            rule.waitForTag(PRACTICE_BANNER_TAG)
            rule.waitUntil(20_000) { rule.balloon("Your target for today.") }
            rule.clickTag(DEMO_SKIP_TAG)
            rule.waitForText("Practice workout with tour")
            assertFalse("the demo is gone", rule.hasTag(DEMO_BALLOON_TAG))
            assertNothingSaved()
        }
    }

    @Test
    fun `F-11 existing users do not get the offer`() {
        freshInstall()
        // A user from before the tour: trainings in the log, introduction not marked yet (an old install).
        runBlocking {
            container.db.sessions().insert(
                nl.ibs.gym8020.data.db.SessionEntity(schemaName = "Full body", startedAt = 1_000L, endedAt = 2_000L),
            )
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForText("Weight today")
            rule.waitForSplash()
            rule.waitUntil(20_000) { runBlocking { container.settings.current().onboarded } }
            val settings = runBlocking { container.settings.current() }
            assertTrue("counts as offered", settings.tourOffered)
            assertFalse(rule.hasTag(TOUR_OFFER_TAG))
            assertEquals(1, runBlocking { container.db.sessions().all().size })
        }
    }
}
