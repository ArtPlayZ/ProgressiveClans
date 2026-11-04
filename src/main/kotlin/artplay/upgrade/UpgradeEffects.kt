package artplay.upgrade

import artplay.ProgressiveClansPlugin
import artplay.clan.ClanManager
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

class UpgradeEffects(
 private val plugin: ProgressiveClansPlugin,
 private val clanManager: ClanManager
) : Listener {

 private fun has(p: Player, t: UpgradeType): Boolean {
 val clan = clanManager.clanOf(p) ?: return false
 return clanManager.upgradeManager.isPurchased(clan.id, t)
 }

 @EventHandler
 fun onBlockBreak(e: BlockBreakEvent) {
 val p = e.player
 fun drop() { e.block.drops.forEach { d -> e.block.world.dropItemNaturally(e.block.location, d) } }
 if (has(p, UpgradeType.MINER_1) && Random.nextDouble() < 0.02) drop()
 if (has(p, UpgradeType.MINER_2) && Random.nextDouble() < 0.03) drop()
 if (has(p, UpgradeType.MINER_3) && Random.nextDouble() < 0.04) drop()
 if (has(p, UpgradeType.MINER_4) && Random.nextDouble() < 0.05) drop()
 if (has(p, UpgradeType.MINER_5) && Random.nextDouble() < 0.06) drop()
 if (has(p, UpgradeType.MINER_6) && Random.nextDouble() < 0.07) drop()
 if (has(p, UpgradeType.MINER_7) && Random.nextDouble() < 0.08) drop()
 if (has(p, UpgradeType.MINER_8) && Random.nextDouble() < 0.10) drop()
 if (has(p, UpgradeType.AGRONOM_1) && Random.nextDouble() < 0.02) drop()
 if (has(p, UpgradeType.AGRONOM_2) && Random.nextDouble() < 0.03) drop()
 if (has(p, UpgradeType.AGRONOM_3) && Random.nextDouble() < 0.04) drop()
 if (has(p, UpgradeType.AGRONOM_4) && Random.nextDouble() < 0.05) drop()
 if (has(p, UpgradeType.AGRONOM_5) && Random.nextDouble() < 0.06) drop()
 if (has(p, UpgradeType.AGRONOM_6) && Random.nextDouble() < 0.08) drop()
 if (has(p, UpgradeType.AGRONOM_7) && Random.nextDouble() < 0.10) drop()
 if (has(p, UpgradeType.FISHER) && Random.nextDouble() < 0.05) drop()
 if (has(p, UpgradeType.TREASURE_HUNTER) && Random.nextDouble() < 0.02) drop()
 if (has(p, UpgradeType.GOLD_DIGGER) && Random.nextDouble() < 0.05) drop()
 if (has(p, UpgradeType.ARCHAEOLOGIST) && Random.nextDouble() < 0.05) drop()
 }

 @EventHandler
 fun onDeath(e: EntityDeathEvent) {
 val p = e.entity.killer ?: return
 fun drop() { e.drops.forEach { d -> e.entity.world.dropItemNaturally(e.entity.location, d) } }
 if (has(p, UpgradeType.BUTCHER_1) && Random.nextDouble() < 0.03) drop()
 if (has(p, UpgradeType.BUTCHER_2) && Random.nextDouble() < 0.05) drop()
 if (has(p, UpgradeType.BUTCHER_3) && Random.nextDouble() < 0.08) drop()
 if (has(p, UpgradeType.BUTCHER_4) && Random.nextDouble() < 0.10) drop()
 if (has(p, UpgradeType.BUTCHER_5) && Random.nextDouble() < 0.12) drop()
 }

 @EventHandler
 fun onDamage(e: EntityDamageByEntityEvent) {
 val p = e.damager as? Player ?: return
 if (has(p, UpgradeType.VAMPIRE) && Random.nextDouble() < 0.05) {
 p.health = minOf(p.maxHealth, p.health + 2.0)
 }
 if (e.entity is Monster && has(p, UpgradeType.HUNTER_1)) e.damage = e.damage * 1.05
 if (e.entity is Monster && has(p, UpgradeType.HUNTER_2)) e.damage = e.damage * 1.10
 if (e.entity is Monster && has(p, UpgradeType.HUNTER_3)) e.damage = e.damage * 1.15
 if (has(p, UpgradeType.POISON_BLADE) && Random.nextDouble() < 0.05) {
 e.entity as? org.bukkit.entity.LivingEntity ?: return
 (e.entity as org.bukkit.entity.LivingEntity).addPotionEffect(PotionEffect(PotionEffectType.POISON, 100, 0))
 }
 if (has(p, UpgradeType.ICE_STRIKE) && Random.nextDouble() < 0.05) {
 (e.entity as? org.bukkit.entity.LivingEntity)?.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 60, 1))
 }
 if (has(p, UpgradeType.EXHAUSTION) && Random.nextDouble() < 0.05) {
 (e.entity as? org.bukkit.entity.LivingEntity)?.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 100, 0))
 }
 }
}
