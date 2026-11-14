package nl.ibs.gym8020.ui.training

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import nl.ibs.gym8020.core.logic.PlanRoles
import nl.ibs.gym8020.core.logic.Warmup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.platform.testTag
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
import kotlin.math.abs
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.Advice
import nl.ibs.gym8020.core.logic.Finding
import nl.ibs.gym8020.core.logic.FindingKind
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.LastTime
import nl.ibs.gym8020.core.logic.Missing
import nl.ibs.gym8020.core.logic.PerformanceWeight
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.ProgressionSwitch
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
import nl.ibs.gym8020.ui.deco.drawRibbon
import nl.ibs.gym8020.ui.exercises.ExercisePickerDialog
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins
import java.time.format.DateTimeFormatter

/** Test tags for the UI tests of F-2 (TESTING.md): the exercise timer and the 3-2-1 countdown. */
const val EXERCISE_TIMER_TAG = "exercise_timer"
const val COUNTDOWN_TAG = "countdown"

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

/** Warm-up hint for a training item, including assistance for pull-ups and dips (B-4). */
@Composable
fun warmupHint(item: TrainingItem, unit: WeightUnit, assistPercent: Int): String {
    val ex = item.exercise
    if (Warmup.needsAssistance(ex.loadType, ex.bodyweightFactor)) {
        return item.warmupAssistKg?.let { stringResource(R.string.tr_warmup_hint_assist, kgText(-it, unit)) }
            ?: stringResource(R.string.tr_warmup_hint_assist_pct, assistPercent)
    }
    return warmupHint(ex, item.calc, unit)
}

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
    // The check at finishing (#47): what is missing, and the line you tapped (with a counter so a second tap works too).
    var findings by remember { mutableStateOf<List<Finding>>(emptyList()) }
    var focus by remember { mutableStateOf<Pair<Finding, Int>?>(null) }
    // Survives rotation (B-5): which exercise is counting down and since when.
    var countdownFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var countdownStart by rememberSaveable { mutableStateOf(0L) }

    // Screen (V-1): on before GO and during the countdown; after that it may go off to save battery.
    // During the training the app shows over the lock screen, so waking the phone brings you straight back.
    val view = LocalView.current
    val current = items.getOrNull(pager.currentPage)
    val keepOn = countdownFor != null || (current != null && current.se.goAt == null && current.se.firstInputAt == null)
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        activity?.showOverLockScreen(true)
        onDispose { activity?.showOverLockScreen(false) }
    }
    // No keyboard after GO (B-2): only when you tap a field yourself.
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun dropFocus() {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }
    fun askFinish() {
        val found = vm.finishCheck()
        if (found.isEmpty()) confirmFinish = true else findings = found
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

    Box(Modifier.fillMaxSize()) {
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
                IconButton(onClick = { askFinish() }) { Icon(Icons.Filled.Flag, stringResource(R.string.tr_finish), tint = Gym.Yellow) }
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
                            assistPercent = state.settings.warmupAssistPercent,
                            isLast = page == items.lastIndex,
                            onGo = { dropFocus(); countdownStart = System.currentTimeMillis(); countdownFor = item.se.id },
                            onFinish = { askFinish() },
                            focus = focus?.takeIf { it.first.sessionExerciseId == item.se.id },
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
            CountdownOverlay(startedAt = countdownStart, onDone = { vm.go(seId); countdownFor = null; dropFocus() })
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

    if (findings.isNotEmpty()) {
        FinishCheckDialog(
            findings = findings,
            items = items,
            onOpen = { f ->
                findings = emptyList()
                focus = f to ((focus?.second ?: 0) + 1)
                val page = items.indexOfFirst { it.se.id == f.sessionExerciseId }
                if (page >= 0) scope.launch { pager.animateScrollToPage(page) }
            },
            onFinish = { findings = emptyList(); vm.finish { onFinished(sessionId) } },
            onDismiss = { findings = emptyList() },
        )
    }
}

/** One line of the check at finishing, e.g. "Back squat, heavy set: reps missing". */
@Composable
private fun findingText(f: Finding, name: String): String {
    val set = when (f.role) {
        SetRole.HEAVY -> stringResource(R.string.fc_set_heavy)
        SetRole.LIGHT -> stringResource(R.string.fc_set_light)
        SetRole.WARMUP -> roleLabel(f.role)
    }
    return when {
        f.kind == FindingKind.WARN -> stringResource(R.string.fc_target_weight, name, set)
        f.kind == FindingKind.SKIP -> stringResource(R.string.fc_skip, name, set)
        f.missing == Missing.WEIGHT -> stringResource(R.string.fc_missing_weight, name, set)
        else -> stringResource(R.string.fc_missing_reps, name, set)
    }
}

/**
 * The overview at finishing (#47): one line per finding; tap a line to go to that set.
 * Anything blocking: no way to finish yet. Only skipped sets or target warnings: finish anyway.
 */
@Composable
private fun FinishCheckDialog(
    findings: List<Finding>,
    items: List<TrainingItem>,
    onOpen: (Finding) -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
) {
    val blocked = findings.any { it.kind == FindingKind.BLOCK }
    val names = items.associate { it.se.id to it.exercise.name }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (blocked) R.string.fc_title_blocked else R.string.fc_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(if (blocked) R.string.fc_body_blocked else R.string.fc_body),
                    style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                )
                Spacer(Modifier.height(4.dp))
                // Blocking lines first, then skipped sets, then target warnings.
                findings.sortedBy { it.kind.ordinal }.forEach { f ->
                    Text(
                        findingText(f, names[f.sessionExerciseId].orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (f.kind == FindingKind.BLOCK) Gym.Yellow else Gym.Cream,
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(f) }.padding(vertical = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!blocked) TextButton(onClick = onFinish) { Text(stringResource(R.string.fc_finish_anyway)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** The art deco swipe ribbon at the screen edge (D-71); tap or swipe to change exercise. */
@Composable
private fun SwipeRibbon(pointsRight: Boolean, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ribbon")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    Canvas(
        modifier
            .width(30.dp).height(176.dp)
            .alpha(if (pointsRight) 1f else .45f)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
    ) { drawRibbon(phase, mirror = !pointsRight) }
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
    assistPercent: Int,
    isLast: Boolean,
    onGo: () -> Unit,
    onFinish: () -> Unit,
    focus: Pair<Finding, Int>? = null,
) {
    val ex = item.exercise
    val se = item.se
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val running = se.goAt != null && se.firstInputAt == null
    val warmingUp = se.warmupAt != null && se.goAt == null && se.firstInputAt == null
    val beforeGo = se.goAt == null && se.firstInputAt == null
    val isReps = item.progressionToday == Progression.REPS
    val firstInput = { vm.firstInput(se.id) }
    // One set is open for editing at a time; the rest are compact lines.
    var openSet by remember(se.id) { mutableStateOf(item.sets.firstOrNull { it.reps == null }?.id) }
    // Tapped in the check at finishing (#47): open that set.
    LaunchedEffect(focus) {
        val f = focus?.first ?: return@LaunchedEffect
        if (f.kind != FindingKind.WARN) item.sets.getOrNull(f.position)?.let { openSet = it.id }
    }

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
                    stringResource(R.string.tr_warmup_button, warmupHint(item, unit, assistPercent)),
                    onClick = { vm.startWarmup(se.id) }, icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.fillMaxWidth(),
                )
            }
            GoButton(stringResource(R.string.tr_go), onClick = onGo)
        }

        DecoDivider()

        // Sets: filling in the first number stops the exercise timer (D-31). Warm-up reps don't.
        item.sets.forEach { set ->
            val target = headerTarget(item, set, targetOf)
            // Reps progression: the reps to beat are the header number, your reps target for this set (#52, #69).
            val prevReps = if (item.repsTargets != null) target else item.extra?.last?.sets?.filter { it.role == set.role }?.maxOfOrNull { it.reps ?: 0 }
            if (openSet == set.id) {
                SetEditor(
                    set = set, ex = ex, calc = item.calc, warmupText = warmupHint(item, unit, assistPercent), unit = unit, step = step, target = target, goodMargin = goodMargin,
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

        item.plan?.let { plan -> NextTimePlanner(item, plan, unit, step, targetOf, goodMargin, vm, assistPercent, focus) }

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

/** Short training date in "Laatste keer", e.g. "2 okt" (#65). */
private val lastTimeDate = DateTimeFormatter.ofPattern("d MMM")

@Composable
private fun LastTimeLine(item: TrainingItem, unit: WeightUnit) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val last = item.extra?.last
    // "Laatste keer" per set (#65): from any workout, by training date; one date, or a date per set when they differ.
    val perSet = item.lastPerSet.filterNotNull()
    if (perSet.isNotEmpty()) {
        val heavy = stringResource(R.string.role_heavy_short)
        val light = stringResource(R.string.role_light_short)
        val shared = LastTime.sharedDate(perSet)
        val summary = perSet.joinToString("  ·  ") { l ->
            val s = l.set
            val w = PerformanceWeight.orNone(s.weightKg, ex.progression, ex.loadType)
            // A bodyweight exercise in effective weight with the body weight of that day (#87); signed when unknown.
            val moved = if (signed) l.effectiveKg(ex.loadType, ex.bodyweightFactor) else null
            val value = when {
                ex.progression == Progression.REPS && (w == null || w == 0.0) -> "${s.reps}"
                moved != null -> "${kgText(moved, unit)} × ${s.reps}"
                else -> "${w?.let { WeightFormat.format(it, unit, signed) } ?: "—"} × ${s.reps}"
            }
            val role = if (s.role == SetRole.LIGHT) light else heavy
            if (shared == null) "$role $value · ${l.date.format(lastTimeDate)}" else "$role $value"
        }
        val text = if (shared != null) {
            stringResource(R.string.tr_last_time_on, shared.format(lastTimeDate), summary)
        } else {
            stringResource(R.string.tr_last_time, summary)
        }
        Text(text, color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
    }
    item.extra?.lastNote?.let { Text(stringResource(R.string.tr_note_last, it), color = Gym.Gold, style = MaterialTheme.typography.bodySmall) }
    // No history yet: show similar exercises as a reference (D-68).
    item.extra?.similar?.takeIf { last == null && it.isNotEmpty() }?.let { list ->
        val text = list.joinToString("  ·  ") { (ex, perf) ->
            val sets = perf.sets.filter { it.role.isWork && it.reps != null }
            val best = sets.maxByOrNull { it.weightKg ?: 0.0 }
            val w = best?.weightKg?.let { WeightFormat.format(it, unit, ex.loadType == LoadType.BODYWEIGHT) }
            ex.name + (if (best != null) " ${w ?: "—"} × ${best.reps}" else "")
        }
        Text(stringResource(R.string.tr_similar, text), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Human text for how to set up a weight with the material (D-23, D-26). */
@Composable
private fun loadText(weightKg: Double?, calc: WeightCalculator?, step: LoadStep?, isFirst: Boolean, unit: WeightUnit): String? {
    if (calc == null || weightKg == null) return null
    val p = calc.profile
    val w: Double = weightKg
    fun plates(l: List<Double>) = l.joinToString(" · ") { WeightFormat.format(it, unit) }
    return when (p.kind) {
        MaterialKind.PLATES -> if (step == null) null else {
            // Stacked plates (#71): between sets only what comes off; putting on only when the stock cannot stack.
            val change: String? = if (isFirst) null else listOfNotNull(
                step.remove.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.ld_remove, plates(it)) },
                step.add.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.ld_add, plates(it)) },
            ).joinToString("  ·  ").ifEmpty { null }
            // Every weight shows its stack per side, inside → outside.
            val stack: String = if (step.loading.plates.isEmpty()) stringResource(R.string.ld_no_plates)
            else stringResource(R.string.ld_per_side, plates(step.loading.plates))
            val base: String = if (isFirst && p.baseKg > 0) "  ·  " + stringResource(R.string.ld_base, kgText(p.baseKg, unit)) else ""
            val note: String? = if (isFirst && !step.stacked) stringResource(R.string.ld_not_stacked) else null
            listOfNotNull(change, stack + base, note).joinToString("\n")
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

/**
 * The number in a set's header (#52). Reps progression: your reps target for that set as the training started
 * (#69), else the reps of that set's role last time, none without either. Weight progression: the heavy/light rep
 * target from the settings.
 */
private fun headerTarget(item: TrainingItem, set: SetEntity, targetOf: (SetRole) -> Int?): Int? {
    val reps = item.repsTargets ?: return targetOf(set.role)
    return reps.getOrNull(item.sets.indexOf(set))
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
            val target = headerTarget(item, set, targetOf)
            Eyebrow(roleLabel(set.role) + (target?.let { " · " + stringResource(R.string.tr_target, it) } ?: ""))
            if (item.progressionToday == Progression.REPS && (weights[i] ?: 0.0) == 0.0) {
                val prev = target
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
                // Bodyweight exercise (#87): the target is an effective weight; the big number is what you hang today.
                val effective = item.targetEffectiveKg.getOrNull(item.sets.indexOf(set))
                val today = weights[i]
                if (effective != null && today != null) {
                    Text(
                        stringResource(R.string.tr_bodyweight_target_today, kgText(effective, unit), kgText(today, unit, signed = true)),
                        style = MaterialTheme.typography.bodySmall, color = Gym.Gold,
                    )
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
            Text(
                formatDuration(elapsed), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Gym.Yellow,
                modifier = Modifier.testTag(EXERCISE_TIMER_TAG),
            )
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
        // Not filled in yet: the target weight in grey, like the rep target (#47).
        val shownKg = set.weightKg ?: set.goalWeightKg.takeIf { set.reps == null }
        val weight = shownKg?.takeIf { !(isReps && it == 0.0) }?.let { WeightFormat.format(it, unit, signed) + " " + unit.name.lowercase() }
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
internal fun RoleBadge(role: SetRole) {
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
    warmupText: String,
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
            Text(warmupText, color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
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
                        // Never prefilled (#47): the target is a grey hint; tap it (or − / +) to take it over.
                        placeholder = set.goalWeightKg?.let { WeightFormat.format(it, unit, signed) },
                        startFrom = set.goalWeightKg,
                        placeholderTakesOver = true,
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
                // An empty extra means none (#47).
                signed -> set.bodyweightKg?.let { bw -> stringResource(R.string.tr_effective, kgText(bw * ex.bodyweightFactor + (w ?: 0.0), unit)) }
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
private fun NextTimePlanner(
    item: TrainingItem,
    plan: PlanDraft,
    unit: WeightUnit,
    step: Double,
    targetOf: (SetRole) -> Int?,
    goodMargin: Int,
    vm: TrainingViewModel,
    assistPercent: Int,
    focus: Pair<Finding, Int>? = null,
) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val isReps = ex.progression == Progression.REPS
    // Advice per set (#63), only where it differs from your target; on weight, today and next time.
    val advice = Advice.perSet(
        plan.sets, item.sets.map { PerformedSet(it.role, it.weightKg, it.reps) },
        if (isReps) Progression.REPS else item.progressionToday, ex.loadType, targetOf, goodMargin, step, item.calc,
    )
    val taken = vm.takenAdvice.collectAsStateWithLifecycle().value[item.se.id].orEmpty()
    val switchTo = ProgressionSwitch.offered(ex.progression, ex.loadType)
    var open by remember(item.se.id) { mutableStateOf(false) }
    // A target warning tapped in the check at finishing (#47) opens the target.
    LaunchedEffect(focus) { if (focus?.first?.kind == FindingKind.WARN) open = true }
    DecoCard(corners = true, padding = 14.dp) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.tr_next_time), color = Gym.Gold)
                val letters = SetRole.entries.associateWith { roleLabel(it).take(1) }
                val summary = plan.sets.withIndex().filter { it.value.role.isWork }.joinToString("  ·  ") { (_, p) ->
                    val role = letters.getValue(p.role)
                    val w = p.weightKg
                    val reps = p.reps
                    when {
                        isReps -> if (reps != null) "$role → $reps" else role
                        w == null -> role
                        else -> "$role ${WeightFormat.format(w, unit, signed)}"
                    }
                }
                if (!open) Text(summary, style = MaterialTheme.typography.titleMedium, color = Gym.Yellow)
                // Collapsed: a hint that there is advice; open the target to take it over. Set by set like the
                // target above it (#72), so with two heavy sets each advice stands under its own set.
                val hint = Advice.line(plan.sets, advice)?.joinToString("  ·  ") { p ->
                    "${letters.getValue(p.role)} ${p.weightKg?.let { WeightFormat.format(it, unit, signed) } ?: "–"}"
                }
                if (!open && hint != null) {
                    Text(stringResource(R.string.tr_advice, hint), style = MaterialTheme.typography.bodySmall, color = Gym.Advice)
                }
            }
            Icon(if (open) Icons.Filled.Check else Icons.Filled.Edit, stringResource(R.string.tr_edit_plan), tint = Gym.Gold, modifier = Modifier.size(20.dp))
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            plan.sets.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tap to switch heavy → light → warm-up (B-3).
                    Box(Modifier.clickable { vm.editPlan(item.se.id) { d -> d.copy(sets = PlanRoles.cycle(d.sets, i)) } }) { RoleBadge(p.role) }
                    if (p.role.isWork && isReps) {
                        // Reps progression (#69): your reps target for this set, by default the reps you did today.
                        // Whole reps, stored right away like a weight.
                        NumberStepper(
                            value = p.reps?.toDouble(),
                            onValueChange = { v ->
                                vm.editPlan(item.se.id) { d -> d.copy(sets = d.sets.mapIndexed { j, s -> if (j == i) s.copy(reps = v?.toInt()?.coerceAtLeast(0)) else s }) }
                            },
                            step = 1.0,
                            decimals = false,
                            min = 0.0,
                            formatter = { it.toInt().toString() },
                            textSize = 20.sp,
                            buttonSize = 40.dp,
                            modifier = Modifier.weight(1f),
                            minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                            suffix = stringResource(R.string.tr_reps_suffix),
                        )
                    } else if (p.role.isWork) {
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
                            warmupHint(item, unit, assistPercent),
                            color = Gym.Muted, modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(onClick = { vm.editPlan(item.se.id) { d -> d.copy(sets = d.sets.filterIndexed { j, _ -> j != i }) } }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Gym.Muted, modifier = Modifier.size(18.dp))
                    }
                }
                // Under the target of the set: the advice, or the advice you took over while it is still your target.
                val took = taken[i]?.takeIf { t -> p.weightKg?.let { abs(it - t.adviceKg) < 1e-9 } == true }
                val proposed = advice.getOrNull(i)
                if (took != null) {
                    AdviceChip(
                        stringResource(R.string.tr_advice_taken, took.previousKg?.let { kgText(it, unit, signed) } ?: "–"),
                        stringResource(R.string.tr_advice_undo),
                        onClick = { vm.undoAdvice(item.se.id, i) },
                    )
                } else if (proposed != null) {
                    AdviceChip(
                        stringResource(R.string.tr_advice, kgText(proposed, unit, signed)),
                        stringResource(R.string.tr_advice_take),
                        onClick = { vm.takeAdvice(item.se.id, i, proposed) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                LineButton(
                    stringResource(R.string.tr_add_set),
                    onClick = {
                        vm.editPlan(item.se.id) { d ->
                            val last = d.sets.lastOrNull { it.role.isWork }
                            d.copy(sets = d.sets + PlannedSet(SetRole.HEAVY, if (isReps) null else last?.weightKg ?: if (signed) 0.0 else null, if (isReps) last?.reps else null))
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
            // "Volgende keer op gewicht / op reps" (#52, ADR 0001): applies from the next session.
            if (switchTo != null) {
                LineButton(
                    stringResource(if (switchTo == Progression.WEIGHT) R.string.tr_next_by_weight else R.string.tr_next_by_reps),
                    onClick = { vm.switchProgression(item.se.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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

/** The advice under a set of your target (#63): a light-blue dashed chip, apart from the yellow of your target. */
@Composable
private fun AdviceChip(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 4.dp, bottom = 4.dp)
            .drawBehind {
                drawRoundRect(
                    color = Gym.Advice,
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, color = Gym.Advice, style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = onClick) {
            Text(action, color = Gym.Advice, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 3-2-1 with sound and vibration (D-32), then the exercise timer starts. */
/** 3-2-1 before an exercise. Counts from [startedAt], so it continues where it was after a rotation (B-5). */
@Composable
private fun CountdownOverlay(startedAt: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    fun remaining() = (3 - ((System.currentTimeMillis() - startedAt) / 1000L).toInt()).coerceIn(0, 3)
    var count by remember { mutableIntStateOf(remaining().coerceAtLeast(1)) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(startedAt) {
        var shown = -1
        while (true) {
            val r = remaining()
            if (r == 0) break
            if (r != shown) {
                shown = r
                count = r
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                vibrate(context, 60)
            }
            delay(50)
        }
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        vibrate(context, 250)
        onDone()
    }
    Box(
        Modifier.fillMaxSize()
            .testTag(COUNTDOWN_TAG)
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

/** Show the training over the lock screen (V-1), only while the training screen is open. */
private fun Activity.showOverLockScreen(on: Boolean) {
    if (Build.VERSION.SDK_INT >= 27) {
        setShowWhenLocked(on)
    } else {
        @Suppress("DEPRECATION")
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }
}
