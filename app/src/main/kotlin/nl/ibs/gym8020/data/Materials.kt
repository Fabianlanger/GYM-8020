package nl.ibs.gym8020.data

import nl.ibs.gym8020.core.library.DefaultLibrary
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import java.util.concurrent.ConcurrentHashMap

/** Calculators are cached: enumerating plate combinations is not free. */
object Calculators {
    private val cache = ConcurrentHashMap<Pair<MaterialProfile, PlateStock>, WeightCalculator>()

    fun of(profile: MaterialProfile, stock: PlateStock): WeightCalculator =
        cache.getOrPut(profile to stock) { WeightCalculator(profile, stock) }

    fun forExercise(ex: ExerciseEntity, profiles: Map<Long, MaterialProfileEntity>, stock: PlateStock): WeightCalculator? =
        ex.materialProfileId?.let { profiles[it] }?.let { of(it.toProfile(), stock) }
}

/** Icon for an exercise: its own choice, the library default, or a guess from equipment and load. */
val ExerciseEntity.movementOrDefault: Movement
    get() = movement ?: libraryKey?.let(DefaultLibrary::movementFor) ?: when {
        loadType == LoadType.BODYWEIGHT -> Movement.CORE
        equipment == Equipment.BARBELL -> Movement.SQUAT
        equipment == Equipment.CABLE -> Movement.TRICEPS
        equipment == Equipment.DUMBBELL -> Movement.CURL
        else -> Movement.LEG_MACHINE
    }
