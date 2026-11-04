package artplay.clan

object NewbieTax {

 private const val BASE = 2.0
 private const val GROWTH = 1.3

 fun priceForMembers(n: Int): Long {
 if (n <= 1) return BASE.toLong()
 val value = BASE * Math.pow(GROWTH, (n - 1).toDouble())
 return Math.round(value)
 }

 fun recomputeCost(base: Int, members: Int): Double =
 base.toDouble() + members.toDouble() * priceForMembers(members).toDouble()
}
