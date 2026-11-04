package artplay.command

import artplay.ProgressiveClansPlugin
import artplay.clan.BaseLevelRegistry
import artplay.clan.ClanException
import artplay.database.Clan
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Locale

class ClanCommand(private val plugin: ProgressiveClansPlugin) : CommandExecutor, TabCompleter {

    private val manager get() = plugin.clanManager

    private val subcommands = listOf(
        "create", "invite", "join", "leave", "info", "pay", "setbase", "upgrade", "buy", "upgrades", "list"
    )

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("$PREFIX §cКоманда доступна только игрокам.")
            return true
        }
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        try {
            when (args[0].lowercase(Locale.ROOT)) {
                "create" -> handleCreate(sender, args)
                "invite" -> handleInvite(sender, args)
                "join" -> handleJoin(sender, args)
                "leave" -> handleLeave(sender)
                "info" -> handleInfo(sender, args)
                "pay" -> handlePay(sender, args)
                "setbase" -> handleSetBase(sender)
                "upgrade" -> handleUpgrade(sender, args)
                "buy" -> handleBuy(sender, args)
                "upgrades" -> handleUpgradesList(sender)
                else -> sendHelp(sender)
            }
        } catch (exception: ClanException) {
            sender.sendMessage("$PREFIX §c${exception.message}")
        } catch (exception: Exception) {
            plugin.logger.warning("Ошибка выполнения /clan ${args.joinToString(" ")}: ${exception.message}")
            sender.sendMessage("$PREFIX §cВнутренняя ошибка. Сообщите администратору.")
        }
        return true
    }

    private fun handleCreate(player: Player, args: Array<out String>) {
        requireArgs(args, 2, "/clan create <название>")
        val clan = manager.createClan(player, args[1])
        player.sendMessage("$PREFIX §aКлан §e${clan.name}§a создан! Вы — лидер.")
    }

    private fun handleInvite(player: Player, args: Array<out String>) {
        requireArgs(args, 2, "/clan invite <игрок>")
        val target = Bukkit.getPlayerExact(args[1])
            ?: throw ClanException("Игрок «${args[1]}» не в сети.")
        val clan = manager.invite(player, target)
        player.sendMessage("$PREFIX §aПриглашение отправлено игроку §e${target.name}§a.")
        target.sendMessage(
            "$PREFIX §e${player.name}§a приглашает вас в клан §e${clan.name}§a. " +
                "Вступить: §e/clan join ${clan.name}"
        )
    }

    private fun handleJoin(player: Player, args: Array<out String>) {
        requireArgs(args, 2, "/clan join <название>")
        val clan = manager.join(player, args[1])
        player.sendMessage("$PREFIX §aВы вступили в клан §e${clan.name}§a!")
    }

    private fun handleLeave(player: Player) {
        val outcome = manager.leave(player)
        when {
            outcome.disbanded ->
                player.sendMessage("$PREFIX §eКлан ${outcome.clanName}§a распущен: вы были последним участником.")
            outcome.newLeader != null -> {
                val leaderName = Bukkit.getOfflinePlayer(outcome.newLeader).name
                    ?: outcome.newLeader.toString()
                player.sendMessage(
                    "$PREFIX §aВы покинули клан §e${outcome.clanName}§a. Новый лидер: §e$leaderName§a."
                )
            }
            else -> player.sendMessage("$PREFIX §aВы покинули клан §e${outcome.clanName}§a.")
        }
    }

    private fun handleInfo(player: Player, args: Array<out String>) {
        val clan = if (args.size >= 2) {
            manager.clanByName(args[1])
                ?: throw ClanException("Клан «${args[1]}» не найден.")
        } else {
            manager.clanOf(player)
                ?: throw ClanException("Вы не состоите в клане. Укажите название: /clan info <клан>")
        }
        sendInfo(player, clan)
    }

    private fun handlePay(player: Player, args: Array<out String>) {
        requireArgs(args, 2, "/clan pay <сумма>")
        val amount = args[1].toIntOrNull()
            ?: throw ClanException("Сумма должна быть целым числом.")
        val balance = manager.deposit(player, amount)
        player.sendMessage(
            "$PREFIX §aВнесено §e$amount XP§a (1 XP = 1 монета). Баланс клана: §e${money(balance)}"
        )
    }

    private fun handleSetBase(player: Player) {
        val clan = manager.setBase(player)
        player.sendMessage("$PREFIX §aБаза клана установлена: §e${baseText(clan)}")
    }

    private fun handleUpgrade(player: Player, args: Array<out String>) {
        requireArgs(args, 2, "/clan upgrade <base|улучшение> [on|off]")

        if (args[1].equals("base", ignoreCase = true)) {
            val outcome = manager.upgradeBase(player)
            player.sendMessage(
                "$PREFIX §aБаза улучшена до уровня §e${outcome.level}§a. " +
                    "Стоимость: §e${money(outcome.cost)}§a.${debtSuffix(outcome.debtAdded)}"
            )
            return
        }

        val definition = manager.upgradeDefinition(args[1])
            ?: throw ClanException(
                "Неизвестное улучшение «${args[1]}». Доступны: base, ${manager.upgradeKeys().joinToString(", ")}."
            )

        if (args.size >= 3) {
            val flag = args[2].lowercase(Locale.ROOT)
            if (flag != "on" && flag != "off") {
                throw ClanException("Использование: /clan upgrade <ключ> on|off")
            }
            val active = manager.setUpgradeActive(player, definition.key, flag == "on")
            player.sendMessage(
                "$PREFIX §aУлучшение §e${definition.displayName}§a ${if (active) "включено" else "выключено"}."
            )
            return
        }

        val outcome = manager.purchaseUpgrade(player, definition.key)
        player.sendMessage(
            "$PREFIX §aУлучшение §e${definition.displayName}§a улучшено до уровня §e${outcome.level}§a. " +
                "Стоимость: §e${money(outcome.cost)}§a.${debtSuffix(outcome.debtAdded)}"
        )
    }

 private fun handleUpgradesList(player: Player) {
 val clan = manager.clanOf(player) ?: throw ClanException("Вы не в клане")
 val set = manager.upgradeManager.purchasedOf(clan.id)
 if (set.isEmpty()) {
 player.sendMessage("§eУлучшений нет")
 return
 }
 player.sendMessage("§6Куплено (" + set.size + "):")
 for (t in set) {
 player.sendMessage("§a- " + t.displayName)
 }
 }

 private fun handleBuy(player: Player, args: Array<out String>) {
        requireArgs(args, 2, "/clan buy <key>")
        val outcome = manager.purchaseUpgradeType(player, args[1])
        player.sendMessage("$PREFIX §aКуплено: §e${outcome.key}§a. Стоимость: §e${outcome.cost}§a.")
    }

 private fun handleList(player: Player) {
        val clans = manager.allClans()
        if (clans.isEmpty()) {
            player.sendMessage("$PREFIX §7Кланов пока нет. Создайте первый: §e/clan create <название>")
            return
        }
        player.sendMessage("§6Кланы (${clans.size}):")
        for (clan in clans) {
            val level = BaseLevelRegistry.get(clan.level)
            val members = manager.membersOf(clan).size
            player.sendMessage(
                " §e${clan.name} §7— ур. §f${clan.level}§7, " +
                    "участники: §f$members/${level.maxMembers}§7, банк: §f${money(clan.balance)}"
            )
        }
    }

    private fun sendInfo(player: Player, clan: Clan) {
        val level = BaseLevelRegistry.get(clan.level)
        val members = manager.membersOf(clan)
        val leaderName = Bukkit.getOfflinePlayer(clan.leaderUuid).name ?: clan.leaderUuid.toString()
        val upgrades = plugin.databaseManager.getUpgrades(clan.id)

        player.sendMessage("§6=== Клан ${clan.name} ===")
        player.sendMessage("§7Лидер: §f$leaderName")
        player.sendMessage("§7Участники: §f${members.size}/${level.maxMembers}")
        player.sendMessage("§7Баланс: §f${money(clan.balance)}")
        player.sendMessage("§7Долг: §f${money(clan.debt)}")
        player.sendMessage(
            "§7Уровень базы: §f${clan.level}/${BaseLevelRegistry.maxLevel} " +
                "§7(радиус ${plugin.configManager.radiusForLevel(clan.level).toInt()} бл.)"
        )
        player.sendMessage("§7База: §f${baseText(clan)}")
        if (upgrades.isEmpty()) {
            player.sendMessage("§7Улучшения: §fнет")
        } else {
            player.sendMessage(
                "§7Улучшения: §f" + upgrades.joinToString(", ") {
                    "${it.key} ур.${it.level}${if (it.active) "" else " (выкл)"}"
                }
            )
        }
    }

    private fun sendHelp(player: Player) {
        player.sendMessage("§6=== ProgressiveClans ===")
        player.sendMessage("§e/clan create <название> §7— создать клан")
        player.sendMessage("§e/clan invite <игрок> §7— пригласить игрока (лидер)")
        player.sendMessage("§e/clan join <клан> §7— вступить по приглашению")
        player.sendMessage("§e/clan leave §7— покинуть клан")
        player.sendMessage("§e/clan info [клан] §7— информация о клане")
        player.sendMessage("§e/clan pay <сумма> §7— взнос в банк (XP)")
        player.sendMessage("§e/clan setbase §7— установить базу (лидер)")
        player.sendMessage("§e/clan upgrade base §7— улучшить базу (лидер)")
        player.sendMessage("§e/clan upgrade <ключ> [on|off] §7— купить/переключить улучшение")
        player.sendMessage("§e/clan list §7— список кланов")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (sender !is Player) {
            return mutableListOf()
        }

        val options: List<String> = when (args.size) {
            1 -> subcommands
            2 -> when (args[0].lowercase(Locale.ROOT)) {
                "invite" -> Bukkit.getOnlinePlayers()
                    .filter { it.uniqueId != sender.uniqueId }
                    .map { it.name }
                "join" -> manager.invitedClanNames(sender.uniqueId)
                "info" -> manager.allClanNames()
                "upgrade" -> listOf("base") + manager.upgradeKeys()
                "pay" -> listOf("100", "500", "1000", "5000")
                else -> emptyList()
            }
            3 -> {
                if (args[0].equals("upgrade", ignoreCase = true) &&
                    !args[1].equals("base", ignoreCase = true)
                ) {
                    listOf("on", "off")
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }

        val prefix = args.lastOrNull()?.lowercase(Locale.ROOT) ?: ""
        return options
            .filter { it.lowercase(Locale.ROOT).startsWith(prefix) }
            .toMutableList()
    }

    private fun requireArgs(args: Array<out String>, count: Int, usage: String) {
        if (args.size < count) {
            throw ClanException("Использование: $usage")
        }
    }

    private fun baseText(clan: Clan): String {
        val world = clan.baseWorld ?: return "не установлена"
        return "$world ${coord(clan.baseX)}, ${coord(clan.baseY)}, ${coord(clan.baseZ)}"
    }

    private fun coord(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f", it) } ?: "?"

    private fun money(value: Double): String =
        String.format(Locale.US, "%,.2f", value)

    private fun debtSuffix(debt: Double): String =
        if (debt > 0.0001) " §c(в долг: ${money(debt)})" else ""

    companion object {
        private const val PREFIX = "§6[Кланы] §r"
    }
}
