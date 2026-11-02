package nl.ibs.gym8020.data

import nl.ibs.gym8020.core.logic.Measurement
import nl.ibs.gym8020.core.model.MeasurementOrigin
import nl.ibs.gym8020.data.db.BodyweightEntity

/** A body weight you enter now: today's date and this moment (V-14, A-14). */
fun manualMeasurement(kg: Double, now: Long = System.currentTimeMillis()) =
    BodyweightEntity(epochDay = now.toLocalDate().toEpochDay(), kg = kg, measuredAt = now, origin = MeasurementOrigin.MANUAL)

fun BodyweightEntity.toMeasurement() = Measurement(epochDay, measuredAt, kg)
