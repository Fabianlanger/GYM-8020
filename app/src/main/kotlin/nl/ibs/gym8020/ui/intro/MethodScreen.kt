package nl.ibs.gym8020.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.deco.FlourishDivider
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.theme.Gym

/** "About the method" (D-59): the text of docs/methode.md, inside the app so it works offline. */
private val sections = listOf(
    R.string.method_idea_title to R.string.method_idea_body,
    R.string.method_sets_title to R.string.method_sets_body,
    R.string.method_plan_title to R.string.method_plan_body,
    R.string.method_bw_title to R.string.method_bw_body,
    R.string.method_go_title to R.string.method_go_body,
    R.string.method_week_title to R.string.method_week_body,
    R.string.method_progress_title to R.string.method_progress_body,
)

@Composable
fun MethodScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.BottomCenter) {
            Sunburst(Modifier.fillMaxSize().alpha(.45f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Eyebrow("GYM · 8020")
                Text(stringResource(R.string.about_method), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            }
        }
        FlourishDivider()
        MethodTable()
        sections.forEach { (title, body) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow(stringResource(title), color = Gym.Yellow)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
        DecoCard {
            Eyebrow(stringResource(R.string.method_safety_title), color = Gym.Gold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.method_safety_body), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        }
        LineButton(stringResource(R.string.action_back), onClick = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

/** Heavy and light set at a glance. */
@Composable
fun MethodTable(modifier: Modifier = Modifier) {
    DecoCard(modifier, corners = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MethodCell(stringResource(R.string.method_heavy), "± 6", stringResource(R.string.method_heavy_why))
            MethodCell(stringResource(R.string.method_light), "± 16", stringResource(R.string.method_light_why))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.method_to_failure), style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MethodCell(label: String, reps: String, why: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(label, color = Gym.Gold)
        Text(reps, style = MaterialTheme.typography.headlineMedium, color = Gym.Yellow)
        Text(why, style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
    }
}
