package com.infowings.catalog.auth

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.infowings.catalog.UserRole
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
    fun findByUsername(username: String): UserEntity? {
        transaction(database) {
            val query = "SELECT * from User where username = ?"
            it.query(query, username).use {
                if (it.hasNext()) {
                    val row: OResult = it.next()
                    return JSON.nonstrict.parse(row.toJSON())
                }
            }
        }
        return null
    }
}