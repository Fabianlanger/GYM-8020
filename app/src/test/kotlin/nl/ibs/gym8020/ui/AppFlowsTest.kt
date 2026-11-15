package nl.ibs.gym8020.ui

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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
import nl.ibs.gym8020.data.manualMeasurement
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.ComparisonFormat
import nl.ibs.gym8020.ui.training.COUNTDOWN_TAG
import nl.ibs.gym8020.ui.training.EXERCISE_TIMER_TAG
import nl.ibs.gym8020.ui.tour.TOUR_OFFER_SKIP_TAG
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * UI flows and screenshots (TESTING.md: T-4, T-5). Runs on the JVM with Robolectric; the screenshots land in
 * app/build/outputs/roborazzi and are uploaded by CI. Example data only, never personal data (D-06a).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AppFlowsTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * Each test gets a new database, but the settings store lives for the whole test process. Start every test
     * as a fresh install: default settings, then the standard content in the chosen language.
     */
    private fun freshInstall(dutch: Boolean = false) = runBlocking {
        AppLanguage.set(context, if (dutch) AppLanguage.DUTCH else AppLanguage.ENGLISH)
        val app = context as GymApp
        app.seeding.join() // the app's own first-start fill, based on the previous test's settings
        val container = app.container
        withContext(Dispatchers.IO) { container.db.clearAllTables() }
        container.settings.update { AppSettings() }
        Seeder(container.db, container.settings, dutch).seedIfNeeded()
    }

    private fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = 20_000) =
        waitUntil(timeoutMs) { onAllNodesWithText(text, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeTestRule.tap(text: String) = onAllNodesWithText(text, ignoreCase = true).onFirst().performClick()

    private fun ComposeTestRule.has(text: String) = onAllNodesWithText(text, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()

    /** Skips the introduction and the practice workout offered after it (#97), then waits for the home screen. */
    private fun ComposeTestRule.toHome(skip: String, home: String) {
        waitForText(skip)
        tap(skip)
        skipPracticeOffer()
        waitForText(home)
        waitForSplash()
    }

    /** After the first introduction the app offers a practice workout once (#97, F-11); skip it. */
    private fun ComposeTestRule.skipPracticeOffer() {
        waitUntil(20_000) { onAllNodesWithTag(TOUR_OFFER_SKIP_TAG).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithTag(TOUR_OFFER_SKIP_TAG).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    /** The opening screen lies over the app for a moment; screenshots should show what is under it. */
    private fun ComposeTestRule.waitForSplash() =
        waitUntil(20_000) { onAllNodesWithTag(SPLASH_TAG).fetchSemanticsNodes().isEmpty() }

    private fun ComposeTestRule.shot(name: String) {
        waitForIdle()
        onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** F-9 (intro on first start) plus the main screens, in English (the default, D-88). */
    @Test
    fun `F-9 first start shows the introduction, then the main screens`() {
        freshInstall()
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForText("Skip")
            rule.shot("en_01_intro")
            rule.tap("Skip")
            rule.skipPracticeOffer()
            rule.waitForText("Weight today")
            rule.waitForSplash()
            rule.shot("en_02_home")
            val settings = runBlocking { (context as GymApp).container.settings.current() }
            assertTrue("introduction is marked as done", settings.onboarded)

            rule.tap("Workouts")
            rule.waitForText("Programs")
            rule.shot("en_03_workouts")
            rule.tap("Exercises")
            rule.shot("en_04_exercises")
            rule.tap("Dashboard")
            rule.waitForText("Body weight")
            rule.shot("en_05_dashboard")
            rule.tap("Settings")
            rule.shot("en_06_settings")
        }
    }

    /**
     * F-1 start: a workout from the home screen opens the training screen. Body weight is already logged, so no
     * question window appears: windows (dialogs) never get idle in these tests on top of the app (T-7).
     */
    @Test
    fun `F-1 start a workout`() {
        freshInstall()
        runBlocking { (context as GymApp).container.db.bodyweights().insert(manualMeasurement(80.0)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.toHome("Skip", "Weight today")
            rule.shot("en_07_home_weight_logged")
            // The workout card under "Other workout": the big button reads "Start" or "Train anyway" depending on the day.
            rule.onAllNodesWithText("Full body").onLast().performClick()
            rule.waitForText("Back squat")
            rule.shot("en_08_training")
        }
    }

    /** The text of the exercise timer, or null when it is not shown. */
    private fun ComposeTestRule.timerText(): String? =
        onAllNodesWithTag(EXERCISE_TIMER_TAG, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.config
            ?.let { if (SemanticsProperties.Text in it) it[SemanticsProperties.Text].joinToString("") { t -> t.text } else "" }

    /** On the page on screen: the pager also builds the next exercise, one screen width to the right. */
    private val visible = SemanticsMatcher("on the page on screen") {
        it.positionInRoot.x >= 0f && it.positionInRoot.x < context.resources.displayMetrics.widthPixels
    }

    /** Robolectric's clock only moves when told to; the countdown and the timers read it. */
    private fun passTime(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

    /**
     * F-2: warm-up → GO → 3-2-1 → the exercise timer runs → the first input stops it. Body weight is logged, so no
     * question window appears (T-7). Clicks only on the visible page: the pager also builds the next exercise.
     */
    @Test
    fun `F-2 warm-up, GO, countdown, timer, first input stops the timer`() {
        freshInstall()
        val db = (context as GymApp).container.db
        runBlocking { db.bodyweights().insert(manualMeasurement(80.0)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.toHome("Skip", "Weight today")
            rule.onAllNodesWithText("Full body").onLast().performClick()
            rule.waitForText("Back squat")

            // Warm-up: the button starts the warm-up time and adds a warm-up set.
            rule.onAllNodesWithText("Warm-up ·", substring = true).filter(visible).onFirst().performClick()
            rule.waitUntil(20_000) { rule.onAllNodesWithText("load the weight", substring = true).fetchSemanticsNodes().isNotEmpty() }

            // GO starts the 3-2-1; the timer starts when it is over.
            rule.onAllNodesWithText("Go", ignoreCase = true).filter(visible).onFirst().performClick()
            rule.waitUntil(20_000) { rule.onAllNodesWithTag(COUNTDOWN_TAG).fetchSemanticsNodes().isNotEmpty() }
            rule.shot("en_09_countdown")
            passTime(4)
            rule.waitUntil(20_000) { rule.timerText() != null }
            assertTrue("countdown is gone", rule.onAllNodesWithTag(COUNTDOWN_TAG).fetchSemanticsNodes().isEmpty())

            // The timer runs.
            passTime(3)
            rule.waitUntil(20_000) { rule.timerText().let { it != null && it != "00:00" } }
            rule.shot("en_10_timer_running")

            // The first input (+ on the weight of the open heavy set) stops the timer.
            val more = rule.onAllNodesWithContentDescription("More").filter(visible).onFirst()
            more.performScrollTo()
            more.performClick()
            rule.waitUntil(20_000) { rule.timerText() == null }

            val times = runBlocking {
                val session = db.sessions().all().single()
                val squat = db.exercises().all().single { it.name == "Back squat" }
                db.sessions().exercises(session.id).single { it.exerciseId == squat.id }
            }
            val warmupAt = times.warmupAt ?: error("no warm-up time")
            val goAt = times.goAt ?: error("no GO time")
            val firstInputAt = times.firstInputAt ?: error("no first input time")
            assertTrue("GO after the warm-up", goAt >= warmupAt)
            // Order only: Robolectric's wall clock does not follow the test clock closely enough for exact durations.
            assertTrue("first input after GO", firstInputAt >= goAt)
        }
    }

    /** Click without a touch (runs the node's click action), also when the node is scrolled out of view. */
    private fun ComposeTestRule.click(text: String) =
        onAllNodesWithText(text, ignoreCase = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    /** A finished "Full body" training on [date] with one heavy back squat set (example data, D-06a). */
    private suspend fun finishedSquat(date: LocalDate, kg: Double, reps: Int) {
        val db = (context as GymApp).container.db
        val squat = db.exercises().all().single { it.name == "Back squat" }
        val start = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Full body", startedAt = start, endedAt = start + 3_600_000L))
        val seId = db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = squat.id, position = 0))
        db.sessions().insertSet(SetEntity(sessionExerciseId = seId, position = 0, role = SetRole.HEAVY, weightKg = kg, reps = reps))
    }

    /**
     * F-13 (#100): log → an older training opens as its summary → Edit. The summary compares with the training
     * before it and keeps its record as it held then, although a later training was better. No windows (T-7).
     */
    @Test
    fun `F-13 log, older training as summary, edit`() {
        freshInstall()
        val older = LocalDate.of(2026, 10, 8)
        runBlocking {
            val container = (context as GymApp).container
            // An existing user: no introduction, no practice offer.
            container.settings.update { it.copy(onboarded = true, tourOffered = true) }
            finishedSquat(LocalDate.of(2026, 10, 1), 90.0, 6) // e1RM 108
            finishedSquat(older, 100.0, 4) // e1RM 113,3: the one we open, a record then
            finishedSquat(LocalDate.of(2026, 10, 15), 120.0, 5) // e1RM 140: later and better, must not count
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForText("Weight today")
            rule.waitForSplash()
            rule.tap("Dashboard")
            rule.waitForText("All")
            rule.click("All")
            // The log replaced the dashboard (which shows the same rows, so wait until it is gone).
            rule.waitUntil(20_000) { !rule.has("Body weight") }
            rule.waitUntil(20_000) { rule.onAllNodesWithText("Full body").fetchSemanticsNodes().size == 3 }
            rule.shot("en_15_log")

            // Newest first: the middle row is the training of 8 October.
            rule.onAllNodesWithText("Full body")[1].performSemanticsAction(SemanticsActions.OnClick)

            // Its summary: the date as heading, against 1 October (not the 15th), and the record as it held then.
            rule.waitForText("Back to sessions")
            val change = ComparisonFormat.weight(Strength.e1rm(100.0, 4) - Strength.e1rm(90.0, 6), WeightUnit.KG)
            rule.waitUntil(20_000) { rule.onAllNodesWithText(change, substring = true).fetchSemanticsNodes().isNotEmpty() }
            assertTrue("heading shows the date", rule.has(older.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))))
            assertTrue("not the heading of a training just finished", !rule.has("Done for today"))
            assertTrue("the record of then still counts", rule.has("New record"))
            rule.shot("en_16_older_summary")

            // Edit: the correction screen of that training, and back to its summary.
            rule.click("Edit session")
            rule.waitForText("Duration (minutes)")
            assertTrue("edit screen shows the exercise", rule.has("Back squat"))
            rule.shot("en_17_older_edit")
            rule.click("Done")
            rule.waitForText("Back to sessions")
        }
    }

    /** V-8: Dutch is selectable; the same screens in Dutch. */
    @Test
    fun `V-8 Dutch when chosen`() {
        freshInstall(dutch = true)
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.toHome("Overslaan", "Gewicht vandaag")
            rule.shot("nl_02_home")
            rule.tap("Workouts")
            rule.waitForText("Programma's")
            rule.shot("nl_03_workouts")
        }
        AppLanguage.set(context, AppLanguage.ENGLISH)
    }
}
