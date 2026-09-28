package nl.ibs.gym8020.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleGroup
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.ExerciseMuscleEntity
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.components.PlainCard
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.MovementIcon
import nl.ibs.gym8020.ui.settings.kindName
import nl.ibs.gym8020.ui.settings.profileSummary
import nl.ibs.gym8020.data.movementOrDefault
import nl.ibs.gym8020.core.logic.WeightUnit
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import nl.ibs.gym8020.ui.theme.Gym

@Composable
fun muscleName(m: Muscle): String = stringResource(
    when (m) {
        Muscle.CHEST -> R.string.muscle_chest
        Muscle.FRONT_DELT -> R.string.muscle_front_delt
        Muscle.SIDE_DELT -> R.string.muscle_side_delt
        Muscle.REAR_DELT -> R.string.muscle_rear_delt
        Muscle.LATS -> R.string.muscle_lats
        Muscle.TRAPS -> R.string.muscle_traps
        Muscle.MID_BACK -> R.string.muscle_mid_back
        Muscle.LOWER_BACK -> R.string.muscle_lower_back
        Muscle.BICEPS -> R.string.muscle_biceps
        Muscle.TRICEPS -> R.string.muscle_triceps
        Muscle.FOREARMS -> R.string.muscle_forearms
        Muscle.ABS -> R.string.muscle_abs
        Muscle.OBLIQUES -> R.string.muscle_obliques
        Muscle.QUADS -> R.string.muscle_quads
        Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
        Muscle.GLUTES -> R.string.muscle_glutes
        Muscle.ADDUCTORS -> R.string.muscle_adductors
        Muscle.CALVES -> R.string.muscle_calves
    },
)

@Composable
fun groupName(g: MuscleGroup): String = stringResource(
    when (g) {
        MuscleGroup.CHEST -> R.string.group_chest
        MuscleGroup.SHOULDERS -> R.string.group_shoulders
        MuscleGroup.BACK -> R.string.group_back
        MuscleGroup.ARMS -> R.string.group_arms
        MuscleGroup.CORE -> R.string.group_core
        MuscleGroup.LEGS -> R.string.group_legs
    },
)

@Composable
fun equipmentName(e: Equipment): String = stringResource(
    when (e) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    },
)

@Composable
fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.exercises_search)) },
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
fun ExercisePickerDialog(exercises: List<ExerciseEntity>, onPick: (ExerciseEntity) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tr_add_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(query, { query = it })
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(exercises.filter { !it.archived && it.name.contains(query.trim(), ignoreCase = true) }, key = { it.id }) { ex ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExerciseIcon(ex, size = 36)
                            Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun ExerciseListScreen(container: AppContainer, onOpen: (Long) -> Unit) {
    val exercises by container.db.exercises().observeAll().collectAsStateWithLifecycle(emptyList())
    val muscles by container.db.exercises().observeAllMuscles().collectAsStateWithLifecycle(emptyList())
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<MuscleGroup?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    val primaryGroups = remember(muscles) {
        muscles.filter { it.percent == 100 }.groupBy({ it.exerciseId }, { it.muscle.group })
    }
    val shown = exercises.filter { ex ->
        (showArchived || !ex.archived) &&
            ex.name.contains(query.trim(), ignoreCase = true) &&
            (group == null || primaryGroups[ex.id].orEmpty().contains(group))
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.nav_exercises), style = MaterialTheme.typography.headlineMedium)
                    SearchField(query, { query = it })
                    GroupChips(group) { group = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.show_archived), modifier = Modifier.weight(1f), color = Gym.Muted)
                        Switch(checked = showArchived, onCheckedChange = { showArchived = it })
                    }
                }
            }
            items(shown, key = { it.id }) { ex ->
                PlainCard(onClick = { onOpen(ex.id) }) {
                    ExerciseIcon(ex)
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.titleMedium, color = if (ex.archived) Gym.Muted else Gym.Cream)
                        Text(
                            equipmentName(ex.equipment) + (if (ex.archived) " · " + stringResource(R.string.exercise_archived) else ""),
                            style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
        FloatingActionButton(
            onClick = { onOpen(0L) },
            containerColor = Gym.Yellow,
            contentColor = Gym.OnYellow,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
        ) { Icon(Icons.Filled.Add, stringResource(R.string.exercise_new)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChips(selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MuscleGroup.entries.forEach { g ->
            FilterChip(
                selected = selected == g,
                onClick = { onSelect(if (selected == g) null else g) },
                label = { Text(groupName(g)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gym.Yellow, selectedLabelColor = Gym.OnYellow),
            )
        }
    }
}

/** Create or edit an exercise (D-13). Exercises with history are archived, never deleted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditScreen(container: AppContainer, exerciseId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(exerciseId == 0L) }
    var original by remember { mutableStateOf<ExerciseEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf(Equipment.MACHINE) }
    var loadType by remember { mutableStateOf(LoadType.EXTERNAL) }
    var progression by remember { mutableStateOf(Progression.WEIGHT) }
    var factor by remember { mutableStateOf<Double?>(1.0) }
    var heavy by remember { mutableStateOf<Double?>(null) }
    var light by remember { mutableStateOf<Double?>(null) }
    var shares by remember { mutableStateOf<Map<Muscle, Int>>(emptyMap()) }
    var hasHistory by remember { mutableStateOf(false) }
    var materialId by remember { mutableStateOf<Long?>(null) }
    var movement by remember { mutableStateOf<Movement?>(null) }
    val profiles by container.db.materials().observeActive().collectAsStateWithLifecycle(emptyList())
    val unit by container.settings.settings.map { it.unit }.collectAsStateWithLifecycle(WeightUnit.KG)

    LaunchedEffect(exerciseId) {
        if (exerciseId != 0L) {
            val e = container.db.exercises().get(exerciseId)
            if (e != null) {
                original = e
                materialId = e.materialProfileId
                movement = e.movementOrDefault
                name = e.name; equipment = e.equipment; loadType = e.loadType; progression = e.progression
                factor = e.bodyweightFactor; heavy = e.heavyTarget?.toDouble(); light = e.lightTarget?.toDouble()
                shares = container.db.exercises().muscles(e.id).associate { it.muscle to it.percent }
                hasHistory = container.db.exercises().historyCount(e.id) > 0
            }
            loaded = true
        }
    }
    if (!loaded) return

    fun save() = scope.launch {
        val entity = (original ?: ExerciseEntity(name = name, equipment = equipment, loadType = loadType, progression = progression)).copy(
            name = name.trim(), equipment = equipment, loadType = loadType, progression = progression,
            bodyweightFactor = (factor ?: 1.0).coerceIn(0.0, 1.5),
            heavyTarget = heavy?.toInt()?.takeIf { it > 0 }, lightTarget = light?.toInt()?.takeIf { it > 0 },
            materialProfileId = materialId, movement = movement,
        )
        val id = if (original == null) container.db.exercises().insert(entity) else entity.id.also { container.db.exercises().update(entity) }
        container.db.exercises().replaceMuscles(id, shares.map { (m, p) -> ExerciseMuscleEntity(id, m, p) })
        onDone()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(if (original == null) R.string.exercise_new else R.string.exercise_edit), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.schema_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Eyebrow(stringResource(R.string.exercise_icon))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Movement.entries.forEach { m ->
                val selected = movement == m
                Box(
                    Modifier.size(52.dp)
                        .background(if (selected) Gym.SurfaceHigh else Gym.Surface, RoundedCornerShape(12.dp))
                        .border(if (selected) 2.dp else 0.dp, if (selected) Gym.Yellow else Gym.Surface, RoundedCornerShape(12.dp))
                        .clickable { movement = m }
                        .padding(6.dp),
                ) { MovementIcon(m, Modifier.fillMaxSize()) }
            }
        }

        Eyebrow(stringResource(R.string.exercise_material))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(materialId == null, { materialId = null }, label = { Text(stringResource(R.string.exercise_material_none)) })
            profiles.forEach { p -> FilterChip(materialId == p.id, { materialId = p.id }, label = { Text(p.name) }) }
        }
        profiles.firstOrNull { it.id == materialId }?.let { p ->
            Text(kindName(p.kind) + " · " + profileSummary(p, unit), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        }

        Eyebrow(stringResource(R.string.exercise_equipment))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Equipment.entries.forEach { e ->
                FilterChip(
                    selected = equipment == e,
                    onClick = {
                        equipment = e
                        if (e == Equipment.BODYWEIGHT) loadType = LoadType.BODYWEIGHT
                    },
                    label = { Text(equipmentName(e)) },
                )
            }
        }
        Eyebrow(stringResource(R.string.exercise_load))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(loadType == LoadType.EXTERNAL, { loadType = LoadType.EXTERNAL }, label = { Text(stringResource(R.string.load_external)) })
            FilterChip(loadType == LoadType.BODYWEIGHT, { loadType = LoadType.BODYWEIGHT }, label = { Text(stringResource(R.string.load_bodyweight)) })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(progression == Progression.WEIGHT, { progression = Progression.WEIGHT }, label = { Text(stringResource(R.string.progression_weight)) })
            FilterChip(progression == Progression.REPS, { progression = Progression.REPS }, label = { Text(stringResource(R.string.progression_reps)) })
        }
        if (loadType == LoadType.BODYWEIGHT) {
            Eyebrow(stringResource(R.string.exercise_factor))
            NumberStepper(factor, { factor = it }, step = 0.05, min = 0.0, formatter = { "%.2f".format(it).replace('.', ',') })
        }
        Eyebrow(stringResource(R.string.exercise_targets))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.role_heavy), color = Gym.Muted)
                NumberStepper(heavy, { heavy = it }, step = 1.0, decimals = false, min = 0.0, formatter = { it.toInt().toString() }, buttonSize = 44.dp, textSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.role_light), color = Gym.Muted)
                NumberStepper(light, { light = it }, step = 1.0, decimals = false, min = 0.0, formatter = { it.toInt().toString() }, buttonSize = 44.dp, textSize = 22.sp)
            }
        }

        DecoDivider()
        Eyebrow(stringResource(R.string.exercise_muscles))
        Text(stringResource(R.string.exercise_muscles_help), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        MuscleGroup.entries.forEach { g ->
            Text(groupName(g), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Muscle.entries.filter { it.group == g }.forEach { m ->
                    val p = shares[m]
                    FilterChip(
                        selected = p != null,
                        onClick = {
                            // Cycle: none → primary (100) → secondary (50) → none
                            shares = when (p) {
                                null -> shares + (m to 100)
                                100 -> shares + (m to 50)
                                else -> shares - m
                            }
                        },
                        label = {
                            Text(
                                muscleName(m) + when (p) {
                                    100 -> " · " + stringResource(R.string.muscle_primary)
                                    null -> ""
                                    else -> " · " + stringResource(R.string.muscle_secondary)
                                },
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (p == 100) Gym.Yellow else Gym.SurfaceHigh,
                            selectedLabelColor = if (p == 100) Gym.OnYellow else Gym.Cream,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        GoButton(stringResource(R.string.action_save), onClick = { save() }, enabled = name.isNotBlank(), height = 56.dp)
        original?.let { e ->
            if (e.archived) {
                LineButton(stringResource(R.string.exercise_unarchive), onClick = {
                    scope.launch { container.db.exercises().update(e.copy(archived = false)); onDone() }
                }, modifier = Modifier.fillMaxWidth())
            } else {
                LineButton(stringResource(if (hasHistory || e.libraryKey != null) R.string.exercise_archive else R.string.action_delete), onClick = {
                    scope.launch {
                        if (hasHistory || e.libraryKey != null) container.db.exercises().update(e.copy(archived = true))
                        else container.db.exercises().delete(e.id)
                        onDone()
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The exercise's movement pictogram on a small dark tile. */
@Composable
fun ExerciseIcon(ex: ExerciseEntity, size: Int = 44) {
    Box(Modifier.size(size.dp).background(Gym.SurfaceHigh, RoundedCornerShape((size / 4).dp)).padding((size / 9).dp)) {
        MovementIcon(ex.movementOrDefault, Modifier.fillMaxSize(), figure = if (ex.archived) Gym.Muted else Gym.Cream)
    }
}
