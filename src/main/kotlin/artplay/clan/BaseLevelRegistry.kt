package artplay.clan

data class BaseLevel(
 val level: Int,
 val diamondPrice: Int,
 val maxMembers: Int
)

object BaseLevelRegistry {

 private val prices = listOf(
 0, 16, 24, 32, 48, 56, 64, 72, 80, 128,
 160, 176, 192, 208, 320, 384, 416, 448, 480, 640
 )

 val levels: List<BaseLevel> = prices.mapIndexed { index, price ->
 BaseLevel(
 level = index + 1,
 diamondPrice = price,
 maxMembers = 10 + index * 2
 )
 }

 val maxLevel: Int get() = levels.size

 fun get(level: Int): BaseLevel = levels[level.coerceIn(1, maxLevel) - 1]

 fun upgradeCost(currentLevel: Int): Int? {
 if (currentLevel < 1 || currentLevel >= maxLevel) return null
 return levels[currentLevel].diamondPrice
 }

 fun canUpgrade(currentLevel: Int, diamonds: Int): Boolean {
 val cost = upgradeCost(currentLevel) ?: return false
 return diamonds >= cost
 }

 fun applyUpgrade(currentLevel: Int): Int =
 if (currentLevel >= maxLevel) maxLevel else currentLevel + 1
}
