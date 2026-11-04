package artplay.spawn

import org.bukkit.Location
import org.bukkit.util.BoundingBox

/**
 * Зона, в которой ограничивается естественный спавн мобов.
 *
 * chance — вероятность отмены спавна (0.0–1.0),
 * maxMobs — максимальное число мобов данного типа в зоне.
 */
sealed class SpawnZone {

    abstract val world: String
    abstract val chance: Double
    abstract val maxMobs: Int

    abstract fun contains(location: Location): Boolean

    /** Область подсчёта мобов вокруг точки спавна. */
    abstract fun countBox(location: Location): BoundingBox

    /** Вся поверхность мира. */
    data class Surface(
        override val world: String,
        override val chance: Double,
        override val maxMobs: Int
    ) : SpawnZone() {

        override fun contains(location: Location): Boolean =
            location.world?.name == world

        override fun countBox(location: Location): BoundingBox {
            val actorWorld = requireNotNull(location.world) { "Location without world" }
            return BoundingBox(
                location.x - 64.0,
                actorWorld.minHeight.toDouble(),
                location.z - 64.0,
                location.x + 64.0,
                actorWorld.maxHeight.toDouble(),
                location.z + 64.0
            )
        }
    }

    /** Вертикальный слой мира: minY..maxY. */
    data class Height(
        override val world: String,
        val minY: Int,
        val maxY: Int,
        override val chance: Double,
        override val maxMobs: Int
    ) : SpawnZone() {

        override fun contains(location: Location): Boolean {
            if (location.world?.name != world) {
                return false
            }
            val y = location.blockY
            return y in minY..maxY
        }

        override fun countBox(location: Location): BoundingBox = BoundingBox(
            location.x - 64.0,
            minY.toDouble(),
            location.z - 64.0,
            location.x + 64.0,
            (maxY + 1).toDouble(),
            location.z + 64.0
        )
    }

    /** Прямоугольная область в плоскости X/Z. */
    data class Rect(
        override val world: String,
        val minX: Int,
        val minZ: Int,
        val maxX: Int,
        val maxZ: Int,
        override val chance: Double,
        override val maxMobs: Int
    ) : SpawnZone() {

        override fun contains(location: Location): Boolean {
            if (location.world?.name != world) {
                return false
            }
            val x = location.blockX
            val z = location.blockZ
            return x in minX..maxX && z in minZ..maxZ
        }

        override fun countBox(location: Location): BoundingBox {
            val actorWorld = requireNotNull(location.world) { "Location without world" }
            return BoundingBox(
                minX.toDouble(),
                actorWorld.minHeight.toDouble(),
                minZ.toDouble(),
                (maxX + 1).toDouble(),
                actorWorld.maxHeight.toDouble(),
                (maxZ + 1).toDouble()
            )
        }
    }
}
