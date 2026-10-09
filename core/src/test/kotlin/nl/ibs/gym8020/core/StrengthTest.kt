package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Change
import nl.ibs.gym8020.core.logic.EffectiveWeight
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.SetRecord
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The strength model (A-3, #12): effective weight, e1RM, strength per exercise, volume and comparison. */
class StrengthTest {
    private val d = LocalDate.of(2026, 10, 1)
    private fun set(role: SetRole, effective: Double?, reps: Int?, ex: Long = 1) =
        SetRecord(1, d, ex, role, effective, reps, effective, null, Progression.WEIGHT)

    // Effective weight (ADR 0001)

    @Test
    fun `bodyweight exercise counts bodyweight share plus or minus the signed load`() {
        // Dips with 95 % of 80 kg, +10 extra.
        assertEquals(86.0, EffectiveWeight.of(LoadType.BODYWEIGHT, 10.0, 80.0, 0.95)!!, 1e-9)
        // Assisted pull-up: 80 kg, −20 support.
        assertEquals(60.0, EffectiveWeight.of(LoadType.BODYWEIGHT, -20.0, 80.0, 1.0))
        assertEquals(80.0, EffectiveWeight.of(LoadType.BODYWEIGHT, 0.0, 80.0, 1.0))
        assertNull(EffectiveWeight.of(LoadType.BODYWEIGHT, 0.0, null, 1.0))
        assertEquals(100.0, EffectiveWeight.of(LoadType.EXTERNAL, 100.0, null, 1.0))
    }

    // e1RM per set

    @Test
    fun `e1RM is Epley, one rep is the weight itself`() {
        assertEquals(120.0, Strength.e1rmOrNull(100.0, 6)!!, 1e-9)
        assertEquals(102.0, Strength.e1rmOrNull(85.0, 6)!!, 1e-9)
        assertEquals(100.0, Strength.e1rmOrNull(100.0, 1)!!, 1e-9)
    }

    @Test
    fun `e1RM is unknown without weight or reps`() {
        assertNull(Strength.e1rmOrNull(null, 6))
        assertNull(Strength.e1rmOrNull(100.0, null))
        assertNull(Strength.e1rmOrNull(100.0, 0))
    }

    @Test
    fun `e1RM rises when weight or reps rise`() {
        val r = Random(12)
        repeat(2000) {
            val w = r.nextInt(1, 400) / 2.0
            val reps = r.nextInt(1, 30)
            val e = Strength.e1rmOrNull(w, reps)!!
            assertTrue(Strength.e1rmOrNull(w + r.nextInt(1, 40) / 4.0, reps)!! > e, "heavier $w × $reps")
            assertTrue(Strength.e1rmOrNull(w, reps + r.nextInt(1, 5))!! > e, "more reps $w × $reps")
        }
    }

    // Strength of an exercise in a session

    @Test
    fun `strength is the best heavy-set e1RM, warm-ups and light sets do not count`() {
        val sets = listOf(
            set(SetRole.WARMUP, 100.0, 10),
            set(SetRole.HEAVY, 85.0, 6),
            set(SetRole.HEAVY, 90.0, 3),
            set(SetRole.LIGHT, 60.0, 20),
        )
        assertEquals(102.0, Strength.of(sets)!!, 1e-9)
        assertEquals(85.0, Strength.bestSet(sets)!!.effectiveKg)
    }

    @Test
    fun `strength is unknown without a heavy set with weight and reps`() {
        assertNull(Strength.of(listOf(set(SetRole.WARMUP, 100.0, 5), set(SetRole.LIGHT, 60.0, 16))))
        assertNull(Strength.of(listOf(set(SetRole.HEAVY, null, 6), set(SetRole.HEAVY, 80.0, null))))
        assertNull(Strength.of(emptyList()))
    }

    // Volume

    @Test
    fun `volume is effective weight times reps over work sets`() {
        val sets = listOf(
            set(SetRole.WARMUP, 40.0, 10),
            set(SetRole.HEAVY, 85.0, 6),
            set(SetRole.LIGHT, 60.0, 16),
            set(SetRole.HEAVY, null, 6),
        )
        assertEquals(1470.0, Strength.volume(sets), 1e-9)
        assertEquals(0.0, Strength.volume(emptyList()), 1e-9)
    }

    @Test
    fun `volume per muscle goes by share`() {
        val muscles = mapOf(
            1L to listOf(MuscleShare(Muscle.CHEST, 100), MuscleShare(Muscle.TRICEPS, 50)),
            2L to listOf(MuscleShare(Muscle.TRICEPS, 100)),
        )
        val v = Strength.volumePerMuscle(
            listOf(set(SetRole.WARMUP, 40.0, 10), set(SetRole.HEAVY, 85.0, 6), set(SetRole.LIGHT, 20.0, 15, ex = 2)),
            muscles,
        )
        assertEquals(510.0, v.getValue(Muscle.CHEST), 1e-9)
        assertEquals(255.0 + 300.0, v.getValue(Muscle.TRICEPS), 1e-9)
        assertNull(v[Muscle.QUADS])
    }

    // Comparison

    @Test
    fun `comparison gives absolute and relative change`() {
        // "e1RM 104 kg (+2,5 kg · +2,4%)"
        val c = Change.between(101.5, 104.0)!!
        assertEquals(2.5, c.absolute, 1e-9)
        assertEquals(0.0246, c.relative!!, 1e-4)
        // "volume 12.400 kg (−1.200 kg)" and reps "16 (+2)"
        assertEquals(-0.25, Change.between(1600.0, 1200.0)!!.relative!!, 1e-9)
        assertEquals(2.0, Change.between(14.0, 16.0)!!.absolute, 1e-9)
    }

    @Test
    fun `comparison against zero has no percentage, against nothing no change`() {
        val fromZero = Change.between(0.0, 350.0)!!
        assertEquals(350.0, fromZero.absolute, 1e-9)
        assertNull(fromZero.relative)
        assertNull(Change.between(null, 104.0))
        assertNull(Change.between(104.0, null))
        assertEquals(0.0, Change.between(104.0, 104.0)!!.relative!!, 1e-9)
    }
}
