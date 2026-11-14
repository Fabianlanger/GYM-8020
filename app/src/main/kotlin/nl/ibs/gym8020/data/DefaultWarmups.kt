package nl.ibs.gym8020.data

import nl.ibs.gym8020.core.logic.DefaultWarmup
import nl.ibs.gym8020.core.logic.WarmupMeasure
import nl.ibs.gym8020.data.db.ExerciseEntity

/*
 * The default warm-up of an exercise (#93) in `exercises.defaultWarmupKg` / `defaultWarmupNote` (schema v3). With
 * reps progression the "kg" column holds reps: always go through these helpers and `DefaultWarmup`.
 */

/** What the exercise's default warm-up value means: kg, a signed extra, or reps. */
val ExerciseEntity.warmupMeasure: WarmupMeasure
    get() = DefaultWarmup.measureOf(loadType, progression, bodyweightFactor)

/** The exercise's default warm-up as stored; null without a value and a note. */
val ExerciseEntity.defaultWarmup: DefaultWarmup?
    get() = DefaultWarmup.read(defaultWarmupKg, defaultWarmupNote, warmupMeasure)

/** This exercise with [warmup] as its default warm-up (null = none). */
fun ExerciseEntity.withDefaultWarmup(warmup: DefaultWarmup?): ExerciseEntity =
    copy(defaultWarmupKg = warmup?.storedValue, defaultWarmupNote = warmup?.storedNote)

/**
 * This exercise after a change from [before]: when the measure of the default warm-up changed (e.g. from weight to
 * reps), the stored value no longer means the same and is dropped, so it is prefilled again; the note stays.
 */
fun ExerciseEntity.keepingWarmupMeaning(before: ExerciseEntity): ExerciseEntity =
    if (warmupMeasure == before.warmupMeasure) this else copy(defaultWarmupKg = null)
