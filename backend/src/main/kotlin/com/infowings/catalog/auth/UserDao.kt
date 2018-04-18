package com.infowings.catalog.auth

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.USER_CLASS
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex

class UserDao(private val db: OrientDatabase) {

    fun saveUser(user: UserEntity): UserVertex = session(db) {
        val userVertex = createUserVertex()
        userVertex.username = user.username
        userVertex.password = user.password
        userVertex.role = user.role.name
        return@session userVertex.save<OVertex>().toUserVertex()
    }

    private fun createUserVertex() = db.createNewVertex(USER_CLASS).toUserVertex()
}