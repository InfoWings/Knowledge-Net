package com.infowings.catalog.auth.user

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.USER_CLASS
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertex
import com.orientechnologies.orient.core.record.OVertex

const val selectUserByName = "SELECT * from User where username = ?"

class UserDao(private val db: OrientDatabase) {

    fun createUser(user: User) = session(db) {
        val userVertex = createUserVertex()
        userVertex.username = user.username
        userVertex.password = user.password
        userVertex.role = user.role.name
        userVertex.save<OVertex>()
        return@session
    }

    private fun createUserVertex() = db.createNewVertex(USER_CLASS).toUserVertex()

    fun findByUsername(username: String): UserVertex? = session(db) {
        return@session db.query(selectUserByName, username) { rs ->
            rs.map { it.toVertex().toUserVertex() }.firstOrNull()
        }
    }
}