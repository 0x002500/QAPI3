package org.qo.orm

import org.qo.datas.Mapping.Users
import org.qo.ConnectionPool
import java.sql.PreparedStatement
import java.sql.ResultSet

class UserORM : CrudDao<Users> {

    companion object {
        private const val INSERT_USER_SQL = "INSERT INTO users (username, uid, frozen, remain, economy, signed, playtime, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        private const val SELECT_USER_BY_ID_SQL = "SELECT * FROM users WHERE uid = ?"
        private const val SELECT_USER_BY_USERNAME_SQL = "SELECT * FROM users WHERE username = ?"
        private const val DELETE_USER_BY_ID_SQL = "DELETE FROM users WHERE uid = ?"
        private const val DELETE_USER_BY_USERNAME_SQL = "DELETE FROM users WHERE username = ?"
    }

    override fun create(user: Users): Long {
        return ConnectionPool.getConnection().use { connection ->
            connection.prepareStatement(INSERT_USER_SQL, PreparedStatement.RETURN_GENERATED_KEYS).use { stmt ->
                setStatementParams(stmt, user)
                stmt.executeUpdate()
                val keys = stmt.generatedKeys
                if (keys.next()) keys.getLong(1) else -1
            }
        }
    }

    override fun read(input: Any): Users? {
        val (sql, paramSetter) = when (input) {
            is Long -> SELECT_USER_BY_ID_SQL to fun(stmt: PreparedStatement) { stmt.setLong(1, input) }
            is String -> SELECT_USER_BY_USERNAME_SQL to fun(stmt: PreparedStatement) { stmt.setString(1, input) }
            else -> throw IllegalArgumentException("Input must be either a String or a Long")
        }
        return ConnectionPool.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                paramSetter(stmt)
                val rs = stmt.executeQuery()
                if (rs.next()) mapResultSetToUser(rs) else null
            }
        }
    }

    override fun update(user: Users): Boolean {
        val fields = mutableListOf<String>()
        val values = mutableListOf<Any?>()
        user.username.let {
            fields.add("username = ?")
            values.add(it)
        }
        user.frozen?.let {
            fields.add("frozen = ?")
            values.add(it)
        }
        user.remain?.let {
            fields.add("remain = ?")
            values.add(it)
        }
        user.economy?.let {
            fields.add("economy = ?")
            values.add(it)
        }
        user.signed?.let {
            fields.add("signed = ?")
            values.add(it)
        }
        user.playtime?.let {
            fields.add("playtime = ?")
            values.add(it)
        }
        user.password?.let {
            fields.add("password = ?")
            values.add(it)
        }
        if (fields.isEmpty()) return false
        val sql = "UPDATE users SET ${fields.joinToString(", ")} WHERE uid = ?"
        values.add(user.uid)
        return ConnectionPool.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                setParamValues(stmt, values)
                stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(input: Any): Boolean {
        val (sql, paramSetter) = when (input) {
            is Long -> DELETE_USER_BY_ID_SQL to { stmt: PreparedStatement -> stmt.setLong(1, input) }
            is String -> DELETE_USER_BY_USERNAME_SQL to { stmt: PreparedStatement -> stmt.setString(1, input) }
            else -> throw IllegalArgumentException("Input must be either a String or a Long")

        }
        return ConnectionPool.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                paramSetter(stmt)
                stmt.executeUpdate() > 0
            }
        }
    }

    private fun setStatementParams(stmt: PreparedStatement, user: Users) {
        stmt.setString(1, user.username)
        stmt.setLong(2, user.uid)
        stmt.setBoolean(3, user.frozen ?: false)
        stmt.setInt(4, user.remain ?: 3)
        stmt.setInt(5, user.economy ?: 0)
        stmt.setBoolean(6, user.signed ?: false)
        stmt.setInt(7, user.playtime ?: 0)
        stmt.setString(8, user.password)
    }

    private fun mapResultSetToUser(rs: ResultSet): Users {
        return Users(
            username = rs.getString("username"),
            uid = rs.getLong("uid"),
            frozen = rs.getBoolean("frozen"),
            remain = rs.getInt("remain"),
            economy = rs.getInt("economy"),
            signed = rs.getBoolean("signed"),
            playtime = rs.getInt("playtime"),
            password = rs.getString("password")
        )
    }

    private fun setParamValues(stmt: PreparedStatement, values: MutableList<Any?>) {
        for ((index, value) in values.withIndex()) {
            when (value) {
                is String -> stmt.setString(index + 1, value)
                is Boolean -> stmt.setBoolean(index + 1, value)
                is Int -> stmt.setInt(index + 1, value)
                is Long -> stmt.setLong(index + 1, value)
                else -> throw IllegalArgumentException("Unsupported data type")
            }
        }
    }
}