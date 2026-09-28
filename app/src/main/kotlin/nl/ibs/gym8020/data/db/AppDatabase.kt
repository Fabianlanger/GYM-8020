package nl.ibs.gym8020.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class, ExerciseMuscleEntity::class,
        SchemaEntity::class, SchemaEntryEntity::class,
        WeekPlanEntity::class, WeekPlanSlotEntity::class,
        BodyweightEntity::class,
        SessionEntity::class, SessionExerciseEntity::class, SetEntity::class,
        PlanEntity::class, PlanSetEntity::class,
        MaterialProfileEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun schemas(): SchemaDao
    abstract fun weekPlans(): WeekPlanDao
    abstract fun bodyweights(): BodyweightDao
    abstract fun sessions(): SessionDao
    abstract fun plans(): PlanDao
    abstract fun materials(): MaterialDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gym8020.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v0.2: material profiles, and material + movement per exercise. Keeps all data. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `material_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`libraryKey` TEXT, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `baseKg` REAL NOT NULL, " +
                        "`loadingPoints` INTEGER NOT NULL, `minKg` REAL NOT NULL, `stepKg` REAL NOT NULL, " +
                        "`maxKg` REAL NOT NULL, `archived` INTEGER NOT NULL)",
                )
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `materialProfileId` INTEGER")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `movement` TEXT")
            }
        }
    }
}
