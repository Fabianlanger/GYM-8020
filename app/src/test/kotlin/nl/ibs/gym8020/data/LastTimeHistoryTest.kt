package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.core.logic.LastSet
import nl.ibs.gym8020.core.logic.LastTime
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * #65: "Laatste keer" per set comes from every workout, by training date; a skipped exercise or an empty set
 * does not count, and the training you are in is left out.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LastTimeHistoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private var exerciseId = se(0)L

    private val sep28 = LocalDate.of(2026, 9, 28)
    private val oct2 = LocalDate.of(2026, 10, 2)
    private val oct5 = LocalDate.of(2026, 10, 5)

    private fun repository() = TrainingRepository(db, SettingsStore(context))

    private fun millis(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).plusHours(18).toInstant().toEpochMilli()

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        exerciseId = db.exercises().insert(
            ExerciseEntity(name = "Squat", equipment = Equipment.BARBELL, loadType = LoadType.EXTERNAL, progression = Progression.WEIGHT),
        )
    }

    @After
    fun tearDown() = db.close()

    /** A training of the squat in [workout] on [date]: heavy and light set as (kg, reps), null when not done. */
    private suspend fun training(
        workout: String,
        date: LocalDate,
        heavy: Pair<Double, Int>?,
        light: Pair<Double, Int>?,
        skipped: Boolean = false,
        finished: Boolean = true,
    ): Long {
        val start = millis(date)
        val sessionId = db.sessions().insert(
            SessionEntity(schemaName = workout, startedAt = start, endedAt = if (finished) start + 3_600_000L else null),
        )
        val seId = db.sessions().insertExercise(
            SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = 0, skipped = skipped),
        )
        db.sessions().insertSets(
            listOf(
                SetEntity(sessionExerciseId = seId, position = 0, role = SetRole.HEAVY, weightKg = heavy?.first, reps = heavy?.second),
                SetEntity(sessionExerciseId = seId, position = 1, role = SetRole.LIGHT, weightKg = light?.first, reps = light?.second),
            ),
        )
        return sessionId
    }

    @Test
    fun `each set shows its last time from any workout with the training date`() = runBlocking {
        training("Legs", oct2, heavy = 80.0 to 7, light = null)
        // Entered afterwards: an older training does not become the last time.
        training("Full body", sep28, heavy = 90.0 to 5, light = 40.0 to 15)
        training("Legs", oct5, heavy = 100.0 to 3, light = 50.0 to 10, skipped = true)
        val today = training("Full body", LocalDate.of(2026, 10, 9), heavy = null, light = null, finished = false)

        val history = repository().lastTimeHistory(exerciseId, excludeSession = today)

        assertEquals(
            listOf(LastSet(PerformedSet(SetRole.HEAVY, 80.0, 7), oct2), LastSet(PerformedSet(SetRole.LIGHT, 40.0, 15), sep28)),
            LastTime.perSet(listOf(SetRole.HEAVY, SetRole.LIGHT), history),
        )
    }
}
