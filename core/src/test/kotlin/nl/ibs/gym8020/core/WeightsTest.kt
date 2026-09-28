package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.EffectiveWeight
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.LoadType
import kotlin.test.Test
import kotlin.test.assertEquals

class WeightsTest {
    @Test
    fun `assisted pull-up example from the spec`() {
        assertEquals(60.0, EffectiveWeight.of(LoadType.BODYWEIGHT, -20.0, 80.0, 1.0))
        assertEquals(50.0, EffectiveWeight.of(LoadType.BODYWEIGHT, -20.0, 70.0, 1.0))
    }

    @Test
    fun `external and weighted bodyweight`() {
        assertEquals(100.0, EffectiveWeight.of(LoadType.EXTERNAL, 100.0, 80.0, 1.0))
        assertEquals(86.0, EffectiveWeight.of(LoadType.BODYWEIGHT, 10.0, 80.0, 0.95)!!, 1e-9)
        assertEquals(null, EffectiveWeight.of(LoadType.BODYWEIGHT, 10.0, null, 0.95))
    }

    @Test
    fun `epley`() {
        assertEquals(72.0, Strength.e1rm(60.0, 6), 1e-9)
        assertEquals(100.0, Strength.e1rm(100.0, 1), 1e-9)
        assertEquals(0.0, Strength.e1rm(100.0, 0), 1e-9)
    }

    @Test
    fun `format weights`() {
        assertEquals("60", WeightFormat.format(60.0))
        assertEquals("62,5", WeightFormat.format(62.5))
        assertEquals("1,25", WeightFormat.format(1.25))
        assertEquals("+10", WeightFormat.format(10.0, signed = true))
        assertEquals("−20", WeightFormat.format(-20.0, signed = true))
        assertEquals("0", WeightFormat.format(0.0, signed = true))
        assertEquals("220,46", WeightFormat.format(100.0, WeightUnit.LBS))
        assertEquals(100.0, WeightUnit.LBS.toKg(WeightUnit.LBS.fromKg(100.0)), 1e-9)
    }
}
