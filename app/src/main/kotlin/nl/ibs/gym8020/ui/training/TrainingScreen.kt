package nl.ibs.gym8020.ui.training

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.LoadStep
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.Signal
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.data.movementOrDefault
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.components.WeightText
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.MovementIcon
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.exercises.ExercisePickerDialog
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun roleLabel(role: SetRole): String = when (role) {
    SetRole.HEAVY -> stringResource(R.string.role_heavy)
    SetRole.LIGHT -> stringResource(R.string.role_light)
    SetRole.WARMUP -> stringResource(R.string.role_warmup)
}

private fun kgText(kg: Double, unit: WeightUnit, signed: Boolean = false) = WeightFormat.format(kg, unit, signed) + " " + unit.name.lowercase()

/** Warm-up weight from the material (D-44): empty bar 20 kg, lightest pin, lightest dumbbell, bodyweight only. */
@Composable
fun warmupHint(ex: ExerciseEntity, calc: WeightCalculator?, unit: WeightUnit): String {
    if (ex.loadType == LoadType.BODYWEIGHT) return stringResource(R.string.tr_warmup_hint_bodyweight)
    if (calc == null) {
        return stringResource(if (ex.equipment == Equipment.BARBELL) R.string.tr_warmup_hint_barbell else R.string.tr_warmup_hint_light)
    }
    val p = calc.profile
    return when {
        p.kind == MaterialKind.BODYWEIGHT -> stringResource(R.string.tr_warmup_hint_bodyweight)
        p.kind == MaterialKind.PLATES && p.baseKg > 0 -> stringResource(R.string.tr_warmup_hint_barbell) + " " + kgText(p.baseKg, unit)
        else -> stringResource(R.string.tr_warmup_hint_light) + " " + kgText(calc.warmupKg, unit)
    }
}

@Composable
fun TrainingScreen(container: AppContainer, sessionId: Long, onFinished: (Long) -> Unit, onExit: () -> Unit) {
    val vm: TrainingViewModel = viewModel(key = "training-$sessionId") { TrainingViewModel(container, sessionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session = state.session
    val items = state.items
    val pager = rememberPagerState { items.size }
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmFinish by remember { mutableStateOf(false) }
    var countdownFor by remember { mutableStateOf<Long?>(null) }

    // Keep the screen on during a training.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    // Timestamps: shown on arrival, done when swiping away.
    LaunchedEffect(items.isNotEmpty()) {
        if (items.isEmpty()) return@LaunchedEffect
        var previous: Long? = null
        snapshotFlow { pager.currentPage }.collect { page ->
            val current = vm.state.value.items.getOrNull(page)?.se?.id
            previous?.let { if (it != current) vm.done(it) }
            current?.let { vm.shown(it) }
            previous = current
        }
    }
    BackHandler { onExit() }

    if (state.loaded && session == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }

    Box(Modifier.fillMaxSize().background(Gym.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                Column(Modifier.weight(1f)) {
                    Text(session?.schemaName ?: "", style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.tr_total, formatDuration(now - (session?.startedAt ?: now))),
                        style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                    )
                }
                IconButton(onClick = { confirmFinish = true }) { Icon(Icons.Filled.Flag, stringResource(R.string.tr_finish), tint = Gym.Yellow) }
            }
            if (items.isNotEmpty()) {
                Box(Modifier.weight(1f)) {
                    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                        val item = items[page]
                        ExercisePage(
                            item = item,
                            index = page,
                            total = items.size,
                            unit = state.settings.unit,
                            step = state.settings.weightStep,
                            targetOf = { role -> state.settings.method.targetFor(role, item.exercise) },
                            goodMargin = state.settings.method.goodMargin,
                            now = now,
                            allExercises = state.allExercises,
                            vm = vm,
                            isLast = page == items.lastIndex,
                            onGo = { countdownFor = item.se.id },
                            onFinish = { confirmFinish = true },
                        )
                    }
                    // Art deco ribbons at the edges: pull them (or tap) to go to the next/previous exercise.
                    if (pager.currentPage < items.lastIndex) {
                        SwipeRibbon(
                            pointsRight = true,
                            description = stringResource(R.string.tr_next_exercise),
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        )
                    }
                    if (pager.currentPage > 0) {
                        SwipeRibbon(
                            pointsRight = false,
                            description = stringResource(R.string.tr_previous_exercise),
                            modifier = Modifier.align(Alignment.CenterStart),
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        )
                    }
                }
            }
        }
        countdownFor?.let { seId ->
            CountdownOverlay(onDone = { vm.go(seId); countdownFor = null })
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.tr_confirm_finish)) },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; vm.finish { onFinished(sessionId) } }) { Text(stringResource(R.string.tr_finish)) }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/**
 * A slim vertical art deco ribbon at the screen edge that gently slides in and out,
 * suggesting you can pull the page. The next ribbon is bright, the previous one subtle.
 */
@Composable
private fun SwipeRibbon(pointsRight: Boolean, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ribbon")
    val shift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "shift",
    )
    val alpha = if (pointsRight) 1f else .45f
    Canvas(
        modifier
            .offset(x = ((if (pointsRight) -1 else 1) * 6 * shift).dp)
            .width(22.dp).height(150.dp)
            .alpha(alpha)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
    ) {
        scale(if (pointsRight) 1f else -1f, 1f) {
            val w = size.width
            val h = size.height
            val sw = 1.2.dp.toPx()
            val gold = Gym.Gold
            // Double vertical lines hugging the edge, ending in diagonal kinks.
            val outer = w - 3.dp.toPx()
            val inner = w - 8.dp.toPx()
            drawLine(gold, Offset(outer, 0f), Offset(outer, h), sw)
            drawLine(gold, Offset(inner, h * .12f), Offset(inner, h * .88f), sw)
            drawLine(gold, Offset(inner, h * .12f), Offset(outer, 0f), sw)
            drawLine(gold, Offset(inner, h * .88f), Offset(outer, h), sw)
            // Chevrons pointing into the screen, with a diamond in the middle.
            val cy = h / 2
            listOf(-26f, 26f).forEach { d ->
                val y = cy + d.dp.toPx()
                val p = Path().apply {
                    moveTo(inner - 2.dp.toPx(), y - 6.dp.toPx()); lineTo(inner - 9.dp.toPx(), y); lineTo(inner - 2.dp.toPx(), y + 6.dp.toPx())
                }
                drawPath(p, gold, style = Stroke(sw))
            }
            val dm = Path().apply {
                moveTo(inner - 5.dp.toPx(), cy - 8.dp.toPx()); lineTo(inner + 2.dp.toPx(), cy)
                lineTo(inner - 5.dp.toPx(), cy + 8.dp.toPx()); lineTo(inner - 12.dp.toPx(), cy); close()
            }
            drawPath(dm, Gym.Yellow)
            // A small curl (the floral touch).
            val curl = Path().apply {
                moveTo(inner, cy - 40.dp.toPx())
                cubicTo(inner - 12.dp.toPx(), cy - 44.dp.toPx(), inner - 12.dp.toPx(), cy - 32.dp.toPx(), inner - 5.dp.toPx(), cy - 33.dp.toPx())
                moveTo(inner, cy + 40.dp.toPx())
                cubicTo(inner - 12.dp.toPx(), cy + 44.dp.toPx(), inner - 12.dp.toPx(), cy + 32.dp.toPx(), inner - 5.dp.toPx(), cy + 33.dp.toPx())
            }
            drawPath(curl, gold, style = Stroke(sw))
        }
    }
}

@Composable
private fun ExercisePage(
    item: TrainingItem,
    index: Int,
    total: Int,
    unit: WeightUnit,
    step: Double,
    targetOf: (SetRole) -> Int?,
    goodMargin: Int,
    now: Long,
    allExercises: List<ExerciseEntity>,
    vm: TrainingViewModel,
    isLast: Boolean,
    onGo: () -> Unit,
    onFinish: () -> Unit,
) {
    val ex = item.exercise
    val se = item.se
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val running = se.goAt != null && se.firstInputAt == null
    val warmingUp = se.warmupAt != null && se.goAt == null && se.firstInputAt == null
    val beforeGo = se.goAt == null && se.firstInputAt == null
    val isReps = ex.progression == Progression.REPS
    val firstInput = { vm.firstInput(se.id) }
    // One set is open for editing at a time; the rest are compact lines.
    var openSet by remember(se.id) { mutableStateOf(item.sets.firstOrNull { it.reps == null }?.id) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 26.dp, top = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).background(Gym.Surface, RoundedCornerShape(14.dp)).padding(6.dp)) {
                MovementIcon(ex.movementOrDefault, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f)) {
                Eyebrow("${index + 1} / $total")
                Text(ex.name, style = MaterialTheme.typography.headlineSmall)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.tr_menu)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (se.skipped) R.string.tr_unskip else R.string.tr_skip)) },
                        onClick = { menu = false; vm.skip(se.id, !se.skipped) },
                    )
                    DropdownMenuItem(text = { Text(stringResource(R.string.tr_move_end)) }, onClick = { menu = false; vm.moveToEnd(se.id) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.tr_add_exercise)) }, onClick = { menu = false; picker = true })
                }
            }
        }

        if (se.skipped) Eyebrow(stringResource(R.string.tr_skipped), color = Gym.Negative)
        LastTimeLine(item, unit)

        when {
            running -> RunningTimer(now - (se.goAt ?: now))
            beforeGo -> GoalCard(item, unit, targetOf)
            else -> {}
        }

        if (beforeGo) {
            if (warmingUp) {
                Text(stringResource(R.string.tr_warmup_running, formatDuration(now - (se.warmupAt ?: now))), color = Gym.Gold, style = MaterialTheme.typography.bodyMedium)
            } else if (se.warmupAt == null) {
                LineButton(
                    stringResource(R.string.tr_warmup_button, warmupHint(ex, item.calc, unit)),
                    onClick = { vm.startWarmup(se.id) }, icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.fillMaxWidth(),
                )
            }
            GoButton(stringResource(R.string.tr_go), onClick = onGo)
        }

        DecoDivider()

        // Sets: filling in the first number stops the exercise timer (D-31). Warm-up reps don't.
        item.sets.forEach { set ->
            val target = targetOf(set.role)
            val prevReps = item.extra?.last?.sets?.filter { it.role == set.role }?.maxOfOrNull { it.reps ?: 0 }
            if (openSet == set.id) {
                SetEditor(
                    set = set, ex = ex, calc = item.calc, unit = unit, step = step, target = target, goodMargin = goodMargin,
                    previousReps = prevReps, isReps = isReps,
                    onChange = vm::updateSet,
                    onFirstInput = if (set.role.isWork) firstInput else ({}),
                    onToggleRole = { vm.toggleRole(set) }, onDelete = { vm.deleteSet(set.id) },
                    onDone = {
                        val i = item.sets.indexOfFirst { it.id == set.id }
                        openSet = item.sets.drop(i + 1).firstOrNull { it.reps == null }?.id
                    },
                )
            } else {
                SetLine(set, ex, unit, target, goodMargin, prevReps, isReps, onClick = { openSet = set.id })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LineButton(stringResource(R.string.tr_add_set), onClick = { vm.addSet(se.id, SetRole.HEAVY) }, icon = Icons.Filled.Add, modifier = Modifier.weight(1f))
            LineButton(stringResource(R.string.tr_add_warmup), onClick = { vm.addSet(se.id, SetRole.WARMUP) }, icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.weight(1f))
        }

        item.plan?.let { plan -> NextTimePlanner(item, plan, unit, step, targetOf, vm) }

        Spacer(Modifier.height(4.dp))
        if (isLast) {
            GoButton(stringResource(R.string.tr_finish), onClick = onFinish)
        } else {
            Text(stringResource(R.string.tr_swipe_hint), color = Gym.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (picker) {
        ExercisePickerDialog(allExercises, onPick = { vm.addExercise(it.id); picker = false }, onDismiss = { picker = false })
    }
}

@Composable
private fun LastTimeLine(item: TrainingItem, unit: WeightUnit) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val last = item.extra?.last
    if (last != null) {
        val summary = last.sets.filter { it.role.isWork && it.reps != null }.joinToString("  ·  ") { s ->
            val w = s.weightKg
            if (ex.progression == Progression.REPS && (w == null || w == 0.0)) "${s.reps}"
            else "${w?.let { WeightFormat.format(it, unit, signed) } ?: "—"} × ${s.reps}"
        }
        if (summary.isNotEmpty()) Text(stringResource(R.string.tr_last_time, summary), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
    }
    item.extra?.lastNote?.let { Text(stringResource(R.string.tr_note_last, it), color = Gym.Gold, style = MaterialTheme.typography.bodySmall) }
}

/** Human text for how to set up a weight with the material (D-23, D-26). */
@Composable
private fun loadText(weightKg: Double?, calc: WeightCalculator?, step: LoadStep?, isFirst: Boolean, unit: WeightUnit): String? {
    if (calc == null || weightKg == null) return null
    val p = calc.profile
    val w: Double = weightKg
    fun plates(l: List<Double>) = l.joinToString(" · ") { WeightFormat.format(it, unit) }
    if (calc.isAchievable(w).not()) return stringResource(R.string.ld_not_achievable, kgText(calc.snap(w), unit, p.kind == MaterialKind.BODYWEIGHT))
    return when (p.kind) {
        MaterialKind.PLATES -> when {
            step == null -> null
            isFirst -> {
                val base = if (p.baseKg > 0) "  ·  " + stringResource(R.string.ld_base, kgText(p.baseKg, unit)) else ""
                if (step.loading.plates.isEmpty()) stringResource(R.string.ld_no_plates) + base
                else stringResource(R.string.ld_per_side, plates(step.loading.plates)) + base
            }
            else -> listOfNotNull(
                step.remove.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.ld_remove, plates(it)) },
                step.add.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.ld_add, plates(it)) },
            ).joinToString("  ·  ").ifEmpty { null }
        }
        MaterialKind.STACK -> stringResource(R.string.ld_pin, kgText(w, unit))
        MaterialKind.DUMBBELLS -> stringResource(R.string.ld_dumbbells, kgText(w, unit))
        MaterialKind.BODYWEIGHT -> when {
            w < 0 -> stringResource(R.string.ld_assist, kgText(-w, unit))
            w == 0.0 -> stringResource(R.string.tr_warmup_hint_bodyweight)
            else -> step?.loading?.plates?.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.ld_belt, plates(it)) }
        }
    }
}

/** Goal before GO: all work sets in one card; big weight, small plate instructions. */
@Composable
private fun GoalCard(item: TrainingItem, unit: WeightUnit, targetOf: (SetRole) -> Int?) {
    val ex = item.exercise
    val work = item.sets.filter { it.role.isWork }
    if (work.isEmpty()) return
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val weights = work.map { it.goalWeightKg ?: it.weightKg }
    val plan = item.calc?.let { c -> weights.takeIf { l -> l.all { it != null && c.isAchievable(it) } }?.let { c.plan(it.filterNotNull()) } }
    DecoCard(corners = true, padding = 16.dp) {
        work.forEachIndexed { i, set ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            val target = targetOf(set.role)
            Eyebrow(roleLabel(set.role) + (target?.let { " · " + stringResource(R.string.tr_target, it) } ?: ""))
            if (ex.progression == Progression.REPS && (weights[i] ?: 0.0) == 0.0) {
                val prev = item.extra?.last?.sets?.filter { it.role == set.role }?.maxOfOrNull { it.reps ?: 0 }
                Text(
                    prev?.let { stringResource(R.string.tr_reps_progression, it) } ?: stringResource(R.string.tr_to_failure),
                    style = MaterialTheme.typography.titleLarge, color = Gym.Yellow,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    WeightText(weights[i], unit, size = if (i == 0) 48.sp else 36.sp, signed = signed)
                    val step = plan?.getOrNull(i)
                    if (step != null && item.calc?.profile?.kind == MaterialKind.PLATES && step.loading.plates.isNotEmpty()) {
                        PlateStrip(step.loading.plates, removed = plan?.getOrNull(i + 1)?.remove.orEmpty(), modifier = Modifier.height(40.dp))
                    }
                }
                loadText(weights[i], item.calc, plan?.getOrNull(i), i == 0, unit)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                }
            }
        }
    }
}

/** Plates on one side of the bar, inside → outside; plates to take off for the next set are outlined in yellow. */
@Composable
private fun PlateStrip(plates: List<Double>, removed: List<Double>, modifier: Modifier = Modifier) {
    val maxKg = 25.0
    Canvas(modifier.width((10 + plates.size * 10).dp)) {
        val h = size.height
        val pw = 7.dp.toPx()
        val gap = 3.dp.toPx()
        drawLine(Gym.Muted, Offset(0f, h / 2), Offset(size.width, h / 2), 3.dp.toPx())
        drawRect(Gym.Muted, Offset(0f, h / 2 - 5.dp.toPx()), Size(3.dp.toPx(), 10.dp.toPx()))
        // The outermost plates are the ones that come off.
        val offCount = removed.size
        plates.forEachIndexed { i, kg ->
            val ph = (h * (0.35 + 0.65 * kotlin.math.sqrt(kg / maxKg).coerceAtMost(1.0))).toFloat()
            val x = 6.dp.toPx() + i * (pw + gap)
            val top = (h - ph) / 2
            val isOff = i >= plates.size - offCount
            if (isOff) {
                drawRoundRect(
                    Gym.Yellow, Offset(x, top), Size(pw, ph), CornerRadius(2.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))),
                )
            } else {
                drawRoundRect(Gym.Cream, Offset(x, top), Size(pw, ph), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun RunningTimer(elapsed: Long) {
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Sunburst(Modifier.fillMaxSize().alpha(.55f), full = true)
        Box(Modifier.size(140.dp).background(Gym.Bg, RoundedCornerShape(70.dp)))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatDuration(elapsed), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Gym.Yellow)
            Text(stringResource(R.string.tr_put_phone_away), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A finished or not-yet-open set as one compact line; tap to edit. */
@Composable
private fun SetLine(
    set: SetEntity,
    ex: ExerciseEntity,
    unit: WeightUnit,
    target: Int?,
    goodMargin: Int,
    previousReps: Int?,
    isReps: Boolean,
    onClick: () -> Unit,
) {
    val signed = ex.loadType == LoadType.BODYWEIGHT
    Row(
        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoleBadge(set.role)
        val weight = set.weightKg?.takeIf { !(isReps && it == 0.0) }?.let { WeightFormat.format(it, unit, signed) + " " + unit.name.lowercase() }
        Text(
            listOfNotNull(weight, set.reps?.let { "× $it" } ?: target?.let { "→ $it" }).joinToString("  "),
            style = MaterialTheme.typography.titleMedium, color = if (set.reps != null) Gym.Cream else Gym.Muted,
            modifier = Modifier.weight(1f),
        )
        SignalChip(set, target, goodMargin, isReps, previousReps)
        Icon(Icons.Filled.Edit, contentDescription = null, tint = Gym.Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RoleBadge(role: SetRole) {
    val (bg, fg) = when (role) {
        SetRole.HEAVY -> Gym.Yellow to Gym.OnYellow
        SetRole.LIGHT -> Gym.SurfaceHigh to Gym.Cream
        SetRole.WARMUP -> Gym.Glow to Gym.Cream
    }
    Box(Modifier.size(30.dp).background(bg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        if (role == SetRole.WARMUP) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = roleLabel(role), tint = fg, modifier = Modifier.size(18.dp))
        } else {
            Text(roleLabel(role).take(1), color = fg, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** The open set: weight and reps side by side, both with big tap targets. */
@Composable
private fun SetEditor(
    set: SetEntity,
    ex: ExerciseEntity,
    calc: WeightCalculator?,
    unit: WeightUnit,
    step: Double,
    target: Int?,
    goodMargin: Int,
    previousReps: Int?,
    isReps: Boolean,
    onChange: (SetEntity) -> Unit,
    onFirstInput: () -> Unit,
    onToggleRole: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val signed = ex.loadType == LoadType.BODYWEIGHT
    var showWeight by remember(set.id) { mutableStateOf(!isReps || (set.weightKg ?: 0.0) != 0.0) }
    Column(
        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(20.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.clickable(enabled = set.role.isWork, onClick = onToggleRole)) { RoleBadge(set.role) }
            Text(
                (roleLabel(set.role) + (target?.let { " · " + stringResource(R.string.tr_target, it) } ?: "")).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (set.role == SetRole.WARMUP) Gym.Gold else Gym.Cream,
                modifier = Modifier.weight(1f),
            )
            SignalChip(set, target, goodMargin, isReps, previousReps)
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, stringResource(R.string.tr_set_delete), tint = Gym.Muted, modifier = Modifier.size(18.dp))
            }
        }
        if (set.role == SetRole.WARMUP) {
            Text(warmupHint(ex, calc, unit), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (set.role != SetRole.WARMUP && showWeight) {
                Column(Modifier.weight(1.15f)) {
                    Eyebrow(stringResource(if (signed) R.string.tr_extra_short else R.string.tr_weight))
                    NumberStepper(
                        value = set.weightKg,
                        onValueChange = { onChange(set.copy(weightKg = it)) },
                        step = step,
                        allowNegative = signed,
                        min = if (signed) null else 0.0,
                        next = calc?.let { c -> { v: Double -> c.up(v) } },
                        previous = calc?.let { c -> { v: Double -> c.down(v) } },
                        onFirstInput = onFirstInput,
                        formatter = { WeightFormat.format(it, unit, signed) },
                        parser = unit::toKg,
                        textSize = 22.sp, buttonSize = 42.dp,
                        minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.tr_reps))
                NumberStepper(
                    value = set.reps?.toDouble(),
                    onValueChange = { onChange(set.copy(reps = it?.toInt()?.coerceAtLeast(0))) },
                    step = 1.0,
                    decimals = false,
                    min = 0.0,
                    startFrom = (target ?: previousReps)?.toDouble(),
                    onFirstInput = onFirstInput,
                    formatter = { it.toInt().toString() },
                    textSize = 22.sp, buttonSize = 42.dp,
                    minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                )
            }
        }
        if (set.role != SetRole.WARMUP && !showWeight) {
            Text(stringResource(R.string.tr_add_weight), color = Gym.Yellow, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { showWeight = true })
        }
        if (set.role != SetRole.WARMUP && showWeight) {
            val w = set.weightKg
            val info = when {
                w != null && calc != null && !calc.isAchievable(w) -> stringResource(R.string.ld_not_achievable, kgText(calc.snap(w), unit, signed))
                signed -> w?.let { set.bodyweightKg?.let { bw -> stringResource(R.string.tr_effective, kgText(bw * ex.bodyweightFactor + w, unit)) } }
                    ?: stringResource(R.string.tr_bodyweight_missing)
                else -> null
            }
            info?.let { Text(it, color = Gym.Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Gym.Yellow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_done), color = Gym.Yellow)
            }
        }
    }
}

@Composable
private fun SignalChip(set: SetEntity, target: Int?, goodMargin: Int, isReps: Boolean, previousReps: Int?) {
    val reps = set.reps ?: return
    if (!set.role.isWork) return
    val reached = GoalAdvisor.goalReached(
        if (isReps && (set.weightKg ?: 0.0) == 0.0) Progression.REPS else Progression.WEIGHT,
        set.goalWeightKg, set.weightKg, previousReps, reps,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (reached == true) Icon(Icons.Filled.Check, stringResource(R.string.tr_goal_reached), tint = Gym.Yellow, modifier = Modifier.size(16.dp))
        if (target != null && !isReps) {
            val (text, color) = when (GoalAdvisor.signal(reps, target, goodMargin)) {
                Signal.TOO_LIGHT -> stringResource(R.string.signal_too_light) to Gym.Yellow
                Signal.GOOD -> stringResource(R.string.signal_good) to Gym.Positive
                Signal.TOO_HEAVY -> stringResource(R.string.signal_too_heavy) to Gym.Muted
            }
            Text(text, color = color, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Plan for next time: a one-line summary; tap to adjust (D-18a). */
@Composable
private fun NextTimePlanner(item: TrainingItem, plan: PlanDraft, unit: WeightUnit, step: Double, targetOf: (SetRole) -> Int?, vm: TrainingViewModel) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val isReps = ex.progression == Progression.REPS
    var open by remember(item.se.id) { mutableStateOf(false) }
    DecoCard(corners = true, padding = 14.dp) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.tr_next_time), color = Gym.Gold)
                val letters = SetRole.entries.associateWith { roleLabel(it).take(1) }
                val summary = plan.sets.filter { it.role.isWork }.joinToString("  ·  ") { p ->
                    val role = letters.getValue(p.role)
                    val w = p.weightKg
                    if (isReps || w == null) role else "$role ${WeightFormat.format(w, unit, signed)}"
                }
                if (!open) Text(summary, style = MaterialTheme.typography.titleMedium, color = Gym.Yellow)
            }
            Icon(if (open) Icons.Filled.Check else Icons.Filled.Edit, stringResource(R.string.tr_edit_plan), tint = Gym.Gold, modifier = Modifier.size(20.dp))
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            plan.sets.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleBadge(p.role)
                    if (p.role.isWork && !isReps) {
                        NumberStepper(
                            value = p.weightKg,
                            onValueChange = { v -> vm.editPlan(item.se.id) { d -> d.copy(sets = d.sets.mapIndexed { j, s -> if (j == i) s.copy(weightKg = v) else s }) } },
                            step = step,
                            allowNegative = signed,
                            min = if (signed) null else 0.0,
                            next = item.calc?.let { c -> { v: Double -> c.up(v) } },
                            previous = item.calc?.let { c -> { v: Double -> c.down(v) } },
                            formatter = { WeightFormat.format(it, unit, signed) },
                            parser = unit::toKg,
                            textSize = 20.sp,
                            buttonSize = 40.dp,
                            modifier = Modifier.weight(1f),
                            minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                        )
                    } else {
                        Text(
                            if (p.role == SetRole.WARMUP) warmupHint(ex, item.calc, unit) else (targetOf(p.role)?.let { stringResource(R.string.tr_target, it) } ?: stringResource(R.string.tr_to_failure)),
                            color = Gym.Muted, modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(onClick = { vm.editPlan(item.se.id) { d -> d.copy(sets = d.sets.filterIndexed { j, _ -> j != i }) } }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Gym.Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                LineButton(
                    stringResource(R.string.tr_add_set),
                    onClick = {
                        vm.editPlan(item.se.id) { d ->
                            val last = d.sets.lastOrNull { it.role.isWork }
                            d.copy(sets = d.sets + PlannedSet(SetRole.HEAVY, last?.weightKg ?: if (signed || isReps) 0.0 else null))
                        }
                    },
                    icon = Icons.Filled.Add,
                    modifier = Modifier.weight(1f),
                )
                LineButton(
                    stringResource(R.string.tr_add_warmup),
                    onClick = { vm.editPlan(item.se.id) { d -> d.copy(sets = listOf(PlannedSet(SetRole.WARMUP, null)) + d.sets) } },
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = item.note,
                onValueChange = { vm.setPlanNote(item.se.id, it) },
                label = { Text(stringResource(R.string.tr_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
        }
    }
}

/** 3-2-1 with sound and vibration (D-32), then the exercise timer starts. */
@Composable
private fun CountdownOverlay(onDone: () -> Unit) {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(3) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(Unit) {
        for (i in 3 downTo 1) {
            count = i
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            vibrate(context, 60)
            delay(1000)
        }
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        vibrate(context, 250)
        onDone()
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Gym.Glow.copy(alpha = .9f), Gym.Bg)))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Sunburst(Modifier.size(340.dp), full = true)
        Box(Modifier.size(190.dp).background(Gym.Bg, RoundedCornerShape(95.dp)))
        Text("$count", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 150.sp, color = Gym.Yellow)
    }
}

private fun vibrate(context: Context, ms: Long) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

/** Small, skippable question for today's body weight (D-16). */
@Composable
fun BodyweightPrompt(onSave: (Double) -> Unit, onSkip: () -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf<Double?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bw_ask_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.bw_ask_body), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                NumberStepper(value = value, onValueChange = { value = it }, step = 0.5, min = 0.0, startFrom = 75.0)
            }
        },
        confirmButton = { TextButton(onClick = { value?.takeIf { it > 0 }?.let(onSave) }, enabled = (value ?: 0.0) > 0) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.action_skip)) } },
    )
}
