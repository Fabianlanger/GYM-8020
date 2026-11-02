package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.backup.Backup
import nl.ibs.gym8020.core.backup.BackupJson
import nl.ibs.gym8020.core.backup.ExerciseDto
import nl.ibs.gym8020.core.backup.MuscleShareDto
import nl.ibs.gym8020.core.backup.PlanDto
import nl.ibs.gym8020.core.backup.PlanSetDto
import nl.ibs.gym8020.core.library.DefaultLibrary
import nl.ibs.gym8020.core.library.ExerciseCodeTable
import nl.ibs.gym8020.core.library.ExerciseCodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryAndBackupTest {
    @Test
    fun `library keys are unique and schemas reference existing exercises`() {
        val keys = DefaultLibrary.exercises.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.size in 45..60, "library size ${keys.size}")
        DefaultLibrary.schemas.flatMap { it.exerciseKeys }.forEach { assertTrue(it in keys, "unknown exercise $it") }
        val schemaKeys = DefaultLibrary.schemas.map { it.key }.toSet()
        DefaultLibrary.weekPlans.flatMap { it.slots }.forEach { assertTrue(it.first in schemaKeys, "unknown schema ${it.first}") }
        assertTrue(DefaultLibrary.weekPlans.any { it.key == DefaultLibrary.DEFAULT_ACTIVE_WEEK_PLAN })
    }

    @Test
    fun `every exercise has a primary muscle`() {
        DefaultLibrary.exercises.forEach { ex -> assertTrue(ex.muscles.any { it.percent == 100 }, ex.key) }
    }

    @Test
    fun `full body schema follows the owner's order`() {
        assertEquals(
            listOf(
                "back_squat", "incline_bench_press", "pull_up", "seated_calf_raise", "overhead_press",
                "seated_cable_row", "decline_crunch", "cable_biceps_curl", "cable_triceps_pushdown",
            ),
            DefaultLibrary.schemas.first { it.key == "full_body" }.exerciseKeys,
        )
    }

    @Test
    fun `backup round trip`() {
        val b = Backup(
            exportedAt = 1L,
            exercises = listOf(ExerciseDto(1, "pull_up", "Pull-up", "BODYWEIGHT", "BODYWEIGHT", "WEIGHT", 1.0, muscles = listOf(MuscleShareDto("LATS", 100)))),
        )
        assertEquals(b, BackupJson.decode(BackupJson.encode(b)))
    }

    @Test
    fun `#69 a reps target goes along in the backup`() {
        val b = Backup(exportedAt = 1L, plans = listOf(PlanDto(1, sets = listOf(PlanSetDto("HEAVY", null, 18), PlanSetDto("LIGHT", null, 30)))))
        assertEquals(b, BackupJson.decode(BackupJson.encode(b)))
    }

    @Test
    fun `#69 an older backup without reps targets is still read`() {
        val text = """{"format":"${Backup.FORMAT}","exportedAt":1,"plans":[{"exerciseId":1,"sets":[{"role":"HEAVY","weightKg":80.0}]}]}"""
        assertEquals(listOf(PlanSetDto("HEAVY", 80.0, null)), BackupJson.decode(text).plans.single().sets)
    }

    @Test
    fun `rejects foreign json`() {
        assertFailsWith<IllegalArgumentException> { BackupJson.decode("""{"format":"other","exportedAt":1}""") }
    }
}

class LibraryMaterialTest {
    @Test
    fun `every material reference exists`() {
        val keys = DefaultLibrary.materials.map { it.key }.toSet()
        DefaultLibrary.exercises.forEach { ex -> DefaultLibrary.materialFor(ex)?.let { assertTrue(it in keys, "${ex.key} → $it") } }
        assertEquals(keys.size, DefaultLibrary.materials.size)
    }

    @Test
    fun `only core exercises fall back to the core icon`() {
        DefaultLibrary.exercises.filter { DefaultLibrary.movementFor(it.key) == nl.ibs.gym8020.core.model.Movement.CORE }
            .forEach { assertTrue(it.muscles.any { m -> m.muscle.group == nl.ibs.gym8020.core.model.MuscleGroup.CORE }, it.key) }
    }
}

/** A-5 (#18): standard codes per library exercise for Health Connect and Garmin FIT. */
class ExerciseCodesTest {
    @Test
    fun `codes belong to library exercises and use known names`() {
        val keys = DefaultLibrary.exercises.map { it.key }.toSet()
        ExerciseCodeTable.byKey.forEach { (key, codes) ->
            assertTrue(key in keys, "unknown exercise $key")
            codes.healthConnect?.let { assertTrue(it in ExerciseCodeTable.healthConnectTypes, "$key → $it") }
            codes.garmin?.let { assertTrue(it in ExerciseCodeTable.garminCategories, "$key → $it") }
        }
    }

    @Test
    fun `the main lifts have both codes, unclear ones none`() {
        assertEquals(ExerciseCodes("SQUAT", "squat"), ExerciseCodeTable.of("back_squat"))
        assertEquals(ExerciseCodes("BENCH_PRESS", "bench_press"), ExerciseCodeTable.of("bench_press"))
        assertEquals(ExerciseCodes("DEADLIFT", "deadlift"), ExerciseCodeTable.of("deadlift"))
        assertNull(ExerciseCodeTable.of("russian_twist"))
        assertNull(ExerciseCodeTable.of(null))
    }
}
