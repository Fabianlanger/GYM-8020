package nl.ibs.gym8020.ui.log

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.RepsTarget
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.Calculators
import nl.ibs.gym8020.data.Target
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.deco.FlourishDivider
import nl.ibs.gym8020.ui.exercises.ExerciseIcon
import nl.ibs.gym8020.ui.exercises.ExercisePickerDialog
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.training.RoleBadge
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Doel volgende keer" (#64), only in your latest training with the exercise: the [sets] as they are now, and
 * [was] the sets before your first change in this screen visit (null while you changed nothing).
 */
data class NextTarget(val sets: List<PlannedSet>, val note: String?, val was: List<PlannedSet>? = null)

data class EditItem(
    val se: SessionExerciseEntity,
    val exercise: ExerciseEntity,
    val sets: List<SetEntity>,
    val calc: WeightCalculator?,
    val next: NextTarget? = null,
)

data class EditState(
    val session: SessionEntity? = null,
    val items: List<EditItem> = emptyList(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
)

/** Correcting a finished training. Every change is saved right away, like during the training. */
class SessionEditViewModel(private val c: AppContainer, private val sessionId: Long) : ViewModel() {
    private val repo = c.training
    /** Exercises whose sets were changed here: on saving, their default target follows the correction (#64). */
    private val changed = MutableStateFlow<Set<Long>>(emptySet())
    /** Per session exercise the target you set here (shown at once) and the target before your first change. */
    private val ownEdits = MutableStateFlow<Map<Long, NextTarget>>(emptyMap())

    val state: StateFlow<EditState> = combine(
        c.db.sessions().observe(sessionId),
        c.db.sessions().observeExercises(sessionId),
        c.db.sessions().observeSets(sessionId),
        c.db.exercises().observeAll(),
        combine(c.settings.settings, c.db.materials().observeAll(), ownEdits) { s, m, e -> Triple(s, m, e) },
    ) { session, ses, sets, exercises, (settings, materials, edits) ->
        val byId = exercises.associateBy { it.id }
        val profiles = materials.associateBy { it.id }
        val bySe = sets.groupBy { it.sessionExerciseId }
        val items = ses.mapNotNull { se ->
            val ex = byId[se.exerciseId] ?: return@mapNotNull null
            // Asked again on every change, so the default target follows your corrected performance at once.
            val next = edits[se.id] ?: if (se.skipped || session?.endedAt == null) null else repo.nextTargetIn(sessionId, ex)?.let { NextTarget(it.sets, it.note) }
            EditItem(se, ex, bySe[se.id].orEmpty(), Calculators.forExercise(ex, profiles, settings.plateStock), next)
        }
        EditState(session, items, exercises.filter { !it.archived }, settings, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditState())

    private fun touched(exerciseId: Long) = changed.update { it + exerciseId }

    fun updateSet(item: EditItem, set: SetEntity) = viewModelScope.launch { repo.updateSet(set); touched(item.exercise.id) }
    fun addSet(item: EditItem, role: SetRole) = viewModelScope.launch { repo.addSet(item.se.id, role); touched(item.exercise.id) }
    fun deleteSet(item: EditItem, set: SetEntity) = viewModelScope.launch { repo.deleteSet(set.id); touched(item.exercise.id) }
    fun setSkipped(item: EditItem, skipped: Boolean) = viewModelScope.launch { repo.setSkipped(item.se.id, skipped); touched(item.exercise.id) }
    fun removeExercise(item: EditItem) = viewModelScope.launch { repo.removeExercise(item.se.id); touched(item.exercise.id) }
    fun addExercise(ex: ExerciseEntity) = viewModelScope.launch { repo.addExercise(sessionId, ex.id, withGoals = false); touched(ex.id) }
    /** A new date can make this the latest training for its exercises, so all of them count as changed (#9). */
    fun updateTimes(startedAt: Long, endedAt: Long) = viewModelScope.launch {
        repo.updateTimes(sessionId, startedAt, endedAt)
        changed.update { it + state.value.items.map { i -> i.exercise.id } }
    }
    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        changed.value = emptySet()
        repo.discard(sessionId)
        c.autoBackup.runSoon()
        onDone()
    }

    /** The session target of a set (what you aimed at that day): a correction like any other. */
    fun updateSessionTarget(set: SetEntity, kg: Double?) = viewModelScope.launch { repo.updateSet(set.copy(goalWeightKg = kg)) }

    /** You change the weight of target set [index] for next time: stored right away as your own target (#64). */
    fun editNextTarget(item: EditItem, index: Int, kg: Double?) {
        val current = item.next ?: return
        val sets = current.sets.mapIndexed { j, s -> if (j == index) s.copy(weightKg = kg) else s }
        ownEdits.update { it + (item.se.id to current.copy(sets = sets, was = current.was ?: current.sets)) }
        viewModelScope.launch { repo.setTarget(item.se.id, item.exercise.id, Target(sets, current.note)) }
    }

    /**
     * Saving (#64): the default target of each changed exercise follows the correction when this is the latest
     * training with it. Runs in the app scope, so leaving the screen does not cut it off.
     */
    fun save() {
        val exerciseIds = changed.value
        changed.value = emptySet()
        c.appScope.launch {
            if (exerciseIds.isNotEmpty()) repo.applyCorrection(sessionId, exerciseIds)
            c.autoBackup.runSoon()
        }
    }

    /** Leaving with the back button saves as well. */
    override fun onCleared() {
        if (changed.value.isNotEmpty()) save()
    }
}

@Composable
fun SessionEditScreen(container: AppContainer, sessionId: Long, onDone: () -> Unit, onDeleted: () -> Unit) {
    val vm: SessionEditViewModel = viewModel(key = "edit-$sessionId") { SessionEditViewModel(container, sessionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val session = state.session
    val unit = state.settings.unit
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<EditItem?>(null) }

    fun finish() {
        vm.save()
        onDone()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow(stringResource(R.string.log_edit_title))
        Text(session?.schemaName ?: "", style = MaterialTheme.typography.headlineMedium)
        if (session != null) {
            val zone = ZoneId.systemDefault()
            val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(session.startedAt), zone)
            val durationMin = (((session.endedAt ?: session.startedAt) - session.startedAt) / 60_000L).toInt()
            fun save(newStart: LocalDateTime, minutes: Int) {
                val ms = newStart.atZone(zone).toInstant().toEpochMilli()
                vm.updateTimes(ms, ms + minutes.coerceAtLeast(1) * 60_000L)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineButton(
                    start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    onClick = {
                        DatePickerDialog(context, { _, y, m, d -> save(start.withYear(y).withMonth(m + 1).withDayOfMonth(d), durationMin) }, start.year, start.monthValue - 1, start.dayOfMonth).show()
                    },
                    modifier = Modifier.weight(1.3f),
                )
                LineButton(
                    start.format(DateTimeFormatter.ofPattern("HH:mm")),
                    onClick = {
                        TimePickerDialog(context, { _, h, min -> save(start.withHour(h).withMinute(min), durationMin) }, start.hour, start.minute, true).show()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Eyebrow(stringResource(R.string.log_duration_min))
            NumberStepper(
                value = durationMin.toDouble(),
                onValueChange = { v -> v?.toInt()?.takeIf { it >= 1 }?.let { save(start, it) } },
                step = 5.0, decimals = false, min = 1.0,
                formatter = { it.toInt().toString() }, textSize = 22.sp, buttonSize = 44.dp,
            )
        }
        FlourishDivider()

        state.items.forEach { item ->
            key(item.se.id) { ExerciseBlock(item, unit, state.settings.method.weightStepKg, vm, onRemove = { removing = item }) }
        }

        LineButton(stringResource(R.string.tr_add_exercise), onClick = { picking = true }, icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        GoButton(stringResource(R.string.action_done), onClick = { finish() })
        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Gym.Negative, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.log_delete_training), color = Gym.Negative)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (picking) {
        ExercisePickerDialog(state.allExercises, onPick = { vm.addExercise(it); picking = false }, onDismiss = { picking = false })
    }
    removing?.let { item ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(item.exercise.name) },
            text = { Text(stringResource(R.string.log_remove_exercise_confirm)) },
            confirmButton = { TextButton(onClick = { vm.removeExercise(item); removing = null }) { Text(stringResource(R.string.remove), color = Gym.Negative) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.log_delete_training)) },
            text = { Text(stringResource(R.string.log_delete_confirm)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onDeleted) }) { Text(stringResource(R.string.action_delete), color = Gym.Negative) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ExerciseBlock(item: EditItem, unit: WeightUnit, step: Double, vm: SessionEditViewModel, onRemove: () -> Unit) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val isReps = ex.progression == Progression.REPS
    DecoCard(Modifier.alpha(if (item.se.skipped) .55f else 1f), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExerciseIcon(ex, size = 36)
            Text(ex.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Gym.Muted, modifier = Modifier.size(20.dp))
            }
        }
        if (item.se.skipped) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.log_skipped), color = Gym.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.setSkipped(item, false) }) { Text(stringResource(R.string.log_unskip), color = Gym.Yellow) }
            }
            return@DecoCard
        }
        item.sets.forEach { set ->
            key(set.id) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp).background(Gym.Bg.copy(alpha = .35f), RoundedCornerShape(14.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Tap the badge to switch: heavy → light → warm-up.
                        Box(Modifier.clickable { vm.updateSet(item, set.copy(role = nextRole(set.role))) }) { RoleBadge(set.role) }
                        Text(
                            stringResource(R.string.log_tap_role), color = Gym.Muted, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.deleteSet(item, set) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, stringResource(R.string.tr_set_delete), tint = Gym.Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (set.role != SetRole.WARMUP) {
                            Column(Modifier.weight(1.15f)) {
                                Eyebrow(stringResource(if (signed) R.string.tr_extra_short else R.string.tr_weight))
                                NumberStepper(
                                    value = set.weightKg,
                                    onValueChange = { vm.updateSet(item, set.copy(weightKg = it)) },
                                    step = step, allowNegative = signed, min = if (signed) null else 0.0,
                                    next = item.calc?.let { c -> { v: Double -> c.up(v) } },
                                    previous = item.calc?.let { c -> { v: Double -> c.down(v) } },
                                    formatter = { WeightFormat.format(it, unit, signed) }, parser = unit::toKg,
                                    textSize = 18.sp, buttonSize = 38.dp,
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Eyebrow(stringResource(R.string.tr_reps))
                            NumberStepper(
                                value = set.reps?.toDouble(),
                                onValueChange = { vm.updateSet(item, set.copy(reps = it?.toInt()?.coerceAtLeast(0))) },
                                step = 1.0, decimals = false, min = 0.0,
                                formatter = { it.toInt().toString() }, textSize = 18.sp, buttonSize = 38.dp,
                            )
                        }
                    }
                    // Reps in the session target wait for schema v3.
                    if (set.role.isWork && !isReps) SessionTarget(item, set, unit, step, vm)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            LineButton(stringResource(R.string.tr_add_set), onClick = { vm.addSet(item, SetRole.HEAVY) }, icon = Icons.Filled.Add, modifier = Modifier.weight(1f))
            LineButton(stringResource(R.string.tr_add_warmup), onClick = { vm.addSet(item, SetRole.WARMUP) }, icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.weight(1f))
        }
        item.next?.let { next -> NextTargetEditor(item, next, unit, step, vm) }
    }
}

/** The session target (#64): editable, but not inviting. Small grey text; a tap reveals the stepper. */
@Composable
private fun SessionTarget(item: EditItem, set: SetEntity, unit: WeightUnit, step: Double, vm: SessionEditViewModel) {
    val signed = item.exercise.loadType == LoadType.BODYWEIGHT
    var open by remember(set.id) { mutableStateOf(false) }
    val goal = set.goalWeightKg
    Text(
        if (goal != null) stringResource(R.string.log_session_target, WeightFormat.format(goal, unit, signed)) else stringResource(R.string.log_session_target_none),
        color = Gym.Muted, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.clickable { open = !open }.padding(vertical = 2.dp),
    )
    if (open) {
        NumberStepper(
            value = goal,
            onValueChange = { vm.updateSessionTarget(set, it) },
            step = step, allowNegative = signed, min = if (signed) null else 0.0,
            next = item.calc?.let { c -> { v: Double -> c.up(v) } },
            previous = item.calc?.let { c -> { v: Double -> c.down(v) } },
            formatter = { WeightFormat.format(it, unit, signed) }, parser = unit::toKg,
            textSize = 14.sp, buttonSize = 32.dp,
        )
    }
}

/**
 * "Doel volgende keer" (#64), only in your latest training with the exercise: as visible as the performance.
 * Changing a weight makes it your own target; "was 80 kg" shows the weight before your first change here.
 * With reps progression the target is reps, read-only as in the training screen.
 */
@Composable
private fun NextTargetEditor(item: EditItem, next: NextTarget, unit: WeightUnit, step: Double, vm: SessionEditViewModel) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
    val isReps = ex.progression == Progression.REPS
    val repsTargets = if (isReps) RepsTarget.forSets(next.sets.map { it.role }, item.sets.map { PerformedSet(it.role, it.weightKg, it.reps) }) else null
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp).background(Gym.Bg.copy(alpha = .35f), RoundedCornerShape(14.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow(stringResource(R.string.log_next_target), color = Gym.Gold)
        next.sets.forEachIndexed { i, p ->
            if (!p.role.isWork) return@forEachIndexed
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleBadge(p.role)
                if (isReps) {
                    Text(
                        repsTargets?.getOrNull(i)?.let { stringResource(R.string.tr_target, it) } ?: stringResource(R.string.tr_to_failure),
                        color = Gym.Muted, modifier = Modifier.weight(1f),
                    )
                } else {
                    Column(Modifier.weight(1f)) {
                        NumberStepper(
                            value = p.weightKg,
                            onValueChange = { vm.editNextTarget(item, i, it) },
                            step = step, allowNegative = signed, min = if (signed) null else 0.0,
                            next = item.calc?.let { c -> { v: Double -> c.up(v) } },
                            previous = item.calc?.let { c -> { v: Double -> c.down(v) } },
                            formatter = { WeightFormat.format(it, unit, signed) }, parser = unit::toKg,
                            textSize = 18.sp, buttonSize = 38.dp,
                        )
                        val was = next.was?.getOrNull(i)
                        if (was != null && was.weightKg != p.weightKg) {
                            Text(
                                stringResource(R.string.log_was, was.weightKg?.let { WeightFormat.format(it, unit, signed) } ?: "–"),
                                color = Gym.Muted, style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun nextRole(role: SetRole): SetRole = when (role) {
    SetRole.HEAVY -> SetRole.LIGHT
    SetRole.LIGHT -> SetRole.WARMUP
    SetRole.WARMUP -> SetRole.HEAVY
}
