package nl.ibs.gym8020

import android.content.Context
import android.os.Bundle
import nl.ibs.gym8020.data.AppLanguage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import nl.ibs.gym8020.ui.body.BodyweightScreen
import nl.ibs.gym8020.ui.dashboard.DashboardScreen
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.deco.DecoBackground
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.DecoPattern
import nl.ibs.gym8020.ui.deco.IbsMark
import nl.ibs.gym8020.ui.deco.Lattice
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.exercises.ExerciseEditScreen
import nl.ibs.gym8020.ui.exercises.ExerciseListScreen
import nl.ibs.gym8020.ui.home.HomeScreen
import nl.ibs.gym8020.ui.intro.MethodScreen
import nl.ibs.gym8020.ui.log.LogScreen
import nl.ibs.gym8020.ui.log.SessionEditScreen
import nl.ibs.gym8020.ui.intro.OnboardingScreen
import nl.ibs.gym8020.ui.schemas.SchemaEditScreen
import nl.ibs.gym8020.ui.schemas.SchemaListScreen
import nl.ibs.gym8020.ui.schemas.WeekPlanEditScreen
import nl.ibs.gym8020.ui.settings.MaterialScreen
import nl.ibs.gym8020.ui.settings.SettingsScreen
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.GymTheme
import nl.ibs.gym8020.ui.training.SummaryScreen
import nl.ibs.gym8020.ui.training.TrainingScreen

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GymApp).container
        setContent {
            GymTheme {
                var splash by rememberSaveable { mutableStateOf(savedInstanceState == null) }
                Box(Modifier.fillMaxSize().background(Gym.Bg)) {
                    GymNavHost(container)
                    AnimatedVisibility(visible = splash, exit = fadeOut()) { Splash() }
                }
                LaunchedEffect(Unit) {
                    delay(1100)
                    splash = false
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val SCHEMAS = "schemas"
    const val EXERCISES = "exercises"
    const val BODY = "body"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SCHEMA = "schema/{id}"
    const val WEEK_PLAN = "weekplan/{id}"
    const val EXERCISE = "exercise/{id}"
    const val TRAINING = "training/{id}"
    const val SUMMARY = "summary/{id}"
    const val MATERIAL = "material"
    const val METHOD = "method"
    const val INTRO = "intro"
    const val LOG = "log"
    const val SESSION = "session/{id}"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Tab(Routes.SCHEMAS, R.string.nav_schemas, Icons.AutoMirrored.Filled.ViewList),
    Tab(Routes.EXERCISES, R.string.nav_exercises, Icons.Filled.FitnessCenter),
    Tab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Insights),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
private fun GymNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }

    // Introduction for new users (G-2); existing users with trainings skip it silently.
    LaunchedEffect(Unit) {
        val settings = container.settings.current()
        if (!settings.onboarded) {
            nav.currentBackStackEntryFlow.first() // wait until the graph is set
            if (container.db.sessions().all().isEmpty()) {
                // Navigation must run on the main thread; after a database call we may be elsewhere.
                withContext(Dispatchers.Main) { nav.navigate(Routes.INTRO) }
            } else {
                container.settings.update { it.copy(onboarded = true) }
            }
        }
    }

    Scaffold(
        containerColor = Gym.Bg,
        // Insets are handled per screen (training and summary run edge to edge).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Gym.Surface) {
                    // One size for all labels: shrinks a little when the longest does not fit.
                    var labelSize by remember { mutableStateOf(12.sp) }
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { TabLabel(stringResource(tab.label), labelSize) { labelSize = it } },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Gym.OnYellow,
                                indicatorColor = Gym.Yellow,
                                selectedTextColor = Gym.Yellow,
                                unselectedIconColor = Gym.Muted,
                                unselectedTextColor = Gym.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val idArg = listOf(navArgument("id") { type = NavType.LongType })
        // Each area has its own art deco pattern: lattice at home, fan scales while training, arches elsewhere.
        val pattern = when {
            route == Routes.HOME -> DecoPattern.LATTICE
            route == Routes.TRAINING || route == Routes.SUMMARY || route == Routes.INTRO -> DecoPattern.FANS
            else -> DecoPattern.ARCHES
        }
        Box(Modifier.fillMaxSize()) {
        DecoBackground(pattern, Modifier.fillMaxSize())
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).let { if (showBar) it.statusBarsPadding() else it },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container,
                    onOpenTraining = { id -> nav.navigate("training/$id") },
                    onAllSchemas = { nav.switchTab(Routes.SCHEMAS) },
                )
            }
            composable(Routes.SCHEMAS) {
                SchemaListScreen(container, onOpenSchema = { nav.navigate("schema/$it") }, onOpenWeekPlan = { nav.navigate("weekplan/$it") })
            }
            composable(Routes.EXERCISES) { ExerciseListScreen(container, onOpen = { nav.navigate("exercise/$it") }) }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    container,
                    onOpenBodyweight = { nav.navigate(Routes.BODY) },
                    onOpenLog = { nav.navigate(Routes.LOG) },
                    onOpenSession = { nav.navigate("session/$it") },
                )
            }
            composable(Routes.LOG) { Box(Modifier.systemBarsPadding()) { LogScreen(container, onOpen = { nav.navigate("session/$it") }) } }
            composable(Routes.SESSION, idArg) { e ->
                Box(Modifier.systemBarsPadding().imePadding()) {
                    SessionEditScreen(
                        container, e.arguments?.getLong("id") ?: 0L,
                        onDone = { if (nav.currentDestination?.route == Routes.SESSION) nav.popBackStack() },
                        onDeleted = {
                            // The summary of a deleted training has nothing left to show.
                            nav.popBackStack()
                            if (nav.currentDestination?.route == Routes.SUMMARY) nav.popBackStack()
                        },
                    )
                }
            }
            composable(Routes.BODY) { Box(Modifier.systemBarsPadding().imePadding()) { BodyweightScreen(container) } }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container,
                    onOpenMaterial = { nav.navigate(Routes.MATERIAL) },
                    onOpenMethod = { nav.navigate(Routes.METHOD) },
                    onOpenIntro = { nav.navigate(Routes.INTRO) },
                )
            }
            composable(Routes.METHOD) { Box(Modifier.systemBarsPadding()) { MethodScreen(onBack = { if (nav.currentDestination?.route == Routes.METHOD) nav.popBackStack() }) } }
            composable(Routes.INTRO) {
                Box(Modifier.systemBarsPadding()) {
                    OnboardingScreen(container, onOpenMethod = { nav.navigate(Routes.METHOD) }, onDone = { if (nav.currentDestination?.route == Routes.INTRO) nav.popBackStack() })
                }
            }
            composable(Routes.MATERIAL) { Box(Modifier.systemBarsPadding().imePadding()) { MaterialScreen(container) } }
            composable(Routes.SCHEMA, idArg) { e ->
                Box(Modifier.systemBarsPadding().imePadding()) {
                    SchemaEditScreen(
                        container, e.arguments?.getLong("id") ?: 0L,
                        onDone = { nav.popBackStack() },
                        onOpenSchema = { id -> nav.popBackStack(); nav.navigate("schema/$id") },
                    )
                }
            }
            composable(Routes.WEEK_PLAN, idArg) { e ->
                Box(Modifier.systemBarsPadding().imePadding()) { WeekPlanEditScreen(container, e.arguments?.getLong("id") ?: 0L, onDone = { if (nav.currentDestination?.route == Routes.INTRO) nav.popBackStack() }) }
            }
            composable(Routes.EXERCISE, idArg) { e ->
                Box(Modifier.systemBarsPadding().imePadding()) { ExerciseEditScreen(container, e.arguments?.getLong("id") ?: 0L, onDone = { if (nav.currentDestination?.route == Routes.INTRO) nav.popBackStack() }) }
            }
            composable(Routes.TRAINING, idArg) { e ->
                val id = e.arguments?.getLong("id") ?: 0L
                TrainingScreen(
                    container, id,
                    onFinished = { sid -> nav.navigate("summary/$sid") { popUpTo(Routes.HOME) } },
                    onExit = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(Routes.SUMMARY, idArg) { e ->
                val id = e.arguments?.getLong("id") ?: 0L
                SummaryScreen(container, id, onDone = { nav.popBackStack(Routes.HOME, inclusive = false) }, onEdit = { nav.navigate("session/$id") })
            }
        }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Tab label; asks for a smaller [size] when it does not fit (five tabs, long Dutch words). */
@Composable
private fun TabLabel(text: String, size: TextUnit, onShrink: (TextUnit) -> Unit) {
    Text(
        text, maxLines = 1, softWrap = false,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = size, letterSpacing = 0.01.em),
        onTextLayout = { if (it.hasVisualOverflow && size > 8.sp) onShrink(size * 0.9f) },
    )
}

/** Test tag of the opening screen, so UI tests can wait until it is gone. */
const val SPLASH_TAG = "splash"

/** Opening screen with the IBS mark (D-51, D-52). */
@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(Gym.Bg).testTag(SPLASH_TAG), contentAlignment = Alignment.Center) {
        Lattice(Modifier.fillMaxSize(), alpha = .07f)
        Sunburst(Modifier.size(360.dp).padding(bottom = 180.dp), arcs = true)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            IbsMark(Modifier.size(96.dp))
            Text("GYM · 8020", style = MaterialTheme.typography.headlineLarge, color = Gym.Cream)
            DecoDivider(Modifier.fillMaxWidth(.8f))
            Eyebrow(stringResource(R.string.about_ibs))
        }
    }
}
