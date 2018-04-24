package com.infowings.catalog.auth.user

import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserRole
import org.springframework.boot.context.properties.ConfigurationProperties

object Users {
    private val admin = User("admin", "admin", UserRole.ADMIN)
    private val user = User("user", "user", UserRole.USER)
    private val poweredUser = User("powereduser", "powereduser", UserRole.POWERED_USER)

    fun toList() = listOf(
        admin,
        user,
        poweredUser
    )
}

@ConfigurationProperties("")
data class UserProperties(var users: Map<String, Map<String, String>> = mutableMapOf()) {
    fun toUsers(): List<User> = users.map {
        val name = it.key
        val data = it.value
        val password = data["password"] ?: throw IllegalArgumentException("Password is not specified for user $name")
        val roleString = data["role"] ?: throw IllegalArgumentException("Role is not specified for user $name")
        val role = UserRole.valueOf(roleString)
        User(name, password, role)
    }
}
