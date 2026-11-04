package artplay.database

import artplay.ProgressiveClansPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

data class Clan(
    val id: Long,
    var name: String,
    var leaderUuid: UUID,
    var balance: Double,
    var level: Int,
    var baseWorld: String?,
    var baseX: Double?,
    var baseY: Double?,
    var baseZ: Double?,
    var lastSetbase: Long,
    val created: Long,
    var debt: Double = 0.0
)

data class ClanUpgrade(
    val id: Long,
    val clanId: Long,
    val key: String,
    var level: Int,
    var active: Boolean,
    var paidBase: Double
)

/**
 * SQLite-хранилище плагина. Все обращения к единственному соединению
 * синхронизированы, БД работает в режиме WAL.
 */
class DatabaseManager(private val plugin: ProgressiveClansPlugin) {

    private val lock = Any()
    private lateinit var connection: Connection
    private val databaseFile = File(plugin.dataFolder, "clans.db")

    fun init() {
        plugin.dataFolder.mkdirs()
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")

        withConnection { conn ->
            conn.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL")
                statement.execute("PRAGMA foreign_keys=ON")
                statement.execute("PRAGMA synchronous=NORMAL")
                statement.execute("PRAGMA busy_timeout=5000")

                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS clans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        leader_uuid TEXT NOT NULL,
                        balance REAL NOT NULL DEFAULT 0,
                        level INTEGER NOT NULL DEFAULT 1,
                        base_world TEXT,
                        base_x REAL,
                        base_y REAL,
                        base_z REAL,
                        last_setbase INTEGER NOT NULL DEFAULT 0,
                        created INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS clan_players (
                        clan_id INTEGER NOT NULL,
                        uuid TEXT NOT NULL UNIQUE,
                        role TEXT NOT NULL,
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS upgrades (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        clan_id INTEGER NOT NULL,
                        upgrade_key TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 0,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        paid_base REAL NOT NULL DEFAULT 0,
                        UNIQUE (clan_id, upgrade_key),
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS clan_debt (
                        clan_id INTEGER PRIMARY KEY,
                        debt REAL NOT NULL DEFAULT 0,
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }
        plugin.logger.info("SQLite инициализирована: ${databaseFile.name}")
    }

    fun close() {
        if (!this::connection.isInitialized) {
            return
        }
        synchronized(lock) {
            try {
                if (!connection.isClosed) {
                    connection.close()
                }
            } catch (exception: Exception) {
                plugin.logger.warning("Не удалось закрыть SQLite: ${exception.message}")
            }
        }
    }

    private fun <T> withConnection(block: (Connection) -> T): T = synchronized(lock) {
        block(connection)
    }

    fun createClan(name: String, leader: UUID): Clan = withConnection { conn ->
        val now = System.currentTimeMillis()
        val sql = """
            INSERT INTO clans (name, leader_uuid, balance, level, last_setbase, created)
            VALUES (?, ?, 0, 1, 0, ?)
        """.trimIndent()
        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, name)
            statement.setString(2, leader.toString())
            statement.setLong(3, now)
            statement.executeUpdate()
            statement.generatedKeys.use { keys ->
                val id = if (keys.next()) keys.getLong(1) else -1L
                Clan(id, name, leader, 0.0, 1, null, null, null, null, 0L, now)
            }
        }
    }

    fun getClanByName(name: String): Clan? = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM clans WHERE name = ? COLLATE NOCASE LIMIT 1").use { statement ->
            statement.setString(1, name)
            statement.executeQuery().use { result ->
                if (result.next()) result.toClan() else null
            }
        }
    }

    fun getClanByPlayer(player: UUID): Clan? = withConnection { conn ->
        val sql = """
            SELECT c.* FROM clans c
            INNER JOIN clan_players p ON p.clan_id = c.id
            WHERE p.uuid = ?
            LIMIT 1
        """.trimIndent()
        conn.prepareStatement(sql).use { statement ->
            statement.setString(1, player.toString())
            statement.executeQuery().use { result ->
                if (result.next()) result.toClan() else null
            }
        }
    }

    fun getAllClans(): List<Clan> = withConnection { conn ->
        val clans = ArrayList<Clan>()
        conn.prepareStatement("SELECT * FROM clans ORDER BY created").use { statement ->
            statement.executeQuery().use { result ->
                while (result.next()) {
                    clans.add(result.toClan())
                }
            }
        }
        clans
    }

    fun getPlayers(clanId: Long): Map<UUID, String> = withConnection { conn ->
        val players = LinkedHashMap<UUID, String>()
        conn.prepareStatement("SELECT uuid, role FROM clan_players WHERE clan_id = ?").use { statement ->
            statement.setLong(1, clanId)
            statement.executeQuery().use { result ->
                while (result.next()) {
                    players[UUID.fromString(result.getString("uuid"))] = result.getString("role")
                }
            }
        }
        players
    }

    fun addPlayer(clanId: Long, player: UUID, role: String) = withConnection { conn ->
        val sql = """
            INSERT INTO clan_players (clan_id, uuid, role) VALUES (?, ?, ?)
            ON CONFLICT (uuid) DO UPDATE SET clan_id = excluded.clan_id, role = excluded.role
        """.trimIndent()
        conn.prepareStatement(sql).use { statement ->
            statement.setLong(1, clanId)
            statement.setString(2, player.toString())
            statement.setString(3, role)
            statement.executeUpdate()
        }
    }

    fun removePlayer(player: UUID) = withConnection { conn ->
        conn.prepareStatement("DELETE FROM clan_players WHERE uuid = ?").use { statement ->
            statement.setString(1, player.toString())
            statement.executeUpdate()
        }
    }

    fun updateLeader(clanId: Long, leader: UUID) = withConnection { conn ->
        conn.autoCommit = false
        try {
            conn.prepareStatement("UPDATE clans SET leader_uuid = ? WHERE id = ?").use { statement ->
                statement.setString(1, leader.toString())
                statement.setLong(2, clanId)
                statement.executeUpdate()
            }
            conn.prepareStatement("UPDATE clan_players SET role = 'member' WHERE clan_id = ?").use { statement ->
                statement.setLong(1, clanId)
                statement.executeUpdate()
            }
            conn.prepareStatement("UPDATE clan_players SET role = 'leader' WHERE clan_id = ? AND uuid = ?").use { statement ->
                statement.setLong(1, clanId)
                statement.setString(2, leader.toString())
                statement.executeUpdate()
            }
            conn.commit()
        } catch (exception: Exception) {
            conn.rollback()
            throw exception
        } finally {
            conn.autoCommit = true
        }
    }

    fun deleteClan(clanId: Long) = withConnection { conn ->
        conn.autoCommit = false
        try {
            conn.prepareStatement("DELETE FROM clan_players WHERE clan_id = ?").use {
                it.setLong(1, clanId)
                it.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM upgrades WHERE clan_id = ?").use {
                it.setLong(1, clanId)
                it.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM clan_debt WHERE clan_id = ?").use {
                it.setLong(1, clanId)
                it.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM clans WHERE id = ?").use {
                it.setLong(1, clanId)
                it.executeUpdate()
            }
            conn.commit()
        } catch (exception: Exception) {
            conn.rollback()
            throw exception
        } finally {
            conn.autoCommit = true
        }
    }

    fun setBalance(clanId: Long, balance: Double) = withConnection { conn ->
        conn.prepareStatement("UPDATE clans SET balance = ? WHERE id = ?").use { statement ->
            statement.setDouble(1, balance)
            statement.setLong(2, clanId)
            statement.executeUpdate()
        }
    }

    fun getBalance(clanId: Long): Double = withConnection { conn ->
        conn.prepareStatement("SELECT balance FROM clans WHERE id = ?").use { statement ->
            statement.setLong(1, clanId)
            statement.executeQuery().use { result ->
                if (result.next()) result.getDouble("balance") else 0.0
            }
        }
    }

    fun setLevel(clanId: Long, level: Int) = withConnection { conn ->
        conn.prepareStatement("UPDATE clans SET level = ? WHERE id = ?").use { statement ->
            statement.setInt(1, level)
            statement.setLong(2, clanId)
            statement.executeUpdate()
        }
    }

    fun setBase(
        clanId: Long,
        world: String,
        x: Double,
        y: Double,
        z: Double,
        timestamp: Long
    ) = withConnection { conn ->
        val sql = """
            UPDATE clans
            SET base_world = ?, base_x = ?, base_y = ?, base_z = ?, last_setbase = ?
            WHERE id = ?
        """.trimIndent()
        conn.prepareStatement(sql).use { statement ->
            statement.setString(1, world)
            statement.setDouble(2, x)
            statement.setDouble(3, y)
            statement.setDouble(4, z)
            statement.setLong(5, timestamp)
            statement.setLong(6, clanId)
            statement.executeUpdate()
        }
    }

    fun getUpgrades(clanId: Long): List<ClanUpgrade> = withConnection { conn ->
        val upgrades = ArrayList<ClanUpgrade>()
        conn.prepareStatement("SELECT * FROM upgrades WHERE clan_id = ? ORDER BY upgrade_key").use { statement ->
            statement.setLong(1, clanId)
            statement.executeQuery().use { result ->
                while (result.next()) {
                    upgrades.add(
                        ClanUpgrade(
                            result.getLong("id"),
                            result.getLong("clan_id"),
                            result.getString("upgrade_key"),
                            result.getInt("level"),
                            result.getInt("is_active") != 0,
                            result.getDouble("paid_base")
                        )
                    )
                }
            }
        }
        upgrades
    }

    fun addUpgrade(clanId: Long, key: String, level: Int, paidBase: Double) = withConnection { conn ->
        val sql = """
            INSERT INTO upgrades (clan_id, upgrade_key, level, is_active, paid_base)
            VALUES (?, ?, ?, 1, ?)
            ON CONFLICT (clan_id, upgrade_key)
            DO UPDATE SET level = excluded.level, paid_base = excluded.paid_base
        """.trimIndent()
        conn.prepareStatement(sql).use { statement ->
            statement.setLong(1, clanId)
            statement.setString(2, key)
            statement.setInt(3, level)
            statement.setDouble(4, paidBase)
            statement.executeUpdate()
        }
    }

    fun setActive(clanId: Long, key: String, active: Boolean) = withConnection { conn ->
        val sql = "UPDATE upgrades SET is_active = ? WHERE clan_id = ? AND upgrade_key = ?"
        conn.prepareStatement(sql).use { statement ->
            statement.setInt(1, if (active) 1 else 0)
            statement.setLong(2, clanId)
            statement.setString(3, key)
            statement.executeUpdate()
        }
    }

    fun addDebt(clanId: Long, amount: Double) = withConnection { conn ->
        val sql = """
            INSERT INTO clan_debt (clan_id, debt) VALUES (?, ?)
            ON CONFLICT (clan_id) DO UPDATE SET debt = debt + excluded.debt
        """.trimIndent()
        conn.prepareStatement(sql).use { statement ->
            statement.setLong(1, clanId)
            statement.setDouble(2, amount)
            statement.executeUpdate()
        }
    }

    fun getDebt(clanId: Long): Double = withConnection { conn ->
        conn.prepareStatement("SELECT debt FROM clan_debt WHERE clan_id = ?").use { statement ->
            statement.setLong(1, clanId)
            statement.executeQuery().use { result ->
                if (result.next()) result.getDouble("debt") else 0.0
            }
        }
    }

    private fun ResultSet.toClan(): Clan {
        val baseX = getDouble("base_x").let { if (wasNull()) null else it }
        val baseY = getDouble("base_y").let { if (wasNull()) null else it }
        val baseZ = getDouble("base_z").let { if (wasNull()) null else it }
        return Clan(
            getLong("id"),
            getString("name"),
            UUID.fromString(getString("leader_uuid")),
            getDouble("balance"),
            getInt("level"),
            getString("base_world"),
            baseX,
            baseY,
            baseZ,
            getLong("last_setbase"),
            getLong("created")
        )
    }
}
