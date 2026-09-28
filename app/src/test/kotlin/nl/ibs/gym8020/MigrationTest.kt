package nl.ibs.gym8020

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Existing users must keep their data when the database schema changes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2KeepsData() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO exercises (id, libraryKey, name, equipment, loadType, progression, bodyweightFactor, archived) " +
                    "VALUES (7, 'back_squat', 'Back squat', 'BARBELL', 'EXTERNAL', 'WEIGHT', 1.0, 0)",
            )
            execSQL("INSERT INTO bodyweights (id, epochDay, kg) VALUES (1, 20000, 80.5)")
            close()
        }
        // Opening with Room runs the migration and validates the result against the current entities.
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            val ex = db.exercises().get(7)!!
            assertEquals("Back squat", ex.name)
            assertNull(ex.materialProfileId)
            assertNull(ex.movement)
            assertEquals(80.5, db.bodyweights().all().single().kg, 0.0)
            assertEquals(0, db.materials().count())
        }
        db.close()
    }
}
