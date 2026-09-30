package nl.ibs.gym8020.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.toLocalDate
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.PlainCard
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.training.formatDuration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** All finished trainings, newest first, grouped per month; tap one to correct it. */
@Composable
fun LogScreen(container: AppContainer, onOpen: (Long) -> Unit) {
    val sessions by container.db.sessions().observeFinished().collectAsStateWithLifecycle(emptyList())
    val counts by container.db.sessions().observeWorkSetCounts().collectAsStateWithLifecycle(emptyList())
    val setCount = counts.associate { it.sessionId to it.workSets }
    val months = sessions.groupBy { it.startedAt.toLocalDate().withDayOfMonth(1) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium) }
        if (sessions.isEmpty()) {
            item { Text(stringResource(R.string.log_empty), color = Gym.Muted) }
        }
        months.forEach { (month, list) ->
            item(key = "m$month") {
                val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                Eyebrow("$name ${month.year}", Modifier.padding(top = 10.dp), color = Gym.Gold)
            }
            items(list, key = { it.id }) { s -> SessionRow(s, setCount[s.id] ?: 0) { onOpen(s.id) } }
        }
    }
}

@Composable
fun SessionRow(session: SessionEntity, workSets: Int, onClick: () -> Unit) {
    PlainCard(onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(session.schemaName, style = MaterialTheme.typography.titleMedium)
            val date = session.startedAt.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            val duration = formatDuration((session.endedAt ?: session.startedAt) - session.startedAt)
            Text(
                stringResource(R.string.log_row, date, duration, workSets),
                style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
            )
        }
        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.log_edit_title), tint = Gym.Muted)
    }
}
