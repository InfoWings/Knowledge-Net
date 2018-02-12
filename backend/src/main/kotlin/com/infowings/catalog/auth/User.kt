package com.infowings.catalog.auth

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.infowings.common.UserRole
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.sql.executor.OResult

val admin = UserEntity("admin", "admin", UserRole.ADMIN)
val user = UserEntity("user", "user", UserRole.USER)
val poweredUser = UserEntity("powereduser", "powereduser", UserRole.POWERED_USER)

data class UserEntity(var username: String,
                      var password: String,
                      var role: UserRole) {
    companion object {
        fun fromOrient(element: OElement): UserEntity {
            return UserEntity(
                    element.getProperty("username"),
                    element.getProperty("password"),
                    UserRole.valueOf(element.getProperty("role")))
        }
    }
}

class UserAcceptService(var database: OrientDatabase) {
    fun findByUsername(username: String): UserEntity? {
        transaction(database) {
            val query = "SELECT * from User where username = ?"
            it.query(query, username).use {
                if (it.hasNext()) {
                    val row: OResult = it.next()
                    return UserEntity.fromOrient(row.toElement())
                }
            }
        }
        return null
    }
}