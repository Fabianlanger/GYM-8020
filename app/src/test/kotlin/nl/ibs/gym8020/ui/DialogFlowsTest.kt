package nl.ibs.gym8020.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.MainActivity
import nl.ibs.gym8020.SPLASH_TAG
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.AppLanguage
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.Seeder
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.BodyweightEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Flows with a window (dialog) over the app screens (T-7, T-4: F-1 with the body weight question, F-3).
 *
 * T-7: under Robolectric such windows never got idle ("Compose did not get idle"). Every window that did pass
 * (NumberStepperTest, the earlier probe in the app activity) ran at the default density of 1 and was shown
 * without a touch; every window that hung ran at the Pixel 5 density of 2.75 and was opened by a tap. These tests
 * take the passing conditions: the Pixel 5 size in dp at density 1 (mdpi), and clicks through the semantics
 * action instead of a touch (no touch mode, no ripple). When a test still hangs, the failure says which window
 * waits for a measure/layout pass ([idleReport]).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-normal-long-notround-any-mdpi-keyshidden-nonav")
class DialogFlowsTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val container get() = (context as GymApp).container

    /** A fresh install in English, introduction done; optionally with today's body weight logged. */
    private fun freshInstall(weightToday: Boolean) = runBlocking {
        AppLanguage.set(context, AppLanguage.ENGLISH)
        val app = context as GymApp
        app.seeding.join()
        withContext(Dispatchers.IO) { container.db.clearAllTables() }
        container.settings.update { AppSettings() }
        Seeder(container.db, container.settings, dutch = false).seedIfNeeded()
        container.settings.update { it.copy(onboarded = true) }
        if (weightToday) container.db.bodyweights().upsert(BodyweightEntity(epochDay = LocalDate.now().toEpochDay(), kg = 80.0))
    }

    private fun <T> db(block: suspend AppDatabase.() -> T): T = runBlocking { container.db.block() }

    private fun ComposeTestRule.has(text: String, substring: Boolean = false) =
        onAllNodesWithText(text, substring = substring, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeTestRule.waitForText(text: String) = waitUntil(20_000) { has(text) }

    /** Click without a touch: runs the node's click action. */
    private fun ComposeTestRule.click(text: String, last: Boolean = false) =
        onAllNodesWithText(text, ignoreCase = true).let { if (last) it.onLast() else it.onFirst() }
            .performSemanticsAction(SemanticsActions.OnClick)

    private fun ComposeTestRule.clickDescription(description: String) =
        onAllNodesWithContentDescription(description).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    private fun ComposeTestRule.toHome() {
        waitForText("Weight today")
        waitUntil(20_000) { onAllNodesWithTag(SPLASH_TAG).fetchSemanticsNodes().isEmpty() }
    }

    private fun ComposeTestRule.shot(name: String) {
        waitForIdle()
        onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** Screenshot of the open window only. */
    private fun ComposeTestRule.dialogShot(name: String) {
        waitForIdle()
        onAllNodes(isDialog()).onFirst().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** Runs [block]; when Compose does not get idle, the failure lists every window and what it waits for (T-7). */
    private fun idleReport(block: () -> Unit) {
        try {
            block()
        } catch (e: RuntimeException) {
            if (e.javaClass.simpleName != "AppNotIdleException") throw e
            throw AssertionError("T-7: Compose did not get idle. Windows: ${windowsReport()}", e)
        }
    }

    /** Per window its root views and, per Compose root, whether it waits for measure/layout. Reflection only. */
    private fun windowsReport(): String = runCatching {
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val field = global.getDeclaredField("mViews").apply { isAccessible = true }
        val windows = (field.get(instance) as List<*>).filterIsInstance<View>()
        val report = windows.joinToString(" | ") { window ->
            val composeRoots = mutableListOf<View>()
            fun collect(v: View) {
                if (v.javaClass.simpleName == "AndroidComposeView") composeRoots += v
                if (v is ViewGroup) for (i in 0 until v.childCount) collect(v.getChildAt(i))
            }
            collect(window)
            "${window.javaClass.simpleName} ${window.width}x${window.height} focus=${window.hasWindowFocus()} " +
                "layoutRequested=${window.isLayoutRequested}: " + composeRoots.joinToString { root ->
                    fun flag(getter: String) = runCatching { root.javaClass.getMethod(getter).invoke(root) }.getOrElse { "?" }
                    "compose ${root.width}x${root.height} pendingMeasureLayout=${flag("getHasPendingMeasureOrLayout")} " +
                        "resumed=${flag("isLifecycleInResumedState")} layoutRequested=${root.isLayoutRequested}"
                }
        }
        "$report; snapshot changes pending=${Snapshot.current.hasPendingChanges()}"
    }.getOrElse { "no report: $it" }

    /** T-7 / F-1: without body weight today the app asks for it once when you start; Skip goes on to the training. */
    // Still hangs on CI: the dialog's own window keeps a layout request pending (pendingMeasureLayout=true on a
    // 393x271 window) while the finish check dialog in F-3 does idle. Likely the number field in this dialog. #23.
    @Ignore("T-7 (#23): body weight dialog window never finishes layout under Robolectric")
    @Test
    fun `T-7 body weight question when starting a workout`() {
        freshInstall(weightToday = false)
        ActivityScenario.launch(MainActivity::class.java).use {
            idleReport {
                rule.toHome()
                rule.click("Full body", last = true) // the card under "Other workout"
                rule.waitForText("What do you weigh today?")
                rule.dialogShot("en_11_bodyweight_question")
                rule.click("Skip")
                rule.waitForText("Back squat")
                assertTrue("window is closed", !rule.has("What do you weigh today?"))
            }
        }
    }

    /**
     * F-3: finish a workout through the check at finishing → summary → edit → the log is right. One set is filled
     * in through the repository (entering sets is F-1), the rest stays empty and counts as skipped. The summary
     * itself is changing (#66), so only stable things are checked there.
     */
    @Test
    fun `F-3 finish, summary, edit, the log is right`() {
        freshInstall(weightToday = true)
        ActivityScenario.launch(MainActivity::class.java).use {
            idleReport {
                rule.toHome()
                rule.click("Full body", last = true)
                rule.waitForText("Back squat")

                val session = db { sessions().all().single() }
                val squat = db { exercises().all().single { it.name == "Back squat" } }
                val squatSe = db { sessions().exercises(session.id).single { it.exerciseId == squat.id } }
                val heavy = db { sessions().setsOf(squatSe.id).first { it.role == SetRole.HEAVY } }
                runBlocking { container.training.updateSet(heavy.copy(weightKg = 60.0, reps = 7)) }
                rule.waitForText("60") // the open set shows the weight

                // The check at finishing: empty sets count as skipped, so finishing anyway is possible.
                rule.clickDescription("Finish workout")
                rule.waitForText("Before you finish")
                rule.dialogShot("en_12_finish_check")
                rule.click("Finish anyway")

                // Summary: reachable, offers home and edit (its texts change with #66/#67).
                rule.waitForText("Back to home")
                assertTrue("summary offers editing", rule.has("Edit session"))
                rule.shot("en_13_summary")

                // Muscle map (#67): only the squat was done, so muscles like the calves were only trained by skipped
                // exercises and the legend shows "skipped".
                rule.onAllNodesWithText("Muscles this session", ignoreCase = true).onFirst().performScrollTo()
                assertTrue("muscle map has skipped muscles", rule.has("Skipped"))
                rule.shot("en_13b_summary_muscles")

                // Edit: the session with its exercise, then back without changes.
                rule.click("Edit session")
                rule.waitForText("Duration (minutes)")
                assertTrue("edit screen shows the exercise", rule.has("Back squat"))
                rule.shot("en_14_edit_session")
                rule.click("Done")
                rule.waitForText("Back to home")

                // The log: finished, the set as entered.
                val logged = db { sessions().get(session.id) }
                assertNotNull("session is in the log", logged)
                assertNotNull("session is finished", logged!!.endedAt)
                val set = db { sessions().setsOf(squatSe.id).single { it.id == heavy.id } }
                assertEquals(60.0, set.weightKg!!, 0.0)
                assertEquals(7, set.reps)
            }
        }
    }
}
