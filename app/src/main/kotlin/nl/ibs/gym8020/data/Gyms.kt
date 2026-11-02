package nl.ibs.gym8020.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.GymEntity

/**
 * The plate stock of a gym: the gym of the training when it has one, else your gym (the first that is not
 * archived), else [fallback] (the settings value, until it has moved into the gym).
 */
fun plateStockFor(gymId: Long?, gyms: List<GymEntity>, fallback: PlateStock): PlateStock {
    val gym = gyms.firstOrNull { it.id == gymId } ?: defaultGym(gyms)
    return gym?.plates?.let(PlateStock::decode) ?: fallback
}

/** Your gym until gyms can be chosen (E4): the first one that is not archived. */
fun defaultGym(gyms: List<GymEntity>): GymEntity? = gyms.filter { !it.archived }.minByOrNull { it.id }

/**
 * Gyms (A-4, schema v3). The plate stock lived in the settings until v3; it moves into the first gym once, and the
 * settings value stays as the fallback until then. There is no gym screen yet (E4, #50): the plate settings edit
 * your gym.
 */
class Gyms(private val db: AppDatabase, private val settings: SettingsStore) {

    /** The plate stock of your gym. */
    val stock: Flow<PlateStock> = combine(db.gyms().observeAll(), settings.settings) { gyms, s -> plateStockFor(null, gyms, s.plateStock) }

    /** Changes the plate stock of your gym (or the settings value while there is no gym with a stock yet). */
    suspend fun updateStock(transform: (PlateStock) -> PlateStock) {
        val fallback = settings.current().plateStock
        val stored = db.withTransaction {
            val gym = defaultGym(db.gyms().all())?.takeIf { it.plates != null } ?: return@withTransaction false
            db.gyms().update(gym.copy(plates = transform(plateStockFor(gym.id, listOf(gym), fallback)).encode()))
            true
        }
        if (!stored) settings.update { it.copy(plateStock = transform(it.plateStock)) }
    }

    /**
     * Once after opening (idempotent): you have a gym, and every gym without a plate stock gets the one from the
     * settings. The gym the migration made gets a name in your language.
     */
    suspend fun ensureDefault(dutch: Boolean) {
        val stock = settings.current().plateStock.encode()
        db.withTransaction {
            val gyms = db.gyms().all()
            if (gyms.isEmpty()) {
                db.gyms().insert(GymEntity(name = defaultName(dutch), plates = stock))
            } else {
                gyms.filter { it.plates == null }.forEach { gym ->
                    val name = if (gym.name == AppDatabase.MIGRATED_GYM_NAME) defaultName(dutch) else gym.name
                    db.gyms().update(gym.copy(name = name, plates = stock))
                }
            }
        }
    }

    companion object {
        /** Stored in the language of the first start, like the library names (A-10). */
        fun defaultName(dutch: Boolean): String = if (dutch) "Mijn gym" else "My gym"
    }
}
