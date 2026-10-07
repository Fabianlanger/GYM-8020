package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * B-6, D-97: the target for next time is yours. What you set is stored right away, survives leaving the
 * training screen, and moving on never replaces it with advice.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TargetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private var exerciseId = 0L

    /** A fresh repository is what you get after leaving the training screen and coming back. */
    private fun repository() = TrainingRepository(db, SettingsStore(context), PrefsTargetMarks(context))

    private fun heavy(kg: Double) = Target(listOf(PlannedSet(SetRole.HEAVY, kg), PlannedSet(SetRole.LIGHT, kg / 2)))

    @Before
    fun setUp() = runBlocking {
        context.getSharedPreferences("target_marks", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        exerciseId = db.exercises().insert(
            ExerciseEntity(name = "Cable row", equipment = Equipment.CABLE, loadType = LoadType.EXTERNAL, progression = Progression.WEIGHT),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a target you set survives leaving the training screen`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = heavy(80.0))

        assertEquals(heavy(80.0), repository().targetSetIn(seId = 1, exerciseId = exerciseId))
    }

    @Test
    fun `moving on does not replace your target with advice`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = heavy(80.0))

        repository().storeAdvice(seId = 1, exerciseId = exerciseId, advice = heavy(82.5))

        assertEquals(heavy(80.0), repository().target(exerciseId))
    }

    @Test
    fun `without a target of your own the advice is stored when moving on`() = runBlocking {
        repository().storeAdvice(seId = 2, exerciseId = exerciseId, advice = heavy(82.5))

        assertEquals(heavy(82.5), repository().target(exerciseId))
        assertNull(repository().targetSetIn(seId = 2, exerciseId = exerciseId))
    }

    @Test
    fun `a target set in an earlier session does not count as set in this one`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = heavy(80.0))

        assertNull(repository().targetSetIn(seId = 3, exerciseId = exerciseId))
    }
}
