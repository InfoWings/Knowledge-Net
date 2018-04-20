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
        userVertex.blocked = false
        return@transaction dao.saveUserVertex(userVertex)
    }.toUser()

    fun findByUsername(username: String) = findUserVertexByUsername(username).toUser()

    fun findUserVertexByUsername(username: String) =
        dao.findByUsername(username) ?: throw UsernameNotFoundException(username)

    fun getAllUsers() = dao.getAllUserVertices().map { it.toUser() }.toSet()

    fun blockUser(username: String): User = transaction(db) {
        val userVertex = findUserVertexByUsername(username)
        userVertex.blocked = true
        return@transaction dao.saveUserVertex(userVertex)
    }.toUser()
}
