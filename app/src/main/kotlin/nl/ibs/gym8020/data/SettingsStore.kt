package nl.ibs.gym8020.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import nl.ibs.gym8020.core.logic.ReminderFlags
import nl.ibs.gym8020.core.logic.Warmup
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.model.MethodSettings

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val method: MethodSettings = MethodSettings(),
    val unit: WeightUnit = WeightUnit.KG,
    val seeded: Boolean = false,
    val seededMaterials: Boolean = false,
    val plateStock: PlateStock = PlateStock.DEFAULT,
    /** Introduction for new users was finished or skipped (G-2). */
    val onboarded: Boolean = false,
    /** Reminders (D-65), all off by default. */
    val reminders: ReminderFlags = ReminderFlags(),
    val reminderOpenSession: Boolean = false,
    /** Time of the daily reminder, minutes after midnight. */
    val reminderMinutes: Int = 18 * 60,
    /** Folder (document tree URI) for the automatic backup after each training (D-03). */
    val backupFolder: String? = null,
    val lastBackupAt: Long? = null,
    val backupFailed: Boolean = false,
    /** Warm-up assistance for pull-ups and dips, % of the moved body weight (B-4, D-83). */
    val warmupAssistPercent: Int = Warmup.DEFAULT_ASSIST_PERCENT,
    /** The one-time move of data into schema v3 is done (`V3Upgrade`, #11). */
    val v3DataMoved: Boolean = false,
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
        val seededMaterials = booleanPreferencesKey("seeded_materials")
        val plates = stringPreferencesKey("plate_stock")
        val onboarded = booleanPreferencesKey("onboarded")
        val remindDay = booleanPreferencesKey("remind_training_day")
        val remindBehind = booleanPreferencesKey("remind_behind")
        val remindBodyweight = booleanPreferencesKey("remind_bodyweight")
        val remindBodyweightDaily = booleanPreferencesKey("remind_bodyweight_daily")
        val remindOpen = booleanPreferencesKey("remind_open_session")
        val remindMinutes = intPreferencesKey("remind_minutes")
        val backupFolder = stringPreferencesKey("backup_folder")
        val lastBackup = longPreferencesKey("last_backup_at")
        val backupFailed = booleanPreferencesKey("backup_failed")
        val warmupAssist = intPreferencesKey("warmup_assist_percent")
        val v3DataMoved = booleanPreferencesKey("v3_data_moved")
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
            seededMaterials = this[Keys.seededMaterials] ?: false,
            plateStock = PlateStock.decode(this[Keys.plates]),
            onboarded = this[Keys.onboarded] ?: false,
            reminders = ReminderFlags(
                trainingDay = this[Keys.remindDay] ?: false,
                behind = this[Keys.remindBehind] ?: false,
                bodyweight = this[Keys.remindBodyweight] ?: false,
                bodyweightDaily = this[Keys.remindBodyweightDaily] ?: false,
            ),
            reminderOpenSession = this[Keys.remindOpen] ?: false,
            reminderMinutes = this[Keys.remindMinutes] ?: (18 * 60),
            backupFolder = this[Keys.backupFolder],
            lastBackupAt = this[Keys.lastBackup],
            backupFailed = this[Keys.backupFailed] ?: false,
            warmupAssistPercent = this[Keys.warmupAssist] ?: Warmup.DEFAULT_ASSIST_PERCENT,
            v3DataMoved = this[Keys.v3DataMoved] ?: false,
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
            prefs[Keys.seededMaterials] = next.seededMaterials
            prefs[Keys.plates] = next.plateStock.encode()
            prefs[Keys.onboarded] = next.onboarded
            prefs[Keys.remindDay] = next.reminders.trainingDay
            prefs[Keys.remindBehind] = next.reminders.behind
            prefs[Keys.remindBodyweight] = next.reminders.bodyweight
            prefs[Keys.remindBodyweightDaily] = next.reminders.bodyweightDaily
            prefs[Keys.remindOpen] = next.reminderOpenSession
            prefs[Keys.remindMinutes] = next.reminderMinutes
            if (next.backupFolder != null) prefs[Keys.backupFolder] = next.backupFolder else prefs.remove(Keys.backupFolder)
            if (next.lastBackupAt != null) prefs[Keys.lastBackup] = next.lastBackupAt else prefs.remove(Keys.lastBackup)
            prefs[Keys.backupFailed] = next.backupFailed
            prefs[Keys.warmupAssist] = next.warmupAssistPercent
            prefs[Keys.v3DataMoved] = next.v3DataMoved
        }
    }
}
