package com.infowings.catalog.auth

import com.infowings.catalog.common.UserRole
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.sql.executor.OResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import org.springframework.boot.context.properties.ConfigurationProperties

object Users {
    val admin = UserEntity("admin", "admin", UserRole.ADMIN)
    val user = UserEntity("user", "user", UserRole.USER)
    val poweredUser = UserEntity("powereduser", "powereduser", UserRole.POWERED_USER)

    fun toUserEntities() = listOf(admin, user, poweredUser)
}

@Serializable
data class UserEntity(
    var username: String,
    var password: String,
    var role: UserRole
)

@ConfigurationProperties("")
data class UserProperties(var users: Map<String, Map<String, String>> = mutableMapOf()) {
    fun toUserEntities(): List<UserEntity> = users.map {
        val name = it.key
        val data = it.value
        val password = data.get("password")
                ?: throw IllegalArgumentException("Password is not specified for user $name")
        val roleString = data.get("role") ?: throw IllegalArgumentException("Role is not specified for user $name")
        val role = UserRole.valueOf(roleString)
        UserEntity(name, password, role)
    }
}

class UserAcceptService(var database: OrientDatabase) {
    fun findByUsernameAsJson(username: String): String? = session(database) {
        val query = "SELECT * from User where username = ?"
        return@session it.query(query, username).use {
            return@use if (it.hasNext()) {
                val row: OResult = it.next()
                row.toJSON()
            } else null
        }
    }

    fun findByUsername(username: String): UserEntity? =
        findByUsernameAsJson(username)?.let { JSON.nonstrict.parse(it) }
}

class UserNotFoundException(username: String) : Exception("user $username not found")
