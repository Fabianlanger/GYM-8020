package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.DefaultWarmup
import nl.ibs.gym8020.core.logic.Warmup
import nl.ibs.gym8020.core.logic.WarmupMeasure
import nl.ibs.gym8020.core.logic.WarmupMeasure.REPS
import nl.ibs.gym8020.core.logic.WarmupMeasure.SIGNED_WEIGHT
import nl.ibs.gym8020.core.logic.WarmupMeasure.WEIGHT
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.LoadType.BODYWEIGHT
import nl.ibs.gym8020.core.model.LoadType.EXTERNAL
import nl.ibs.gym8020.core.model.Progression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** #93: the default warm-up, prefilled once from the material, and when "Bewaar … als standaard" shows. */
class DefaultWarmupTest {

    private fun calc(profile: MaterialProfile) = WeightCalculator(profile, PlateStock.DEFAULT)

    private fun prefill(
        loadType: LoadType = EXTERNAL,
        progression: Progression = Progression.WEIGHT,
        factor: Double = 1.0,
        equipment: Equipment = Equipment.MACHINE,
        calc: WeightCalculator? = null,
        assistKg: Double? = null,
    ): Double? = DefaultWarmup.prefill(DefaultWarmup.measureOf(loadType, progression, factor), loadType, factor, equipment, calc, assistKg)

    // ---- measure ----

    @Test
    fun `the value matches the exercise - kg, plus-or-minus weight or reps`() {
        assertEquals(WEIGHT, DefaultWarmup.measureOf(EXTERNAL, Progression.WEIGHT, 1.0))
        assertEquals(SIGNED_WEIGHT, DefaultWarmup.measureOf(BODYWEIGHT, Progression.WEIGHT, 0.65))
        assertEquals(SIGNED_WEIGHT, DefaultWarmup.measureOf(BODYWEIGHT, Progression.WEIGHT, 1.0))
        assertEquals(REPS, DefaultWarmup.measureOf(BODYWEIGHT, Progression.REPS, 0.30))
        assertEquals(REPS, DefaultWarmup.measureOf(EXTERNAL, Progression.REPS, 1.0))
        // Pull-ups and dips warm up with assistance, also on reps.
        assertEquals(SIGNED_WEIGHT, DefaultWarmup.measureOf(BODYWEIGHT, Progression.REPS, 0.95))
    }

    // ---- prefill per material ----

    @Test
    fun `a bar prefills the empty bar`() {
        assertEquals(20.0, prefill(equipment = Equipment.BARBELL, calc = calc(MaterialProfile(MaterialKind.PLATES, baseKg = 20.0))))
        assertEquals(10.0, prefill(equipment = Equipment.BARBELL, calc = calc(MaterialProfile(MaterialKind.PLATES, baseKg = 10.0))))
        // Without a material profile a barbell is an Olympic bar.
        assertEquals(DefaultWarmup.EMPTY_BAR_KG, prefill(equipment = Equipment.BARBELL))
    }

    @Test
    fun `a plate-loaded machine prefills its empty sled`() {
        assertEquals(0.0, prefill(calc = calc(MaterialProfile(MaterialKind.PLATES, baseKg = 0.0))))
    }

    @Test
    fun `a pin stack or dumbbells prefill the lightest pin`() {
        assertEquals(5.0, prefill(calc = calc(MaterialProfile(MaterialKind.STACK, minKg = 5.0, stepKg = 5.0))))
        assertEquals(2.5, prefill(equipment = Equipment.CABLE, calc = calc(MaterialProfile(MaterialKind.STACK, minKg = 2.5, stepKg = 2.5))))
        assertEquals(2.0, prefill(equipment = Equipment.DUMBBELL, calc = calc(MaterialProfile(MaterialKind.DUMBBELLS, minKg = 2.0, stepKg = 2.0))))
        // A stack that starts at 0: the first pin is one step up.
        assertEquals(7.0, prefill(calc = calc(MaterialProfile(MaterialKind.STACK, minKg = 0.0, stepKg = 7.0))))
    }

    @Test
    fun `without material a machine has nothing to prefill`() {
        assertNull(prefill(equipment = Equipment.MACHINE))
        assertNull(prefill(equipment = Equipment.DUMBBELL))
    }

    @Test
    fun `pull-ups and dips prefill their warm-up assistance, none while the body weight is unknown`() {
        val assisted = calc(MaterialProfile(MaterialKind.BODYWEIGHT, stepKg = 5.0))
        val assist = Warmup.assistKg(80.0, 1.0, Warmup.DEFAULT_ASSIST_PERCENT, assisted)
        assertEquals(-40.0, prefill(BODYWEIGHT, factor = 1.0, equipment = Equipment.BODYWEIGHT, calc = assisted, assistKg = assist))
        assertNull(prefill(BODYWEIGHT, factor = 0.95, equipment = Equipment.BODYWEIGHT, calc = assisted, assistKg = null))
    }

    @Test
    fun `other bodyweight exercises prefill body weight only`() {
        assertEquals(0.0, prefill(BODYWEIGHT, factor = 0.65, equipment = Equipment.BODYWEIGHT))
    }

    @Test
    fun `an exercise on reps prefills a few mini-reps`() {
        assertEquals(DefaultWarmup.MINI_REPS.toDouble(), prefill(BODYWEIGHT, Progression.REPS, factor = 0.30, equipment = Equipment.BODYWEIGHT))
    }

    // ---- once: only while there is no default value ----

    @Test
    fun `the prefill only applies while there is no default value - after that it is yours`() {
        val yours = DefaultWarmup(WEIGHT, 30.0, "slow")
        assertEquals(yours, DefaultWarmup.effective(yours, WEIGHT, prefill = 20.0))
        assertEquals(DefaultWarmup(WEIGHT, 20.0, null), DefaultWarmup.effective(null, WEIGHT, prefill = 20.0))
        // A note without a value keeps its note and gets the prefilled value.
        assertEquals(DefaultWarmup(WEIGHT, 20.0, "slow"), DefaultWarmup.effective(DefaultWarmup(WEIGHT, null, "slow"), WEIGHT, 20.0))
        assertNull(DefaultWarmup.effective(null, WEIGHT, prefill = null))
    }

    // ---- storage: reps in the kg column ----

    @Test
    fun `reading back - reps live in the kg column as whole reps, a blank note is none`() {
        assertEquals(DefaultWarmup(REPS, 5.0, null), DefaultWarmup.read(5.0, " ", REPS))
        assertEquals(5, DefaultWarmup.read(5.0, null, REPS)?.reps)
        assertNull(DefaultWarmup.read(5.0, null, REPS)?.weightKg)
        assertEquals(-40.0, DefaultWarmup.read(-40.0, null, SIGNED_WEIGHT)?.weightKg)
        assertNull(DefaultWarmup.read(-40.0, null, SIGNED_WEIGHT)?.reps)
        assertNull(DefaultWarmup.read(null, null, WEIGHT))
        assertNull(DefaultWarmup.read(null, "  ", WEIGHT))
        assertEquals(DefaultWarmup(WEIGHT, null, "rotations"), DefaultWarmup.read(null, " rotations ", WEIGHT))
    }

    @Test
    fun `storing - reps as a whole number, note trimmed`() {
        assertEquals(6.0, DefaultWarmup(REPS, 5.6, null).storedValue)
        assertEquals(-12.5, DefaultWarmup(SIGNED_WEIGHT, -12.5, null).storedValue)
        assertEquals("slow", DefaultWarmup(WEIGHT, 20.0, "  slow ").storedNote)
        assertNull(DefaultWarmup(WEIGHT, 20.0, "  ").storedNote)
    }

    // ---- variant B: when "Bewaar … als standaard" shows ----

    @Test
    fun `the save link shows only when the warm-up set differs from the default`() {
        val default = DefaultWarmup(WEIGHT, 40.0, null)
        assertTrue(DefaultWarmup.offersSave(default, WEIGHT, 50.0))
        assertFalse(DefaultWarmup.offersSave(default, WEIGHT, 40.0))
        assertFalse(DefaultWarmup.offersSave(default, WEIGHT, 40.0000001))
        assertFalse(DefaultWarmup.offersSave(default, WEIGHT, null))
        // No default value yet: any value can become the default.
        assertTrue(DefaultWarmup.offersSave(null, WEIGHT, 20.0))
        assertTrue(DefaultWarmup.offersSave(DefaultWarmup(WEIGHT, null, "slow"), WEIGHT, 20.0))
        // Assistance: −30 is not −40.
        assertTrue(DefaultWarmup.offersSave(DefaultWarmup(SIGNED_WEIGHT, -40.0), SIGNED_WEIGHT, -30.0))
        assertFalse(DefaultWarmup.offersSave(DefaultWarmup(SIGNED_WEIGHT, 0.0), SIGNED_WEIGHT, 0.0))
        // Reps compare as whole reps.
        assertFalse(DefaultWarmup.offersSave(DefaultWarmup(REPS, 5.0), REPS, 5.0))
        assertTrue(DefaultWarmup.offersSave(DefaultWarmup(REPS, 5.0), REPS, 8.0))
    }

    @Test
    fun `the value of a warm-up set is its reps on reps, else its weight`() {
        assertEquals(8.0, DefaultWarmup.valueOf(REPS, 0.0, 8))
        assertEquals(40.0, DefaultWarmup.valueOf(WEIGHT, 40.0, 8))
        assertNull(DefaultWarmup.valueOf(WarmupMeasure.SIGNED_WEIGHT, null, 8))
    }
}
