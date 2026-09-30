package nl.ibs.gym8020

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nl.ibs.gym8020.data.AutoBackup
import nl.ibs.gym8020.data.BackupService
import nl.ibs.gym8020.data.Seeder
import nl.ibs.gym8020.data.SettingsStore
import nl.ibs.gym8020.data.TrainingRepository
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.notify.Reminders

/**
 * Simple manual dependency container. All data is local (D-02); the repository layer
 * is where a sync backend can be plugged in later (D-03).
 */
class AppContainer(val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: AppDatabase = AppDatabase.create(app)
    val settings = SettingsStore(app)
    val backup = BackupService(db, settings)
    val autoBackup = AutoBackup(app, backup, settings, appScope)
    val training = TrainingRepository(db, settings, onFinished = { autoBackup.runSoon() })
    val seeder = Seeder(db, settings, dutch = java.util.Locale.getDefault().language == "nl")
}

class GymApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.seeder.seedIfNeeded() }
        Reminders.createChannel(this)
        // Replan the daily reminder whenever its settings change (and once at start).
        container.settings.settings
            .map { it.reminders to it.reminderMinutes }
            .distinctUntilChanged()
            .onEach { Reminders.reschedule(this, container.settings.current()) }
            .launchIn(container.appScope)
    }
}
