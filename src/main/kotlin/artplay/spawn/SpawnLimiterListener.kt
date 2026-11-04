package artplay.spawn

import artplay.ProgressiveClansPlugin
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.concurrent.ThreadLocalRandom

/**
 * Ограничивает естественный спавн мобов в настроенных зонах.
 * Железные/снежные големы, дракон Края и Иссушитель не ограничиваются.
 */
class SpawnLimiterListener(private val plugin: ProgressiveClansPlugin) : Listener {

    companion object {
        private val HARD_PROTECTED = setOf(
            EntityType.IRON_GOLEM,
            EntityType.SNOW_GOLEM,
            EntityType.ENDER_DRAGON,
            EntityType.WITHER
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return
        }

        val type = event.entityType
        if (type in HARD_PROTECTED) {
            return
        }

        val location = event.location
        val world = location.world ?: return
        val zones = plugin.configManager.spawnZones[world.name]
            ?: plugin.configManager.spawnZones["*"]
            ?: return
        if (zones.isEmpty()) {
            return
        }

        for (zone in zones) {
            if (!zone.contains(location)) {
                continue
            }
            val nearby = world.getNearbyEntities(zone.countBox(location)) { it.type == type }
            if (nearby.size >= zone.maxMobs) {
                event.isCancelled = true
                return
            }
            if (zone.chance > 0.0 && ThreadLocalRandom.current().nextDouble() < zone.chance) {
                event.isCancelled = true
                return
            }
        }
    }
}
