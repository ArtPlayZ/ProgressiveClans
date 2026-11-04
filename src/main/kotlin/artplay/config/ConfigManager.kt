package artplay.config

import artplay.ProgressiveClansPlugin
import artplay.spawn.SpawnZone
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

/**
 * Читает и хранит настройки плагина.
 */
class ConfigManager(private val plugin: ProgressiveClansPlugin) {

    var baseRadius: Double = 50.0
        private set

    var setbaseCooldownHours: Int = 24
        private set

    /** Мир -> список зон ограничения спавна. */
    var spawnZones: Map<String, List<SpawnZone>> = emptyMap()
        private set

    fun load() {
        plugin.reloadConfig()
        val config = plugin.config
        baseRadius = readDouble(config, "clan.baseRadius", "clan.base-radius") ?: 50.0
        setbaseCooldownHours = (readDouble(config, "clan.setbaseCooldownHours", "clan.setbase-cooldown-hours") ?: 24.0)
            .toInt()
            .coerceAtLeast(0)
        spawnZones = readSpawnZones(config)
    }

    /** Радиус территории клана на указанном уровне базы. */
    fun radiusForLevel(level: Int): Double = baseRadius + (level.coerceAtLeast(1) - 1) * 5.0

    private fun readDouble(config: FileConfiguration, vararg paths: String): Double? {
        for (path in paths) {
            if (config.isSet(path)) {
                return config.getDouble(path)
            }
        }
        return null
    }

    private fun readSpawnZones(config: FileConfiguration): Map<String, List<SpawnZone>> {
        val root = config.getConfigurationSection("spawn.worlds") ?: return emptyMap()
        val result = LinkedHashMap<String, List<SpawnZone>>()
        for (world in root.getKeys(false)) {
            val zones = parseWorld(world, root.get(world))
            if (zones.isNotEmpty()) {
                result[world] = zones
            }
        }
        return result
    }

    private fun parseWorld(world: String, value: Any?): List<SpawnZone> = when (value) {
        is ConfigurationSection -> parseWorldSection(world, value)
        is Map<*, *> -> listOfNotNull(parseZoneMap(world, value))
        is List<*> -> value.mapNotNull { parseZoneItem(world, it) }
        else -> emptyList()
    }

    private fun parseWorldSection(world: String, section: ConfigurationSection): List<SpawnZone> {
        val zoneList = section.getList("zones")
        if (zoneList != null) {
            return zoneList.mapNotNull { parseZoneItem(world, it) }
        }
        if (section.isSet("type")) {
            return listOfNotNull(parseZoneMap(world, section.getValues(false)))
        }

        val children = ArrayList<SpawnZone>()
        for (key in section.getKeys(false)) {
            val child = section.getConfigurationSection(key)
            if (child != null) {
                parseZoneMap(world, child.getValues(false))?.let { children.add(it) }
            }
        }
        if (children.isNotEmpty()) {
            return children
        }
        return listOfNotNull(parseZoneMap(world, section.getValues(false)))
    }

    private fun parseZoneItem(world: String, item: Any?): SpawnZone? = when (item) {
        is ConfigurationSection -> parseZoneMap(world, item.getValues(false))
        is Map<*, *> -> parseZoneMap(world, item)
        else -> null
    }

    private fun parseZoneMap(world: String, raw: Map<*, *>): SpawnZone? {
        if (raw.isEmpty()) {
            return null
        }
        val map = HashMap<String, Any?>()
        for ((key, value) in raw) {
            map[key.toString().lowercase()] = value
        }

        val type = (map["type"] as? String)?.trim()?.lowercase() ?: "surface"
        val chance = (number(map, "chance") ?: 0.5).coerceIn(0.0, 1.0)
        val maxMobs = (number(map, "max-mobs", "maxmobs", "max_mobs") ?: 50.0)
            .toInt()
            .coerceAtLeast(1)

        return when (type) {
            "surface" -> SpawnZone.Surface(world, chance, maxMobs)
            "height" -> {
                val minY = (number(map, "min-y", "miny", "min_y") ?: 0.0).toInt()
                val maxY = (number(map, "max-y", "maxy", "max_y") ?: 256.0).toInt()
                SpawnZone.Height(world, minOf(minY, maxY), maxOf(minY, maxY), chance, maxMobs)
            }
            "rect" -> {
                val minX = (number(map, "min-x", "minx", "min_x") ?: -100.0).toInt()
                val maxX = (number(map, "max-x", "maxx", "max_x") ?: 100.0).toInt()
                val minZ = (number(map, "min-z", "minz", "min_z") ?: -100.0).toInt()
                val maxZ = (number(map, "max-z", "maxz", "max_z") ?: 100.0).toInt()
                SpawnZone.Rect(
                    world,
                    minOf(minX, maxX),
                    minOf(minZ, maxZ),
                    maxOf(minX, maxX),
                    maxOf(minZ, maxZ),
                    chance,
                    maxMobs
                )
            }
            else -> null
        }
    }

    private fun number(map: Map<String, Any?>, vararg keys: String): Double? {
        for (key in keys) {
            val value = map[key]
            if (value is Number) {
                return value.toDouble()
            }
        }
        return null
    }
}
