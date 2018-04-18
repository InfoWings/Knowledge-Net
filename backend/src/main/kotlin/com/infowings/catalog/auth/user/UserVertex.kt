package com.infowings.catalog.auth.user

import com.infowings.catalog.common.UserRole
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toUserVertex() = UserVertex(this)

data class UserVertex(private val vertex: OVertex) : OVertex by vertex {
    var username: String
        get() = this["username"]
        set(value) {
            this["username"] = value
        }

    var password: String
        get() = this["password"]
        set(value) {
            this["password"] = value
        }

    var role: String
        get() = this["role"]
        set(value) {
            this["role"] = value
        }

    fun toUserEntity(): UserEntity = UserEntity(username, password, UserRole.valueOf(role))
}