package nl.ibs.gym8020.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.db.BodyweightEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.theme.Gym
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Body weight log (D-14). The full chart with trend line comes with the dashboard (v0.3). */
@Composable
fun BodyweightScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val list by container.db.bodyweights().observeAll().collectAsStateWithLifecycle(emptyList())
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    var value by remember { mutableStateOf<Double?>(null) }
    val unit = settings.unit

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.body_title), style = MaterialTheme.typography.headlineMedium)
                DecoCard(corners = true) {
                    Eyebrow(stringResource(R.string.body_today))
                    Spacer(Modifier.height(8.dp))
                    NumberStepper(
                        value = value ?: list.firstOrNull()?.kg,
                        onValueChange = { value = it },
                        step = 0.1,
                        min = 0.0,
                        formatter = { WeightFormat.format(it, unit) },
                        parser = unit::toKg,
                    )
                    Spacer(Modifier.height(12.dp))
                    GoButton(stringResource(R.string.action_save), onClick = {
                        val kg = value ?: list.firstOrNull()?.kg
                        if (kg != null && kg > 0) scope.launch {
                            container.db.bodyweights().upsert(BodyweightEntity(epochDay = LocalDate.now().toEpochDay(), kg = kg))
                            value = null
                        }
                    }, height = 52.dp)
                }
                if (list.size >= 2) Sparkline(list.reversed().map { it.kg })
                if (list.isEmpty()) Text(stringResource(R.string.body_empty), color = Gym.Muted)
            }
        }
        items(list, key = { it.id }) { b ->
            Row(
                Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    LocalDate.ofEpochDay(b.epochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    modifier = Modifier.weight(1f), color = Gym.Muted,
                )
                Text(WeightFormat.format(b.kg, unit) + " " + unit.name.lowercase(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { scope.launch { container.db.bodyweights().delete(b.id) } }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_delete), tint = Gym.Muted)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Sparkline(values: List<Double>) {
    val min = values.min()
    val max = values.max()
    Canvas(Modifier.fillMaxWidth().height(80.dp).background(Gym.Surface, RoundedCornerShape(16.dp)).padding(12.dp)) {
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = (size.height * (1 - (v - min) / span)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Gym.Yellow, style = Stroke(width = 2.dp.toPx()))
        val lastY = (size.height * (1 - (values.last() - min) / span)).toFloat()
        drawCircle(Gym.Yellow, 4.dp.toPx(), Offset(size.width, lastY))
    }
}
