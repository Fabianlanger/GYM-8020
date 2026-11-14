package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.backup.Backup
import nl.ibs.gym8020.core.backup.BackupJson
import nl.ibs.gym8020.core.backup.SessionDto
import nl.ibs.gym8020.core.backup.SessionExerciseDto
import nl.ibs.gym8020.core.backup.TargetDto
import nl.ibs.gym8020.core.backup.TargetSetDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A-7: schema v4 (#92) keeps who set each set of a target; v3 backups stay readable. Synthetic data. */
class BackupFormatV4Test {
    @Test
    fun `a v4 backup comes back with the origin per target set`() {
        val b = Backup(
            exportedAt = 1L,
            sessions = listOf(
                SessionDto(
                    7, schemaName = "Legs", startedAt = 10, endedAt = 20,
                    exercises = listOf(
                        SessionExerciseDto(
                            exerciseId = 1,
                            target = TargetDto(
                                "YOURS", setAt = 15,
                                sets = listOf(TargetSetDto("HEAVY", 77.5, origin = "YOURS"), TargetSetDto("LIGHT", 30.0, origin = "DEFAULT")),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val back = BackupJson.decode(BackupJson.encode(b))

        assertEquals(b, back)
        assertEquals(4, back.version)
    }

    /** A target as a version 3 file holds it (synthetic data): no origin per set. */
    private val v3 = """
        {"format":"gym-8020-backup","version":3,"exportedAt":1,
         "sessions":[{"id":5,"schemaName":"Legs","startedAt":10,"endedAt":20,
                      "exercises":[{"exerciseId":1,"target":{"origin":"YOURS","note":"deeper","setAt":15,
                                    "sets":[{"role":"HEAVY","weightKg":80.0,"reps":null},{"role":"LIGHT","weightKg":40.0}]}}]}]}
    """.trimIndent()

    @Test
    fun `a v3 backup is read, its target sets without an origin of their own`() {
        val b = BackupJson.decode(v3)

        assertEquals(3, b.version)
        val target = b.sessions.single().exercises.single().target!!
        assertEquals("YOURS", target.origin)
        assertEquals(listOf(TargetSetDto("HEAVY", 80.0), TargetSetDto("LIGHT", 40.0)), target.sets)
        assertNull(target.sets.first().origin)
    }
}
