package nl.ibs.gym8020.ui.intro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.manualMeasurement
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.AppEmblem
import nl.ibs.gym8020.ui.theme.Gym

private const val PAGES = 4
private const val OLYMPIC_BAR = "olympic_bar"
private val PLATE_SIZES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

/**
 * Introduction for new users (G-2): the method, body weight (optional), bar and plates, and a week plan.
 * Everything can be changed later in the settings; [onDone] also marks the introduction as finished.
 */
@Composable
fun OnboardingScreen(container: AppContainer, onOpenMethod: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val stock by container.gyms.stock.collectAsStateWithLifecycle(settings.plateStock)
    val plans by container.db.weekPlans().observeAll().collectAsStateWithLifecycle(emptyList())
    val materials by container.db.materials().observeActive().collectAsStateWithLifecycle(emptyList())
    var page by rememberSaveable { mutableStateOf(0) }
    var bodyweight by rememberSaveable { mutableStateOf<Double?>(null) }
    val bar = materials.firstOrNull { it.libraryKey == OLYMPIC_BAR }

    fun finish() = scope.launch {
        bodyweight?.takeIf { it > 0 }?.let { container.db.bodyweights().replaceManualOfDay(manualMeasurement(it)) }
        container.settings.update { it.copy(onboarded = true) }
        withContext(Dispatchers.Main) { onDone() } // navigation on the main thread

    }

    BackHandler(enabled = page > 0) { page-- }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PageDots(page, Modifier.weight(1f))
            if (page < PAGES - 1) TextButton(onClick = { finish() }) { Text(stringResource(R.string.intro_skip), color = Gym.Muted) }
        }
        AnimatedContent(page, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "intro") { p ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (p) {
                    0 -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppEmblem(Modifier.size(104.dp)) }
                        Title(stringResource(R.string.intro_welcome_title))
                        Body(stringResource(R.string.intro_welcome_body))
                        MethodTable()
                        Body(stringResource(R.string.intro_welcome_plan))
                        TextButton(onClick = onOpenMethod, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(R.string.intro_read_more), color = Gym.Yellow)
                        }
                    }
                    1 -> {
                        Title(stringResource(R.string.intro_bw_title))
                        Body(stringResource(R.string.intro_bw_body))
                        Eyebrow(stringResource(R.string.settings_unit))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeightUnit.entries.forEach { u ->
                                FilterChip(settings.unit == u, { scope.launch { container.settings.update { it.copy(unit = u) } } }, label = { Text(u.name.lowercase()) })
                            }
                        }
                        NumberStepper(
                            value = bodyweight, onValueChange = { bodyweight = it }, step = 0.5, min = 0.0, startFrom = 75.0,
                            formatter = { WeightFormat.format(it, settings.unit) }, parser = settings.unit::toKg,
                        )
                        Hint(stringResource(R.string.intro_optional))
                    }
                    2 -> {
                        Title(stringResource(R.string.intro_plates_title))
                        Body(stringResource(R.string.intro_plates_body))
                        if (bar != null) {
                            Eyebrow(stringResource(R.string.intro_bar_weight))
                            NumberStepper(
                                value = bar.baseKg,
                                onValueChange = { v -> if (v != null && v >= 0) scope.launch { container.db.materials().update(bar.copy(baseKg = v)) } },
                                step = 2.5, min = 0.0, textSize = 24.sp, buttonSize = 48.dp,
                                formatter = { WeightFormat.format(it, settings.unit) }, parser = settings.unit::toKg,
                            )
                        }
                        Eyebrow(stringResource(R.string.intro_plate_sizes))
                        // The plates of your gym (A-4).
                        PlateChips(stock, settings.unit) { kg ->
                            scope.launch {
                                container.gyms.updateStock { current ->
                                    val map = current.plates.toMutableMap()
                                    if ((map[kg] ?: 0) > 0) map.remove(kg) else map[kg] = PlateStock.DEFAULT.plates[kg] ?: 2
                                    if (map.values.any { n -> n > 0 }) PlateStock(map) else current
                                }
                            }
                        }
                        Hint(stringResource(R.string.intro_plates_later))
                    }
                    else -> {
                        Title(stringResource(R.string.intro_week_title))
                        Body(stringResource(R.string.intro_week_body))
                        plans.forEach { plan ->
                            val selected = plan.active
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(if (selected) Gym.SurfaceHigh else Gym.Surface, RoundedCornerShape(16.dp))
                                    .border(2.dp, if (selected) Gym.Yellow else Gym.Surface, RoundedCornerShape(16.dp))
                                    .clickable { scope.launch { container.db.weekPlans().activate(plan.id) } }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(plan.name, style = MaterialTheme.typography.titleMedium, color = if (selected) Gym.Yellow else Gym.Cream)
                                Text(stringResource(R.string.intro_per_week, plan.targetPerWeek), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                            }
                        }
                        Hint(stringResource(R.string.intro_week_later))
                    }
                }
            }
        }
        DecoDivider(Modifier.fillMaxWidth().padding(vertical = 10.dp))
        if (page < PAGES - 1) {
            GoButton(stringResource(R.string.intro_next), onClick = { page++ })
        } else {
            GoButton(stringResource(R.string.intro_start), onClick = { finish() })
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlateChips(stock: PlateStock, unit: WeightUnit, onToggle: (Double) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PLATE_SIZES.forEach { kg ->
            FilterChip((stock.plates[kg] ?: 0) > 0, { onToggle(kg) }, label = { Text(WeightFormat.format(kg, unit) + " " + unit.name.lowercase()) })
        }
    }
}

@Composable
private fun PageDots(page: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(PAGES) { i ->
            Box(Modifier.size(if (i == page) 12.dp else 8.dp).background(if (i <= page) Gym.Yellow else Gym.SurfaceHigh, CircleShape).align(Alignment.CenterVertically))
        }
    }
}

@Composable
private fun Title(text: String) = Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Start)

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyLarge)

@Composable
private fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
