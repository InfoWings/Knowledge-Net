package com.infowings.catalog.auth.user

class UserService(private val dao: UserDao) {
    fun createUser(user: User) =
        dao.createUser(user)

    fun findByUsername(username: String): User {
        val userVertex = findUserVertexByUsername(username)
        return userVertex.toUser()
    }

    fun findUserVertexByUsername(username: String) =
        dao.findByUsername(username) ?: throw UserNotFoundException(username)
}

sealed class UserException(message: String? = null) : Exception(message)

class UserNotFoundException(val username: String) : UserException("username: $username")