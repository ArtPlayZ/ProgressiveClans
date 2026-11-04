package artplay.upgrade

import artplay.ProgressiveClansPlugin
import artplay.clan.ClanManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class UpgradeAuras(
 private val plugin: ProgressiveClansPlugin,
 private val clanManager: ClanManager
) : Listener {

 private fun has(p: Player, t: UpgradeType): Boolean {
 val clan = clanManager.clanOf(p) ?: return false
 return clanManager.upgradeManager.isPurchased(clan.id, t)
 }

 @EventHandler
 fun onMove(e: PlayerMoveEvent) {
 val p = e.player
 if (has(p, UpgradeType.SPEEDSTER)) p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 60, 0))
 if (has(p, UpgradeType.NIGHT_EYE)) p.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0))
 if (has(p, UpgradeType.FIREPROOF)) p.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0))
 if (has(p, UpgradeType.COMBAT_SPIRIT)) p.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, 60, 0))
 if (has(p, UpgradeType.JUMPER)) p.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0))
 if (has(p, UpgradeType.CLAN_POWER)) p.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, 60, 0))
 if (has(p, UpgradeType.AURA_MASTER)) p.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 60, 0))
 if (has(p, UpgradeType.DRAGON_SKIN)) p.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0))
 }
}
