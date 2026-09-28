package nl.ibs.gym8020.ui.training

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.Signal
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.components.WeightText
import nl.ibs.gym8020.ui.deco.DecoDivider
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

@Composable
fun warmupHint(ex: ExerciseEntity): String = when {
    ex.loadType == LoadType.BODYWEIGHT -> stringResource(R.string.tr_warmup_hint_bodyweight)
    ex.equipment == Equipment.BARBELL -> stringResource(R.string.tr_warmup_hint_barbell)
    else -> stringResource(R.string.tr_warmup_hint_light)
}

@Composable
fun TrainingScreen(container: AppContainer, sessionId: Long, onFinished: (Long) -> Unit, onExit: () -> Unit) {
    val vm: TrainingViewModel = viewModel(key = "training-$sessionId") { TrainingViewModel(container, sessionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session = state.session
    val items = state.items
    val pager = rememberPagerState { items.size }
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
                HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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

        if (se.skipped) {
            Eyebrow(stringResource(R.string.tr_skipped), color = Gym.Negative)
        }

        LastTimeLine(item, unit)

        when {
            running -> RunningTimer(now - (se.goAt ?: now))
            beforeGo -> GoalCards(item, unit, targetOf)
            else -> {}
        }

        if (beforeGo) {
            if (warmingUp) {
                Text(stringResource(R.string.tr_warmup_running, formatDuration(now - (se.warmupAt ?: now))), color = Gym.Gold, style = MaterialTheme.typography.bodyMedium)
            } else if (se.warmupAt == null) {
                LineButton(stringResource(R.string.tr_warmup_button, warmupHint(ex)), onClick = { vm.startWarmup(se.id) }, modifier = Modifier.fillMaxWidth())
            }
            GoButton(stringResource(R.string.tr_go), onClick = onGo)
        }

        DecoDivider()

        // Set entry; filling in the first number stops the exercise timer (D-31).
        item.sets.forEach { set ->
            SetRow(
                set = set, ex = ex, unit = unit, step = step, target = targetOf(set.role), goodMargin = goodMargin,
                previousReps = item.extra?.last?.sets?.filter { it.role == set.role }?.maxOfOrNull { it.reps ?: 0 },
                onChange = vm::updateSet,
                // Warm-up reps may be entered before GO; only work sets end the exercise timer.
                onFirstInput = if (set.role.isWork) firstInput else ({}),
                onToggleRole = { vm.toggleRole(set) }, onDelete = { vm.deleteSet(set.id) }, isReps = isReps,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LineButton(stringResource(R.string.tr_add_set), onClick = { vm.addSet(se.id, SetRole.HEAVY) }, modifier = Modifier.weight(1f))
            LineButton(stringResource(R.string.tr_add_warmup), onClick = { vm.addSet(se.id, SetRole.WARMUP) }, modifier = Modifier.weight(1f))
        }

        item.plan?.let { plan ->
            NextTimePlanner(item, plan, unit, step, targetOf, vm)
        }

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
    val last = item.extra?.last ?: return
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val summary = last.sets.filter { it.role.isWork && it.reps != null }.joinToString("  ·  ") { s ->
        val w = s.weightKg
        if (ex.progression == Progression.REPS && (w == null || w == 0.0)) "${s.reps}"
        else "${w?.let { WeightFormat.format(it, unit, signed) } ?: "—"} × ${s.reps}"
    }
    if (summary.isNotEmpty()) {
        Text(stringResource(R.string.tr_last_time, summary), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
    }
    item.extra?.lastNote?.let {
        Text(stringResource(R.string.tr_note_last, it), color = Gym.Gold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GoalCards(item: TrainingItem, unit: WeightUnit, targetOf: (SetRole) -> Int?) {
    val ex = item.exercise
    val work = item.sets.filter { it.role.isWork }
    val signed = ex.loadType == LoadType.BODYWEIGHT
    work.forEachIndexed { i, set ->
        DecoCard(corners = i == 0) {
            val target = targetOf(set.role)
            Eyebrow(roleLabel(set.role) + (target?.let { " · " + stringResource(R.string.tr_target, it) } ?: ""))
            if (ex.progression == Progression.REPS && (set.goalWeightKg ?: 0.0) == 0.0) {
                val prev = item.extra?.last?.sets?.filter { it.role == set.role }?.maxOfOrNull { it.reps ?: 0 }
                Text(
                    prev?.let { stringResource(R.string.tr_reps_progression, it) } ?: stringResource(R.string.tr_to_failure),
                    style = MaterialTheme.typography.headlineSmall, color = Gym.Yellow,
                )
            } else {
                WeightText(set.goalWeightKg ?: set.weightKg, unit, size = if (i == 0) 56.sp else 44.sp, signed = signed)
            }
            if (i > 0) {
                val prevW = work[i - 1].goalWeightKg ?: work[i - 1].weightKg
                val w = set.goalWeightKg ?: set.weightKg
                if (prevW != null && w != null && w < prevW) {
                    Text(stringResource(R.string.tr_take_off, WeightFormat.format(prevW - w, unit)), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RunningTimer(elapsed: Long) {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Sunburst(Modifier.fillMaxSize().alpha(.55f), full = true)
        Box(Modifier.size(150.dp).background(Gym.Bg, RoundedCornerShape(75.dp)))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatDuration(elapsed), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, color = Gym.Yellow)
            Text(stringResource(R.string.tr_put_phone_away), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SetRow(
    set: SetEntity,
    ex: ExerciseEntity,
    unit: WeightUnit,
    step: Double,
    target: Int?,
    goodMargin: Int,
    previousReps: Int?,
    onChange: (SetEntity) -> Unit,
    onFirstInput: () -> Unit,
    onToggleRole: () -> Unit,
    onDelete: () -> Unit,
    isReps: Boolean,
) {
    val signed = ex.loadType == LoadType.BODYWEIGHT
    var showWeight by remember(set.id) { mutableStateOf(!isReps || (set.weightKg ?: 0.0) != 0.0) }
    Column(
        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (roleLabel(set.role) + (target?.let { " · " + stringResource(R.string.tr_target, it) } ?: "")).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (set.role == SetRole.WARMUP) Gym.Gold else Gym.Cream,
                modifier = Modifier.weight(1f).then(if (set.role.isWork) Modifier.clickable(onClick = onToggleRole) else Modifier),
            )
            SignalChip(set, target, goodMargin, isReps, previousReps)
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, stringResource(R.string.tr_set_delete), tint = Gym.Muted, modifier = Modifier.size(18.dp))
            }
        }
        if (set.role == SetRole.WARMUP) {
            Text(warmupHint(ex), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
        } else if (showWeight) {
            Eyebrow(stringResource(if (signed) R.string.tr_extra else R.string.tr_weight))
            NumberStepper(
                value = set.weightKg,
                onValueChange = { onChange(set.copy(weightKg = it)) },
                step = step,
                allowNegative = signed,
                min = if (signed) null else 0.0,
                onFirstInput = onFirstInput,
                formatter = { WeightFormat.format(it, unit, signed) },
                parser = unit::toKg,
                minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
            )
            if (signed) {
                val eff = set.weightKg?.let { w -> set.bodyweightKg?.let { it * ex.bodyweightFactor + w } }
                Text(
                    eff?.let { stringResource(R.string.tr_effective, WeightFormat.format(it, unit) + " " + unit.name.lowercase()) }
                        ?: stringResource(R.string.tr_bodyweight_missing),
                    color = Gym.Muted, style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            Text(
                stringResource(R.string.tr_add_weight), color = Gym.Yellow, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { showWeight = true },
            )
        }
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
            minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
        )
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (reached == true) Icon(Icons.Filled.Check, stringResource(R.string.tr_goal_reached), tint = Gym.Yellow, modifier = Modifier.size(18.dp))
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

@Composable
private fun NextTimePlanner(item: TrainingItem, plan: PlanDraft, unit: WeightUnit, step: Double, targetOf: (SetRole) -> Int?, vm: TrainingViewModel) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val isReps = ex.progression == Progression.REPS
    DecoCard(corners = true) {
        Eyebrow(stringResource(R.string.tr_next_time), color = Gym.Gold)
        Spacer(Modifier.height(8.dp))
        plan.sets.forEachIndexed { i, p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.width(92.dp)) {
                    Text(roleLabel(p.role).uppercase(), style = MaterialTheme.typography.labelMedium)
                    targetOf(p.role)?.let { Text(stringResource(R.string.tr_target, it), style = MaterialTheme.typography.bodySmall, color = Gym.Muted) }
                }
                if (p.role.isWork && !isReps) {
                    NumberStepper(
                        value = p.weightKg,
                        onValueChange = { v -> vm.editPlan(item.se.id) { d -> d.copy(sets = d.sets.mapIndexed { j, s -> if (j == i) s.copy(weightKg = v) else s }) } },
                        step = step,
                        allowNegative = signed,
                        min = if (signed) null else 0.0,
                        formatter = { WeightFormat.format(it, unit, signed) },
                        parser = unit::toKg,
                        textSize = 22.sp,
                        buttonSize = 44.dp,
                        modifier = Modifier.weight(1f),
                        minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                    )
                } else {
                    Text(if (p.role == SetRole.WARMUP) warmupHint(ex) else stringResource(R.string.tr_to_failure), color = Gym.Muted, modifier = Modifier.weight(1f))
                }
                IconButton(onClick = { vm.editPlan(item.se.id) { d -> d.copy(sets = d.sets.filterIndexed { j, _ -> j != i }) } }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Gym.Muted)
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
                modifier = Modifier.weight(1f),
            )
            LineButton(
                stringResource(R.string.tr_add_warmup),
                onClick = { vm.editPlan(item.se.id) { d -> d.copy(sets = listOf(PlannedSet(SetRole.WARMUP, null)) + d.sets) } },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = item.note,
            onValueChange = { vm.setPlanNote(item.se.id, it) },
            label = { Text(stringResource(R.string.tr_note_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
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
