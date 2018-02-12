package com.infowings.catalog.auth

import com.infowings.common.UserRole
import org.springframework.stereotype.Service

val admin = UserEntity(0, "admin", "admin", UserRole.ADMIN)
val user = UserEntity(1, "user", "user", UserRole.USER)
val poweredUser = UserEntity(2, "powereduser", "powereduser", UserRole.POWERED_USER)

data class UserEntity(var id: Long,
                      var username: String,
                      var password: String,
                      var role: UserRole)

@Service
class UserRepository {
    fun findByUsername(username: String): UserEntity? {
        return when (username) {
            admin.username -> admin
            user.username -> user
            poweredUser.username -> poweredUser
            else -> throw IllegalStateException("There are no such user")
        }
    }
}