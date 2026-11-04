package artplay.upgrade

import artplay.ProgressiveClansPlugin
import artplay.clan.ClanManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

class UpgradeMisc(
 private val plugin: ProgressiveClansPlugin,
 private val clanManager: ClanManager
) : Listener {

 private fun has(p: Player, t: UpgradeType): Boolean {
 val clan = clanManager.clanOf(p) ?: return false
 return clanManager.upgradeManager.isPurchased(clan.id, t)
 }

 @EventHandler
 fun onFall(e: EntityDamageEvent) {
 val p = e.entity as? Player ?: return
 if (e.cause != EntityDamageEvent.DamageCause.FALL) return
 if (has(p, UpgradeType.ACROBAT)) e.damage = e.damage * 0.7
 if (has(p, UpgradeType.BOGATYR)) e.damage = e.damage * 0.8
 }

 @EventHandler
 fun onHit(e: EntityDamageByEntityEvent) {
 val p = e.damager as? Player ?: return
 if (has(p, UpgradeType.SHIELD_MASTER) && Random.nextDouble() < 0.05) e.isCancelled = true
 if (has(p, UpgradeType.ENTOMOLOGIST_1) && e.entity is org.bukkit.entity.Spider) e.damage = e.damage * 1.05
 if (has(p, UpgradeType.ENTOMOLOGIST_2) && e.entity is org.bukkit.entity.Spider) e.damage = e.damage * 1.10
 if (has(p, UpgradeType.ENTOMOLOGIST_3) && e.entity is org.bukkit.entity.Spider) e.damage = e.damage * 1.15
 if (has(p, UpgradeType.RAIDER) && e.entity is org.bukkit.entity.Raider) e.damage = e.damage * 1.10
 if (has(p, UpgradeType.IGNITER) && Random.nextDouble() < 0.05) e.entity.fireTicks = 100
 }

 @EventHandler
 fun onVoid(e: EntityDamageEvent) {
 val p = e.entity as? Player ?: return
 if (e.cause != EntityDamageEvent.DamageCause.VOID) return
 if (has(p, UpgradeType.VOID_STEP)) {
 e.isCancelled = true
 p.teleport(p.world.spawnLocation)
 }
 }

 @EventHandler
 fun onMove(e: PlayerMoveEvent) {
 val p = e.player
 if (has(p, UpgradeType.HARDNESS_1)) p.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 40, 0))
 if (has(p, UpgradeType.VITALITY)) p.addPotionEffect(PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 0))
 if (has(p, UpgradeType.BOGATYR)) p.addPotionEffect(PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 1))
 if (has(p, UpgradeType.TIRELESS)) p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 40, 0))
 }
}
