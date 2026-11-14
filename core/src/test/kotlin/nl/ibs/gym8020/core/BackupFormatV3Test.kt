package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.backup.Backup
import nl.ibs.gym8020.core.backup.BackupJson
import nl.ibs.gym8020.core.backup.BodyweightDto
import nl.ibs.gym8020.core.backup.ExerciseDto
import nl.ibs.gym8020.core.backup.GymDto
import nl.ibs.gym8020.core.backup.MaterialDto
import nl.ibs.gym8020.core.backup.PlanSetDto
import nl.ibs.gym8020.core.backup.SessionDto
import nl.ibs.gym8020.core.backup.SessionExerciseDto
import nl.ibs.gym8020.core.backup.SetDto
import nl.ibs.gym8020.core.backup.TargetDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** A-7: the backup format grows with schema v3, and backups from before v3 stay readable. */
class BackupFormatV3Test {
    @Test
    fun `a v3 backup comes back with every new field`() {
        val b = Backup(
            exportedAt = 1L,
            exercises = listOf(
                ExerciseDto(
                    1, "pull_up", "Pull-up", "BODYWEIGHT", "BODYWEIGHT", "WEIGHT", 1.0,
                    defaultWarmupKg = -30.0, defaultWarmupNote = "slow", healthConnectType = "PULL_UP", garminCategory = "pull_up",
                ),
            ),
            materials = listOf(MaterialDto(4, "cable", "Cable", "STACK", 0.0, 1, 2.5, 2.5)),
            bodyweights = listOf(BodyweightDto(20_000, 80.5, measuredAt = 1_728_000_000_000, origin = "WEARABLE")),
            gyms = listOf(GymDto(2, "My gym", "20.0:4,10.0:2")),
            sessions = listOf(
                SessionDto(
                    7, schemaName = "Pull", startedAt = 10, endedAt = 20, origin = "AFTERWARDS", gymId = 2, bodyweightKg = 80.5,
                    exercises = listOf(
                        SessionExerciseDto(
                            exerciseId = 1, materialId = 4,
                            sets = listOf(SetDto("HEAVY", -10.0, 6, 80.5, goalWeightKg = -12.5, goalReps = 6, repsInReserve = 1)),
                            target = TargetDto("YOURS", "next week", 15, listOf(PlanSetDto("HEAVY", -7.5, 6))),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(b, BackupJson.decode(BackupJson.encode(b)))
        assertEquals(3, BackupJson.decode(BackupJson.encode(b)).version)
    }

    @Test
    fun `the backup says whether bodyweight targets are effective weights, older files hold signed loads (#87)`() {
        val effective = Backup(exportedAt = 1L, bodyweightTargetsEffective = true)

        assertEquals(true, BackupJson.decode(BackupJson.encode(effective)).bodyweightTargetsEffective)
        assertEquals(false, BackupJson.decode("""{"format":"gym-8020-backup","version":3,"exportedAt":1}""").bodyweightTargetsEffective)
    }

    /** A version 2 file as the app wrote it before schema v3 (synthetic data). */
    private val v2 = """
        {"format":"gym-8020-backup","version":2,"exportedAt":1,
         "exercises":[{"id":1,"libraryKey":"back_squat","name":"Back squat","equipment":"BARBELL","loadType":"EXTERNAL",
                       "progression":"WEIGHT","bodyweightFactor":1.0,"materialId":3}],
         "materials":[{"id":3,"libraryKey":"olympic_bar","name":"Bar","kind":"PLATES","baseKg":20.0,"loadingPoints":2,
                       "minKg":0.0,"stepKg":5.0,"maxKg":100.0}],
         "bodyweights":[{"date":20000,"kg":80.5}],
         "sessions":[{"id":5,"schemaName":"Legs","startedAt":10,"endedAt":20,
                      "exercises":[{"exerciseId":1,"sets":[{"role":"HEAVY","weightKg":100.0,"reps":6,"goalWeightKg":97.5}]}]}],
         "plans":[{"exerciseId":1,"sets":[{"role":"HEAVY","weightKg":102.5}]}],
         "plateStock":"20.0:4,10.0:4"}
    """.trimIndent()

    @Test
    fun `a v2 backup is read with defaults for the new fields`() {
        val b = BackupJson.decode(v2)

        assertEquals(2, b.version)
        assertEquals(MaterialDto(3, "olympic_bar", "Bar", "PLATES", 20.0, 2, 0.0, 5.0), b.materials.single())
        assertEquals(BodyweightDto(20_000, 80.5, measuredAt = null, origin = "MANUAL"), b.bodyweights.single())
        val session = b.sessions.single()
        assertEquals("LIVE", session.origin)
        assertNull(session.gymId)
        val se = session.exercises.single()
        assertNull(se.target)
        assertNull(se.materialId)
        assertEquals(SetDto("HEAVY", 100.0, 6, null, 97.5, goalReps = null, repsInReserve = null), se.sets.single())
        assertNull(b.exercises.single().healthConnectType)
        assertEquals(emptyList(), b.gyms)
        assertEquals("20.0:4,10.0:4", b.plateStock)
    }

    @Test
    fun `a v1 backup without materials is still read`() {
        val text = """{"format":"gym-8020-backup","version":1,"exportedAt":1,"bodyweights":[{"date":19000,"kg":79.0}]}"""

        assertEquals(listOf(BodyweightDto(19_000, 79.0)), BackupJson.decode(text).bodyweights)
    }

    @Test
    fun `a backup from a newer app is refused`() {
        assertFailsWith<IllegalArgumentException> { BackupJson.decode("""{"format":"gym-8020-backup","version":4,"exportedAt":1}""") }
    }
}
