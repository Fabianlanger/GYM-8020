package nl.ibs.gym8020

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.ibs.gym8020.data.BackupService
import nl.ibs.gym8020.data.Seeder
import nl.ibs.gym8020.data.SettingsStore
import nl.ibs.gym8020.data.TrainingRepository
import nl.ibs.gym8020.data.db.AppDatabase

/**
 * Simple manual dependency container. All data is local (D-02); the repository layer
 * is where a sync backend can be plugged in later (D-03).
 */
class AppContainer(app: Application) {
    val db: AppDatabase = AppDatabase.create(app)
    val settings = SettingsStore(app)
    val training = TrainingRepository(db, settings)
    val backup = BackupService(db, settings)
    val seeder = Seeder(db, settings, dutch = java.util.Locale.getDefault().language == "nl")
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class GymApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.seeder.seedIfNeeded() }
    }
}
