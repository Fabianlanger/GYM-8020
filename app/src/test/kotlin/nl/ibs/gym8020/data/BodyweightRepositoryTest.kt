package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MeasurementOrigin
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.BodyweightEntity
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SchemaEntity
import nl.ibs.gym8020.data.db.SchemaEntryEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * #94 (decided in #81): a day can have several body weight measurements. Adding never replaces one; each one can be
 * changed or deleted on its own; a training takes the last measurement before its start.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BodyweightRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val settings get() = SettingsStore(context)
    private val day = LocalDate.of(2026, 3, 10)

    /** The moment [hour]:[minute] of [date], in this device's time zone. */
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun repository() = BodyweightRepository(db)

    @Before
    fun setUp() = runBlocking<Unit> {
        // The app runs its own start steps; let them finish first. The settings store lives for the whole test process.
        (context as GymApp).seeding.join()
        settings.update { AppSettings(bodyweightTargetsEffective = true) }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `#94 adding a measurement on the same day keeps the earlier one`() = runBlocking<Unit> {
        repository().add(80.0, at(7))
        repository().add(81.2, at(21))

        val all = db.bodyweights().all()
        assertEquals(listOf(80.0, 81.2), all.map { it.kg })
        assertEquals(listOf(day.toEpochDay(), day.toEpochDay()), all.map { it.epochDay })
        assertEquals(listOf(MeasurementOrigin.MANUAL, MeasurementOrigin.MANUAL), all.map { it.origin })
    }

    @Test
    fun `#94 changing a measurement changes its weight and moment, its day follows the moment, the others stay`() = runBlocking<Unit> {
        val morning = repository().add(80.0, at(7))
        repository().add(81.2, at(21))

        repository().update(morning, 79.6, at(8, 30, day.minusDays(1)))

        val changed = db.bodyweights().get(morning)!!
        assertEquals(79.6, changed.kg, 0.0)
        assertEquals(at(8, 30, day.minusDays(1)), changed.measuredAt)
        assertEquals(day.minusDays(1).toEpochDay(), changed.epochDay)
        assertEquals(listOf(79.6, 81.2), db.bodyweights().all().map { it.kg })
    }

    @Test
    fun `#94 changing a measurement from a wearable keeps its origin`() = runBlocking<Unit> {
        val id = db.bodyweights().insert(BodyweightEntity(epochDay = day.toEpochDay(), kg = 80.0, measuredAt = at(7), origin = MeasurementOrigin.WEARABLE))

        repository().update(id, 80.4, at(7, 5))

        assertEquals(MeasurementOrigin.WEARABLE, db.bodyweights().get(id)!!.origin)
    }

    @Test
    fun `#94 deleting a measurement removes only that one`() = runBlocking<Unit> {
        val morning = repository().add(80.0, at(7))
        repository().add(81.2, at(21))

        repository().delete(morning)

        assertEquals(listOf(81.2), db.bodyweights().all().map { it.kg })
    }

    @Test
    fun `#94 a training counts the last measurement before its start, not a later one that day`() = runBlocking<Unit> {
        repository().add(79.0, at(20, date = day.minusDays(1)))
        repository().add(80.0, at(7))
        repository().add(82.0, at(21))

        assertEquals(80.0, repository().countingFor(at(18))!!.kg, 0.0)
        assertEquals(79.0, repository().countingFor(at(6))!!.kg, 0.0)
        assertNull(repository().countingFor(at(6, date = day.minusDays(2))))
    }

    @Test
    fun `#94 a training keeps the body weight of before its start, also after a measurement later that day`() = runBlocking<Unit> {
        val pullUp = db.exercises().insert(
            ExerciseEntity(name = "Pull-up", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT, progression = Progression.WEIGHT),
        )
        val schema = db.schemas().insert(SchemaEntity(name = "Pull"))
        db.schemas().insertEntries(listOf(SchemaEntryEntity(schemaId = schema, exerciseId = pullUp, position = 0)))
        repository().add(80.0, at(7))
        repository().add(82.0, at(21)) // entered later, with an evening time

        val sessionId = TrainingRepository(db, settings).startSession(schema, now = at(18))

        assertEquals(80.0, db.sessions().get(sessionId)!!.bodyweightKg!!, 0.0)
        assertEquals(setOf(80.0), db.sessions().sets(sessionId).map { it.bodyweightKg }.toSet())
    }
}
