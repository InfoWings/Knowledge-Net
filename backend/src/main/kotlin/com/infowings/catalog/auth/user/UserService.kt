package com.infowings.catalog.auth.user

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import org.springframework.security.core.userdetails.UsernameNotFoundException

class UserService(private val db: OrientDatabase, private val dao: UserDao) {

    fun createUser(user: User) = transaction(db) {
        val userVertex = dao.createUserVertex()
        userVertex.username = user.username
        userVertex.password = user.password
        userVertex.role = user.role.name
        return@transaction dao.saveUserVertex(userVertex)
    }.toUser()

    fun findByUsername(username: String): User {
        val userVertex = findUserVertexByUsername(username)
        return userVertex.toUser()
    }

    fun findUserVertexByUsername(username: String) =
        dao.findByUsername(username) ?: throw UsernameNotFoundException(username)

    fun getAllUsers() = dao.getAllUserVertices().map { it.toUser() }

}
