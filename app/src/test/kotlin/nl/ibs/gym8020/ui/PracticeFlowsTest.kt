package nl.ibs.gym8020.ui

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
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
import nl.ibs.gym8020.ui.tour.PRACTICE_BANNER_TAG
import nl.ibs.gym8020.ui.tour.TOUR_BALLOON_TAG
import nl.ibs.gym8020.ui.tour.TOUR_NEXT_TAG
import nl.ibs.gym8020.ui.tour.TOUR_OFFER_START_TAG
import nl.ibs.gym8020.ui.tour.TOUR_OFFER_TAG
import nl.ibs.gym8020.ui.tour.TOUR_SKIP_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * F-11 (TESTING.md, #97): the practice workout with the tour. Offered once after the introduction, the tour finds each
 * step, the practice leaves nothing behind in your own data, and it can be started again from the settings. Clicks run
 * the nodes' click actions (no windows/dialogs are opened, T-7). Example data only (D-06a).
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

    /** Waits until the tour's balloon shows [title] (the step it lights up). */
    private fun ComposeTestRule.waitForBalloon(title: String) = waitUntil(20_000) { hasTag(TOUR_BALLOON_TAG) && has(title) }

    private fun ComposeTestRule.noBalloon() = !hasTag(TOUR_BALLOON_TAG)

    private fun ComposeTestRule.waitForSplash() = waitUntil(20_000) { onAllNodesWithTag(SPLASH_TAG).fetchSemanticsNodes().isEmpty() }

    private fun ComposeTestRule.shot(name: String) {
        waitForIdle()
        onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** On the page on screen: the pager builds pages beside it too. */
    private val visible = SemanticsMatcher("on the page on screen") {
        it.positionInRoot.x >= 0f && it.positionInRoot.x < context.resources.displayMetrics.widthPixels
    }

    private fun passTime(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

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
    fun `F-11 offered after the introduction, the tour finds every step, nothing is saved`() {
        freshInstall()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            rule.waitForText("Skip")
            rule.waitForSplash()
            rule.click("Skip")

            // Once, after the first introduction: the offer.
            rule.waitForTag(TOUR_OFFER_TAG)
            rule.shot("en_20_practice_offer")
            assertTrue("offered", runBlocking { container.settings.current().tourOffered })
            rule.clickTag(TOUR_OFFER_START_TAG)
            rule.waitForTag(PRACTICE_BANNER_TAG)

            // Before GO: the target, stacking, the warm-up and GO, each with its balloon.
            rule.waitForBalloon("Your target for today.")
            rule.shot("en_21_tour_goal")
            rule.clickTag(TOUR_NEXT_TAG)
            rule.waitForBalloon("Between sets you only take plates off.")
            rule.shot("en_22_tour_stacking")
            rule.clickTag(TOUR_NEXT_TAG)
            rule.waitForBalloon("Warm-up (optional).")
            rule.clickTag(TOUR_NEXT_TAG)
            rule.waitForBalloon("GO starts the countdown and the timer.")
            rule.clickTag(TOUR_NEXT_TAG)

            // The reps step waits for GO: no balloon until then.
            rule.waitUntil(20_000) { rule.noBalloon() }
            rule.onAllNodesWithText("Go", ignoreCase = true).filter(visible).onFirst().performSemanticsAction(SemanticsActions.OnClick)
            passTime(3) // the short countdown
            rule.waitForBalloon("Enter your reps.")
            rule.shot("en_23_tour_reps")
            rule.clickTag(TOUR_NEXT_TAG)

            // Heavy set: the weight is there already; + on the reps takes the rep target.
            rule.waitUntil(20_000) { rule.noBalloon() }
            rule.onAllNodesWithContentDescription("More").filter(visible)[1].performSemanticsAction(SemanticsActions.OnClick)
            rule.click("Done")

            // The next target appears; its balloon comes once it is in view.
            rule.waitUntil(20_000) { rule.has("Next target") }
            rule.onAllNodesWithText("Next target", ignoreCase = true).onFirst().performScrollTo()
            rule.waitForBalloon("You set your next target.")
            rule.shot("en_24_tour_next_target")
            rule.clickTag(TOUR_NEXT_TAG)
            rule.waitUntil(20_000) { rule.noBalloon() }

            // The practice lives in its own database in memory.
            val practiceSets = runBlocking { container.practice.db.sessions().allSets() }
            assertTrue("the practice has your reps", practiceSets.any { it.reps != null })
            assertNothingSaved()

            // Leaving: back to home, still nothing saved.
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.waitForText("Weight today")
            assertNothingSaved()
        }
    }

    @Test
    fun `F-11 offered once, again from the settings, skip ends the tour`() {
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

            // Started again from the settings; skip ends the tour at once.
            rule.click("Practice workout with tour")
            rule.waitForTag(PRACTICE_BANNER_TAG)
            rule.waitForBalloon("Your target for today.")
            rule.clickTag(TOUR_SKIP_TAG)
            rule.waitUntil(20_000) { rule.noBalloon() }
            rule.waitForIdle()
            assertTrue("no balloon after skip", rule.noBalloon())
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
