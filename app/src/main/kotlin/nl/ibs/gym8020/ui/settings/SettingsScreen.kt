package nl.ibs.gym8020.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.BuildConfig
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.IbsMark
import nl.ibs.gym8020.ui.theme.Gym
import java.time.LocalDate

const val METHOD_URL = "https://github.com/Fabianlanger/GYM-8020/blob/main/docs/methode.md"

@Composable
fun SettingsScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val exportDone = stringResource(R.string.settings_export_done)
    val importDone = stringResource(R.string.settings_import_done)
    val errorText = stringResource(R.string.settings_error)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            message = runCatching {
                val json = container.backup.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("no stream")
                }
                exportDone
            }.getOrElse { String.format(errorText, it.message ?: it.javaClass.simpleName) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    fun update(f: (AppSettings) -> AppSettings) = scope.launch { container.settings.update(f) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Eyebrow(stringResource(R.string.settings_method))
        SettingStepper(stringResource(R.string.settings_heavy_target), settings.method.heavyTarget) { v -> update { it.copy(method = it.method.copy(heavyTarget = v)) } }
        SettingStepper(stringResource(R.string.settings_light_target), settings.method.lightTarget) { v -> update { it.copy(method = it.method.copy(lightTarget = v)) } }
        SettingStepper(stringResource(R.string.settings_default_sets), settings.method.defaultSetCount, min = 1) { v -> update { it.copy(method = it.method.copy(defaultSetCount = v)) } }
        SettingStepper(stringResource(R.string.settings_margin), settings.method.goodMargin, min = 0) { v -> update { it.copy(method = it.method.copy(goodMargin = v)) } }
        Text(stringResource(R.string.settings_step), color = Gym.Muted)
        NumberStepper(
            value = settings.method.weightStepKg,
            onValueChange = { v -> if (v != null && v > 0) update { it.copy(method = it.method.copy(weightStepKg = v)) } },
            step = 0.25, min = 0.25,
            formatter = { WeightFormat.format(it, settings.unit) }, parser = settings.unit::toKg,
            textSize = 22.sp, buttonSize = 48.dp,
        )
        Text(stringResource(R.string.settings_unit), color = Gym.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightUnit.entries.forEach { u ->
                FilterChip(settings.unit == u, { update { it.copy(unit = u) } }, label = { Text(u.name.lowercase()) })
            }
        }

        DecoDivider(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Eyebrow(stringResource(R.string.settings_data))
        Text(stringResource(R.string.settings_local_warning), style = MaterialTheme.typography.bodySmall, color = Gym.Gold)
        LineButton(stringResource(R.string.settings_export), onClick = { exportLauncher.launch("gym-8020-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth())
        LineButton(stringResource(R.string.settings_import), onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, color = Gym.Yellow, style = MaterialTheme.typography.bodySmall) }

        DecoDivider(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        DecoCard(corners = true, padding = 22.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(stringResource(R.string.about_title))
                IbsMark(Modifier.size(64.dp))
                Text("GYM-8020", style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = Gym.Muted, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.about_ibs), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.about_method_text), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                LineButton(stringResource(R.string.about_method), onClick = { runCatching { uriHandler.openUri(METHOD_URL) } })
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.settings_import)) },
            text = { Text(stringResource(R.string.settings_import_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        message = runCatching {
                            val text = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("no stream")
                            }
                            container.backup.import(text)
                            importDone
                        }.getOrElse { String.format(errorText, it.message ?: it.javaClass.simpleName) }
                    }
                }) { Text(stringResource(R.string.settings_import)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SettingStepper(label: String, value: Int, min: Int = 1, onChange: (Int) -> Unit) {
    Column {
        Text(label, color = Gym.Muted)
        NumberStepper(
            value = value.toDouble(),
            onValueChange = { v -> v?.toInt()?.takeIf { it >= min }?.let(onChange) },
            step = 1.0, decimals = false, min = min.toDouble(),
            formatter = { it.toInt().toString() }, textSize = 22.sp, buttonSize = 48.dp,
        )
    }
}
