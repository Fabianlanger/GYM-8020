package nl.ibs.gym8020.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.R

/** Brand tokens, see docs/DESIGN.md. */
object Gym {
    val Bg = Color(0xFF171717)
    val Surface = Color(0xFF232323)
    val SurfaceHigh = Color(0xFF3A3A3A)
    val Yellow = Color(0xFFF9C852)
    val OnYellow = Color(0xFF171717)
    val Cream = Color(0xFFFEF9ED)
    val Muted = Color(0xFF9A958C)
    val Gold = Color(0xFFC9A24A)
    val GoldSoft = Color(0x73C9A24A)
    val Glow = Color(0xFF613F0F)
    val Positive = Color(0xFF7FB77E)
    val Negative = Color(0xFFD9725B)
}

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val colors = darkColorScheme(
    primary = Gym.Yellow,
    onPrimary = Gym.OnYellow,
    secondary = Gym.Gold,
    onSecondary = Gym.OnYellow,
    background = Gym.Bg,
    onBackground = Gym.Cream,
    surface = Gym.Bg,
    onSurface = Gym.Cream,
    surfaceVariant = Gym.Surface,
    onSurfaceVariant = Gym.Muted,
    surfaceContainer = Gym.Surface,
    surfaceContainerHigh = Gym.SurfaceHigh,
    surfaceContainerHighest = Gym.SurfaceHigh,
    surfaceContainerLow = Gym.Surface,
    outline = Gym.Muted,
    outlineVariant = Gym.SurfaceHigh,
    error = Gym.Negative,
)

private fun style(size: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) =
    TextStyle(fontFamily = Poppins, fontSize = size.sp, fontWeight = weight, letterSpacing = spacing.em)

private val typography = Typography(
    displayLarge = style(96, FontWeight.SemiBold),
    displayMedium = style(72, FontWeight.SemiBold),
    displaySmall = style(44, FontWeight.SemiBold),
    headlineLarge = style(32, FontWeight.Light),
    headlineMedium = style(28, FontWeight.Light),
    headlineSmall = style(24, FontWeight.Light),
    titleLarge = style(20, FontWeight.Normal),
    titleMedium = style(16, FontWeight.Medium),
    titleSmall = style(14, FontWeight.Medium),
    bodyLarge = style(16),
    bodyMedium = style(14),
    bodySmall = style(12),
    labelLarge = style(14, FontWeight.Medium),
    labelMedium = style(12, FontWeight.SemiBold, 0.15),
    labelSmall = style(11, FontWeight.SemiBold, 0.15),
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
