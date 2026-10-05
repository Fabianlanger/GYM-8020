package nl.ibs.gym8020.ui

import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.ibs.gym8020.MainActivity
import nl.ibs.gym8020.ui.theme.GymTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A plain window inside the app's own activity, to see whether the activity or the screens keep Compose busy. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DialogProbeTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    @Test
    fun `plain window in the app activity`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent { AlertDialog(onDismissRequest = {}, confirmButton = { TextButton({}) { Text("Save") } }) } }
            rule.onNodeWithText("Save").assertExists()
        }
    }

    @Test
    fun `themed window in the app activity`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent { GymTheme { AlertDialog(onDismissRequest = {}, confirmButton = { TextButton({}) { Text("Save") } }) } } }
            rule.onNodeWithText("Save").assertExists()
        }
    }
}
