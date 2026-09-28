package nl.ibs.gym8020

import android.os.Bundle
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
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import nl.ibs.gym8020.ui.body.BodyweightScreen
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.IbsMark
import nl.ibs.gym8020.ui.deco.Lattice
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.exercises.ExerciseEditScreen
import nl.ibs.gym8020.ui.exercises.ExerciseListScreen
import nl.ibs.gym8020.ui.home.HomeScreen
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
    const val SETTINGS = "settings"
    const val SCHEMA = "schema/{id}"
    const val WEEK_PLAN = "weekplan/{id}"
    const val EXERCISE = "exercise/{id}"
    const val TRAINING = "training/{id}"
    const val SUMMARY = "summary/{id}"
    const val MATERIAL = "material"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Tab(Routes.SCHEMAS, R.string.nav_schemas, Icons.Filled.ViewList),
    Tab(Routes.EXERCISES, R.string.nav_exercises, Icons.Filled.FitnessCenter),
    Tab(Routes.BODY, R.string.nav_body, Icons.Filled.MonitorWeight),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
private fun GymNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }

    Scaffold(
        containerColor = Gym.Bg,
        // Insets are handled per screen (training and summary run edge to edge).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Gym.Surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label), maxLines = 1) },
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
            composable(Routes.BODY) { BodyweightScreen(container) }
            composable(Routes.SETTINGS) { SettingsScreen(container, onOpenMaterial = { nav.navigate(Routes.MATERIAL) }) }
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
                Box(Modifier.systemBarsPadding().imePadding()) { WeekPlanEditScreen(container, e.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() }) }
            }
            composable(Routes.EXERCISE, idArg) { e ->
                Box(Modifier.systemBarsPadding().imePadding()) { ExerciseEditScreen(container, e.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() }) }
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
                SummaryScreen(container, e.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack(Routes.HOME, inclusive = false) })
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

/** Opening screen with the IBS mark (D-51, D-52). */
@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(Gym.Bg), contentAlignment = Alignment.Center) {
        Lattice(Modifier.fillMaxSize(), alpha = .07f)
        Sunburst(Modifier.size(360.dp).padding(bottom = 180.dp), arcs = true)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            IbsMark(Modifier.size(96.dp))
            Text("GYM · 8020", style = MaterialTheme.typography.headlineLarge)
            DecoDivider(Modifier.fillMaxWidth(.8f))
            Eyebrow(stringResource(R.string.about_ibs))
        }
    }
}
