package artplay.gui

import artplay.ProgressiveClansPlugin
import artplay.database.Clan
import artplay.clan.ClanManager
import artplay.upgrade.UpgradeType
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class UpgradeGui(
 private val plugin: ProgressiveClansPlugin,
 private val clanManager: ClanManager
) : Listener {

 companion object {
 const val TITLE = "Улучшения клана"
 const val SIZE = 54
 const val ITEMS_PER_PAGE = 45
 }

 fun open(player: Player, clan: Clan) {
 val inv = Bukkit.createInventory(null, SIZE, TITLE)
 val all = UpgradeType.values()
 val end = minOf(ITEMS_PER_PAGE, all.size)
 for (i in 0 until end) {
 val type = all[i]
 val bought = clanManager.upgradeManager.isPurchased(clan.id, type)
 inv.setItem(i, createItem(type, bought))
 }
 val glass = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
 glass.itemMeta = glass.itemMeta?.apply { setDisplayName(" ") }
 for (i in ITEMS_PER_PAGE until SIZE) inv.setItem(i, glass)
 player.openInventory(inv)
 }

 private fun createItem(type: UpgradeType, bought: Boolean): ItemStack {
 val item = ItemStack(if (bought) Material.EMERALD else Material.DIAMOND)
 val meta = item.itemMeta ?: return item
 meta.setDisplayName(ChatColor.GREEN.toString() + type.displayName)
 meta.lore = listOf(
 ChatColor.GRAY.toString() + "Уровень: " + type.tier,
 ChatColor.GRAY.toString() + "Цена: " + type.baseDiamond,
 if (bought) ChatColor.GREEN.toString() + "Куплено" else ChatColor.YELLOW.toString() + "Нажмите"
 )
 item.itemMeta = meta
 return item
 }

 @EventHandler
 fun onClick(e: InventoryClickEvent) {
 if (e.view.title != TITLE) return
 e.isCancelled = true
 val p = e.whoClicked as? Player ?: return
 val clan = clanManager.clanOf(p) ?: return
 val item = e.currentItem ?: return
 if (item.type == Material.GRAY_STAINED_GLASS_PANE) return
 val name = item.itemMeta?.displayName ?: return
 val type = UpgradeType.values().firstOrNull { name.contains(it.displayName) } ?: return
 if (clanManager.upgradeManager.isPurchased(clan.id, type)) {
 p.sendMessage(ChatColor.RED.toString() + "Уже куплено")
 return
 }
 try {
 clanManager.upgradeManager.purchase(clan.id, type, clan.balance.toLong())
 p.sendMessage(ChatColor.GREEN.toString() + "Куплено!")
 open(p, clan)
 } catch (ex: Exception) {
 p.sendMessage(ChatColor.RED.toString() + "Ошибка: " + ex.message)
 }
 }
}
