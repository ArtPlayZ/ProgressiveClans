package artplay.clan

import artplay.ProgressiveClansPlugin
import artplay.database.Clan
import artplay.database.DatabaseManager
import artplay.upgrade.UpgradeManager
import artplay.upgrade.UpgradeType

import org.bukkit.entity.Player
import java.util.UUID

class ClanException(message: String) : RuntimeException(message)

data class UpgradeDefinition(
    val key: String,
    val displayName: String,
    val description: String,
    val baseCost: Double,
    val costMultiplier: Double,
    val maxLevel: Int
)

data class LeaveOutcome(
    val clanName: String,
    val disbanded: Boolean,
    val newLeader: UUID?
)

data class UpgradeOutcome(
    val key: String,
    val level: Int,
    val cost: Double,
    val debtAdded: Double
)

/**
 * Кэш кланов и бизнес-логика создания, вступления, взносов и улучшений.
 */
class ClanManager(
 private val plugin: ProgressiveClansPlugin,
 private val database: DatabaseManager,
 val upgradeManager: UpgradeManager
) {

    companion object {
        private val NAME_PATTERN = Regex("^[A-Za-zА-Яа-яЁё0-9_]{3,16}$")
        private const val INVITE_TTL_MS = 5 * 60 * 1000L

        private val UPGRADE_DEFINITIONS = listOf(
            UpgradeDefinition(
                "mobs",
                "Контроль мобов",
                "+10 к лимиту мобов на территории базы за уровень",
                25_000.0,
                1.9,
                10
            ),
            UpgradeDefinition(
                "radius",
                "Расширение базы",
                "+5 блоков к радиусу территории за уровень",
                40_000.0,
                1.8,
                10
            ),
            UpgradeDefinition(
                "slots",
                "Слоты участников",
                "+2 слота участников за уровень",
                30_000.0,
                2.0,
                10
            )
        )
    }

    private val clansByName = mutableMapOf<String, Clan>()
    private val clansByPlayer = mutableMapOf<UUID, Clan>()
    private val invites = mutableMapOf<UUID, MutableMap<String, Long>>()

    fun loadAll() {
        clansByName.clear()
        clansByPlayer.clear()
        invites.clear()

        for (clan in database.getAllClans()) {
            clan.debt = database.getDebt(clan.id)
            clansByName[clan.name.lowercase()] = clan
            for (uuid in database.getPlayers(clan.id).keys) {
                clansByPlayer[uuid] = clan
            }
        }
        plugin.logger.info("Загружено кланов: ${clansByName.size}, участников: ${clansByPlayer.size}.")
    }

    fun allClans(): List<Clan> = clansByName.values.sortedBy { it.name.lowercase() }

    fun allClanNames(): List<String> = allClans().map { it.name }

    fun clanOf(player: Player): Clan? = clansByPlayer[player.uniqueId]

    fun clanByName(name: String): Clan? = clansByName[name.lowercase()]

    fun membersOf(clan: Clan): List<UUID> =
        clansByPlayer.entries.filter { it.value.id == clan.id }.map { it.key }

    fun createClan(player: Player, rawName: String): Clan {
        val name = rawName.trim()
        if (clansByPlayer.containsKey(player.uniqueId)) {
            throw ClanException("Вы уже состоите в клане.")
        }
        if (!NAME_PATTERN.matches(name)) {
            throw ClanException("Название клана: 3–16 символов (буквы, цифры, «_»).")
        }
        if (clansByName.containsKey(name.lowercase())) {
            throw ClanException("Клан «$name» уже существует.")
        }

        val clan = try {
            database.createClan(name, player.uniqueId)
        } catch (exception: Exception) {
            throw ClanException("Не удалось создать клан: ${exception.message}")
        }

        database.addPlayer(clan.id, player.uniqueId, "leader")
        clansByName[clan.name.lowercase()] = clan
        clansByPlayer[player.uniqueId] = clan
        return clan
    }

    fun invite(inviter: Player, target: Player): Clan {
        val clan = requireLeader(inviter)
        if (inviter.uniqueId == target.uniqueId) {
            throw ClanException("Нельзя пригласить самого себя.")
        }
        if (clansByPlayer.containsKey(target.uniqueId)) {
            throw ClanException("Игрок ${target.name} уже состоит в клане.")
        }

        val now = System.currentTimeMillis()
        val targetInvites = invites.getOrPut(target.uniqueId) { mutableMapOf() }
        targetInvites.entries.removeIf { it.value < now }
        targetInvites[clan.name.lowercase()] = now + INVITE_TTL_MS
        return clan
    }

    fun invitedClanNames(uuid: UUID): List<String> {
        val now = System.currentTimeMillis()
        val playerInvites = invites[uuid] ?: return emptyList()
        playerInvites.entries.removeIf { it.value < now }
        return playerInvites.keys.mapNotNull { clansByName[it]?.name }.sorted()
    }

    fun join(player: Player, rawName: String): Clan {
        if (clansByPlayer.containsKey(player.uniqueId)) {
            throw ClanException("Вы уже состоите в клане.")
        }
        val clan = clansByName[rawName.lowercase()]
            ?: throw ClanException("Клан «$rawName» не найден.")

        val playerInvites = invites[player.uniqueId]
        val expiresAt = playerInvites?.get(clan.name.lowercase())
            ?: throw ClanException("Вас не приглашали в клан «${clan.name}».")
        if (expiresAt < System.currentTimeMillis()) {
            playerInvites.remove(clan.name.lowercase())
            throw ClanException("Приглашение в клан «${clan.name}» истекло.")
        }

        val limit = BaseLevelRegistry.get(clan.level).maxMembers
        if (membersOf(clan).size >= limit) {
            throw ClanException("В клане нет свободных мест (максимум $limit).")
        }

        playerInvites.remove(clan.name.lowercase())
        database.addPlayer(clan.id, player.uniqueId, "member")
        clansByPlayer[player.uniqueId] = clan
 val newCount = membersOf(clan).size
 val debt = upgradeManager.recalcForMembers(clan.id, newCount)
 if (debt > 0L) {
 upgradeManager.applyMemberChange(clan.id, newCount, clan.balance.toLong())
 }
 return clan
    }

    fun leave(player: Player): LeaveOutcome {
        val clan = clanOf(player) ?: throw ClanException("Вы не состоите в клане.")
        val remaining = membersOf(clan).filter { it != player.uniqueId }
        val wasLeader = clan.leaderUuid == player.uniqueId

        database.removePlayer(player.uniqueId)
        clansByPlayer.remove(player.uniqueId)

        if (!wasLeader) {
            return LeaveOutcome(clan.name, false, null)
        }

        if (remaining.isEmpty()) {
            database.deleteClan(clan.id)
            clansByName.remove(clan.name.lowercase())
            return LeaveOutcome(clan.name, true, null)
        }

        val nextLeader = remaining.first()
        clan.leaderUuid = nextLeader
        database.updateLeader(clan.id, nextLeader)
        return LeaveOutcome(clan.name, false, nextLeader)
    }

    /** Взнос в банк клана из очков опыта игрока (1 XP = 1 монета). */
    fun deposit(player: Player, amount: Int): Double {
        if (amount <= 0) {
            throw ClanException("Сумма взноса должна быть больше нуля.")
        }
        val clan = clanOf(player) ?: throw ClanException("Вы не состоите в клане.")
        if (player.totalExperience < amount) {
            throw ClanException("Недостаточно опыта: нужно $amount XP.")
        }

        player.giveExp(-amount)
        val balance = clan.balance + amount
        database.setBalance(clan.id, balance)
        clan.balance = balance
        return balance
    }

    fun setBase(player: Player): Clan {
        val clan = requireLeader(player)
        val now = System.currentTimeMillis()
        val cooldown = plugin.configManager.setbaseCooldownHours * 60L * 60L * 1000L
        if (clan.lastSetbase > 0 && now - clan.lastSetbase < cooldown) {
            val left = cooldown - (now - clan.lastSetbase)
            throw ClanException("База уже устанавливалась. Подождите ${formatDuration(left)}.")
        }

        val location = player.location
        val world = location.world?.name ?: player.world.name
        database.setBase(clan.id, world, location.x, location.y, location.z, now)
        clan.baseWorld = world
        clan.baseX = location.x
        clan.baseY = location.y
        clan.baseZ = location.z
        clan.lastSetbase = now
        return clan
    }

    fun upgradeBase(player: Player): UpgradeOutcome {
        val clan = requireLeader(player)
        val nextLevel = BaseLevelRegistry.applyUpgrade(clan.level)
        if (nextLevel == clan.level) {
            throw ClanException("База уже достигла максимального уровня (${BaseLevelRegistry.maxLevel}).")
        }

        val baseCost = BaseLevelRegistry.upgradeCost(clan.level)
            ?: throw ClanException("Улучшение базы недоступно.")
        val cost = NewbieTax.recomputeCost(baseCost, membersOf(clan).size)
        val debt = charge(clan, cost)

        clan.level = nextLevel
        database.setLevel(clan.id, nextLevel)
        return UpgradeOutcome("base", nextLevel, cost, debt)
    }

    fun purchaseUpgrade(player: Player, rawKey: String): UpgradeOutcome {
        val clan = requireLeader(player)
        val definition = upgradeDefinition(rawKey)
            ?: throw ClanException("Неизвестное улучшение «$rawKey». Доступны: ${upgradeKeys().joinToString(", ")}.")

        val existing = database.getUpgrades(clan.id).firstOrNull { it.key == definition.key }
        val currentLevel = existing?.level ?: 0
        if (currentLevel >= definition.maxLevel) {
            throw ClanException("Улучшение «${definition.displayName}» уже максимального уровня.")
        }

        val cost = upgradeCost(definition, currentLevel)
        val debt = charge(clan, cost)
        val newLevel = currentLevel + 1
        database.addUpgrade(clan.id, definition.key, newLevel, (existing?.paidBase ?: 0.0) + cost)
        return UpgradeOutcome(definition.key, newLevel, cost, debt)
    }

    fun setUpgradeActive(player: Player, rawKey: String, active: Boolean): Boolean {
        val clan = requireLeader(player)
        val definition = upgradeDefinition(rawKey)
            ?: throw ClanException("Неизвестное улучшение «$rawKey».")
        database.getUpgrades(clan.id).firstOrNull { it.key == definition.key }
            ?: throw ClanException("Улучшение «${definition.displayName}» ещё не куплено.")
        database.setActive(clan.id, definition.key, active)
        return active
    }

    fun upgradeKeys(): List<String> = UPGRADE_DEFINITIONS.map { it.key }

    fun upgradeDefinition(key: String): UpgradeDefinition? =
        UPGRADE_DEFINITIONS.firstOrNull { it.key.equals(key, ignoreCase = true) }

    fun upgradeCost(definition: UpgradeDefinition, currentLevel: Int): Double =
        Math.round(definition.baseCost * Math.pow(definition.costMultiplier, currentLevel.toDouble())).toDouble()

 fun purchaseUpgradeType(player: Player, key: String): UpgradeOutcome {
 val clan = requireLeader(player)
 val type = UpgradeType.byKey(key) ?: throw RuntimeException()
 val cost = upgradeManager.purchase(clan.id, type, clan.balance.toLong()).toDouble()
 val debt = charge(clan, cost)
 return UpgradeOutcome(type.key, 1, cost, debt)
 }

 private fun requireLeader(player: Player): Clan {
        val clan = clanOf(player) ?: throw ClanException("Вы не состоите в клане.")
        if (clan.leaderUuid != player.uniqueId) {
            throw ClanException("Это действие доступно только лидеру клана.")
        }
        return clan
    }

    /** Списывает средства; при нехватке остаток уходит в долг клана. */
    private fun charge(clan: Clan, cost: Double): Double {
        if (cost <= 0.0) {
            return 0.0
        }
        val paid = minOf(clan.balance, cost)
        val debt = cost - paid
        val newBalance = clan.balance - paid

        database.setBalance(clan.id, newBalance)
        clan.balance = newBalance

        if (debt > 0.0001) {
            database.addDebt(clan.id, debt)
            clan.debt += debt
        }
        return debt
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L -> "$hours ч $minutes мин"
            minutes > 0L -> "$minutes мин"
            else -> "меньше минуты"
        }
    }
}
