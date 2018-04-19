package com.infowings.catalog.auth.user

import org.springframework.security.core.userdetails.UsernameNotFoundException

class UserService(private val dao: UserDao) {
    fun createUser(user: User) =
        dao.createUser(user)

    fun findByUsername(username: String): User {
        val userVertex = findUserVertexByUsername(username)
        return userVertex.toUser()
    }

    fun findUserVertexByUsername(username: String) =
        dao.findByUsername(username) ?: throw UsernameNotFoundException(username)
}
