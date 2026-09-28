package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.backup.Backup
import nl.ibs.gym8020.core.backup.BackupJson
import nl.ibs.gym8020.core.backup.ExerciseDto
import nl.ibs.gym8020.core.backup.MuscleShareDto
import nl.ibs.gym8020.core.library.DefaultLibrary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `rejects foreign json`() {
        assertFailsWith<IllegalArgumentException> { BackupJson.decode("""{"format":"other","exportedAt":1}""") }
    }
}
