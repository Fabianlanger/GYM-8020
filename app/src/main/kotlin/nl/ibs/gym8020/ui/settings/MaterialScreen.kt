package nl.ibs.gym8020.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import nl.ibs.gym8020.data.displayName
import nl.ibs.gym8020.ui.components.appIsDutch
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.components.PlainCard
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.theme.Gym

/** Plate sizes offered in the editor; a count of 0 means the gym doesn't have them. */
private val commonPlates = listOf(50.0, 25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 2.0, 1.25, 1.0, 0.5)

@Composable
fun kindName(kind: MaterialKind): String = stringResource(
    when (kind) {
        MaterialKind.PLATES -> R.string.kind_plates
        MaterialKind.STACK -> R.string.kind_stack
        MaterialKind.DUMBBELLS -> R.string.kind_dumbbells
        MaterialKind.BODYWEIGHT -> R.string.kind_bodyweight
    },
)

@Composable
fun profileSummary(m: MaterialProfileEntity, unit: WeightUnit): String {
    fun w(kg: Double) = WeightFormat.format(kg, unit) + " " + unit.name.lowercase()
    return when (m.kind) {
        MaterialKind.PLATES -> stringResource(R.string.mat_summary_plates, w(m.baseKg), m.loadingPoints)
        MaterialKind.STACK, MaterialKind.DUMBBELLS -> "${w(m.minKg)} – ${w(m.maxKg)} · ${stringResource(R.string.mat_step)} ${w(m.stepKg)}"
        MaterialKind.BODYWEIGHT -> stringResource(R.string.mat_summary_bodyweight, w(m.stepKg), w(m.maxKg))
    }
}

/** Gym plate stock and reusable material profiles (D-25, D-27). */
@Composable
fun MaterialScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val profiles by container.db.materials().observeActive().collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<MaterialProfileEntity?>(null) }
    val unit = settings.unit
    val stock = settings.plateStock.plates

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.mat_title), style = MaterialTheme.typography.headlineMedium)
        Eyebrow(stringResource(R.string.mat_plates))
        Text(stringResource(R.string.mat_plates_help), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        (commonPlates + stock.keys).distinct().sortedDescending().forEach { size ->
            Row(
                Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(WeightFormat.format(size, unit) + " " + unit.name.lowercase(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(88.dp))
                NumberStepper(
                    value = (stock[size] ?: 0).toDouble(),
                    onValueChange = { v ->
                        val n = (v ?: 0.0).toInt().coerceAtLeast(0)
                        scope.launch {
                            container.settings.update {
                                val map = it.plateStock.plates.toMutableMap()
                                if (n == 0) map.remove(size) else map[size] = n
                                it.copy(plateStock = if (map.isEmpty()) it.plateStock else PlateStock(map))
                            }
                        }
                    },
                    step = 2.0, decimals = false, min = 0.0,
                    formatter = { it.toInt().toString() }, textSize = 20.sp, buttonSize = 40.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        DecoDivider(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Eyebrow(stringResource(R.string.mat_profiles))
        profiles.forEach { m ->
            PlainCard(onClick = { editing = m }) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Gym.Gold, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.displayName(appIsDutch()), style = MaterialTheme.typography.titleMedium)
                    Text(kindName(m.kind) + " · " + profileSummary(m, unit), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                }
            }
        }
        LineButton(
            stringResource(R.string.mat_new), icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth(),
            onClick = { editing = MaterialProfileEntity(name = "", kind = MaterialKind.PLATES, baseKg = 20.0) },
        )
        Spacer(Modifier.height(24.dp))
    }

    editing?.let { m ->
        ProfileDialog(
            initial = m, unit = unit,
            onSave = { updated ->
                editing = null
                scope.launch { if (updated.id == 0L) container.db.materials().insert(updated) else container.db.materials().update(updated) }
            },
            onDelete = if (m.id != 0L) ({ editing = null; scope.launch { container.db.materials().delete(m.id) } }) else null,
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileDialog(
    initial: MaterialProfileEntity,
    unit: WeightUnit,
    onSave: (MaterialProfileEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var p by remember { mutableStateOf(initial) }
    fun fmt(kg: Double) = WeightFormat.format(kg, unit)

    @Composable
    fun field(label: String, value: Double, step: Double, onChange: (Double) -> Unit) {
        Text(label, color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
        NumberStepper(
            value = value, onValueChange = { it?.let(onChange) }, step = step, min = 0.0,
            formatter = ::fmt, parser = unit::toKg, textSize = 20.sp, buttonSize = 40.dp,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.mat_new else R.string.mat_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text(stringResource(R.string.schema_name)) }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MaterialKind.entries.forEach { k -> FilterChip(p.kind == k, { p = p.copy(kind = k) }, label = { Text(kindName(k)) }) }
                }
                when (p.kind) {
                    MaterialKind.PLATES -> {
                        field(stringResource(R.string.mat_base), p.baseKg, 2.5) { p = p.copy(baseKg = it) }
                        Text(stringResource(R.string.mat_points), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2).forEach { n -> FilterChip(p.loadingPoints == n, { p = p.copy(loadingPoints = n) }, label = { Text("$n") }) }
                        }
                    }
                    MaterialKind.STACK, MaterialKind.DUMBBELLS -> {
                        field(stringResource(R.string.mat_min), p.minKg, 0.5) { p = p.copy(minKg = it) }
                        field(stringResource(R.string.mat_step), p.stepKg, 0.5) { p = p.copy(stepKg = it.coerceAtLeast(0.25)) }
                        field(stringResource(R.string.mat_max), p.maxKg, 5.0) { p = p.copy(maxKg = it) }
                    }
                    MaterialKind.BODYWEIGHT -> {
                        field(stringResource(R.string.mat_assist_step), p.stepKg, 0.5) { p = p.copy(stepKg = it) }
                        field(stringResource(R.string.mat_assist_max), p.maxKg, 5.0) { p = p.copy(maxKg = it) }
                        Text(stringResource(R.string.mat_belt_help), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(p.copy(name = p.name.trim())) }, enabled = p.name.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = Gym.Negative) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
