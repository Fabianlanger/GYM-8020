package nl.ibs.gym8020.ui

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.MainActivity
import nl.ibs.gym8020.SPLASH_TAG
import nl.ibs.gym8020.data.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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

    private fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = 20_000) =
        waitUntil(timeoutMs) { onAllNodesWithText(text, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeTestRule.tap(text: String) = onAllNodesWithText(text, ignoreCase = true).onFirst().performClick()

    private fun ComposeTestRule.has(text: String) = onAllNodesWithText(text, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()

    /**
     * The settings store lives for the whole test process, so only the first test sees a fresh install.
     * Later tests skip the introduction if it shows, then wait for the home screen.
     */
    private fun ComposeTestRule.toHome(skip: String, home: String) {
        waitUntil(20_000) { has(skip) || has(home) }
        if (has(skip) && !has(home)) tap(skip)
        waitForText(home)
        waitForSplash()
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
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForText("Skip")
            rule.shot("en_01_intro")
            rule.tap("Skip")
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

    /** F-1 start: a workout from the home screen opens the training screen; the weight card opens its window. */
    @Test
    fun `F-1 start a workout and log body weight`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.toHome("Skip", "Weight today")
            // Under Robolectric the dialog window never reports idle, so step the clock by hand while it is open.
            rule.mainClock.autoAdvance = false
            rule.tap("Weight today")
            rule.mainClock.advanceTimeBy(1_000)
            rule.waitForText("Save")
            rule.shot("en_07_weight_window")
            rule.tap("Cancel")
            rule.mainClock.advanceTimeBy(1_000)
            rule.mainClock.autoAdvance = true

            rule.waitForText("Full body")
            rule.tap("Full body")
            // No body weight yet: the app asks once; skip it.
            rule.waitForText("Skip")
            rule.tap("Skip")
            rule.waitForText("Back squat")
            rule.shot("en_08_training")
        }
    }

    /** V-8: Dutch is selectable; the same screens in Dutch. */
    @Test
    fun `V-8 Dutch when chosen`() {
        AppLanguage.set(context, AppLanguage.DUTCH)
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
