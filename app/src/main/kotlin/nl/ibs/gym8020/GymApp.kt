package nl.ibs.gym8020

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nl.ibs.gym8020.data.AppLanguage
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
    val seeder = Seeder(db, settings, dutch = AppLanguage.get(app) == AppLanguage.DUTCH)
}

class GymApp : Application() {
    lateinit var container: AppContainer
        private set

    /** The first-start fill with standard content; tests wait for it. */
    lateinit var seeding: Job
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        seeding = container.appScope.launch { container.seeder.seedIfNeeded() }
        Reminders.createChannel(this)
        // Replan the daily reminder whenever its settings change (and once at start).
        container.settings.settings
            .map { it.reminders to it.reminderMinutes }
            .distinctUntilChanged()
            .onEach { runCatching { Reminders.reschedule(this, container.settings.current()) } }
            .launchIn(container.appScope)
    }
}
