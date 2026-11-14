package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.LastSet
import nl.ibs.gym8020.core.logic.LastTime
import nl.ibs.gym8020.core.logic.PastSession
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LastTimeTest {

    private val sep28 = LocalDate.of(2026, 9, 28)
    private val oct2 = LocalDate.of(2026, 10, 2)

    @Test
    fun `each set shows the most recent session with its date`() {
        val history = listOf(
            PastSession(sep28, listOf(PerformedSet(HEAVY, 77.5, 8), PerformedSet(LIGHT, 40.0, 15))),
            PastSession(oct2, listOf(PerformedSet(HEAVY, 80.0, 7), PerformedSet(LIGHT, 42.5, 14))),
        )
        assertEquals(
            listOf(LastSet(PerformedSet(HEAVY, 80.0, 7), oct2), LastSet(PerformedSet(LIGHT, 42.5, 14), oct2)),
            LastTime.perSet(listOf(HEAVY, LIGHT), history),
        )
    }

    @Test
    fun `a set without reps does not count, so the time before applies with its own date`() {
        val history = listOf(
            PastSession(oct2, listOf(PerformedSet(HEAVY, 80.0, 7), PerformedSet(LIGHT, null, null))),
            PastSession(sep28, listOf(PerformedSet(HEAVY, 77.5, 8), PerformedSet(LIGHT, 40.0, 15))),
        )
        assertEquals(
            listOf(LastSet(PerformedSet(HEAVY, 80.0, 7), oct2), LastSet(PerformedSet(LIGHT, 40.0, 15), sep28)),
            LastTime.perSet(listOf(HEAVY, LIGHT), history),
        )
    }

    @Test
    fun `an exercise you skipped does not count, even when its sets hold numbers`() {
        val history = listOf(
            PastSession(oct2, listOf(PerformedSet(HEAVY, 80.0, 7)), skipped = true),
            PastSession(sep28, listOf(PerformedSet(HEAVY, 77.5, 8))),
        )
        assertEquals(listOf(LastSet(PerformedSet(HEAVY, 77.5, 8), sep28)), LastTime.perSet(listOf(HEAVY), history))
    }

    @Test
    fun `a warm-up has no last time`() {
        val history = listOf(PastSession(oct2, listOf(PerformedSet(WARMUP, 40.0, 10), PerformedSet(HEAVY, 80.0, 7))))
        assertEquals(
            listOf(null, LastSet(PerformedSet(HEAVY, 80.0, 7), oct2)),
            LastTime.perSet(listOf(WARMUP, HEAVY), history),
        )
    }

    @Test
    fun `sets match per role in order, whatever the order of roles`() {
        val history = listOf(
            PastSession(oct2, listOf(PerformedSet(HEAVY, 80.0, 7), PerformedSet(HEAVY, 75.0, 8), PerformedSet(LIGHT, 40.0, 15))),
        )
        assertEquals(
            listOf(
                LastSet(PerformedSet(HEAVY, 80.0, 7), oct2),
                LastSet(PerformedSet(LIGHT, 40.0, 15), oct2),
                LastSet(PerformedSet(HEAVY, 75.0, 8), oct2),
            ),
            LastTime.perSet(listOf(HEAVY, LIGHT, HEAVY), history),
        )
    }

    @Test
    fun `an extra set looks back to the last time you did that many, or has none`() {
        val sep20 = LocalDate.of(2026, 9, 20)
        val history = listOf(
            PastSession(oct2, listOf(PerformedSet(HEAVY, 80.0, 7))),
            PastSession(sep20, listOf(PerformedSet(HEAVY, 75.0, 8), PerformedSet(HEAVY, 72.5, 9))),
        )
        assertEquals(
            listOf(LastSet(PerformedSet(HEAVY, 80.0, 7), oct2), LastSet(PerformedSet(HEAVY, 72.5, 9), sep20), null),
            LastTime.perSet(listOf(HEAVY, HEAVY, HEAVY), history),
        )
    }

    @Test
    fun `an older training entered afterwards does not become the last time`() {
        // Newest training first in the list, the one entered later (older training date) after it.
        val history = listOf(
            PastSession(oct2, listOf(PerformedSet(HEAVY, 80.0, 7))),
            PastSession(sep28, listOf(PerformedSet(HEAVY, 90.0, 5))),
        )
        assertEquals(listOf(LastSet(PerformedSet(HEAVY, 80.0, 7), oct2)), LastTime.perSet(listOf(HEAVY), history))
    }

    @Test
    fun `one date for the whole line when all sets share it`() {
        val same = listOf(null, LastSet(PerformedSet(HEAVY, 80.0, 7), oct2), LastSet(PerformedSet(LIGHT, 40.0, 15), oct2))
        assertEquals(oct2, LastTime.sharedDate(same))
        val mixed = listOf(LastSet(PerformedSet(HEAVY, 80.0, 7), oct2), LastSet(PerformedSet(LIGHT, 40.0, 15), sep28))
        assertEquals(null, LastTime.sharedDate(mixed))
    }
    @Test
    fun `a bodyweight exercise shows last time in effective weight, with the body weight of that training (#87)`() {
        val history = listOf(PastSession(oct2, listOf(PerformedSet(HEAVY, 0.0, 8), PerformedSet(LIGHT, -20.0, 12)), bodyweightKg = 80.0))

        val last = LastTime.perSet(listOf(HEAVY, LIGHT), history)

        assertEquals(listOf(80.0, 60.0), last.map { it?.effectiveKg(LoadType.BODYWEIGHT, factor = 1.0) })
        assertEquals(listOf(72.0, 52.0), last.map { it?.effectiveKg(LoadType.BODYWEIGHT, factor = 0.9) })
    }

    @Test
    fun `last time without the body weight of that training has no effective weight, an external weight is itself`() {
        val history = listOf(PastSession(oct2, listOf(PerformedSet(HEAVY, 5.0, 8))))

        val last = LastTime.perSet(listOf(HEAVY), history).single()!!

        assertEquals(null, last.effectiveKg(LoadType.BODYWEIGHT, factor = 1.0))
        assertEquals(5.0, last.effectiveKg(LoadType.EXTERNAL, factor = 1.0))
    }
}
