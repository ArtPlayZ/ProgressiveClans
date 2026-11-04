package artplay

import artplay.clan.ClanManager
import artplay.command.ClanCommand
import artplay.config.ConfigManager
import artplay.database.DatabaseManager
import artplay.spawn.SpawnLimiterListener
import artplay.upgrade.UpgradeManager
import org.bukkit.plugin.java.JavaPlugin

class ProgressiveClansPlugin : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set

    lateinit var databaseManager: DatabaseManager
        private set

    lateinit var clanManager: ClanManager
        private set

    override fun onEnable() {
        saveDefaultConfig()

        configManager = ConfigManager(this).also { it.load() }
        databaseManager = DatabaseManager(this).also { it.init() }
 val upgradeManager = UpgradeManager(databaseManager)
 clanManager = ClanManager(this, databaseManager, upgradeManager).also { it.loadAll() }

        val command = ClanCommand(this)
        getCommand("clan")?.let {
            it.setExecutor(command)
            it.tabCompleter = command
        }

 server.pluginManager.registerEvents(SpawnLimiterListener(this), this)
 server.pluginManager.registerEvents(artplay.upgrade.UpgradeEffects(this, clanManager), this)
 server.pluginManager.registerEvents(artplay.gui.UpgradeGui(this, clanManager), this)
 server.pluginManager.registerEvents(artplay.upgrade.UpgradeAuras(this, clanManager), this)
 server.pluginManager.registerEvents(artplay.upgrade.UpgradeMisc(this, clanManager), this)
        logger.info("ProgressiveClans включён. Загружено кланов: ${clanManager.allClans().size}.")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
        logger.info("ProgressiveClans выключен.")
    }
}
