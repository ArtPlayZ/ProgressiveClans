package artplay.upgrade

import org.bukkit.Material

/**
 * Клановые улучшения, сгруппированные по уровням (tier 1..20).
 *
 * Цена в алмазах рассчитывается по формуле [BASE_DIAMOND_PER_TIER] + tier * 3,
 * а базовые ресурсы зависят от уровня и заданы в [TIER_RESOURCES].
 */
enum class UpgradeType(
    val key: String,
    val displayName: String,
    val tier: Int,
    val baseDiamond: Int,
    val baseResources: Map<Material, Int>
) {

    // tier 1
    HARDNESS_1("hardness_1", "Прочность I", 1, 6, tierResources(1)),
    MINER_1("miner_1", "Шахтёр I", 1, 6, tierResources(1)),
    AGRONOM_1("agronom_1", "Агроном I", 1, 6, tierResources(1)),

    // tier 2
    SPEEDSTER("speedster", "Скороход", 2, 9, tierResources(2)),
    BUTCHER_1("butcher_1", "Мясник I", 2, 9, tierResources(2)),
    MINER_2("miner_2", "Шахтёр II", 2, 9, tierResources(2)),

    // tier 3
    MINER_HASTE("miner_haste", "Шахтёрский азарт", 3, 12, tierResources(3)),
    XP_MINER_1("xp_miner_1", "Опытный шахтёр I", 3, 12, tierResources(3)),
    AGRONOM_2("agronom_2", "Агроном II", 3, 12, tierResources(3)),

    // tier 4
    ACROBAT("acrobat", "Акробат", 4, 15, tierResources(4)),
    FISHER("fisher", "Рыбак", 4, 15, tierResources(4)),
    MINER_3("miner_3", "Шахтёр III", 4, 15, tierResources(4)),

    // tier 5
    NIGHT_EYE("night_eye", "Ночное зрение", 5, 18, tierResources(5)),
    FIREPROOF("fireproof", "Огнестойкость", 5, 18, tierResources(5)),
    AGRONOM_3("agronom_3", "Агроном III", 5, 18, tierResources(5)),

    // tier 6
    HUNTER_1("hunter_1", "Охотник I", 6, 21, tierResources(6)),
    COMBAT_SPIRIT("combat_spirit", "Боевой дух", 6, 21, tierResources(6)),
    BUTCHER_2("butcher_2", "Мясник II", 6, 21, tierResources(6)),

    // tier 7
    JUMPER("jumper", "Прыгун", 7, 24, tierResources(7)),
    ENTOMOLOGIST_1("entomologist_1", "Энтомолог I", 7, 24, tierResources(7)),
    MINER_4("miner_4", "Шахтёр IV", 7, 24, tierResources(7)),

    // tier 8
    ICE_STRIKE("ice_strike", "Ледяной удар", 8, 27, tierResources(8)),
    TRADER("trader", "Торговец", 8, 27, tierResources(8)),
    WIZARD("wizard", "Чародей", 8, 27, tierResources(8)),

    // tier 9
    RAIDER("raider", "Рейдер", 9, 30, tierResources(9)),
    TREASURE_HUNTER("treasure_hunter", "Кладоискатель", 9, 30, tierResources(9)),
    AGRONOM_4("agronom_4", "Агроном IV", 9, 30, tierResources(9)),

    // tier 10
    SMITH_EXPERT("smith_expert", "Опытный кузнец", 10, 33, tierResources(10)),
    TIRELESS("tireless", "Неутомимость", 10, 33, tierResources(10)),
    XP_MASTER("xp_master", "Мастер опыта", 10, 33, tierResources(10)),

    // tier 11
    HUNTER_2("hunter_2", "Охотник II", 11, 36, tierResources(11)),
    MINER_5("miner_5", "Шахтёр V", 11, 36, tierResources(11)),
    IGNITER("igniter", "Поджигатель", 11, 36, tierResources(11)),

    // tier 12
    EXHAUSTION("exhaustion", "Истощение", 12, 39, tierResources(12)),
    VITALITY("vitality", "Живучесть", 12, 39, tierResources(12)),
    BUTCHER_3("butcher_3", "Мясник III", 12, 39, tierResources(12)),

    // tier 13
    ENTOMOLOGIST_2("entomologist_2", "Энтомолог II", 13, 42, tierResources(13)),
    ANVIL("anvil", "Наковальня", 13, 42, tierResources(13)),
    MINER_6("miner_6", "Шахтёр VI", 13, 42, tierResources(13)),

    // tier 14
    VAMPIRE("vampire", "Вампир", 14, 45, tierResources(14)),
    BUTCHER_4("butcher_4", "Мясник IV", 14, 45, tierResources(14)),
    AGRONOM_5("agronom_5", "Агроном V", 14, 45, tierResources(14)),

    // tier 15
    WEAPONSMITH("weaponsmith", "Оружейник", 15, 48, tierResources(15)),
    CLAN_POWER("clan_power", "Сила клана", 15, 48, tierResources(15)),
    BOGATYR("bogatyr", "Богатырь", 15, 48, tierResources(15)),

    // tier 16
    HUNTER_3("hunter_3", "Охотник III", 16, 51, tierResources(16)),
    MINER_7("miner_7", "Шахтёр VII", 16, 51, tierResources(16)),
    GOLD_DIGGER("gold_digger", "Золотоискатель", 16, 51, tierResources(16)),

    // tier 17
    ENTOMOLOGIST_3("entomologist_3", "Энтомолог III", 17, 54, tierResources(17)),
    AGRONOM_6("agronom_6", "Агроном VI", 17, 54, tierResources(17)),
    VOID_STEP("void_step", "Шаг в пустоту", 17, 54, tierResources(17)),

    // tier 18
    POISON_BLADE("poison_blade", "Ядовитый клинок", 18, 57, tierResources(18)),
    ARCHAEOLOGIST("archaeologist", "Археолог", 18, 57, tierResources(18)),
    AGRONOM_7("agronom_7", "Агроном VII", 18, 57, tierResources(18)),

    // tier 19
    MINER_8("miner_8", "Шахтёр VIII", 19, 60, tierResources(19)),
    BUTCHER_5("butcher_5", "Мясник V", 19, 60, tierResources(19)),
    SHIELD_MASTER("shield_master", "Мастер щита", 19, 60, tierResources(19)),

    // tier 20
    DIVINE_SMITH("divine_smith", "Божественный кузнец", 20, 63, tierResources(20)),
    DRAGON_SKIN("dragon_skin", "Драконья шкура", 20, 63, tierResources(20)),
    AURA_MASTER("aura_master", "Мастер ауры", 20, 63, tierResources(20));

    init {
        require(tier in 1..20) { "Недопустимый tier $tier у улучшения $key" }
        require(baseDiamond == 3 + tier * 3) {
            "Цена $baseDiamond не соответствует tier $tier у улучшения $key"
        }
        require(baseResources.isNotEmpty()) { "У улучшения $key не заданы ресурсы" }
    }

    companion object {
        const val MIN_TIER = 1
        const val MAX_TIER = 20
        const val BASE_DIAMOND_PER_TIER = 3

        /** Поиск улучшения по ключу без учёта регистра. */
        fun byKey(key: String): UpgradeType? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }

        /** Все улучшения указанного уровня. */
        fun byTier(tier: Int): List<UpgradeType> =
            entries.filter { it.tier == tier }

        /** Ключи всех улучшений в порядке объявления. */
        fun keys(): List<String> = entries.map { it.key }
    }
}

/**
 * Базовые ресурсы уровня. Каждый уровень использует 1-2 вида материалов;
 * количество растёт вместе с tier.
 */
private fun tierResources(tier: Int): Map<Material, Int> =
    TIER_RESOURCES.getValue(tier)

private val TIER_RESOURCES: Map<Int, Map<Material, Int>> = mapOf(
    1 to mapOf(Material.OAK_LOG to 16, Material.COBBLESTONE to 32),
    2 to mapOf(Material.IRON_INGOT to 12),
    3 to mapOf(Material.GOLD_INGOT to 8, Material.REDSTONE to 16),
    4 to mapOf(Material.EMERALD to 6),
    5 to mapOf(Material.SLIME_BALL to 16, Material.LAPIS_LAZULI to 16),
    6 to mapOf(Material.ARROW to 32, Material.SUGAR_CANE to 24),
    7 to mapOf(Material.PUMPKIN to 24),
    8 to mapOf(Material.MAGMA_CREAM to 8, Material.GHAST_TEAR to 4),
    9 to mapOf(Material.QUARTZ to 24),
    10 to mapOf(Material.BLAZE_ROD to 16, Material.FIRE_CHARGE to 12),
    11 to mapOf(Material.GLOWSTONE_DUST to 24),
    12 to mapOf(Material.WITHER_SKELETON_SKULL to 3, Material.OBSIDIAN to 24),
    13 to mapOf(Material.END_CRYSTAL to 4),
    14 to mapOf(Material.DRAGON_BREATH to 4),
    15 to mapOf(Material.SHULKER_SHELL to 4),
    16 to mapOf(Material.ELYTRA to 1),
    17 to mapOf(Material.HEART_OF_THE_SEA to 2),
    18 to mapOf(Material.ENCHANTED_GOLDEN_APPLE to 2),
    19 to mapOf(Material.DRAGON_HEAD to 1, Material.NETHER_STAR to 1),
    20 to mapOf(Material.NETHERITE_INGOT to 4, Material.ENDER_CHEST to 2)
)
