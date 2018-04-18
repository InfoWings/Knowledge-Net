package com.infowings.catalog.auth.user

import com.infowings.catalog.common.UserRole
import kotlinx.serialization.Serializable
import org.springframework.boot.context.properties.ConfigurationProperties

object Users {
    private val admin = UserEntity("admin", "admin", UserRole.ADMIN)
    private val user = UserEntity("user", "user", UserRole.USER)
    private val poweredUser = UserEntity("powereduser", "powereduser", UserRole.POWERED_USER)

    fun toUserEntities() = listOf(
        admin,
        user,
        poweredUser
    )
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
        val password = data["password"] ?: throw IllegalArgumentException("Password is not specified for user $name")
        val roleString = data["role"] ?: throw IllegalArgumentException("Role is not specified for user $name")
        val role = UserRole.valueOf(roleString)
        UserEntity(name, password, role)
    }
}
