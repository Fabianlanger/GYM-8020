package nl.ibs.gym8020.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.MethodSettings

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val method: MethodSettings = MethodSettings(),
    val unit: WeightUnit = WeightUnit.KG,
    val seeded: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val heavy = intPreferencesKey("heavy_target")
        val light = intPreferencesKey("light_target")
        val sets = intPreferencesKey("default_sets")
        val margin = intPreferencesKey("good_margin")
        val step = doublePreferencesKey("weight_step_kg")
        val unit = stringPreferencesKey("unit")
        val seeded = booleanPreferencesKey("seeded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings(): AppSettings {
        val d = MethodSettings()
        return AppSettings(
            method = MethodSettings(
                heavyTarget = this[Keys.heavy] ?: d.heavyTarget,
                lightTarget = this[Keys.light] ?: d.lightTarget,
                defaultSetCount = this[Keys.sets] ?: d.defaultSetCount,
                goodMargin = this[Keys.margin] ?: d.goodMargin,
                weightStepKg = this[Keys.step] ?: d.weightStepKg,
            ),
            unit = this[Keys.unit]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.KG,
            seeded = this[Keys.seeded] ?: false,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.heavy] = next.method.heavyTarget
            prefs[Keys.light] = next.method.lightTarget
            prefs[Keys.sets] = next.method.defaultSetCount
            prefs[Keys.margin] = next.method.goodMargin
            prefs[Keys.step] = next.method.weightStepKg
            prefs[Keys.unit] = next.unit.name
            prefs[Keys.seeded] = next.seeded
        }
    }
}
