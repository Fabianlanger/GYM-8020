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
import androidx.compose.runtime.rememberCoroutineScope
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
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.Calculators
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

data class EditItem(val se: SessionExerciseEntity, val exercise: ExerciseEntity, val sets: List<SetEntity>, val calc: WeightCalculator?)

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
    /** Exercises whose sets were changed here, for the "recalculate goal" question. */
    private val changed = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<EditState> = combine(
        c.db.sessions().observe(sessionId),
        c.db.sessions().observeExercises(sessionId),
        c.db.sessions().observeSets(sessionId),
        c.db.exercises().observeAll(),
        combine(c.settings.settings, c.db.materials().observeAll()) { s, m -> s to m },
    ) { session, ses, sets, exercises, (settings, materials) ->
        val byId = exercises.associateBy { it.id }
        val profiles = materials.associateBy { it.id }
        val bySe = sets.groupBy { it.sessionExerciseId }
        val items = ses.mapNotNull { se ->
            val ex = byId[se.exerciseId] ?: return@mapNotNull null
            EditItem(se, ex, bySe[se.id].orEmpty(), Calculators.forExercise(ex, profiles, settings.plateStock))
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
    fun updateTimes(startedAt: Long, endedAt: Long) = viewModelScope.launch { repo.updateTimes(sessionId, startedAt, endedAt) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.discard(sessionId); c.autoBackup.runSoon(); onDone() }

    /** Changed exercises for which this training is the latest one, so their goal comes from here. */
    suspend fun goalCandidates(): List<ExerciseEntity> =
        state.value.items.map { it.exercise }.distinctBy { it.id }
            .filter { it.id in changed.value && repo.isLatestFor(it.id, sessionId) }

    fun recompute(exercises: List<ExerciseEntity>, onDone: () -> Unit) = viewModelScope.launch {
        exercises.forEach { repo.recomputeTarget(it, sessionId) }
        c.autoBackup.runSoon()
        onDone()
    }
}

@Composable
fun SessionEditScreen(container: AppContainer, sessionId: Long, onDone: () -> Unit, onDeleted: () -> Unit) {
    val vm: SessionEditViewModel = viewModel(key = "edit-$sessionId") { SessionEditViewModel(container, sessionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val session = state.session
    val unit = state.settings.unit
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<EditItem?>(null) }
    var goalQuestion by remember { mutableStateOf<List<ExerciseEntity>>(emptyList()) }

    fun finish() = scope.launch {
        container.autoBackup.runSoon()
        val candidates = vm.goalCandidates()
        if (candidates.isEmpty()) onDone() else goalQuestion = candidates
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
    if (goalQuestion.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { goalQuestion = emptyList(); onDone() },
            title = { Text(stringResource(R.string.log_goal_title)) },
            text = { Text(stringResource(R.string.log_goal_body, goalQuestion.joinToString(", ") { it.name })) },
            confirmButton = { TextButton(onClick = { val list = goalQuestion; goalQuestion = emptyList(); vm.recompute(list, onDone) }) { Text(stringResource(R.string.log_goal_yes)) } },
            dismissButton = { TextButton(onClick = { goalQuestion = emptyList(); onDone() }) { Text(stringResource(R.string.log_goal_no)) } },
        )
    }
}

@Composable
private fun ExerciseBlock(item: EditItem, unit: WeightUnit, step: Double, vm: SessionEditViewModel, onRemove: () -> Unit) {
    val ex = item.exercise
    val signed = ex.loadType == LoadType.BODYWEIGHT
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
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            LineButton(stringResource(R.string.tr_add_set), onClick = { vm.addSet(item, SetRole.HEAVY) }, icon = Icons.Filled.Add, modifier = Modifier.weight(1f))
            LineButton(stringResource(R.string.tr_add_warmup), onClick = { vm.addSet(item, SetRole.WARMUP) }, icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.weight(1f))
        }
    }
}

private fun nextRole(role: SetRole): SetRole = when (role) {
    SetRole.HEAVY -> SetRole.LIGHT
    SetRole.LIGHT -> SetRole.WARMUP
    SetRole.WARMUP -> SetRole.HEAVY
}
