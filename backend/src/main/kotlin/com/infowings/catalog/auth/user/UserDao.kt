package com.infowings.catalog.auth.user

import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

const val findByUsername = "SELECT * FROM $USER_CLASS WHERE username = ?"
const val selectAll = "SELECT * FROM $USER_CLASS"

class UserDao(private val db: OrientDatabase) {

    fun createUserVertex() = transaction(db) {
        db.createNewVertex(USER_CLASS).toUserVertex()
    }

    fun saveUserVertex(userVertex: UserVertex) = session(db) {
        return@session userVertex.save<OVertex>().toUserVertex()
    }

    fun findByUsername(username: String): UserVertex? = session(db) {
        return@session db.query(findByUsername, username) { rs ->
            rs.map { it.toVertex().toUserVertex() }.firstOrNull()
        }
    }

    fun getAllUserVertices() = session(db) {
        return@session db.query(selectAll) { rs ->
            rs.map { it.toVertex().toUserVertex() }
        }.toSet()
    }
}