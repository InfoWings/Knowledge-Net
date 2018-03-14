package com.infowings.catalog.auth

import com.infowings.catalog.common.UserRole
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.sql.executor.OResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON

val admin = UserEntity("admin", "admin", UserRole.ADMIN)
val user = UserEntity("user", "user", UserRole.USER)
val poweredUser = UserEntity("powereduser", "powereduser", UserRole.POWERED_USER)

@Serializable
data class UserEntity(var username: String,
                      var password: String,
                      var role: UserRole)

class UserAcceptService(var database: OrientDatabase) {
    fun findByUsername(username: String): UserEntity? = session(database) {
        val query = "SELECT * from User where username = ?"
        return@session it.query(query, username).use {
            return@use if (it.hasNext()) {
                val row: OResult = it.next()
                JSON.nonstrict.parse<UserEntity>(row.toJSON())
            } else null
        }
    }
}
