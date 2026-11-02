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
        GymEntity::class, TargetEntity::class, TargetSetEntity::class,
    ],
    version = 3,
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
    abstract fun gyms(): GymDao
    abstract fun targets(): TargetDao

    companion object {
        const val NAME = "gym8020.db"

        fun create(context: Context): AppDatabase {
            // Before Room opens (and migrates) an older database: a copy of its files, once (#11, Q4).
            PreMigrationCopy.copyIfNeeded(context, NAME)
            return Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

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

        /** Id of the gym the migration to version 3 creates; existing trainings get it. */
        const val FIRST_GYM_ID = 1L

        /** Name of that gym in the migration; the app gives it a name in your language on first start. */
        const val MIGRATED_GYM_NAME = "Gym"

        /**
         * Schema v3 (#11): all new fields in one step (A-1). Only adds and keeps: every existing row stays, with
         * defaults for the new columns. Data that lives outside the database (plate stock, interim stores) and the
         * derived targets of older trainings are moved by the app after opening (`V3Upgrade`).
         *
         * Order matters: `material_profiles` is rebuilt (without `maxKg`) before any table refers to it, and no
         * referenced table is dropped, so the result does not depend on foreign keys being on or off.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Exercises: default warm-up (c) and standard codes (i, A-5).
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `defaultWarmupKg` REAL")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `defaultWarmupNote` TEXT")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `healthConnectType` TEXT")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `garminCategory` TEXT")

                // Material profiles without maxKg (e, #6): SQLite cannot drop a column here, so the table is rebuilt.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `material_profiles_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`libraryKey` TEXT, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `baseKg` REAL NOT NULL, " +
                        "`loadingPoints` INTEGER NOT NULL, `minKg` REAL NOT NULL, `stepKg` REAL NOT NULL, `archived` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `material_profiles_new` (`id`, `libraryKey`, `name`, `kind`, `baseKg`, `loadingPoints`, `minKg`, `stepKg`, `archived`) " +
                        "SELECT `id`, `libraryKey`, `name`, `kind`, `baseKg`, `loadingPoints`, `minKg`, `stepKg`, `archived` FROM `material_profiles`",
                )
                db.execSQL("DROP TABLE `material_profiles`")
                db.execSQL("ALTER TABLE `material_profiles_new` RENAME TO `material_profiles`")

                // Body weight (h, V-14, A-14): moment and origin, several measurements per day.
                db.execSQL("ALTER TABLE `bodyweights` ADD COLUMN `measuredAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `bodyweights` ADD COLUMN `origin` TEXT NOT NULL DEFAULT 'MANUAL'")
                // Noon (UTC) of the day, the same as BodyweightLog.legacyMeasuredAt.
                db.execSQL("UPDATE `bodyweights` SET `measuredAt` = `epochDay` * 86400000 + 43200000")
                db.execSQL("DROP INDEX IF EXISTS `index_bodyweights_epochDay`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bodyweights_epochDay` ON `bodyweights` (`epochDay`)")

                // Gyms (j, A-4): one gym; its plate stock moves from the settings after opening.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `gyms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`plates` TEXT, `archived` INTEGER NOT NULL)",
                )
                db.execSQL("INSERT INTO `gyms` (`id`, `name`, `plates`, `archived`) VALUES ($FIRST_GYM_ID, '$MIGRATED_GYM_NAME', NULL, 0)")

                // Sessions: origin (g, A-2), existing trainings are live; and the gym (j).
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `origin` TEXT NOT NULL DEFAULT 'LIVE'")
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `gymId` INTEGER " +
                        "REFERENCES `gyms`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL",
                )
                db.execSQL("UPDATE `sessions` SET `gymId` = $FIRST_GYM_ID")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_gymId` ON `sessions` (`gymId`)")

                // Material per session (d, #48); null = the exercise's own profile.
                db.execSQL(
                    "ALTER TABLE `session_exercises` ADD COLUMN `materialProfileId` INTEGER " +
                        "REFERENCES `material_profiles`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_session_exercises_materialProfileId` ON `session_exercises` (`materialProfileId`)",
                )

                // Sets: session target in reps (b) and "nog over" (f, D-20).
                db.execSQL("ALTER TABLE `sets` ADD COLUMN `goalReps` INTEGER")
                db.execSQL("ALTER TABLE `sets` ADD COLUMN `repsInReserve` INTEGER")

                // The current target gets reps (a, #69).
                db.execSQL("ALTER TABLE `plan_sets` ADD COLUMN `reps` INTEGER")

                // The target for next time per session exercise (a).
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `targets` (`sessionExerciseId` INTEGER NOT NULL, `origin` TEXT NOT NULL, " +
                        "`note` TEXT, `setAt` INTEGER, PRIMARY KEY(`sessionExerciseId`), " +
                        "FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `target_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionExerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `role` TEXT NOT NULL, `weightKg` REAL, " +
                        "`reps` INTEGER, FOREIGN KEY(`sessionExerciseId`) REFERENCES `targets`(`sessionExerciseId`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_target_sets_sessionExerciseId` ON `target_sets` (`sessionExerciseId`)")
            }
        }
    }
}
