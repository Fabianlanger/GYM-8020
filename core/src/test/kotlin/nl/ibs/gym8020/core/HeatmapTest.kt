package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.Heatmap
import nl.ibs.gym8020.core.stats.SetRecord
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeatmapTest {
    private val d = LocalDate.of(2026, 9, 29)
    private val muscles = mapOf(
        1L to listOf(MuscleShare(Muscle.CHEST, 100), MuscleShare(Muscle.TRICEPS, 50)),
        2L to listOf(MuscleShare(Muscle.TRICEPS, 100)),
        3L to listOf(MuscleShare(Muscle.ABS, 100)),
    )
    private fun s(ex: Long, role: SetRole, w: Double?, reps: Int?, prog: Progression = Progression.WEIGHT) =
        SetRecord(1, d, ex, role, w, reps, w, null, prog)

    @Test
    fun `volume per muscle with shares, no warm-ups`() {
        val v = Heatmap.volume(listOf(s(1, SetRole.WARMUP, 20.0, 10), s(1, SetRole.HEAVY, 60.0, 6), s(2, SetRole.LIGHT, 20.0, 15)), muscles)
        assertEquals(360.0, v[Muscle.CHEST])
        assertEquals(180.0 + 300.0, v[Muscle.TRICEPS])
    }

    @Test
    fun `progress compares with the previous time and weights by share`() {
        val now = listOf(s(1, SetRole.HEAVY, 66.0, 6), s(2, SetRole.HEAVY, 20.0, 6))
        val before = mapOf(1L to listOf(s(1, SetRole.HEAVY, 60.0, 6)), 2L to listOf(s(2, SetRole.HEAVY, 20.0, 6)))
        val change = Heatmap.exerciseChange(now, before)
        assertEquals(0.1, change.getValue(1L), 1e-9)
        assertEquals(0.0, change.getValue(2L), 1e-9)
        val p = Heatmap.progress(change, muscles)
        assertEquals(0.1, p.getValue(Muscle.CHEST), 1e-9)
        // triceps: (0.1 × 0.5 + 0 × 1) / 1.5
        assertEquals(0.05 / 1.5, p.getValue(Muscle.TRICEPS), 1e-9)
    }

    @Test
    fun `reps progression and no previous time`() {
        val now = listOf(s(3, SetRole.HEAVY, 0.0, 22, Progression.REPS))
        val change = Heatmap.exerciseChange(now, mapOf(3L to listOf(s(3, SetRole.HEAVY, 0.0, 20, Progression.REPS))))
        assertEquals(0.1, change.getValue(3L), 1e-9)
        assertNull(Heatmap.exerciseChange(now, emptyMap())[3L])
    }
}
