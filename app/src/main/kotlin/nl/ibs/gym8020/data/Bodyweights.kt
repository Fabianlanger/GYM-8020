package nl.ibs.gym8020.data

import kotlinx.coroutines.flow.Flow
import nl.ibs.gym8020.core.logic.BodyweightLog
import nl.ibs.gym8020.core.logic.Measurement
import nl.ibs.gym8020.core.model.MeasurementOrigin
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.BodyweightEntity

/** A body weight you enter, taken at [measuredAt] (default now); its day is the local date of that moment (V-14, A-14). */
fun manualMeasurement(kg: Double, measuredAt: Long = System.currentTimeMillis()) =
    BodyweightEntity(epochDay = measuredAt.toLocalDate().toEpochDay(), kg = kg, measuredAt = measuredAt, origin = MeasurementOrigin.MANUAL)

fun BodyweightEntity.toMeasurement() = Measurement(epochDay, measuredAt, kg)

/**
 * Your body weight measurements (#94, decided in #81): a day can have several. Adding never replaces a measurement;
 * each one can be changed (weight and moment) or deleted on its own.
 */
class BodyweightRepository(private val db: AppDatabase) {
    private val dao get() = db.bodyweights()

    /** Newest first. */
    fun observeAll(): Flow<List<BodyweightEntity>> = dao.observeAll()

    /** Adds a measurement of [kg] taken at [measuredAt]; earlier ones of that day stay. Returns its id. */
    suspend fun add(kg: Double, measuredAt: Long = System.currentTimeMillis()): Long = dao.insert(manualMeasurement(kg, measuredAt))

    /** Changes the weight and moment of measurement [id]; its day follows the moment. Its origin stays. */
    suspend fun update(id: Long, kg: Double, measuredAt: Long) {
        val old = dao.get(id) ?: return
        dao.update(old.copy(kg = kg, measuredAt = measuredAt, epochDay = measuredAt.toLocalDate().toEpochDay()))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** The measurement that counts for a training starting at [startedAt]: the last one before it (#94). */
    suspend fun countingFor(startedAt: Long): Measurement? =
        BodyweightLog.countsFor(dao.all().map { it.toMeasurement() }, startedAt, startedAt.toLocalDate().toEpochDay())
}
