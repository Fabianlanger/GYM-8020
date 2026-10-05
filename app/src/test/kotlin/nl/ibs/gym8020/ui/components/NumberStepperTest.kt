package nl.ibs.gym8020.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The number field (V-11) on its own and inside a window, to pin down where Compose stays busy. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NumberStepperTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `field on its own`() {
        rule.setContent { NumberStepper(value = 80.0, onValueChange = {}, step = 0.5, suffix = "kg") }
        rule.onNodeWithText("80").assertExists()
    }

    @Test
    fun `window without a field`() {
        rule.setContent { AlertDialog(onDismissRequest = {}, confirmButton = { TextButton({}) { Text("Save") } }, text = { Text("Body") }) }
        rule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `field in a window`() {
        rule.setContent {
            AlertDialog(
                onDismissRequest = {}, confirmButton = { TextButton({}) { Text("Save") } },
                text = { NumberStepper(value = null, onValueChange = {}, step = 0.1, startFrom = 75.0, suffix = "kg", textSize = 26.sp) },
            )
        }
        rule.onNodeWithText("Save").assertExists()
    }
}
