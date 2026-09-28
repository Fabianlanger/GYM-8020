package nl.ibs.gym8020.ui.schemas

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.room.withTransaction
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SchemaEntity
import nl.ibs.gym8020.data.db.SchemaEntryEntity
import nl.ibs.gym8020.data.db.WeekPlanEntity
import nl.ibs.gym8020.data.db.WeekPlanSlotEntity
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.components.PlainCard
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.exercises.ExercisePickerDialog
import nl.ibs.gym8020.ui.theme.Gym
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SchemaListScreen(container: AppContainer, onOpenSchema: (Long) -> Unit, onOpenWeekPlan: (Long) -> Unit) {
    val plans by container.db.weekPlans().observeAll().collectAsStateWithLifecycle(emptyList())
    val schemas by container.db.schemas().observeActive().collectAsStateWithLifecycle(emptyList())
    val entries by container.db.schemas().observeAllEntries().collectAsStateWithLifecycle(emptyList())
    val counts = entries.groupingBy { it.schemaId }.eachCount()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(stringResource(R.string.weekplans_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
        }
        items(plans, key = { "p${it.id}" }) { p ->
            PlainCard(onClick = { onOpenWeekPlan(p.id) }) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.weekplan_per_week, p.targetPerWeek), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                }
                if (p.active) Eyebrow(stringResource(R.string.weekplan_active), color = Gym.Yellow)
            }
        }
        item { LineButton(stringResource(R.string.weekplan_new), onClick = { onOpenWeekPlan(0L) }, modifier = Modifier.fillMaxWidth()) }
        item { DecoDivider(Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
        item { Text(stringResource(R.string.schemas_title), style = MaterialTheme.typography.headlineMedium) }
        items(schemas, key = { "s${it.id}" }) { s ->
            PlainCard(onClick = { onOpenSchema(s.id) }) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_exercises_count, counts[s.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                }
            }
        }
        item { LineButton(stringResource(R.string.schema_new), onClick = { onOpenSchema(0L) }, modifier = Modifier.fillMaxWidth()) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private data class EntryDraft(val exerciseId: Long, val setCount: Int?)

/** Create/edit/duplicate a schema (D-36). Order via up/down. */
@Composable
fun SchemaEditScreen(container: AppContainer, schemaId: Long, onDone: () -> Unit, onOpenSchema: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val exercises by container.db.exercises().observeAll().collectAsStateWithLifecycle(emptyList())
    val byId = exercises.associateBy { it.id }
    var loaded by remember { mutableStateOf(schemaId == 0L) }
    var original by remember { mutableStateOf<SchemaEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<EntryDraft>>(emptyList()) }
    var picker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val copySuffix = stringResource(R.string.schema_copy_suffix)

    LaunchedEffect(schemaId) {
        if (schemaId != 0L) {
            container.db.schemas().get(schemaId)?.let { s ->
                original = s
                name = s.name
                entries = container.db.schemas().entries(s.id).map { EntryDraft(it.exerciseId, it.setCount) }
            }
            loaded = true
        }
    }
    if (!loaded) return

    suspend fun persist(): Long {
        val db = container.db
        return db.withTransaction {
            val id = original?.let { db.schemas().update(it.copy(name = name.trim())); it.id }
                ?: db.schemas().insert(SchemaEntity(name = name.trim()))
            db.schemas().replaceEntries(id, entries.map { SchemaEntryEntity(schemaId = id, exerciseId = it.exerciseId, position = 0, setCount = it.setCount) })
            id
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(if (original == null) R.string.schema_new else R.string.schema_edit), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.schema_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        entries.forEachIndexed { i, entry ->
            val ex: ExerciseEntity? = byId[entry.exerciseId]
            Row(
                Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. ${ex?.name ?: "?"}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.schema_sets, entry.setCount ?: 0).takeIf { entry.setCount != null } ?: stringResource(R.string.schema_sets_default),
                        style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                        modifier = Modifier.clickable {
                            val n = entry.setCount
                            val next = when { n == null -> 1; n < 6 -> n + 1; else -> null }
                            entries = entries.toMutableList().also { it[i] = entry.copy(setCount = next) }
                        },
                    )
                }
                IconButton(onClick = { if (i > 0) entries = entries.toMutableList().also { it.add(i - 1, it.removeAt(i)) } }) {
                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up))
                }
                IconButton(onClick = { if (i < entries.lastIndex) entries = entries.toMutableList().also { it.add(i + 1, it.removeAt(i)) } }) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down))
                }
                IconButton(onClick = { entries = entries.filterIndexed { j, _ -> j != i } }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Gym.Muted)
                }
            }
        }
        LineButton(stringResource(R.string.tr_add_exercise), onClick = { picker = true }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        GoButton(stringResource(R.string.action_save), onClick = { scope.launch { persist(); onDone() } }, enabled = name.isNotBlank(), height = 56.dp)
        original?.let { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineButton(stringResource(R.string.schema_duplicate), onClick = {
                    scope.launch {
                        val db = container.db
                        val newId = db.withTransaction {
                            val id = db.schemas().insert(SchemaEntity(name = String.format(copySuffix, name.trim())))
                            db.schemas().replaceEntries(id, entries.map { SchemaEntryEntity(schemaId = id, exerciseId = it.exerciseId, position = 0, setCount = it.setCount) })
                            id
                        }
                        onOpenSchema(newId)
                    }
                }, modifier = Modifier.weight(1f))
                LineButton(stringResource(R.string.action_delete), onClick = { confirmDelete = true }, modifier = Modifier.weight(1f))
            }
            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text(stringResource(R.string.schema_delete_confirm, s.name)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDelete = false
                            // Archive: past trainings keep their name; week plan slots are removed.
                            scope.launch {
                                val db = container.db
                                db.withTransaction {
                                    db.schemas().update(s.copy(archived = true))
                                    db.weekPlans().allSlots().filter { it.schemaId == s.id }.groupBy { it.weekPlanId }.forEach { (planId, _) ->
                                        db.weekPlans().replaceSlots(planId, db.weekPlans().allSlots().filter { it.weekPlanId == planId && it.schemaId != s.id })
                                    }
                                }
                                onDone()
                            }
                        }) { Text(stringResource(R.string.action_delete)) }
                    },
                    dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (picker) {
        ExercisePickerDialog(exercises, onPick = { entries = entries + EntryDraft(it.id, null); picker = false }, onDismiss = { picker = false })
    }
}

private data class SlotDraft(val schemaId: Long, val day: Int?)

@Composable
fun dayName(day: Int?): String =
    day?.let { DayOfWeek.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { c -> c.uppercase() } }
        ?: stringResource(R.string.day_none)

/** Week plan: target per week, schemas with optional weekday (D-61). */
@Composable
fun WeekPlanEditScreen(container: AppContainer, planId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val schemas by container.db.schemas().observeActive().collectAsStateWithLifecycle(emptyList())
    val byId = schemas.associateBy { it.id }
    var loaded by remember { mutableStateOf(planId == 0L) }
    var original by remember { mutableStateOf<WeekPlanEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<Double?>(3.0) }
    var slots by remember { mutableStateOf<List<SlotDraft>>(emptyList()) }
    var picker by remember { mutableStateOf(false) }

    LaunchedEffect(planId) {
        if (planId != 0L) {
            container.db.weekPlans().get(planId)?.let { p ->
                original = p
                name = p.name
                target = p.targetPerWeek.toDouble()
                slots = container.db.weekPlans().allSlots().filter { it.weekPlanId == p.id }.map { SlotDraft(it.schemaId, it.day) }
            }
            loaded = true
        }
    }
    if (!loaded) return

    suspend fun persist(activate: Boolean) {
        val db = container.db
        db.withTransaction {
            val t = (target ?: 1.0).toInt().coerceIn(1, 7)
            val id = original?.let { db.weekPlans().update(it.copy(name = name.trim(), targetPerWeek = t)); it.id }
                ?: db.weekPlans().insert(WeekPlanEntity(name = name.trim(), targetPerWeek = t))
            db.weekPlans().replaceSlots(id, slots.map { WeekPlanSlotEntity(weekPlanId = id, schemaId = it.schemaId, position = 0, day = it.day) })
            if (activate || db.weekPlans().all().none { it.active }) db.weekPlans().activate(id)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(if (original == null) R.string.weekplan_new else R.string.weekplan_edit), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.schema_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Eyebrow(stringResource(R.string.weekplan_target))
        NumberStepper(target, { target = it }, step = 1.0, decimals = false, min = 1.0, formatter = { it.toInt().toString() }, textSize = 22.sp, buttonSize = 48.dp)
        Eyebrow(stringResource(R.string.schemas_title))
        slots.forEachIndexed { i, slot ->
            var dayMenu by remember(i, slot) { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(byId[slot.schemaId]?.name ?: "?", style = MaterialTheme.typography.bodyLarge)
                    Box {
                        Text(dayName(slot.day), color = Gym.Yellow, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { dayMenu = true }.padding(vertical = 4.dp))
                        DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                            (listOf<Int?>(null) + (1..7)).forEach { d ->
                                DropdownMenuItem(text = { Text(dayName(d)) }, onClick = {
                                    dayMenu = false
                                    slots = slots.toMutableList().also { it[i] = slot.copy(day = d) }
                                })
                            }
                        }
                    }
                }
                IconButton(onClick = { if (i > 0) slots = slots.toMutableList().also { it.add(i - 1, it.removeAt(i)) } }) {
                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up))
                }
                IconButton(onClick = { if (i < slots.lastIndex) slots = slots.toMutableList().also { it.add(i + 1, it.removeAt(i)) } }) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down))
                }
                IconButton(onClick = { slots = slots.filterIndexed { j, _ -> j != i } }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Gym.Muted)
                }
            }
        }
        LineButton(stringResource(R.string.weekplan_add_schema), onClick = { picker = true }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        GoButton(stringResource(R.string.action_save), onClick = { scope.launch { persist(false); onDone() } }, enabled = name.isNotBlank(), height = 56.dp)
        if (original?.active != true) {
            LineButton(stringResource(R.string.weekplan_activate), onClick = { scope.launch { persist(true); onDone() } }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
        }
        original?.let { p ->
            LineButton(stringResource(R.string.action_delete), onClick = {
                scope.launch {
                    container.db.weekPlans().delete(p.id)
                    if (p.active) container.db.weekPlans().all().firstOrNull()?.let { container.db.weekPlans().activate(it.id) }
                    onDone()
                }
            }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
    }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text(stringResource(R.string.weekplan_add_schema)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(schemas, key = { it.id }) { s ->
                        Text(s.name, modifier = Modifier.fillMaxWidth().clickable { slots = slots + SlotDraft(s.id, null); picker = false }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
