package artplay.upgrade

import artplay.clan.NewbieTax
import artplay.database.DatabaseManager
import java.util.concurrent.ConcurrentHashMap

data class UpgradeState(
 val type: UpgradeType,
 val purchased: Boolean,
 val active: Boolean,
 val paidBase: Long,
 val currentCost: Long
)

class UpgradeManager(private val database: DatabaseManager) {

 private val purchased = ConcurrentHashMap<Long, MutableSet<UpgradeType>>()

 fun loadAll() {
 purchased.clear()
 }

 fun isPurchased(clanId: Long, type: UpgradeType): Boolean =
 purchased[clanId]?.contains(type) ?: false

 fun purchasedOf(clanId: Long): Set<UpgradeType> =
 purchased[clanId]?.toSet() ?: emptySet()
 fun purchase(clanId: Long, type: UpgradeType, balance: Long): Long {
 if (isPurchased(clanId, type)) throw IllegalArgumentException()
 val cost = type.baseDiamond.toLong() + type.tier.toLong() * 3L
 if (balance < cost) throw IllegalArgumentException()
 purchased.getOrPut(clanId) { mutableSetOf() }.add(type)
 return cost
 }
 fun recalcForMembers(clanId: Long, memberCount: Int): Long {
 val set = purchased[clanId] ?: return 0L
 var debt = 0L
 val pricePer = NewbieTax.priceForMembers(memberCount)
 for (t in set) {
 val baseCost = t.baseDiamond.toLong() + t.tier.toLong() * 3L
 val newCost = baseCost + memberCount.toLong() * pricePer
 debt += (newCost - baseCost).coerceAtLeast(0L)
 }
 return debt
 }
 fun applyMemberChange(clanId: Long, memberCount: Int, balance: Long): Boolean {
 val debt = recalcForMembers(clanId, memberCount)
 if (debt <= 0L) return true
 return balance >= debt
 }
}
