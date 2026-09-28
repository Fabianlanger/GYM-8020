package nl.ibs.gym8020.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class, ExerciseMuscleEntity::class,
        SchemaEntity::class, SchemaEntryEntity::class,
        WeekPlanEntity::class, WeekPlanSlotEntity::class,
        BodyweightEntity::class,
        SessionEntity::class, SessionExerciseEntity::class, SetEntity::class,
        PlanEntity::class, PlanSetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun schemas(): SchemaDao
    abstract fun weekPlans(): WeekPlanDao
    abstract fun bodyweights(): BodyweightDao
    abstract fun sessions(): SessionDao
    abstract fun plans(): PlanDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gym8020.db").build()
    }
}
